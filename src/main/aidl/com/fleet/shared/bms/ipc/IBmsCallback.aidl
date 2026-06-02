package com.fleet.shared.bms.ipc;

import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot;

interface IBmsCallback {
    void onStateChanged(in ParcelableBatterySnapshot snapshot);
    void onConnectionStatusChanged(int statusCode);
}
