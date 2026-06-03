package com.fleet.shared.bms.ipc.infrastructure

import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot
import com.fleet.shared.bms.ipc.ParcelableBmsCommand
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.BmsCommand
import com.fleet.shared.bms.ipc.domain.CommandType
import com.fleet.shared.bms.ipc.domain.ConnectionStatus

/**
 * Single Responsibility: maps between domain models and IPC parcelables only.
 * No Android framework imports — pure Kotlin mapping logic.
 */
object BatterySnapshotMapper {
    fun toParcelable(domain: BatterySnapshot): ParcelableBatterySnapshot =
        ParcelableBatterySnapshot(
            timestamp = domain.timestamp,
            stateOfChargePercent = domain.stateOfChargePercent,
            totalVoltage = domain.totalVoltage,
            current = domain.current,
            cellVoltageMax = domain.cellVoltageMax,
            cellVoltageMin = domain.cellVoltageMin,
            batteryTempMax = domain.batteryTempMax,
            batteryTempMin = domain.batteryTempMin,
            controllerTemp = domain.controllerTemp,
            motorTemp = domain.motorTemp,
            motorRpm = domain.motorRpm,
            vehicleSpeed = domain.vehicleSpeed,
            faultCodes = domain.faultCodes,
        )

    fun toDomain(parcel: ParcelableBatterySnapshot): BatterySnapshot =
        BatterySnapshot(
            timestamp = parcel.timestamp,
            stateOfChargePercent = parcel.stateOfChargePercent,
            totalVoltage = parcel.totalVoltage,
            current = parcel.current,
            cellVoltageMax = parcel.cellVoltageMax,
            cellVoltageMin = parcel.cellVoltageMin,
            batteryTempMax = parcel.batteryTempMax,
            batteryTempMin = parcel.batteryTempMin,
            controllerTemp = parcel.controllerTemp,
            motorTemp = parcel.motorTemp,
            motorRpm = parcel.motorRpm,
            vehicleSpeed = parcel.vehicleSpeed,
            faultCodes = parcel.faultCodes,
        )
}

object BmsCommandMapper {
    fun toParcelable(domain: BmsCommand): ParcelableBmsCommand =
        ParcelableBmsCommand(
            commandId = domain.commandId,
            typeName = domain.type.name,
            payload = domain.payload,
        )

    fun toDomain(parcel: ParcelableBmsCommand): BmsCommand =
        BmsCommand(
            commandId = parcel.commandId,
            type = CommandType.valueOf(parcel.typeName),
            payload = parcel.payload,
        )
}

object ConnectionStatusMapper {
    const val CODE_CONNECTED = 0
    const val CODE_CONNECTING = 1
    const val CODE_DISCONNECTED = 2
    const val CODE_BMS_OFFLINE = 3
    const val CODE_ERROR = 4

    fun toStatusCode(status: ConnectionStatus): Int =
        when (status) {
            ConnectionStatus.Connected -> CODE_CONNECTED
            ConnectionStatus.Connecting -> CODE_CONNECTING
            ConnectionStatus.Disconnected -> CODE_DISCONNECTED
            is ConnectionStatus.BmsOffline -> CODE_BMS_OFFLINE
            is ConnectionStatus.Error -> CODE_ERROR
        }

    fun toConnectionStatus(code: Int, lastSeenTimestamp: Long? = null): ConnectionStatus =
        when (code) {
            CODE_CONNECTED -> ConnectionStatus.Connected
            CODE_CONNECTING -> ConnectionStatus.Connecting
            CODE_DISCONNECTED -> ConnectionStatus.Disconnected
            CODE_BMS_OFFLINE -> ConnectionStatus.BmsOffline(lastSeenTimestamp)
            CODE_ERROR -> ConnectionStatus.Error("IPC status code $code")
            else -> ConnectionStatus.Error("Unknown status code $code")
        }
}
