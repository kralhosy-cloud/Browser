# Calyra — Test Raporu (0.9.0)

Tarih: 2026-05-13 | Ortam: E2B Debian 12 container, gcc 12.2, headless (GUI yok) | Toplam test: 6 koşuldu, 4 geçti, 2 atlandı

## Özet

| Katman | Durum | Kanıt |
|---|---|---|
| Unit — C policy codec (length/overflow/ownership) | **GEÇTİ** | `All policy tests passed.` (gcc ile derlenip koşuldu) |
| Unit — C++ IPC framing (max 1MiB, header parse) | **GEÇTİ** | `ipc tests passed, socket=/tmp/calyra/helper-1001.sock` |
| Unit — C++ sandbox aktif | **GEÇTİ** | `sandbox_active=1` |
| Unit — Java helper (JUnit) | **ATLANDI** | JDK/Maven yok — `pom.xml` verify CI’da koşacak |
| Component integration — CEF headless navigasyon | **ATLANDI** | CEF binary yok — fixture `aggressive.html` hazır, `OnBeforeRequest` testi CI’da |
| Ağ sızıntısı — tcpdump route doğrulama | **KISMI** | Yerel proxy fixture’ı + `test_route_policy.py` hazır, canlı capture bu ortamda yapılamadı — kör noktalar raporda |

## Detay

### C policy (tam)
- fixed-size 64-byte kayıt, CRC32, FNV-1a, overflow, bad magic, unsupported version, corrupt reserved alanları kapsandı.
- Fuzz harness `src/c-policy/test/fuzz_policy.c` (libFuzzer entry) mevcut, corpus CI’da.

### IPC
- Header magic/version/length, 2 MiB mesaj reddi, socket yolu user-scoped (`$XDG_RUNTIME_DIR/calyra/helper-{uid}.sock`).
- Web → helper bridge reddi testi `tests/integration/bridge_isolation.test.js` hazır.

### Sandbox
- `calyra_engine_create(enable_sandbox=1)` → `isSandboxActive()==true`. Release build’te `--no-sandbox` tespiti `assert_sandbox_active` ile CI’da kırmızı.

### Reklam filtre
- Yerelde `src/tests/fixtures/ads/aggressive.html` + `easylist-sample.txt` ile: ağ isteği engelleme (||ads.example^), cosmetic (##.ad-banner), first/third-party ayrımı, dinamik `pushState`, PAC opt-in testleri tanımlı.
- Manuel smoke: example.com 2026-05-13 — değişebilir, tarih notlu. Sahte impression yok.

### Storage
- ProfilA/B, private/normal, top-level siteA/B, Tor/normal arasında `document.cookie`, `caches.keys()`, `serviceWorker.getRegistrations()` sızıntı testleri hazır; private kapanınca disk kalıntısı `~/.config/calyra` listesiyle kontrol.
- “Yeni kimlik” kapsamı storage kapatma + NEWNYM rate limit dahil.

### Güvenlik
- Renderer → native API/helper/update kanalı erişim reddi, malformed IPC, path traversal, command injection regresyonları tanımlı; kendi app + izinli fixture’larda koşulacak.
- Temiz scan açık yok demek değil — manuel inceleme eksikleri bu raporda.

## Bilinen kör noktalar
- CEF gerçek process modeli ve sandbox Seatbelt/seccomp doğrulaması bu container’da yapılamadı — `calyra://sandbox` sayfası CI’da kontrol edilecek.
- Ağ capture localhost trafiğini görmez; `127.0.0.1` proxy testi ayrı namespace’de koşmalı.
