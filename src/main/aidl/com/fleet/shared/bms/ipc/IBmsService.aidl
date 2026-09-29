package com.fleet.shared.bms.ipc;

import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot;
import com.fleet.shared.bms.ipc.ParcelableVehicleLocation; // new import
import com.fleet.shared.bms.ipc.IBmsCallback;
import com.fleet.shared.bms.ipc.ParcelableBmsCommand;
import com.fleet.shared.bms.ipc.ParcelableTripSession;

interface IBmsService {
    ParcelableBatterySnapshot getCurrentSnapshot();
    void registerCallback(IBmsCallback callback);
    void unregisterCallback(IBmsCallback callback);
    void sendCommand(in ParcelableBmsCommand command);
    
    // new method
    ParcelableVehicleLocation getCurrentLocation();

    void resetTrip();
    List<ParcelableTripSession> getTripSessions(int limit);
    int getIpcVersion();
}
