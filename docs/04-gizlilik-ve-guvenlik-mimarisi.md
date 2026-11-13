# Calyra — Gizlilik ve Güvenlik Mimarisi

## 1) Görsel yaklaşım (Tor Browser’dan esin, kopya değil)

Calyra’nın masaüstü arayüzü Tor Browser’ın sade sekmeli düzeninden esinlenir — sekme çubuğu üstte, adres çubuğu belirgin, kalkan/izin ikonları görünür — fakat **Tor logosu, soğan işareti, marka adı veya resmî ürün çağrışımı kopyalanmaz.**

- **Renk sistemi:** Derin lacivert `#0B1220` (güven), açık gri `#F2F4F8`, vurgu turkuaz `#14C8B5`. Tor moru (`#7D4698`) kullanılmadı.
- **İkonografi:** Kendi line-ikon seti — kalkan (koruma), kilit (TLS), göz (izin), roket (Tor hazır), fiş (proxy). Onion ikonu yok.
- **Tipografi:** Inter (UI) + JetBrains Mono (adres/sertifika detay). Sistem font fallback’i.
- **Yerleşim:** Soldan sağa — ana menü → geri/ileri → yenile/durdur → adres çubuğu (origin + kilit + izin) → kalkan → indirmeler → profil seçici (Normal/Güçlendirilmiş/Tor). Yüksek kontrast ve sistem teması desteklenir.
- **Chrome/Content ayrımı:** Web içeriği üst çubuğu taklit edemez — `chrome` sınırı 2px kontrast çizgi + gölgeyle görünür. Tam ekranda origin bandı ve ESC çıkış yolu kalıcı.
- **Erişilebilirlik:** Koyu/açık/sistem teması, 100–200% ölçekleme, klavye ile sekme/adres/menü, screen reader adları, görünür odak, RTL hazırlığı.

## 2) Ağ envanteri ve route politikası

Tüm kaynaklar envanterlendi; her biri hangi route’a uyar testte doğrulanır:

| Kaynak | Normal | Güçlendirilmiş | Tor |
|---|---|---|---|
| Navigasyon, fetch/XHR, WebSocket, WebTransport, HTTP3/QUIC | Direct | Direct (QUIC gerekirse kapat) | Tor SOCKS5 |
| DNS (sistem resolver) / DoH | Sistem / DoH (kullanıcı seçimi) | DoH yalnızca proxy uzak DNS yoksa | **Asla public DoH** — SOCKS remote DNS |
| WebRTC (STUN/TURN) | Açık, mDNS candidate | Kısıtlı/kapalı (kullanıcıya anlatılır) | Kapalı |
| Prefetch/preconnect/speculative | Açık | Kapalı | Kapalı |
| Safe Browsing, CRL, cert revocation | Direct, rate-limitli | Aynı | Tor üzerinden (gecikmeli, metadata açıklanır) |
| Update kontrolü, crash report, search suggestion, favicon, uzantı güncellemesi | Direct, opt-in | Opt-in, varsayılan kapalı | Tor üzerinden |
| Java helper (filtre indirme) | Direct | Direct | Direct (filtre sunucusu Tor’a uygun değilse) — UI’da açıklanır |

**Arka plan servislerinin doğrudan bağlantısı görmezden gelinmez;** `tests/network/test_route_policy.py` ile packet capture.

## 3) DNS ve sızıntı politikası

- Proxy uzak DNS vs sistem resolver vs DoH **yarışmaz** — öncelik: Tor → proxy uzak DNS → DoH (eğer kullanıcı açtıysa) → sistem.
- IPv4/IPv6, DNS cache, ağ değişimi, sleep/wake kapsanır.
- Tor modunda `chrome://net-internals/dns` benzeri `calyra://net` sayfasında doğrudan DoH çağrısı görülmemeli.

## 4) WebRTC / QUIC / IPv6 kontrolleri

CEF/Chromium güncel kontrolleri doğrulandı:
- `WebRTCIPHandlingPolicy: disable_non_proxied_udp`, `AnonymizeLocalIps`.
- Güçlendirilmiş profilde `enable-webrtc = false` veya `kWebRtcLocalIpsAllowedByPolicy`.
- QUIC: proxy/Tor modunda `disable-quic` bayrağı; DIRECT fallback yok.
- IPv6: proxy SOCKS5’te `remote_dns + ipv6 leak` testi.

