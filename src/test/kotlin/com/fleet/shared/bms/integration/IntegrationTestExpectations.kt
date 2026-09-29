package com.fleet.shared.bms.integration

import java.io.File
import java.util.Properties

/**
 * Reads bms-monitoring-app/integration-test.contract.properties and decodes pack metrics
 * (EcoCar VCU layout — byte 3 = SOC %%, bytes 4–7 = current/voltage LE).
 * Edit the contract file to change the integration test scenario; values are derived here.
 */
object IntegrationTestExpectations {

    private val properties: Properties by lazy { loadProperties(resolveContractFile()) }
    private val packMetrics: PackMetrics by lazy { decodePack() }

    val socPercent: Float get() = packMetrics.soc
    val packVoltageV: Float get() = packMetrics.voltage
    val packCurrentA: Float get() = packMetrics.current
    val powerKw: Float get() = packMetrics.powerKw

    val ipcAuditLine: String
        get() = "onStateChanged: soc=${fmt1(socPercent)}% " +
            "V=${fmt1(packVoltageV)}A=${fmt1(packCurrentA)} " +
            "trip=0.0km co2=0.0kg"

    val descriptorSoc: String get() = "battery-soc=${fmt1(socPercent)}"
    val descriptorVoltage: String get() = "battery-voltage=${fmt1(packVoltageV)}"
    val descriptorCurrent: String get() = "battery-current=${fmt1(packCurrentA)}"
    val descriptorPower: String get() = "battery-power=${"%.2f".format(powerKw)}"

    private data class PackMetrics(val soc: Float, val voltage: Float, val current: Float, val powerKw: Float)

    private fun decodePack(): PackMetrics {
        val hex = properties.getProperty("can.pack.data")
        val data = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val soc = data[3].toUByte().toInt().coerceIn(0, 100).toFloat()
        val currentRaw = (data[4].toInt() and 0xFF) or ((data[5].toInt() and 0xFF) shl 8)
        val voltageRaw = (data[6].toInt() and 0xFF) or ((data[7].toInt() and 0xFF) shl 8)
        val current = kotlin.math.round((currentRaw * 0.1f - 3200f) * 10f) / 10f
        val voltage = kotlin.math.round(voltageRaw * 0.1f * 10f) / 10f
        val powerKw = kotlin.math.round((voltage * current) / 10f) / 100f
        return PackMetrics(soc, voltage, current, powerKw)
    }

    private fun resolveContractFile(): File {
        System.getProperty("integration.contract.file")?.let { return File(it) }
        val candidates = listOf(
            File("integration-test.contract.properties"),
            File("../bms-monitoring-app/integration-test.contract.properties"),
            File("../../bms-monitoring-app/integration-test.contract.properties"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("integration-test.contract.properties not found")
    }

    private fun loadProperties(file: File): Properties =
        Properties().apply { file.reader().use { load(it) } }

    private fun fmt1(v: Float): String =
        if (v == kotlin.math.floor(v.toDouble()).toFloat()) "${v.toLong()}.0" else v.toString()
}
