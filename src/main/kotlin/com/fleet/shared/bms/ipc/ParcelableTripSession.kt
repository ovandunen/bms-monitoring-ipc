package com.fleet.shared.bms.ipc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelableTripSession(
    val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val distanceKm: Float,
    val energyKwh: Float,
) : Parcelable
