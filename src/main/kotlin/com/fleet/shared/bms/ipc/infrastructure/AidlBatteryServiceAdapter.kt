package com.fleet.shared.bms.ipc.infrastructure

import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.fleet.shared.bms.ipc.IBmsCallback
import com.fleet.shared.bms.ipc.IBmsService
import com.fleet.shared.bms.ipc.IpcContract
import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot
import com.fleet.shared.bms.ipc.ParcelableBmsCommand
import com.fleet.shared.bms.ipc.ParcelableTripSession
import com.fleet.shared.bms.ipc.ParcelableVehicleLocation
import com.fleet.shared.bms.ipc.domain.TripSession
import com.fleet.shared.bms.ipc.application.ports.BatteryTelemetryPort
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.BmsCommand
import com.fleet.shared.bms.ipc.domain.ConnectionStatus
import com.fleet.shared.bms.ipc.domain.LocationSource
import com.fleet.shared.bms.ipc.domain.VehicleLocation
import kotlin.Double
import kotlin.Float

/**
 * Server-side IPC adapter.
 *
 *  - Single Responsibility: [IBmsService.Stub] IPC threading and callback fan-out only.
 *  - Interface Segregation: implements [BatteryTelemetryPort] (write + command ingress).
 *  - Dependency Inversion: depends on application port contract, maps to domain at boundaries.
 *
 * Liskov: consumers bind to [IBmsService]; this type is the canonical server implementation.
 */
class AidlBatteryServiceAdapter : IBmsService.Stub(), BatteryTelemetryPort {

    /** Return from [android.app.Service.onBind] - this stub is the [IBmsService] binder.  */
    val binder: IBinder get() = this

    private val callbacks = RemoteCallbackList<IBmsCallback>()
    private var latestSnapshot: BatterySnapshot? = null
    private var latestLocation: VehicleLocation? = null
    private var commandHandler: ((BmsCommand) -> Unit)? = null
    private var tripResetHandler: (() -> Unit)? = null
    private var tripSessionsQuery: ((Int) -> List<TripSession>)? = null

    fun registerTripResetHandler(handler: () -> Unit) {
        tripResetHandler = handler
    }

    fun registerTripSessionsQuery(query: (Int) -> List<TripSession>) {
        tripSessionsQuery = query
    }

    override fun publishState(snapshot: BatterySnapshot) {
        latestSnapshot = snapshot
        val parcelable = BatterySnapshotMapper.toParcelable(snapshot)
        broadcastState(parcelable)
    }

    /** Publishes the latest resolved vehicle location per the USB GPS > Traccar > Android system priority policy. */
    fun publishLocation(location: VehicleLocation) {
        latestLocation = location
        val parcelable = VehicleLocationMapper.toParcelable(location)
        broadcastLocation(parcelable)
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

    override fun getCurrentLocation(): ParcelableVehicleLocation {
        val location = latestLocation ?: return EMPTY_LOCATION
        return VehicleLocationMapper.toParcelable(location)
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

    override fun resetTrip() {
        Log.i(TAG, "BmsMonitorService: resetTrip() called via AIDL")
        tripResetHandler?.invoke()
    }

    override fun getTripSessions(limit: Int): List<ParcelableTripSession> {
        val capped = limit.coerceIn(0, 100)
        return tripSessionsQuery?.invoke(capped).orEmpty().map(TripSessionMapper::toParcelable)
    }

    override fun getIpcVersion(): Int = IpcContract.IPC_VERSION

    companion object {
        private const val TAG = "BmsMonitor"
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

        // Sentinel "no fix yet" location. Uses ANDROID_SYSTEM (lowest priority in the
        // USB GPS > Traccar > Android system policy) so it never masks a real fix if
        // callers compare sources before trusting the value.
        private val EMPTY_LOCATION =
            VehicleLocationMapper.toParcelable(
                VehicleLocation(
                    altitude = 0.0,
                    speed = 0.0f,
                    isMock = false,
                    timestamp = 0L,
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracyMeters = 0f,
                    speedMetersPerSecond = null,
                    bearingDegrees = null,
                    source = LocationSource.ANDROID_SYSTEM,
                ),
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

    private fun broadcastLocation(parcelable: ParcelableVehicleLocation) {
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                try {
                    callbacks.getBroadcastItem(i).onLocationChanged(parcelable)
                } catch (e: RemoteException) {
                    // Client process gone; ignore per callback.
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }
}
