package com.fleet.shared.bms.ipc;

import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot;
import com.fleet.shared.bms.ipc.ParcelableVehicleLocation;


interface IBmsCallback  {
    void onStateChanged(in ParcelableBatterySnapshot snapshot);
    void onConnectionStatusChanged(int statusCode);
    void onLocationChanged(in ParcelableVehicleLocation location); // new method
}
