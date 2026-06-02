package com.fleet.shared.bms.ipc.application.ports

import com.fleet.shared.bms.ipc.domain.BatterySnapshot

/**
 * Synchronous read port for the latest cached snapshot.
 */
interface BatteryQueryPort {
    fun getCurrentSnapshot(): BatterySnapshot?
}
