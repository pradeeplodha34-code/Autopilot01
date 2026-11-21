# AutoPilot (Android — Kotlin & Jetpack Compose)

A native Kotlin & Jetpack Compose reconstruction of **AutoPilot** using Material 3, Room, DataStore, WorkManager, OkHttp, and Android WebView.

## Features

- **Multi-Tab WebView Automation Engine (`AutomationEngine` + `AutomationForegroundService`)**
  - Up to 12 concurrent WebView tabs with real-time live preview switching (`AndroidView` container attach/detach).
  - Four automation modes (`SIMPLE_SCROLL`, `DEEP_SCROLL`, `TAB_SWITCHING`, `CUSTOM_JS`) with configurable refresh/scroll intervals, randomized jitter, multi-URL rotation, and custom JavaScript execution.
  - Persistent foreground service (`foregroundServiceType="dataSync"`) with notification actions (**Pause**, **Resume**, **Stop**), low-memory auto-pause safeguards, and 120-second throttled proxy failover.
- **Stealth, Fingerprints & Proxy Pool**
  - **Device Fingerprints**: Seeded and custom device profiles (User-Agent, platform, viewport, hardware concurrency, device memory, WebGL vendor/renderer, canvas noise seed, WebRTC policy).
  - **Public Proxy Fetcher (`PublicProxyFetcher`)**: Queries GeoNode, Monosans, ProxyScrape v2, and TheSpeedX feeds with transparent per-source fetch counts and error reporting.
  - **Health & Exit IP Verification (`ProxyHealthChecker`)**: Real TCP socket latency probes, `androidx.webkit.ProxyController` WebView routing (accurately labeled as WebView-only, with an explicitly disabled Device-Wide VPN status), and real external IP verification via `ipwho.is`, `api.ipify.org`, and `ipapi.co`.
  - **Country & Timezone Catalog (`CountryRegionCatalog`)**: 63 supported ISO country mappings and 20 regional timezone alignments.
  - **Root Environment & Verified Readback (`RootEnvironmentDetector` + `RootSpoofer`)**: Honest Magisk/Zygisk/LSPosed/`su` detection and `getprop` readback verification; never claims system-wide spoofing on non-rooted devices.
- **Activity, History & Scheduling**
  - Live filtered telemetry event feed, Room-persisted session history with one-tap config reload, and `WorkManager`-backed recurring schedules (`ScheduleWorker`, `SessionSyncWorker`, `SessionResumeWorker`, `BootReceiver`).
- **Settings & Adaptive UI**
  - Local operator profile and defaults in DataStore (`UserSettingsRepository`), Light/Dark/System theme using bundled `Poppins` and `JetBrains Mono` fonts, and adaptive `NavigationBar` / `NavigationRail` navigation.

## Build

- Requires JDK 21, Android SDK 36, Gradle 9.3.1 (Kotlin DSL).
- Build debug APK:
  ```bash
  gradle assembleDebug
  ```
- Output APK path:
  `app/build/outputs/apk/debug/app-debug.apk`
