package com.fleet.shared.bms.ipc.infrastructure

import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import com.fleet.shared.bms.ipc.*
import com.fleet.shared.bms.ipc.application.ports.BatteryTelemetryPort
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.BmsCommand
import com.fleet.shared.bms.ipc.domain.ConnectionStatus
import com.fleet.shared.bms.ipc.domain.VehicleLocation // new import

// ... rest of the class definition ...

override fun publishState(snapshot: BatterySnapshot)  {
    latestSnapshot = snapshot
    val parcelable = BatterySnapshotMapper.toParcelable(snapshot)
    broadcastState(parcelable)
}

fun publishLocation(location: VehicleLocation)  { // new method
    latestLocation = location
    val parcelable = VehicleLocationMapper.toParcelable(location)
    broadcastLocation()
}

override fun getCurrentSnapshot(): ParcelableBatterySnapshot  {
    val snapshot = latestSnapshot ?: return EMPTY_SNAPSHOT
    return BatterySnapshotMapper.toParcelable(snapshot)
}

// new method
override fun getCurrentLocation(): ParcelableVehicleLocation  {
    val location = latestLocation ?: return EMPTY_LOCATION
    return VehicleLocationMapper.toParcelable(location)
}

companion object  {
    private val EMPTY_SNAPSHOT = // ... rest of the code ...
    private val EMPTY_LOCATION = VehicleLocation(0L, 0.0, 0.0, 0.0, 0f, false) // new empty location
}

// ... rest of the class definition ...
