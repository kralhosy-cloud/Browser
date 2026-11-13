# Calyra — Test Planı

## Katmanlar

| Katman | Araç | Kapsam |
|---|---|---|
| **Unit** | CTest (C/C++), NUnit (C#), JUnit 5 (Java) | policy codec length/overflow/ownership, filter parser, URL sınıflandırma, IPC framing |
| **Component integration** | GoogleTest + CppUTest | C++ ↔ C ABI, native-core ↔ helper pipe handshake, filter engine entegrasyonu |
| **Gerçek motor integration** | CEF headless/offscreen (`--headless`, `OSR`) | Navigasyon, request interception, WebRTC/QUIC flag’leri, sandbox aktif |
| **E2E UI** | FlaUI / Playwright (test modunda) | Sekme/pencere, adres çubuğu, kalkan, indirme, izin prompt’u |
| **Ağ sızıntısı** | Local proxy + `tcpdump`/`tshark` + controlled DNS | Route doğrulama |

Sadece mock’la güvenlik iddiası yok; ama deterministik regresyon için mock’lar mevcut.

## Ağ kabul testleri

- **Normal:** `http://example.com` → doğrudan bağlantı gözlemlendi (proxy/tor kapalı).
- **Proxy modu:** `SOCKS5 127.0.0.1:1080` — yalnızca izinli proxy bağlantıları; `tcpdump`’da DIRECT paket yok.
- **Tor modu:** `SOCKS5 127.0.0.1:19050` — yalnız Tor yolu; hazırlıkta `NS_ERROR_PROXY_CONNECTION_REFUSED` değil, “Tor hazırlanıyor” ekranı.
- Yan yollar: DNS/WebRTC/QUIC/IPv6, helper update, filtre download, captive portal (`connectivitycheck.gstatic.com` benzeri) izole.
- Senaryolar: arayüz değişimi, VPN düşmesi, Tor kapanması, proxy auth hatası, sleep/wake, crash — **başarısız route’ta sekmenin yüklenmemesi başarı kriteridir** (DIRECTe yüklemek gizlilik başarısı değil).
- Capture kör noktaları raporda açık (örn. localhost capture missing).

## Reklam engelleme testleri

- Yerel fixture: `src/tests/fixtures/ads/` — `aggressive.html` içinde `||ads.example^`, `##.ad-banner`, `scriptlet` içeren sayfa. `OnBeforeRequest`’te engellenen istek sayısı = fixture beklenen.
- first/third-party ayrımı: `thirdparty.example` iframe’i engellenirken `firstparty.example` engellenmez.
- Dinamik navigation: `history.pushState` sonrası cosmetic yeniden uygulanır.
- Manuel smoke (izinli gerçek siteler): `example.com` (2026-05-13) — site davranışı değişebilir, tarih not düşüldü.
- Sahte impression/click üretilmeden test; yerel fixture kendi beacon endpoint’i (`/__calyra_test_beacon`) çağrılmaz — gerçek ücretlendirme endpoint’i çağrılmaz.
- Kırılan sayfaya dar istisna uygulanabildiği doğrulandı: `@@||cdn.example^$domain=broken.example`.

## Storage testleri

- ProfilA/B, private/normal, top-level siteA/B, Tor/normal arasında çerez/cache/SW/IndexedDB sızıntısı: `document.cookie` + `caches.keys()` + `navigator.serviceWorker.getRegistrations()` ile.
- Private kapanınca disk kalıntısı: `~/.config/calyra-test/Profile/*` içeriği listelenir.
- “Yeni kimlik / tüm veriyi temizle” kapsamı actual observation ile eşleşir.
- İndirilen dosya otomatik silinmez (veri kaybı yok).
- Profil migration: bozuk JSON → sınırlı allocate (1 MiB) + hata.

## Güvenlik kabul testleri

- Renderer sandbox aktif: `calyra://sandbox` ve `chrome://sandbox` benzeri sayfa plus `sandbox::IsSandboxedProcess()` C++ call.
- Web sayfasından native API/helper/update kanalına erişim reddedilir — `tests/integration/bridge_isolation.test.js` FAIL olmalı.
- Malformed IPC (büyük mesaj 2 MiB, path traversal `../../etc/passwd`, command injection `; rm`, token replay) regresyonları.
- Kendi uygulama + izinli fixture’lar üzerinde; başkalarının altyapısında tarama yok.
- Temiz scan = açık yok demek değil — kapsam ve manuel inceleme eksikleri raporlandı.

## Erişilebilirlik kabul

- Klavye ile sekme/adres/menü, screen reader `AutomationProperties.Name`, odak sırası, visible focus, high contrast, zoom 100–200%, RTL, çeviri altyapısı (`.resx` + `I18N`).
- Türkçe arayüz eksiksiz; güvenlik uyarıları yalnız renk değil (ikon+metin).
- Animasyonlar `prefers-reduced-motion`’a uyar.

## Performans hedefleri

- Startup < 2.5s (soğuk, SSD), boşta RAM < 350 MB, 10 sekme < 1.2 GB, filtre gecikmesi < 5ms/istek (p50), helper < 80 MB.
- Şişirilmiş benchmark yok; karşılaştırmalı testlerde sürüm/varsayılan/ölçüt/sınırlama belirtildi.
