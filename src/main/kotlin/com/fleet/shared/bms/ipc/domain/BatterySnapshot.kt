package com.fleet.shared.bms.ipc.domain

/**
 * Immutable CAN bus telemetry snapshot from the EcoCar VCU specification.
 */
data class BatterySnapshot(
    val timestamp: Long,
    val stateOfChargePercent: Float,
    val totalVoltage: Float,
    val current: Float,
    val cellVoltageMax: Int,
    val cellVoltageMin: Int,
    val batteryTempMax: Int,
    val batteryTempMin: Int,
    val controllerTemp: Int,
    val motorTemp: Int,
    val motorRpm: Int,
    val vehicleSpeed: Float,
    val faultCodes: List<String> = emptyList(),
)
