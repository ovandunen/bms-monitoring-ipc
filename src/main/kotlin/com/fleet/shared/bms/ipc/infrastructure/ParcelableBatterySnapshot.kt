package com.fleet.shared.bms.ipc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * IPC parcelable DTO for [com.fleet.shared.bms.ipc.domain.BatterySnapshot].
 * Lives in the AIDL package so generated stubs can reference it.
 *
 * Open/Closed: add optional fields here with defaults before changing AIDL when possible.
 */
@Parcelize
data class ParcelableBatterySnapshot(
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
) : Parcelable
