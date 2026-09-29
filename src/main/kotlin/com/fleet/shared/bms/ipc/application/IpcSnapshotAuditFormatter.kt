package com.fleet.shared.bms.ipc.application

import com.fleet.shared.bms.ipc.domain.BatterySnapshot
import java.util.Locale

/**
 * Application helper: stable audit strings for IPC snapshot propagation (integration logcat).
 */
object IpcSnapshotAuditFormatter {

    fun formatServiceConnectedAuditLine(componentName: Any?, binder: Any?): String =
        "onServiceConnected: $componentName binder=$binder"

    fun formatStateChangedAuditLine(snapshot: BatterySnapshot): String =
        "onStateChanged: soc=${formatPercent(snapshot.stateOfChargePercent)}% " +
            "V=${formatOneDecimal(snapshot.totalVoltage)}" +
            "A=${formatOneDecimal(snapshot.current)} " +
            "trip=${formatOneDecimal(snapshot.tripDistanceKm)}km " +
            "co2=${formatOneDecimal(snapshot.co2SavingKg)}kg"

    private fun formatPercent(value: Float): String = "%.1f".format(Locale.US, value)

    private fun formatOneDecimal(value: Float): String = "%.1f".format(Locale.US, value)
}
