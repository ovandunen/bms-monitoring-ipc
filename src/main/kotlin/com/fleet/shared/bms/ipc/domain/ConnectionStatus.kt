package com.fleet.shared.bms.ipc.domain

sealed class ConnectionStatus {
    data object Connected : ConnectionStatus()

    data object Connecting : ConnectionStatus()

    data object Disconnected : ConnectionStatus()

    data class BmsOffline(val lastSeenTimestamp: Long?) : ConnectionStatus()

    data class Error(val reason: String) : ConnectionStatus()
}
