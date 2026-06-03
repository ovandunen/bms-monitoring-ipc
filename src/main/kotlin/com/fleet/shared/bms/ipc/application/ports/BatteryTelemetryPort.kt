package com.fleet.shared.bms.ipc.application.ports

import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.BmsCommand

/**
 * Driven port — implemented by the BMS monitoring service (server side).
 */
interface BatteryTelemetryPort {
    fun publishState(snapshot: BatterySnapshot)

    fun registerCommandHandler(handler: (BmsCommand) -> Unit)
}
