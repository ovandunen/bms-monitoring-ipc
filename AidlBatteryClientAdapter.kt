package com.fleet.shared.bms.ipc.infrastructure

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.core.content.ContextCompat
import com.fleet.shared.bms.ipc.IBmsCallback
import com.fleet.shared.bms.ipc.IBmsService
import com.fleet.shared.bms.ipc.ParcelableBmsCommand
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
 *  - Single Responsibility: Android [ServiceConnection] lifecycle, bind/reconnect, and callback bridge.
 *  - Liskov Substitution: usable wherever [BatteryQueryPort] is required; flows expose reactive state.
 *  - Dependency Inversion: maps IPC DTOs to domain via [BatterySnapshotMapper] / [VehicleLocationMapper]
 *    before exposing to callers.
 */
class AidlBatteryClientAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
) : BatteryQueryPort {

    private var service: IBmsService? = null
    private var isBound = false
    private var reconnectJob: Job? = null
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS

    private val _batterySnapshot = MutableStateFlow<BatterySnapshot?>(null)
    val batterySnapshot: StateFlow<BatterySnapshot?> = _batterySnapshot.asStateFlow()

    private val _vehicleLocation = MutableStateFlow<VehicleLocation?>(null)
    val vehicleLocation: StateFlow<VehicleLocation?> = _vehicleLocation.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val callback = object : IBmsCallback.Stub() {
        override fun onStateChanged(snapshot: com.fleet.shared.bms.ipc.ParcelableBatterySnapshot) {
            _batterySnapshot.value = BatterySnapshotMapper.toDomain(snapshot)
        }

        override fun onLocationChanged(location: com.fleet.shared.bms.ipc.ParcelableVehicleLocation) {
            _vehicleLocation.value = VehicleLocationMapper.toDomain(location)
        }

        override fun onConnectionStatusChanged(statusCode: Int) {
            _connectionStatus.value = ConnectionStatusMapper.toConnectionStatus(statusCode)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
            val bound = IBmsService.Stub.asInterface(binder)
            service = bound
            try {
                bound.registerCallback(callback)
                _connectionStatus.value = ConnectionStatus.Connected
                refreshFromServiceOnConnect(bound)
            } catch (e: RemoteException) {
                _connectionStatus.value = ConnectionStatus.Error("registerCallback failed: ${e.message}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            scheduleReconnect()
        }

        override fun onBindingDied(name: ComponentName?) {
            service = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            scheduleReconnect()
        }
    }

    /** Binds to the BMS monitoring service. Safe to call multiple times; no-op if already bound. */
    fun bind() {
        if (isBound) return
        _connectionStatus.value = ConnectionStatus.Connecting
        // TODO: replace with the actual bound-service package/class for this deployment.
        val intent = Intent().apply {
            component = ComponentName(BMS_SERVICE_PACKAGE, BMS_SERVICE_CLASS)
        }
        isBound = ContextCompat.getSystemService(context, Context::class.java) != null &&
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!isBound) {
            _connectionStatus.value = ConnectionStatus.Error("bindService failed")
            scheduleReconnect()
        }
    }

    /** Unbinds and cancels any pending reconnect attempts. Call from onDestroy/lifecycle teardown. */
    fun unbind() {
        reconnectJob?.cancel()
        reconnectJob = null
        val bound = service
        if (bound != null) {
            try {
                bound.unregisterCallback(callback)
            } catch (e: RemoteException) {
                // Service already gone; nothing to clean up on the remote side.
            }
        }
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        service = null
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    fun sendCommand(command: BmsCommand) {
        val bound = service ?: return
        try {
            bound.sendCommand(BmsCommandMapper.toParcelable(command))
        } catch (e: RemoteException) {
            _connectionStatus.value = ConnectionStatus.Error("sendCommand failed: ${e.message}")
        }
    }

    override fun getCurrentSnapshot(): BatterySnapshot? {
        val bound = service ?: return _batterySnapshot.value
        return try {
            BatterySnapshotMapper.toDomain(bound.currentSnapshot)
        } catch (e: RemoteException) {
            _batterySnapshot.value
        }
    }

    fun getCurrentLocation(): VehicleLocation? {
        val bound = service ?: return _vehicleLocation.value
        return try {
            VehicleLocationMapper.toDomain(bound.currentLocation)
        } catch (e: RemoteException) {
            _vehicleLocation.value
        }
    }

    private fun refreshFromServiceOnConnect(bound: IBmsService) {
        scope.launch {
            try {
                _batterySnapshot.value = BatterySnapshotMapper.toDomain(bound.currentSnapshot)
                _vehicleLocation.value = VehicleLocationMapper.toDomain(bound.currentLocation)
            } catch (e: RemoteException) {
                // Callback broadcasts will eventually bring state current; not fatal here.
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS)
            bind()
        }
    }

    companion object {
        private const val BMS_SERVICE_PACKAGE = "ch.ecocar.bms" // TODO: confirm actual service package
        private const val BMS_SERVICE_CLASS = "ch.ecocar.bms.BmsMonitoringService" // TODO: confirm actual service class
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }
}
