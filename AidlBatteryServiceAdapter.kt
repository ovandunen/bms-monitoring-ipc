package com.fleet.shared.bms.ipc.infrastructure

import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import com.fleet.shared.bms.ipc.IBmsCallback
import com.fleet.shared.bms.ipc.IBmsService
import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot
import com.fleet.shared.bms.ipc.ParcelableBmsCommand
import com.fleet.shared.bms.ipc.application.ports.BatteryTelemetryPort
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.BmsCommand
import com.fleet.shared.bms.ipc.domain.ConnectionStatus

/**
   * Server-side IPC adapter.
   *
   *   - Single Responsibility:  [IBmsService.Stub] IPC threading and callback fan-out only.
   *   - Interface Segregation: implements [BatteryTelemetryPort] (write + command ingress).
   *   - Dependency Inversion: depends on application port contract, maps to domain at boundaries.
   *
   * Liskov: consumers bind to  [IBmsService]; this type is the canonical server implementation.
   */
class AidlBatteryServiceAdapter : IBmsService.Stub(), BatteryTelemetryPort {

    /** Return from  [android.app.Service.onBind] - this stub is the [IBmsService] binder. */
    val binder: IBinder get() = this

    private val callbacks = RemoteCallbackList<IBmsCallback>()
    private var latestSnapshot: BatterySnapshot? = null
    private var commandHandler: ((BmsCommand) -> Unit)? = null

    override fun publishState(snapshot: BatterySnapshot) {
        latestSnapshot = snapshot
        val parcelable = BatterySnapshotMapper.toParcelable(snapshot)
        broadcastState(parcelable)
    }

    override fun registerCommandHandler(handler: (BmsCommand) -> Unit) {
        commandHandler = handler
    }

    fun publishConnectionStatus(status: ConnectionStatus) {
        val code = ConnectionStatusMapper.toStatusCode(status)
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                try {
                    callbacks.getBroadcastItem(i).onConnectionStatusChanged(code)
                } catch (e: RemoteException) {
                    // Client died; RemoteCallbackList will prune on next broadcast.
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    override fun getCurrentSnapshot(): ParcelableBatterySnapshot {
        val snapshot = latestSnapshot ?: return EMPTY_SNAPSHOT
        return BatterySnapshotMapper.toParcelable(snapshot)
    }

    override fun registerCallback(callback: IBmsCallback?) {
        if (callback != null) {
            callbacks.register(callback)
        }
    }

    override fun unregisterCallback(callback: IBmsCallback?) {
        if (callback != null) {
            callbacks.unregister(callback)
        }
    }

    override fun sendCommand(command: ParcelableBmsCommand?) {
        if (command == null) return
        val domain = BmsCommandMapper.toDomain(command)
        commandHandler?.invoke(domain)
    }

    companion object {
        private val EMPTY_SNAPSHOT =
            ParcelableBatterySnapshot(
                timestamp = 0L,
                stateOfChargePercent = 0f,
                totalVoltage = 0f,
                current = 0f,
                cellVoltageMax = 0,
                cellVoltageMin = 0,
                batteryTempMax = 0,
                batteryTempMin = 0,
                controllerTemp = 0,
                motorTemp = 0,
                motorRpm = 0,
                vehicleSpeed = 0f,
            )
    }

    private fun broadcastState(parcelable: ParcelableBatterySnapshot) {
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                try {
                    callbacks.getBroadcastItem(i).onStateChanged(parcelable)
                } catch (e: RemoteException) {
                    // Client process gone; ignore per callback.
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }
}
