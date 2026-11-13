# Android — Masaüstü ile Mimari Farkları

## Platform kilitleme (Android)
- **OS:** Android 10–15 (API 29–35), **CPU:** arm64-v8a (birincil), x86_64 (emülatör), **SDK:** Android SDK 34.0.0, Build-Tools 34.0.0, NDK 26.1.10909125, Gradle 8.7, Kotlin 2.0.21
- **Motor dağıtım dosyaları:** GeckoView AAR (`org.mozilla.geckoview:geckoview:145.0.20251118022223`), `libxul.so`, `omni.ja`, `icudtl.dat` APK içinde; WebView kullanılsaydı sistem WebView APK’sına bağımlı olurdu.
- **Upstream güncelleme:** Mozilla Security Advisories + `mvnrepository.com/artifact/org.mozilla.geckoview/geckoview` — 6 haftada bir bump, `libs.versions.toml` kilitli.

## Process / Sandbox
- **Masaüstü CEF:** Browser + Renderer + GPU + Network ayrı process, OS sandbox (seccomp/Seatbelt). Helper ayrı düşük yetkili UDS pipe.
- **Android GeckoView:** `GeckoRuntime` kendi multi-process modelini kullanır (content process), ama Android app sandbox’ı (UID izolasyonu) içinde. `isolatedProcess` servisi ile helper izole edilebilir. WebView’da `isolatedProcess=true` için ayrı manifest servisi gerekir.
- **IPC:** Masaüstü UDS/named pipe → Android’de `Binder` / `AIDL` + `Messenger` + `EncryptedFile` (vault). `localhost TCP` yok — aynı yasak.

## Dil dağılımı (Android’e uyarlanmış)
- **Kotlin/Java:** UI (Jetpack Compose), sekme/pencere, ayarlar, indirme, izin prompt’ları — C# Avalonia burada kullanılmaz (Android’de MAUI WebView gerçek tarayıcı değil).
- **C (aynı `c-policy`):** NDK ile derlenen `libcalyra_policy.so` — JNI üzerinden çağrılır, policy codec aynı testlerle doğrulanır.
- **C++ (NDK):** GeckoView entegrasyon adaptörü, network policy hook (`GeckoSession.NavigationDelegate`, `ContentBlockingDelegate`).
- **Java helper:** Masaüstü Java helper → Android’de `WorkManager` + `ForegroundService` ile filtre güncelleme, imza doğrulama; çökerse Geckoview session etkilenmez.

## Ağ farkları
- Proxy: Android’de `Proxy` API + `GeckoRuntimeSettings.setProxyConfig()` (SOCKS5 + PAC). Sistem proxy’si explicit politikayı sessizce ezmez — `calyra://net`’te gösterilir.
- Tor: Orbot `SOCKS 127.0.0.1:9050` veya `tor-android` binary; bootstrap `OrbotHelper` ile, Tor hazır olmadan doğrudan çıkış yok (fail-closed).
- VPN: Android `VpnService` gerçekten OS tüneli kurar — masaüstüdeki “proxy ≠ VPN” ayrımı burada `VpnService.Builder` ile dürüstçe uygulanır; sahte endpoint yok, WireGuard `wg-quick` Android’de `WireGuard Go` ile.

## Tehdit modeli ekleri (mobil)
- Cihazdaki başka uygulama (malicious app) — Android `READ_EXTERNAL_STORAGE` olmadan profil dizini (`/data/data/com.calyra.browser`) 0700, başka UID okuyamaz.
- OS backup, keyboard cache, screenshot, notification preview Calyra kontrolü dışında — güvenli silme garantisi yok.
- WebRTC: GeckoView’da `media.peerconnection.enabled=false` güçlendirilmiş profilde; kamera/mikrofon izinleri OS + `GeckoSession.PermissionDelegate` çift katman.

## Görsel
Tor Browser esinli sade düzen korundu, mobilde alt sekme çubuğu, adres üstte, kalkan belirgin. Tor logosu/soğanı yok, kendi renk `#0B1220` / `#14C8B5` korundu.
