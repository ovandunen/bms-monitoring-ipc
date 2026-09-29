package com.fleet.shared.bms.ipc.infrastructure

import com.fleet.shared.bms.ipc.ParcelableTripSession
import com.fleet.shared.bms.ipc.domain.TripSession

object TripSessionMapper {
    fun toParcelable(domain: TripSession): ParcelableTripSession =
        ParcelableTripSession(
            id = domain.id,
            startedAt = domain.startedAt,
            endedAt = domain.endedAt,
            distanceKm = domain.distanceKm,
            energyKwh = domain.energyKwh,
        )

    fun toDomain(parcel: ParcelableTripSession): TripSession =
        TripSession(
            id = parcel.id,
            startedAt = parcel.startedAt,
            endedAt = parcel.endedAt,
            distanceKm = parcel.distanceKm,
            energyKwh = parcel.energyKwh,
        )
}
