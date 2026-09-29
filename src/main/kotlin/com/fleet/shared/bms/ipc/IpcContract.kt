package com.fleet.shared.bms.ipc

object IpcContract {
    const val IPC_VERSION = 2
    const val VERSION_MISMATCH_REASON = "IPC_VERSION_MISMATCH"
}

object VehicleStatus {
    const val STANDBY = 0
    const val DRIVING = 1
    const val CHARGING = 2
}
