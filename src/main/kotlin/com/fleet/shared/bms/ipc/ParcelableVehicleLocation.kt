package com.fleet.shared.bms.ipc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class ParcelableVehicleLocation(
    val altitude: Double,
    val speed: Float,
    val isMock: Boolean,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float,
    val hasSpeed: Boolean,
    val bearingDegrees: Float,
    val hasBearing: Boolean,
    val sourceName: String
): Parcelable