Rastgele flag eklemenin sızıntıyı kapattığı varsayılmadı — `webrtc-internals` ve `about:webrtc` benzeri teşhis sayfasıyla doğrulandı.

## 5) Reklam engelleme — gerçek ve test edilebilir

### Yetenekler
- URL filtreleme (EasyList formatı alt kümesi), element hiding, cosmetic filter, güvenli scriptlet yürütme (CSP uyumlu).
- Desteklenen syntax: `||domain^`, `@@`, `$third-party`, `$domain=`, `$script`, `$image`, `$xhr`, `##` cosmetic. Desteklenmeyen kurallar “başarıyla yüklendi” sayılmaz — `filter.log`’da `unsupported` olarak işaretlenir.
- Ağ filtreleri `OnBeforeRequest`, cosmetic `DocumentStart`’te.

### Anti-adblock uyumluluk
- Bakımı süren uyumluluk kuralları, dar cosmetic fix, siteye özgü geri alınabilir istisnalar.
- **Garanti yok:** Site kendi DOM değişimini gözleyebilir. *Engellememe* (banttan tasarruf + izleme azaltma) ile *gizleme* (DOM gizleme) farkı UI’da açıklanır. Tüm engellemeyi sessizce kapatma yok.

### Sahtecilik yasağı
- Sahte görüntülenme/tıklama/dönüşüm/telemetri **üretilmez.**
- Paywall/abonelik/DRM aşılmaz.

### Filtre abonelikleri
- Kaynak URL, lisans, imza (varsa Ed25519), indirme tarihi, sürüm/hash, kural sayısı gösterilir.
- HTTPS, bounded body (≤5 MiB), 10s timeout, 3 redirect, atomic update, last-known-good rollback. Bozulursa mevcut doğrulanmış liste korunur.
- Ziyaret edilen URL filtre sunucusuna sorulmaz — karar yerelde. Liste güncellemesi telemetri değildir.

### Performans/güvenlik
- Regex/scriptlet CPU/bellek sınırlı (RE2, 50ms budget, 1 MB/script). Kullanıcı kuralı syntax hatası sekmeyi çökertmez.
- Request log varsayılan kapalı, geçici, sınırlı (son 100, 5dk). Credential maskeleme.
- Engellenen sayı “farklı izleyici” diye şişirilmez; script denemesi tek sayılır.
- İstisna yalnız exact origin / seçili domain; substring eşleşmesi yok.

## 6) Koruma paneli
Tek yerde: etkin ağ yolu, 3P cookie politikası, engellenen istekler, site izinleri, geçici istisnalar. Korumayı düşürme kapsam/süre/etki açık. Tor/fail-closed, reklam uyumluluk düğmesiyle kapanmaz. “Tüm korumaları kapat” düğmesi yok. İstisnalar `calyra://settings/exceptions`’da denetlenir.

## 7) Fingerprint azaltım

- Yalnız UA değil: ekran/pencere, DPR, font, canvas, WebGL, AudioContext, timezone, locale, hardwareConcurrency, deviceMemory, touch, client hints, network stack tutarlılığı.
- Sürekli rastgele değer **savunma değil** — kararsızlık fingerprint’tir. Standardize profil (Tor Uplift benzeri) + tutarlılık testi.
- Motor düzeyi savunma JS injection’la tamamlanmış gibi sunulmadı.
- Letterboxing (boyut standardizasyonu) erişilebilirlik etkisiyle anlatılır.
- Stable noise (varsa) tohum kapsamı, yaşam süresi, reset, linkability test edildi; kendi kripto tasarımımız yok.
- Test skorları mutlak anonimlik değil; küçük anonimlik kümesi etkisi belgeli. Tor modu için özel değişikliklerin ters etkisi ayrıca değerlendirildi.

## 8) Storage izolasyonu

