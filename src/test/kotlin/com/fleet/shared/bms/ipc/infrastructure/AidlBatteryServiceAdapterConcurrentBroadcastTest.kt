package com.fleet.shared.bms.ipc.infrastructure

import android.os.RemoteException
import com.fleet.shared.bms.ipc.IBmsCallback
import com.fleet.shared.bms.ipc.ParcelableBatterySnapshot
import com.fleet.shared.bms.ipc.ParcelableVehicleLocation
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import com.fleet.shared.bms.ipc.domain.LocationSource
import com.fleet.shared.bms.ipc.domain.VehicleLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AidlBatteryServiceAdapterConcurrentBroadcastTest {

    @Test
    fun concurrentPublishStateAndLocation_doNotThrow_andDeliverAllCalls() = runBlocking {
        val adapter = AidlBatteryServiceAdapter()
        val stateCalls = AtomicInteger(0)
        val locationCalls = AtomicInteger(0)
        adapter.registerCallback(
            CountingCallback(
                onState = { stateCalls.incrementAndGet() },
                onLocation = { locationCalls.incrementAndGet() },
            ),
        )

        val errors = CopyOnWriteArrayList<Throwable>()
        val stateJob = async(Dispatchers.Default) {
            repeat(1_000) {
                runCatching { adapter.publishState(sampleSnapshot()) }.onFailure { errors += it }
            }
        }
        val locationJob = async(Dispatchers.IO) {
            repeat(1_000) {
                runCatching { adapter.publishLocation(sampleLocation()) }.onFailure { errors += it }
            }
        }
        stateJob.await()
        locationJob.await()

        assertEquals("concurrent beginBroadcast must not throw", emptyList<Throwable>(), errors)
        assertEquals(1_000, stateCalls.get())
        assertEquals(1_000, locationCalls.get())
    }

    @Test
    fun remoteExceptionOnOneCallback_doesNotStopOtherCallbacks() {
        val adapter = AidlBatteryServiceAdapter()
        val deliveries = AtomicInteger(0)
        adapter.registerCallback(
            object : CountingCallback() {
                override fun onStateChanged(snapshot: ParcelableBatterySnapshot?) {
                    throw RemoteException("dead client")
                }
            },
        )
        adapter.registerCallback(
            CountingCallback(onState = { deliveries.incrementAndGet() }),
        )

        adapter.publishState(sampleSnapshot())

        assertEquals(1, deliveries.get())
    }

    private open class CountingCallback(
        private val onState: () -> Unit = {},
        private val onLocation: () -> Unit = {},
    ) : IBmsCallback.Stub() {
        override fun onStateChanged(snapshot: ParcelableBatterySnapshot?) {
            onState()
        }

        override fun onConnectionStatusChanged(statusCode: Int) = Unit

        override fun onLocationChanged(location: ParcelableVehicleLocation?) {
            onLocation()
        }
    }

    private fun sampleSnapshot() = BatterySnapshot(
        timestamp = 1L,
        stateOfChargePercent = 50f,
        totalVoltage = 310f,
        current = 0f,
        cellVoltageMax = 0,
        cellVoltageMin = 0,
        batteryTempMax = 0,
        batteryTempMin = 0,
        controllerTemp = 0,
        motorTemp = 0,
        motorRpm = 0,
        vehicleSpeed = 0f,
    )

    private fun sampleLocation() = VehicleLocation(
        altitude = 0.0,
        speed = 0f,
        isMock = false,
        timestamp = 1L,
        latitude = 52.52,
        longitude = 13.405,
        accuracyMeters = 5f,
        source = LocationSource.ANDROID_SYSTEM,
    )
}
