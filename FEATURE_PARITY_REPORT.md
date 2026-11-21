# AutoPilot — Feature Parity Report

This report compares the supplied decompiled APK references (`original-reference/`) against the reconstructed Kotlin + Jetpack Compose Android project (`app/`).

## 1. Screen & Navigation Inventory

| Original / Target Destination | Status | Current Implementation File | Notes |
| --- | --- | --- | --- |
| Engine Control & Live Tab Monitor | **Verified** | `app/src/main/java/com/autopilot/app/ui/screens/EngineScreen.kt` | Hero status card, runtime ticker, memory % monitor, start/pause/resume/stop, dynamic add/remove tabs (up to 12), multi-URL rotation, 4 automation modes, sliders, stealth/proxy toggles, live WebView `AndroidView` preview. |
| Activity, History & Schedules | **Verified** | `app/src/main/java/com/autopilot/app/ui/screens/ActivityScreen.kt` | Real-time filtered event feed, Room-persisted session history with one-tap reload, and `WorkManager`-backed recurring schedules. |
| Stealth (Fingerprints & Proxy Pool) | **Verified** | `app/src/main/java/com/autopilot/app/ui/screens/StealthScreen.kt` | Fingerprint profile presets & custom creation, rotation, public proxy fetching (`PublicProxyFetcher`), per-source fetch diagnostics, TCP latency probing, WebView ProxyOverride vs disabled VPN notice, and live exit IP verification. |
| Settings & Diagnostics | **Verified** | `app/src/main/java/com/autopilot/app/ui/screens/SettingsScreen.kt` | Local operator profile, theme switcher, default tab/refresh/scroll preferences, country region chips + timezone preview, Root/Magisk/Zygisk/LSPosed scan + verified readback, notification permission flow, and Cloud Backend status. |
| Adaptive Navigation Shell | **Verified** | `app/src/main/java/com/autopilot/app/ui/navigation/AutoPilotNav.kt` | Type-safe Navigation Compose (`@Serializable` routes) switching between `NavigationBar` and `NavigationRail` at `600.dp`. |

## 2. Feature Comparison Matrix

| Original Reference | Behavior in Original Source | Current Kotlin Implementation | Parity Status |
| --- | --- | --- | --- |
| `ox0.java` + `jx0.java` + `verified-endpoint-strings.txt` | Fetches public HTTP proxy lists from GeoNode JSON, Monosans, ProxyScrape v2, and TheSpeedX; parses `host:port` and scheme (`ox0.d`); maps 63 ISO country codes (`ox0.u`) and aggregates country counts (`ox0.e`). | `PublicProxyFetcher.kt`, `CountryRegionCatalog.kt`, `AutoPilotRepository.kt` | **Verified** — Same 4 endpoints, caps (30/30/40/30), line/JSON parsers, and 63-country lookup table. Reports per-source HTTP errors, rate limits, and timeouts transparently. |
| `verified-endpoint-strings.txt` (`ipify`, `ipapi.co`, `ipwho.is`) | External IP and country lookup endpoints. | `ProxyHealthChecker.verifyExitIp()` | **Verified** — Executes real OkHttp requests (direct or via selected proxy) against `ipwho.is`, `api.ipify.org`, and `ipapi.co`. |
| `ij.java` (Automation Engine, WebView tabs, error recovery, 120s proxy rotation cooldown) | Multi-tab WebView automation, JS stealth injection, recovery up to 3 attempts, 120s-throttled proxy rotation on repeated failures (`ij.i`, `ij.p`), direct fallback if no healthy proxy exists. | `AutomationEngine.kt`, `StealthScriptGenerator.kt`, `AutomationForegroundService.kt` | **Verified** — Core lifecycle, foreground service (`dataSync`), low-memory auto-pause, WebView proxy override, and 120s-throttled proxy rotation/fallback implemented. |
| `em2.java` (Root spoofing & country timezone map) | Maps 20 country codes to IANA timezones (`em2.b`), runs `resetprop` / `settings put secure android_id` / `service call alarm 3` via `su`, and verifies `getprop` readback (`em2.g`). | `CountryRegionCatalog.kt`, `RootEnvironmentDetector.kt`, `RootSpoofer.kt` | **Partial (Safe-Guarded)** — Timezone mapping, `getprop` readback verification, and root-gated `su` commands ported. On non-rooted devices, system spoofing is disabled and honestly labeled as WebView JS-only. |
| `original-reference/AndroidManifest.xml` (No `VpnService`) | Original manifest does not declare `android.net.VpnService`; uses WebView/app-level routing. | `AndroidManifest.xml`, `ProxyHealthChecker.kt`, `StealthScreen.kt` | **Verified (Accurately Labeled)** — Uses `androidx.webkit.ProxyController` for in-app WebView only, with an explicitly disabled Device-Wide VPN Tunnel indicator. |
| `original-reference/AndroidManifest.xml` (Firebase Auth + Realtime Database) | Original APK bundled Firebase Auth, Credential Manager, and Firebase Realtime Database registrars without public credentials. | `AppDatabase.kt`, `UserSettingsRepository.kt`, `SettingsScreen.kt` | **Unavailable (Backend Credentials Absent)** — Replaced with local Room + DataStore persistence so the app runs offline/locally without fabricated secrets; see `BACKEND_SETUP.md`. |
