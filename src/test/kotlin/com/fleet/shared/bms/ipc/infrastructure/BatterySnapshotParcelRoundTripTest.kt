package com.fleet.shared.bms.ipc.infrastructure

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import com.fleet.shared.bms.ipc.IBmsService
import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot
import com.fleet.shared.bms.ipc.ParcelableTripSession
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class BatterySnapshotParcelRoundTripTest {

    @Test
    fun parcelRoundTrip_preservesAllFieldsIncludingNewOnes() {
        val original = BatterySnapshot(
            timestamp = 1_700_000_000_123L,
            stateOfChargePercent = 77.5f,
            totalVoltage = 351.2f,
            current = -18.4f,
            cellVoltageMax = 4120,
            cellVoltageMin = 3975,
            batteryTempMax = 33,
            batteryTempMin = 27,
            controllerTemp = 41,
            motorTemp = 48,
            motorRpm = 2100,
            vehicleSpeed = 42.5f,
            faultCodes = listOf("P0A1F", "U0100"),
            estimatedRangeKm = 188.25f,
            tripDistanceKm = 12.5f,
            co2SavingKg = 1.125f,
            batteryTempAvg = 29.4f,
            vehicleStatus = 1,
            batteryDataStale = true,
            cloudConnected = false,
        )
        val parcelable = BatterySnapshotMapper.toParcelable(original)
        val parcel = Parcel.obtain()
        try {
            parcelable.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            @Suppress("UNCHECKED_CAST")
            val creator = ParcelableBatterySnapshot::class.java.getField("CREATOR")
                .get(null) as Parcelable.Creator<ParcelableBatterySnapshot>
            val restored = creator.createFromParcel(parcel)
            assertEquals(original, BatterySnapshotMapper.toDomain(restored))
        } finally {
            parcel.recycle()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class TripSessionParcelRoundTripTest {

    @Test
    fun parcelRoundTrip_preservesFields_includingOpenEndedAt() {
        val original = com.fleet.shared.bms.ipc.domain.TripSession(
            id = "sess-1",
            startedAt = 100L,
            endedAt = com.fleet.shared.bms.ipc.domain.TripSession.OPEN_ENDED_AT,
            distanceKm = 12.5f,
            energyKwh = 3.25f,
        )
        val parcelable = TripSessionMapper.toParcelable(original)
        val parcel = Parcel.obtain()
        try {
            parcelable.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            @Suppress("UNCHECKED_CAST")
            val creator = ParcelableTripSession::class.java.getField("CREATOR")
                .get(null) as Parcelable.Creator<ParcelableTripSession>
            val restored = creator.createFromParcel(parcel)
            assertEquals(original, TripSessionMapper.toDomain(restored))
        } finally {
            parcel.recycle()
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class IBmsServiceAidlContractTest {

    @Test
    fun resetTrip_isLastTransaction_getCurrentLocationUnchanged() {
        val first = IBinder.FIRST_CALL_TRANSACTION
        assertEquals(first + 0, transaction("getCurrentSnapshot"))
        assertEquals(first + 1, transaction("registerCallback"))
        assertEquals(first + 2, transaction("unregisterCallback"))
        assertEquals(first + 3, transaction("sendCommand"))
        assertEquals(first + 4, transaction("getCurrentLocation"))
        assertEquals(first + 5, transaction("resetTrip"))
        assertEquals(first + 6, transaction("getTripSessions"))
        assertEquals(first + 7, transaction("getIpcVersion"))
        val reset = transaction("resetTrip")
        val others = listOf(
            "getCurrentSnapshot",
            "registerCallback",
            "unregisterCallback",
            "sendCommand",
            "getCurrentLocation",
        ).map { transaction(it) }
        assertTrue(others.all { it < reset })
        assertTrue(transaction("getTripSessions") > reset)
        assertTrue(transaction("getIpcVersion") > transaction("getTripSessions"))
    }

    private fun transaction(method: String): Int {
        val field = IBmsService.Stub::class.java.getDeclaredField("TRANSACTION_$method")
        field.isAccessible = true
        return field.getInt(null)
    }
}
