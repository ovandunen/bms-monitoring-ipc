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
 * - Dependency Inversion: maps IPC DTOs to domain via [BatterySnapshotMapper] before exposing to callers.
 */
class AidlBatteryClientAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
) : BatteryQueryPort {

    private val _batteryState = MutableStateFlow<BatterySnapshot?>(null)
    val batteryState: StateFlow<BatterySnapshot?> = _batteryState.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    @Volatile
    private var service: IBmsService? = null

    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var bound = false

    private val serviceIntent: Intent
        get() = Intent(BMS_MONITOR_ACTION).setPackage(BMS_PACKAGE)

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IBmsService.Stub.asInterface(binder)
                reconnectAttempt = 0
                bound = true
                _connectionStatus.value = ConnectionStatus.Connected
                registerCallback()
                refreshSnapshotFromService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                bound = false
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
            }

            override fun onConnectionStatusChanged(statusCode: Int) {
                val lastSeen = _batteryState.value?.timestamp
                _connectionStatus.value =
                    ConnectionStatusMapper.toConnectionStatus(statusCode, lastSeen)
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
        if (bound) {
            try {
                service?.unregisterCallback(callback)
            } catch (_: RemoteException) {
            }
            context.unbindService(connection)
            bound = false
        }
        service = null
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    fun sendCommand(command: BmsCommand) {
        val remote = service ?: return
        try {
            remote.sendCommand(BmsCommandMapper.toParcelable(command))
        } catch (_: RemoteException) {
            _connectionStatus.value = ConnectionStatus.Error("sendCommand failed: service unreachable")
        }
    }

    override fun getCurrentSnapshot(): BatterySnapshot? = _batteryState.value

    private suspend fun attemptBind(wakeBmsIfNeeded: Boolean): Boolean {
        val boundNow = context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        if (!boundNow && wakeBmsIfNeeded) {
            ContextCompat.startForegroundService(context, serviceIntent)
            delay(BIND_RETRY_DELAY_MS)
            return attemptBind(wakeBmsIfNeeded = false)
        }
        if (boundNow) {
            delay(CONNECTION_SETTLE_MS)
            return service != null && bound
        }
        return false
    }

    private fun registerCallback() {
        try {
            service?.registerCallback(callback)
        } catch (_: RemoteException) {
            _connectionStatus.value = ConnectionStatus.Error("registerCallback failed")
        }
    }

    private fun refreshSnapshotFromService() {
        try {
            val parcel = service?.currentSnapshot ?: return
            if (parcel.timestamp == 0L) return
            _batteryState.value = BatterySnapshotMapper.toDomain(parcel)
        } catch (_: RemoteException) {
            // Ignore; stream callbacks may still deliver updates.
        }
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
        const val BMS_MONITOR_ACTION = "com.fleet.bms.action.MONITOR_SERVICE"
        const val BMS_PACKAGE = "com.fleet.bms"

        private const val BIND_RETRY_DELAY_MS = 500L
        private const val CONNECTION_SETTLE_MS = 300L
        private const val INITIAL_BACKOFF_MS = 2_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }
}
