package com.fleet.shared.bms.ipc.domain

interface DomainEvent

data class BatteryStateUpdated(val snapshot: BatterySnapshot) : DomainEvent

data class CommandReceived(val command: BmsCommand) : DomainEvent
