package com.autopilot.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.WebRtcPolicy
import java.util.UUID

@Database(
    entities = [
        SessionHistoryEntity::class,
        ProxyEntity::class,
        FingerprintEntity::class,
        ScheduledTaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun autoPilotDao(): AutoPilotDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "autopilot.db"
            ).build()

        fun defaultFingerprintProfiles(): List<DeviceFingerprint> = listOf(
            DeviceFingerprint(
                id = "fp_pixel_9_pro",
                androidId = "a4f8c12e90b341d7",
                advertisingId = UUID.nameUUIDFromBytes("pixel9pro".toByteArray()).toString(),
                buildModel = "Pixel 9 Pro",
                buildManufacturer = "Google",
                buildFingerprint = "google/caiman/caiman:15/AP3A.241105.008/12485168:user/release-keys",
                fingerprintAndroidVersion = 15,
                buildProduct = "caiman",
                buildDevice = "caiman",
                buildBoard = "caiman",
                buildBrand = "google",
                serialNumber = "42019Caiman881",
                macAddress = "02:4B:8E:1A:73:C9",
                bluetoothAddress = "22:22:7F:4D:91:04",
                widevineId = "9A3F8E12C4D56701B2E3",
                screenWidth = 1280,
                screenHeight = 2856,
                screenDpi = 495,
                userAgent = "Mozilla/5.0 (Linux; Android 15; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
                timezone = "America/New_York",
                locale = "en-US",
                glRenderer = "Mali-G715 MC7",
                glVendor = "ARM",
                canvasNoiseSeed = 782914305L,
                audioNoiseSeed = 194038271L,
                webrtcPolicy = WebRtcPolicy.BLOCK,
                platform = "Linux armv8l",
                cpuCores = 8,
                deviceMemory = 16,
                maxTouchPoints = 5,
                languages = listOf("en-US", "en")
            ),
            DeviceFingerprint(
                id = "fp_galaxy_s25_ultra",
                androidId = "b71e22d904a518f0",
                advertisingId = UUID.nameUUIDFromBytes("galaxys25ultra".toByteArray()).toString(),
                buildModel = "SM-S938B",
                buildManufacturer = "Samsung",
                buildFingerprint = "samsung/pa3qxxx/pa3q:15/AP3A.240905.015/S938BXXU1AYB2:user/release-keys",
                fingerprintAndroidVersion = 15,
                buildProduct = "pa3qxxx",
                buildDevice = "pa3q",
                buildBoard = "sun",
                buildBrand = "samsung",
                serialNumber = "R5CW809XK2P",
                macAddress = "02:91:4C:A7:20:5B",
                bluetoothAddress = "22:22:19:B4:7C:6E",
                widevineId = "4D7C120E98B34A51F029",
                screenWidth = 1440,
                screenHeight = 3120,
                screenDpi = 505,
                userAgent = "Mozilla/5.0 (Linux; Android 15; SM-S938B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36",
                timezone = "Europe/London",
                locale = "en-GB",
                glRenderer = "Adreno (TM) 830",
                glVendor = "Qualcomm",
                canvasNoiseSeed = 440192837L,
                audioNoiseSeed = 839102745L,
                webrtcPolicy = WebRtcPolicy.SPOOF,
                platform = "Linux armv8l",
                cpuCores = 8,
                deviceMemory = 12,
                maxTouchPoints = 5,
                languages = listOf("en-GB", "en")
            ),
            DeviceFingerprint(
                id = "fp_oneplus_13",
                androidId = "c9043a81e72d55b1",
                advertisingId = UUID.nameUUIDFromBytes("oneplus13".toByteArray()).toString(),
                buildModel = "CPH2655",
                buildManufacturer = "OnePlus",
                buildFingerprint = "OnePlus/CPH2655EEA/OP5D55L1:15/AP3A.240617.008/V.1bd2c70-1:user/release-keys",
                fingerprintAndroidVersion = 15,
                buildProduct = "CPH2655",
                buildDevice = "OP5D55L1",
                buildBoard = "sun",
                buildBrand = "OnePlus",
                serialNumber = "8E42OP13991A",
                macAddress = "02:7F:3B:90:E2:11",
                bluetoothAddress = "22:22:8A:30:F1:42",
                widevineId = "7B2E940A11C85D32E610",
                screenWidth = 1440,
                screenHeight = 3168,
                screenDpi = 510,
                userAgent = "Mozilla/5.0 (Linux; Android 15; CPH2655) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
                timezone = "America/Chicago",
                locale = "en-US",
                glRenderer = "Adreno (TM) 830",
                glVendor = "Qualcomm",
                canvasNoiseSeed = 918273645L,
                audioNoiseSeed = 564738291L,
                webrtcPolicy = WebRtcPolicy.BLOCK,
                platform = "Linux armv8l",
                cpuCores = 8,
                deviceMemory = 16,
                maxTouchPoints = 5,
                languages = listOf("en-US", "en")
            ),
            DeviceFingerprint(
                id = "fp_xiaomi_15_pro",
                androidId = "e3190f67b81a44c2",
                advertisingId = UUID.nameUUIDFromBytes("xiaomi15pro".toByteArray()).toString(),
                buildModel = "24101PNB7C",
                buildManufacturer = "Xiaomi",
                buildFingerprint = "Xiaomi/haotian/haotian:15/AQ3A.240829.003/OS2.0.104.0:user/release-keys",
                fingerprintAndroidVersion = 15,
                buildProduct = "haotian",
                buildDevice = "haotian",
                buildBoard = "sun",
                buildBrand = "Xiaomi",
                serialNumber = "XM15PRO7732C",
                macAddress = "02:C3:18:45:9E:70",
                bluetoothAddress = "22:22:55:E1:28:9A",
                widevineId = "3F90C71B82A44D10C583",
                screenWidth = 1440,
                screenHeight = 3200,
                screenDpi = 522,
                userAgent = "Mozilla/5.0 (Linux; Android 15; 24101PNB7C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Mobile Safari/537.36",
                timezone = "Asia/Singapore",
                locale = "en-SG",
                glRenderer = "Adreno (TM) 830",
                glVendor = "Qualcomm",
                canvasNoiseSeed = 309182746L,
                audioNoiseSeed = 617283940L,
                webrtcPolicy = WebRtcPolicy.BLOCK,
                platform = "Linux armv8l",
                cpuCores = 8,
                deviceMemory = 12,
                maxTouchPoints = 5,
                languages = listOf("en-SG", "en")
            )
        )
    }
}