- Cookies, HTTP cache, serviceWorker, IndexedDB, localStorage, Cache API, HSTS — motorun partitioning yetenekleri incelendi (CHIPS, Storage Partitioning).
- Erişilemeyen storage “temizlendi” sayılmaz.
- 3P cookie varsayılan kapalı, dar istisna.
- Container profilleri (alışveriş/sosyal/iş) ayrı; opener/referrer/link decoration modern standartlarla azaltıldı.

**“Tüm izleri sil”** kapsamı exact: bellek (sekme state, auth, SW), disk (profil dizini, cache, IndexedDB). OS swap/SSD/AV/thumbnail/crash dump kontrol dışı — güvenli silme garantisi yok. Private modda history/sessionRestore/favicon/thumbnail/form/password save ayrı test edildi. Profil dizini 0700, secret’lar OS vault’ta.

## 9) Güvenlik

- Sandbox, renderer izolasyonu, site isolation öncelik.
- CEF sandbox’ı upstream ile birebir değil — platform/paketleme doğrulandı; `--no-sandbox` ile demo yok. Geliştirmede bypass release’e girmesin diye CI testi (`assert_sandbox_active`).
- Remote debugging portu normal dağıtımda kapalı; test modunda localhost + token + kullanıcı onayı.
- TLS: sertifika hatası sessizce yok sayılmaz; accept-all callback yok. HSTS/mixed content/secure context motor varsayılanından sapmaz. Kurumsal sertifika/override scoped + uyarı.
- Phishing/malware koruması gizlilik dengesi açık; uzak sorgu verisi belirtilir. Gizli mod taramayı kapatmaz.
- Native helper düşük yetkili, dar API.

## 10) İndirmeler, yer imleri, adres çubuğu, izinler, şifreler

- İndirme: gerçek durum, ilerleme, kalan boyut, pause/resume, hash doğrulama, path traversal önlemi, route/profile korunur, otomatik çalıştırma yok, Tor risk uyarısı, OS quarantine korunur.
- Yer imleri/geçmiş/pin/sekme grupları/arama/zoom/PDF/print/reader: ilk sürümde yetişmeyen fake toggle değil backlog.
- Adres çubuğu URL/aramayı ayırır, arama motoru seçimi ve suggestion ağ etkisi açık, gizlilik profilinde suggestion varsayılan kapalı, IDN/punycode güvenli gösterim.
- Kamera/mikrofon/konum/bildirim/clipboard vb. ayrı; kalıcı vs oturumluk; origin doğru; OS reddini kabul et.
- Şifre yöneticisi: sıfırdan kripto yok; ilk sürümde kapalı veya OS vault entegrasyonu — karar explicit. Dışa aktarma onayı, sync zorunlu değil. Sync eklenirse E2E tasarım ayrı tehdit modeline girer.

## 11) Uzantı, DRM, Gelişmiş

- Chromium her embedder Chrome uzantılarını tam desteklemez — iddia edilmedi. Destek varsa izin/güncelleme/imza/izolasyon/private erişim açık. Kötü uzantı güçlü izleyici — güçlendirilmişte minimum.
- Widevine/proprietary codec lisansı ayrı doğrulandı; “her medya çalışır” vaadi yok.
- DRM aşılmadı; desteklenmeyen codec listesi açık.
- WebAuthn/passkey test edilmeden “destekleniyor” denmedi; Tor’da kapatılan özelliklerin sebebi anlatıldı.

## 12) Ayarlar ve rapor

- Ayarlarda arama, açıklamalar, güvenli varsayılanlar, geri alma. “Maksimum güvenlik” değişiklikleri listelenir.
- Gizlilik raporu puan değil — etkin kontroller, son test zamanı, bilinmeyenler.
- VPN/Tor/proxy/filtre durumları karıştırılmaz; helper çökerse UI yeşil göstermez.

## 13) Performans ve paketleme

- Startup/idle RAM/sekme CPU/filtre gecikmesi/sayfa yükleme/helper sayısı ölçüldü (hedef: boşta <350 MB, 10 sekme <1.2 GB).
- Tab uyutma veri kaybı yaratmaz.
- Java/.NET/native boyut açık; önceden kurulu varsayılmadı; self-contained güncelleme maliyeti belirtildi.
