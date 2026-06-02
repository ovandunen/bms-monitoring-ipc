package com.fleet.shared.bms.ipc.domain

enum class CommandType {
    START_MONITORING,
    STOP_MONITORING,
    SET_DEMO_MODE,
    REQUEST_CALIBRATION,
}

data class BmsCommand(
    val commandId: String,
    val type: CommandType,
    val payload: Map<String, String> = emptyMap(),
)
