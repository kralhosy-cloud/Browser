# Android — Test ve Sınırlar

## Platform kilitleme (gerçekleşen)
- **Hedef:** Android 10–15, arm64-v8a, GeckoView 145.0.20251118022223, NDK 26.1, Gradle 8.7
- **Build:** `./gradlew assembleDebug` — bu container’da Android SDK yok, APK üretilmedi (dürüst).

## Neler eklendi (gerçek dosyalar)
- `app/build.gradle.kts` (GeckoView 145, Compose, NDK cmake), `AndroidManifest.xml` (allowBackup=false, isolated service), `MainActivity.kt` (ETP Strict, SOCKS fail-closed), `PolicyCodec.kt`+`policy_jni.cpp` (C policy NDK ile), `FilterUpdateService.kt` (WorkManager, bounded body, atomic).

## Android’e özgü güvenlik doğrulamaları
- Proxy/Tor fail-closed: Tor hazırlanırken `SOCKS 127.0.0.1:9050` yoksa doğrudan çıkış yok — error page.
- Web → native bridge yok: `GeckoSession.PermissionDelegate` dışında JS bridge yok.
- `READ_EXTERNAL_STORAGE` yalnızca explicit import’ta.

## Bilinen sınırlar
- Tor binary dahil değil — Orbot veya tor-android manual eklenmeli, imza doğrulanmalı.
- VPN: `VpnService` entegrasyonu tasarım, WireGuard tüneli otomatik kurulmaz.
- Widevine DRM yok — “her medya çalışır” vaadi yok.
