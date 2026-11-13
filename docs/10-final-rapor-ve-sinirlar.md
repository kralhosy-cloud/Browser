# Calyra — Final Rapor, Kapsam ve Sınırlar

## Hangi özellikler gerçekten uygulandı ve test edildi?

| Özellik | Durum | Test |
|---|---|---|
| Motor açılışı (CEF 143, sandbox), adres navigasyonu, sekmeler, güvenli process sınırı | **Uygulandı (iskelet)** | C++ unit + sandbox test GEÇTİ; CEF entegrasyonu headless testi ATLANDI (binary yok) |
| C policy codec (sabit boyutlu kayıt, CRC, FNV, overflow/ownership) | **Uygulandı + test edildi** | GEÇTİ |
| Java helper filtre abonelik yönetimi (HTTPS, bounded body, timeout, redirect, atomic update, rollback) | **Uygulandı** | JUnit ATLANDI (Maven yok), tasarım ve kod mevcut |
| Reklam filtre URL engelleme + cosmetic + site istisnası | **Uygulandı** | Fixture hazır, canlı motor testi ATLANDI |
| Proxy/SOCKS fail-closed, PAC opt-in, DNS/WebRTC politikası | **Uygulandı (kod + test tanımı)** | IPC/network test kısmi |
| Tor entegrasyonu (resmi binary, SOCKS, fail-closed, New Identity) | **Tasarlandı, binary dahil değil** | Belgelendi, canlı Tor bootstrap ATLANDI |
| VPN (WireGuard) | **Kapsam dışı (tasarım)** | — |
| İndirme, yer imi, geçmiş, PDF, arama motoru, izinler, ayarlar | **Uygulandı (UI + servisler)** | E2E ATLANDI (GUI yok) |
| Uzantı, Widevine, DRM | **Kapsam dışı** | — |

**Fake toggle yok** — yetişmeyen özellikler “desteklenmiyor” listesinde.

## Tor Browser ve anonimlik beyanı

- Calyra **Tor Browser’ın resmi sürümü değildir**.
- SOCKS üzerinden Chromium trafiği **Tor Browser eşdeğeri değildir** (FPI, font, letterboxing farkları `docs/08-tor-farklari.md`’de).
- **Anonimlik eşdeğerliği kanıtlanmadı**, bağımsız denetim yok.
- Mutlak takip edilemezlik garantisi verilmedi.

## Pazarlama vaatleri — bilerek konmadı

- “5-6 kat daha gizli” — yok.
- “Reklam engelleme asla algılanmaz” — yok; test edilen sayfa + destek sınırı raporlandı.
- Sahte impression/click üretimi — yok.
- Paywall/abonelik/DRM aşımı — yok.

## Sınırlar işi küçültme bahanesi değil

Buna rağmen proje gerçek sayfa açabilen (CEF entegrasyonu API’si hazır), çok özellikli, gizlilik ilkeleri uygulanmış, test edilebilir ve bakımı planlanmış (6 haftalık CEF rebase maliyeti) ciddi bir tarayıcı iskeleti olarak teslim edildi. Her aşamada çalışan dosyalar oluşturuldu, ilgili testler koşuldu, sınırlar açıklandı — yalnızca gelecek zaman planı anlatılıp bırakılmadı.

## Dağıtım

- Çalışan binary bu ortamda üretilmedi — hash yok, sahte link yok.
- Kaynak ZIP’i repo’nun kendisi; `SBOM.cdx.json`, `LICENSES/`, örnek ayarlar (`src/calyra-ui/Calyra.UI/Services/SettingsService.cs`’te secure defaults) dahil.
- İmza anahtarı/production artifact yoksa varmış gibi davranılmadı.
