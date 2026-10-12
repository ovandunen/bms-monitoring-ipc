package com.fleet.shared.bms.ipc.infrastructure

import android.os.Parcel
import android.os.Parcelable
import com.fleet.shared.bms.ipc.ParcelableVehicleLocation
import com.fleet.shared.bms.ipc.domain.LocationSource
import com.fleet.shared.bms.ipc.domain.VehicleLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class VehicleLocationParcelRoundTripTest {

    @Test
    fun parcelRoundTrip_withBearing_preservesZeroAsNorth() {
        val restored = roundTrip(sample(bearingDegrees = 0f))
        assertEquals(0f, restored.bearingDegrees)
    }

    @Test
    fun parcelRoundTrip_withBearing_preserves359() {
        val restored = roundTrip(sample(bearingDegrees = 359f))
        assertEquals(359f, restored.bearingDegrees)
    }

    @Test
    fun parcelRoundTrip_withoutBearing_isNullNotZero() {
        val restored = roundTrip(sample(bearingDegrees = null))
        assertNull(restored.bearingDegrees)
    }

    private fun roundTrip(location: VehicleLocation): VehicleLocation {
        val parcelable = VehicleLocationMapper.toParcelable(location)
        val parcel = Parcel.obtain()
        try {
            parcelable.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            @Suppress("UNCHECKED_CAST")
            val creator = ParcelableVehicleLocation::class.java.getField("CREATOR")
                .get(null) as Parcelable.Creator<ParcelableVehicleLocation>
            return VehicleLocationMapper.toDomain(creator.createFromParcel(parcel))
        } finally {
            parcel.recycle()
        }
    }

    private fun sample(bearingDegrees: Float?) = VehicleLocation(
        altitude = 12.0,
        speed = 4f,
        isMock = false,
        timestamp = 1_700_000_000_000L,
        latitude = 52.52,
        longitude = 13.405,
        accuracyMeters = 5f,
        speedMetersPerSecond = 1.1f,
        bearingDegrees = bearingDegrees,
        source = LocationSource.USB_GPS,
    )
}
