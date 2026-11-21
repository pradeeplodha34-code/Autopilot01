package com.autopilot.app.automation

import android.os.Build
import com.autopilot.app.domain.model.RootEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object RootEnvironmentDetector {

    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/sd/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    private val magiskIndicators = listOf(
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/data/adb/magisk.db",
        "/debug_ramdisk/.magisk"
    )

    private val zygiskIndicators = listOf(
        "/data/adb/modules",
        "/data/adb/zygisk"
    )

    private val lsposedIndicators = listOf(
        "/data/adb/lspd",
        "/data/adb/modules/zygisk_lsposed",
        "/data/adb/modules/riru_lsposed"
    )

    suspend fun detect(): RootEnvironment = withContext(Dispatchers.IO) {
        val foundSu = suPaths.firstOrNull { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }.orEmpty()

        val hasMagisk = magiskIndicators.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }

        val hasZygisk = zygiskIndicators.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }

        val hasLSPosed = lsposedIndicators.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }

        val testKeys = Build.TAGS?.contains("test-keys") == true
        val isRooted = foundSu.isNotEmpty() || hasMagisk || testKeys

        RootEnvironment(
            isRooted = isRooted,
            hasMagisk = hasMagisk,
            magiskVersion = if (hasMagisk) "Detected" else "",
            hasZygisk = hasZygisk,
            hasLSPosed = hasLSPosed,
            lsposedVersion = if (hasLSPosed) "Detected" else "",
            suPath = foundSu
        )
    }
}
