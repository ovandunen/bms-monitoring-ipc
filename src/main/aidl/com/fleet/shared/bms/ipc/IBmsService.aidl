package com.fleet.shared.bms.ipc;

import com.fleet.shared.bms.ipc.IBmsCallback;
import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot;
import com.fleet.shared.bms.ipc.ParcelableBmsCommand;

interface IBmsService {
    ParcelableBatterySnapshot getCurrentSnapshot();
    void registerCallback(IBmsCallback callback);
    void unregisterCallback(IBmsCallback callback);
    void sendCommand(in ParcelableBmsCommand command);
}
