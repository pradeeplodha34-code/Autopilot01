package com.autopilot.app.automation

import android.os.Build
import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.RootEnvironment
import com.autopilot.app.domain.model.RootVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Ported from original APK class em2.java.
 * Never claims system-wide spoofing unless root access is actually present and verified via readback.
 */
object RootSpoofer {

    suspend fun readObservedDeviceState(hasRoot: Boolean): RootVerificationResult =
        withContext(Dispatchers.IO) {
            val model = runCommand(listOf("getprop", "ro.product.model")).ifBlank { Build.MODEL }
            val manufacturer = runCommand(listOf("getprop", "ro.product.manufacturer"))
                .ifBlank { Build.MANUFACTURER }
            val androidId = if (hasRoot) {
                runSuCommand("settings get secure android_id")
            } else {
                ""
            }
            val scopeMessage = if (hasRoot) {
                "Root detected — readback verified ($manufacturer $model)."
            } else {
                "Standard user mode — WebView JS spoofing only; Android build props are read-only."
            }
            RootVerificationResult(
                observedModel = model,
                observedManufacturer = manufacturer,
                observedAndroidId = androidId,
                systemSpoofApplied = false,
                statusMessage = scopeMessage
            )
        }

    suspend fun applyRootFingerprint(
        fingerprint: DeviceFingerprint,
        env: RootEnvironment
    ): RootVerificationResult = withContext(Dispatchers.IO) {
        if (!env.isRooted) {
            return@withContext readObservedDeviceState(hasRoot = false).copy(
                systemSpoofApplied = false,
                statusMessage = "Root unavailable — system-level resetprop/android_id spoofing skipped. Using WebView JS spoofing only."
            )
        }

        val commands = buildList {
            if (fingerprint.buildModel.isNotBlank()) {
                add("resetprop ro.product.model \"${sanitizeShell(fingerprint.buildModel)}\"")
            }
            if (fingerprint.buildManufacturer.isNotBlank()) {
                add("resetprop ro.product.manufacturer \"${sanitizeShell(fingerprint.buildManufacturer)}\"")
            }
            if (fingerprint.buildFingerprint.isNotBlank()) {
                add("resetprop ro.build.fingerprint \"${sanitizeShell(fingerprint.buildFingerprint)}\"")
            }
            if (fingerprint.buildDevice.isNotBlank()) {
                add("resetprop ro.product.device \"${sanitizeShell(fingerprint.buildDevice)}\"")
            }
            if (fingerprint.buildBrand.isNotBlank()) {
                add("resetprop ro.product.brand \"${sanitizeShell(fingerprint.buildBrand)}\"")
            }
            if (fingerprint.androidId.isNotBlank()) {
                add("settings put secure android_id ${sanitizeShell(fingerprint.androidId)}")
            }
            if (fingerprint.timezone.isNotBlank()) {
                add("service call alarm 3 s16 \"${sanitizeShell(fingerprint.timezone)}\"")
            }
        }

        commands.forEach { cmd -> runSuCommand(cmd) }

        val observedModel = runCommand(listOf("getprop", "ro.product.model")).ifBlank { Build.MODEL }
        val observedMfg = runCommand(listOf("getprop", "ro.product.manufacturer"))
            .ifBlank { Build.MANUFACTURER }
        val observedId = runSuCommand("settings get secure android_id")

        val matched = observedModel == fingerprint.buildModel &&
            observedMfg == fingerprint.buildManufacturer

        RootVerificationResult(
            observedModel = observedModel,
            observedManufacturer = observedMfg,
            observedAndroidId = observedId,
            systemSpoofApplied = matched,
            statusMessage = if (matched) {
                "Verified: system build props updated to $observedMfg $observedModel."
            } else {
                "Root command ran, but readback stayed $observedMfg $observedModel (resetprop may require Magisk/su permission)."
            }
        )
    }

    private fun runCommand(args: List<String>): String = runCatching {
        val process = ProcessBuilder(args).redirectErrorStream(true).start()
        process.waitFor(3, TimeUnit.SECONDS)
        BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
    }.getOrDefault("")

    private fun runSuCommand(cmd: String): String = runCatching {
        val process = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        process.waitFor(4, TimeUnit.SECONDS)
        val out = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
        if (process.exitValue() == 0) out else ""
    }.getOrDefault("")

    private fun sanitizeShell(value: String): String =
        value.replace("\"", "").replace("`", "").replace("$", "").replace(";", "")
}
