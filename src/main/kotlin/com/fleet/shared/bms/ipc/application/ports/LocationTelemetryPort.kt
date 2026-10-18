package com.fleet.shared.bms.ipc.application.ports

import com.fleet.shared.bms.ipc.domain.VehicleLocation

/**
 * Driven port — implemented by the BMS monitoring service (server side).
 */
interface LocationTelemetryPort {
    fun publishLocation(location: VehicleLocation)
}
