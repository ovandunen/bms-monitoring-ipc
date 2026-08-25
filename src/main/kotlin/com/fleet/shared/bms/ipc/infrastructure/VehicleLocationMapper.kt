package com.fleet.shared.bms.ipc.infrastructure

import com.fleet.shared.bms.ipc.ParcelableVehicleLocation
import com.fleet.shared.bms.ipc.domain.LocationSource
import com.fleet.shared.bms.ipc.domain.VehicleLocation


object VehicleLocationMapper {
    fun toParcelable(location: VehicleLocation): ParcelableVehicleLocation =
        ParcelableVehicleLocation(
            altitude = location.altitude,
            speed = location.speed,
            isMock = location.isMock,
            timestamp = location.timestamp,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracyMeters,
            speedMetersPerSecond = location.speedMetersPerSecond ?: 0f,
            hasSpeed = location.speedMetersPerSecond != null,
            bearingDegrees = location.bearingDegrees ?: 0f,
            hasBearing = location.bearingDegrees != null,
            sourceName = location.source.name
        )

    fun toDomain(parcel: ParcelableVehicleLocation): VehicleLocation =
        VehicleLocation(
            altitude = parcel.altitude,
            speed = parcel.speed,
            isMock = parcel.isMock,
            timestamp = parcel.timestamp,
            latitude = parcel.latitude,
            longitude = parcel.longitude,
            accuracyMeters = parcel.accuracyMeters,
            speedMetersPerSecond = parcel.speedMetersPerSecond.takeIf { parcel.hasSpeed },
            bearingDegrees = parcel.bearingDegrees.takeIf { parcel.hasBearing },
            source = LocationSource.valueOf(parcel.sourceName),
        )
}