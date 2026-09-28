package com.fleet.shared.bms.ipc.infrastructure

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AidlBatteryServiceAdapterResetTripTest {

    @Test
    fun resetTrip_invokesRegisteredHandlerOnce() {
        val adapter = AidlBatteryServiceAdapter()
        var invoked = 0
        adapter.registerTripResetHandler { invoked++ }

        adapter.resetTrip()

        assertTrue(invoked == 1)
    }

    @Test
    fun resetTrip_calledTwice_invokesHandlerTwice() {
        val adapter = AidlBatteryServiceAdapter()
        var invoked = 0
        adapter.registerTripResetHandler { invoked++ }

        adapter.resetTrip()
        adapter.resetTrip()

        assertTrue(invoked == 2)
    }
}
