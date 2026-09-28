package com.fleet.shared.bms.ipc.application

import com.fleet.shared.bms.integration.IntegrationTestExpectations
import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Use case: EcoCar integration test greps logcat for a stable IPC snapshot audit line after bind/telemetry.
 * Expected values come from bms-monitoring-app integration-test.contract.properties.
 */
class IpcSnapshotAuditFormatterTest {

    @Test
    fun format_integrationTestFrame_matchesMakefileGrepPattern() {
        val snapshot = integrationTestSnapshot()

        val line = IpcSnapshotAuditFormatter.formatStateChangedAuditLine(snapshot)

        assertEquals(IntegrationTestExpectations.ipcAuditLine, line)
    }

    @Test
    fun formatServiceConnected_matchesIntegrationLogcatGrep() {
        val line = IpcSnapshotAuditFormatter.formatServiceConnectedAuditLine(
            componentName = "ComponentInfo{ch.ecocarsolaire.bms/ch.ecocarsolaire.bms.infrastructure.android.service.BmsMonitorService}",
            binder = "android.os.BinderProxy@476f1d9",
        )

        assertEquals(
            "onServiceConnected: ComponentInfo{ch.ecocarsolaire.bms/ch.ecocarsolaire.bms.infrastructure.android.service.BmsMonitorService} binder=android.os.BinderProxy@476f1d9",
            line,
        )
        assertTrue(line.contains("onServiceConnected"))
    }

    @Test
    fun format_integerSoc_stillUsesOneDecimal() {
        val snapshot = integrationTestSnapshot().copy(stateOfChargePercent = IntegrationTestExpectations.socPercent)

        val line = IpcSnapshotAuditFormatter.formatStateChangedAuditLine(snapshot)

        assertEquals(IntegrationTestExpectations.ipcAuditLine, line)
    }

    private fun integrationTestSnapshot() = BatterySnapshot(
        timestamp = 1L,
        stateOfChargePercent = IntegrationTestExpectations.socPercent,
        totalVoltage = IntegrationTestExpectations.packVoltageV,
        current = IntegrationTestExpectations.packCurrentA,
        cellVoltageMax = 0,
        cellVoltageMin = 0,
        batteryTempMax = 0,
        batteryTempMin = 0,
        controllerTemp = 0,
        motorTemp = 0,
        motorRpm = 0,
        vehicleSpeed = 0f,
    )
}
