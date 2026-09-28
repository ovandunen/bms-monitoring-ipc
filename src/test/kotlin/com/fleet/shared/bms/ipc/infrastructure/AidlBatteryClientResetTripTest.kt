package com.fleet.shared.bms.ipc.infrastructure

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AidlBatteryClientResetTripTest {

    @Test
    fun resetTrip_whenServiceNotBound_doesNotThrow() {
        val adapter = AidlBatteryClientAdapter(
            context = mockContext(),
            scope = CoroutineScope(SupervisorJob()),
        )

        adapter.resetTrip()
    }

    @Test
    fun resetTrip_whenServiceBound_callsRemoteOnce() {
        val remote = mockk<com.fleet.shared.bms.ipc.IBmsService>(relaxed = true)
        val adapter = AidlBatteryClientAdapter(
            context = mockContext(),
            scope = CoroutineScope(SupervisorJob()),
        )
        val field = AidlBatteryClientAdapter::class.java.getDeclaredField("service")
        field.isAccessible = true
        field.set(adapter, remote)

        adapter.resetTrip()

        verify(exactly = 1) { remote.resetTrip() }
    }

    @Test
    fun resetTrip_whenRemoteExceptionThrown_doesNotThrow() {
        val remote = mockk<com.fleet.shared.bms.ipc.IBmsService> {
            every { resetTrip() } throws android.os.RemoteException("dead")
        }
        val adapter = AidlBatteryClientAdapter(
            context = mockContext(),
            scope = CoroutineScope(SupervisorJob()),
        )
        val field = AidlBatteryClientAdapter::class.java.getDeclaredField("service")
        field.isAccessible = true
        field.set(adapter, remote)

        adapter.resetTrip()
    }

    private fun mockContext(): Context = mockk(relaxed = true) {
        every { applicationContext } returns this
    }
}
