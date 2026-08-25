package com.fleet.shared.bms.ipc.domain

/**
 * Immutable vehicle location fix, resolved according to the source priority policy:
 * USB GPS (priority 0) > Traccar (priority 1) > Android system location (priority 2).
 */
data class VehicleLocation(
    val altitude: Double,
    val speed: Float,
    val isMock: Boolean,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null,
    val source: LocationSource,
)

enum class LocationSource(val priority: Int) {
    USB_GPS(priority = 0),
    TRACCAR(priority = 1),
    ANDROID_SYSTEM(priority = 2),
}