package com.fleet.shared.bms.ipc.application.ports

import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.ConnectionStatus

/**
 * Driving port — implemented by the EcoCar GUI client.
 */
interface BatteryTelemetryListenerPort {
    fun onStateChanged(snapshot: BatterySnapshot)

    fun onConnectionStatusChanged(status: ConnectionStatus)
}
