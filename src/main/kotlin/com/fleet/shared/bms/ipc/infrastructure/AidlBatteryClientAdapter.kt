package com.fleet.shared.bms.ipc.infrastructure

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.core.content.ContextCompat
import com.fleet.shared.bms.ipc.*
import com.fleet.shared.bms.ipc.application.ports.BatteryQueryPort
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.VehicleLocation  // new import
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines Job
// … rest of the class definition …

private val  _vehicleLocation = MutableStateFlow<VehicleLocation?>(null)
val vehicleLocation: StateFlow<VehicleLocation?>  = _vehicleLocation.asStateFlow()

// … rest of the class definition …

inner class IBmsCallback : Binder(), IBmsCallback {
    // … rest of the inner class definition …
    
    override fun onLocationChanged(location: ParcelableVehicleLocation) {
        val domain = VehicleLocationMapper.toDomain(location)
        _vehicleLocation.value = domain
    }
}

private suspend fun refreshSnapshotFromService() {
    // … rest of the method definition …
    
    service?.let {
        try {
            val snapshot = it.currentSnapshot
            _batterySnapshot.value = BatterySnapshotMapper.toDomain(snapshot)
            
            // new code to fetch and update location
            val location = it.currentLocation
            if (location != null && VehicleLocationMapper.isValid(location)) {
                _vehicleLocation.value = VehicleLocationMapper.toDomain(location)
            }
        } catch (e: RemoteException) {
            // handle exception
        }
    }
}
