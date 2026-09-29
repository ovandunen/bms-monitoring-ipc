package com.fleet.shared.bms.ipc.domain

data class TripSession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val distanceKm: Float,
    val energyKwh: Float,
) {
    companion object {
        const val OPEN_ENDED_AT = -1L
    }
}
