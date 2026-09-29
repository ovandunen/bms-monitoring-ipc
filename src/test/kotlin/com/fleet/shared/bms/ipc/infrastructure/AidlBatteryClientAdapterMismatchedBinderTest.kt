package com.fleet.shared.bms.ipc.infrastructure

import android.content.Context
import com.fleet.shared.bms.ipc.domain.CommandType
import com.fleet.shared.bms.ipc.domain.ConnectionStatus
import com.fleet.shared.bms.ipc.domain.bmsCommand
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AidlBatteryClientAdapterMismatchedBinderTest {

    @Test
    fun registerCallback_whenSecurityException_setsErrorAndDoesNotThrow() {
        val remote = mockk<com.fleet.shared.bms.ipc.IBmsService> {
            every { registerCallback(any()) } throws
                SecurityException("Binder invocation to an incorrect interface")
        }
        val adapter = adapterWithRemote(remote)

        invokePrivate(adapter, "registerCallback")

        assertTrue(adapter.connectionStatus.value is ConnectionStatus.Error)
    }

    @Test
    fun refreshSnapshotFromService_whenSecurityException_doesNotThrow() {
        val remote = mockk<com.fleet.shared.bms.ipc.IBmsService> {
            every { currentSnapshot } throws
                SecurityException("Binder invocation to an incorrect interface")
        }
        val adapter = adapterWithRemote(remote)

        invokePrivate(adapter, "refreshSnapshotFromService")

        assertTrue(adapter.connectionStatus.value is ConnectionStatus.Error)
    }

    @Test
    fun sendCommand_whenSecurityException_setsErrorAndDoesNotThrow() {
        val remote = mockk<com.fleet.shared.bms.ipc.IBmsService> {
            every { sendCommand(any()) } throws
                SecurityException("Binder invocation to an incorrect interface")
        }
        val adapter = adapterWithRemote(remote)

        adapter.sendCommand(bmsCommand(CommandType.START_MONITORING))

        assertTrue(adapter.connectionStatus.value is ConnectionStatus.Error)
    }

    @Test
    fun resetTrip_whenSecurityException_doesNotThrow() {
        val remote = mockk<com.fleet.shared.bms.ipc.IBmsService> {
            every { resetTrip() } throws
                SecurityException("Binder invocation to an incorrect interface")
        }
        val adapter = adapterWithRemote(remote)

        adapter.resetTrip()

        assertTrue(adapter.connectionStatus.value is ConnectionStatus.Error)
    }

    private fun adapterWithRemote(remote: com.fleet.shared.bms.ipc.IBmsService): AidlBatteryClientAdapter {
        val adapter = AidlBatteryClientAdapter(
            context = mockContext(),
            scope = CoroutineScope(SupervisorJob()),
        )
        val field = AidlBatteryClientAdapter::class.java.getDeclaredField("service")
        field.isAccessible = true
        field.set(adapter, remote)
        return adapter
    }

    private fun invokePrivate(adapter: AidlBatteryClientAdapter, methodName: String) {
        val method = AidlBatteryClientAdapter::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        method.invoke(adapter)
    }

    private fun mockContext(): Context = mockk(relaxed = true) {
        every { applicationContext } returns this
    }
}
