package com.fleet.shared.bms.ipc.domain

import java.util.UUID

fun bmsCommand(type: CommandType, payload: Map<String, String> = emptyMap()): BmsCommand =
    BmsCommand(
        commandId = UUID.randomUUID().toString(),
        type = type,
        payload = payload,
    )
