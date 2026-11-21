# AutoPilot — Backend & Optional Service Setup

This project currently runs in **Local Mode** using **Room** (`autopilot.db`) and **Jetpack DataStore** (`autopilot_prefs`) because private Firebase credentials, database rules, and VPN configurations are not included in the supplied APK reference.

## 1. Optional Firebase Auth & Realtime Database / Firestore

The original APK manifest referenced `FirebaseAuthRegistrar`, `FirebaseDatabaseKtxRegistrar`, and `androidx.credentials` (`CredentialManager`). No `google-services.json`, Web Client ID, or database rules exist in the repository.

If you want to enable cloud sync and Google Sign-In in your own fork:

1. Create a Firebase project in the Firebase Console and register Android package `com.aistudio.autopilot.kxmpzq` (or your own `applicationId`) with your debug/release SHA-1 & SHA-256 fingerprints.
2. Download `google-services.json` and place it in `app/google-services.json`.
3. Enable **Google Sign-In** in Firebase Authentication and add your OAuth Web Client ID via `.env` / Secrets.
4. Provision **Cloud Firestore** or **Firebase Realtime Database** and deploy strict per-user security rules (`request.auth != null && request.auth.uid == userId`).
5. Add the `com.google.gms.google-services` Gradle plugin and Firebase BOM dependencies to `gradle/libs.versions.toml` and `app/build.gradle.kts`.

## 2. Device-Wide VPN (Not Included)

`AndroidManifest.xml` intentionally does **not** declare `android.net.VpnService`, and no VPN server endpoint or tunnel protocol (WireGuard/OpenVPN) is configured. Proxy routing in this app uses `androidx.webkit.ProxyController` and applies strictly to in-app WebView traffic.
