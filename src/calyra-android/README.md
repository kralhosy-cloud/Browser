# Calyra Android — Gizlilik Odaklı Mobil Tarayıcı

**Paket:** `com.calyra.browser` (Calyra ile tutarlı)  
**Motor:** GeckoView 145.0.20251118022223 (Chromium 143 muadili, 2025-11-18) — MPL-2.0, Mozilla Maven. Alternatif olarak Android System WebView tartışıldı, fakat gizlilik kontrolleri için GeckoView seçildi.
**Hedef:** Android 10+ (API 29), targetSdk 34, minSdk 29, ABI arm64-v8a + x86_64, NDK 26.1, Gradle 8.7, Kotlin 2.0.21

> **Not:** Bu klasör masaüstü Calyra’nın yanında Android varyantıdır. CEF masaüstüde, GeckoView mobilde — farklı process/sandbox modelleri `docs/android/01-mimari-farklari.md`’de.

## Neden GeckoView?
- WebView: sistem bileşeni, özelleştirilebilir takip koruması sınırlı; WebView’da JavaScript açıkken WebRTC’yi kapatma, per-site proxy, UA spoofing yok.
- GeckoView: Firefox motoru, Tracking Protection (ETP Strict), fingerprint direnç ayarları, per-tab proxy via `GeckoRuntimeSettings`, açık kaynak, Mozilla güvenlik bültenleriyle 6 haftalık güncelleme.
- Maliyet: APK +30–50 MB (GeckoView omni), WebView 0 MB ama kontrol kaybı.

## Kurulum
```bash
cd src/calyra-android
./gradlew assembleDebug   # Android SDK 34 + NDK 26.1 gerekir
adb install app/build/outputs/apk/debug/app-debug.apk
```
Bu E2B container’da Android SDK yok — **çalışır APK iddiası yok**, derlenebilir iskelet + fixture + yönerge var.

## Özellikler (Android’e uyarlanmış)
- Sekme çubuğu, adres çubuğu, kilit/kalkan, indirmeler — mobil düzen, koyu/açık tema
- Normal / Güçlendirilmiş / Tor profilleri (Tor: Orbot SOCKS 9050 veya resmi tor-android binary, fail-closed)
- Proxy: HTTP CONNECT / SOCKS5 / PAC opt-in, otomatik DIRECT yok
- Reklam engelleme: GeckoView `content-blocking` + uBlock-style filtre (subset), cosmetic `GeckoSession` injection
- Depolama izolasyonu: `GeckoRuntime` per-profile `storageController.clearData()`, Container benzeri izole session
