package com.fleet.shared.bms.ipc.infrastructure

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.fleet.shared.bms.ipc.IBmsCallback
import com.fleet.shared.bms.ipc.IBmsService
import com.fleet.shared.bms.ipc.application.IpcSnapshotAuditFormatter
import com.fleet.shared.bms.ipc.application.ports.BatteryQueryPort
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.BmsCommand
import com.fleet.shared.bms.ipc.domain.ConnectionStatus
import com.fleet.shared.bms.ipc.domain.VehicleLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Client-side IPC adapter.
 *
 * - Single Responsibility: Android [ServiceConnection] lifecycle, bind/reconnect, and callback bridge.
 * - Liskov Substitution: usable wherever [BatteryQueryPort] is required; flows expose reactive state.
 * - Dependency Inversion: maps IPC DTOs to domain via [BatterySnapshotMapper] / [VehicleLocationMapper]
 *   before exposing to callers.
 */
class AidlBatteryClientAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
) : BatteryQueryPort {

    private val _batteryState = MutableStateFlow<BatterySnapshot?>(null)
    val batteryState: StateFlow<BatterySnapshot?> = _batteryState.asStateFlow()

    private val _vehicleLocation = MutableStateFlow<VehicleLocation?>(null)
    val vehicleLocation: StateFlow<VehicleLocation?> = _vehicleLocation.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    @Volatile
    private var service: IBmsService? = null

    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var bound = false
    private var bindRequested = false

    private val serviceIntent: Intent
        get() = Intent(BMS_MONITOR_ACTION).setPackage(BMS_PACKAGE)

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                Log.i(TAG, IpcSnapshotAuditFormatter.formatServiceConnectedAuditLine(name, binder))
                service = IBmsService.Stub.asInterface(binder)
                reconnectAttempt = 0
                bound = true
                if (!registerCallback()) {
                    return
                }
                _connectionStatus.value = ConnectionStatus.Connected
                refreshSnapshotFromService()
                refreshLocationFromService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                Log.d(TAG, "onServiceDisconnected: $name")
                service = null
                bound = false
                bindRequested = false
                _connectionStatus.value = ConnectionStatus.Connecting
                scheduleReconnect()
            }
        }

    private val callback =
        object : IBmsCallback.Stub() {
            override fun onStateChanged(snapshot: com.fleet.shared.bms.ipc.ParcelableBatterySnapshot?) {
                if (snapshot == null) return
                val domain = BatterySnapshotMapper.toDomain(snapshot)
                _batteryState.value = domain
                Log.i(TAG, IpcSnapshotAuditFormatter.formatStateChangedAuditLine(domain))
            }

            override fun onConnectionStatusChanged(statusCode: Int) {
                val lastSeen = _batteryState.value?.timestamp
                _connectionStatus.value =
                    ConnectionStatusMapper.toConnectionStatus(statusCode, lastSeen)
            }

            override fun onLocationChanged(location: com.fleet.shared.bms.ipc.ParcelableVehicleLocation?) {
                if (location == null) return
                _vehicleLocation.value = VehicleLocationMapper.toDomain(location)
            }
        }

    fun connect() {
        if (bound && service != null) return
        _connectionStatus.value = ConnectionStatus.Connecting
        scope.launch {
            val connected = attemptBind(wakeBmsIfNeeded = true)
            if (!connected) {
                scheduleReconnect()
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
        if (bound || bindRequested) {
            try {
                service?.unregisterCallback(callback)
            } catch (_: Exception) {
            }
            if (bindRequested) {
                context.unbindService(connection)
            }
            bound = false
            bindRequested = false
        }
        service = null
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    fun sendCommand(command: BmsCommand) {
        val remote = service ?: return
        try {
            remote.sendCommand(BmsCommandMapper.toParcelable(command))
        } catch (e: Exception) {
            onFamilyAIpcFailed("sendCommand", e)
        }
    }

    fun resetTrip() {
        try {
            service?.resetTrip()
                ?: Log.w(TAG, "AidlBatteryClient: resetTrip() called but service not bound")
        } catch (e: Exception) {
            onFamilyAIpcFailed("resetTrip", e)
        }
    }

    override fun getCurrentSnapshot(): BatterySnapshot? = _batteryState.value

    fun getCurrentLocation(): VehicleLocation? {
        val boundService = service ?: return _vehicleLocation.value
        return try {
            VehicleLocationMapper.toDomain(boundService.currentLocation)
        } catch (e: Exception) {
            onFamilyAIpcFailed("getCurrentLocation", e)
            _vehicleLocation.value
        }
    }

    private suspend fun attemptBind(wakeBmsIfNeeded: Boolean): Boolean {
        if (bound && service != null) return true

        if (wakeBmsIfNeeded) {
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                Log.w(TAG, "startForegroundService failed", e)
            }
            delay(BIND_RETRY_DELAY_MS)
        }

        if (!bindRequested) {
            bindRequested = context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
            if (!bindRequested) {
                Log.w(TAG, "bindService returned false")
                return false
            }
        }

        delay(CONNECTION_SETTLE_MS)
        return service != null && bound
    }

    private fun registerCallback(): Boolean {
        return try {
            service?.registerCallback(callback)
            true
        } catch (e: Exception) {
            onFamilyAIpcFailed("registerCallback", e)
            false
        }
    }

    private fun refreshSnapshotFromService() {
        try {
            val parcel = service?.currentSnapshot ?: return
            if (parcel.timestamp == 0L) return
            val domain = BatterySnapshotMapper.toDomain(parcel)
            _batteryState.value = domain
            Log.i(TAG, IpcSnapshotAuditFormatter.formatStateChangedAuditLine(domain))
        } catch (e: Exception) {
            onFamilyAIpcFailed("getCurrentSnapshot", e)
        }
    }

    private fun refreshLocationFromService() {
        try {
            val parcel = service?.currentLocation ?: return
            _vehicleLocation.value = VehicleLocationMapper.toDomain(parcel)
        } catch (e: Exception) {
            onFamilyAIpcFailed("getCurrentLocation", e)
        }
    }

    private fun onFamilyAIpcFailed(action: String, error: Exception) {
        Log.w(TAG, "AidlBatteryClient: $action failed (incompatible IBmsService binder)", error)
        _connectionStatus.value = ConnectionStatus.Error("$action failed")
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob =
            scope.launch {
                while (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                    val backoffMs = reconnectBackoffMs(reconnectAttempt)
                    delay(backoffMs)
                    reconnectAttempt++
                    val connected = attemptBind(wakeBmsIfNeeded = reconnectAttempt == 1)
                    if (connected) {
                        reconnectAttempt = 0
                        return@launch
                    }
                }
                val lastSeen = _batteryState.value?.timestamp
                _connectionStatus.value = ConnectionStatus.BmsOffline(lastSeen)
            }
    }

    private fun reconnectBackoffMs(attempt: Int): Long {
        val exponential = INITIAL_BACKOFF_MS * (1L shl attempt.coerceAtMost(4))
        return min(exponential, MAX_BACKOFF_MS)
    }

    companion object {
        private const val TAG = "AidlBatteryClient"
        const val BMS_MONITOR_ACTION = "ch.ecocarsolaire.bms.action.DASHBOARD_SERVICE"
        const val BMS_PACKAGE = "ch.ecocarsolaire.bms"

        private const val BIND_RETRY_DELAY_MS = 500L
        private const val CONNECTION_SETTLE_MS = 300L
        private const val INITIAL_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }
}
