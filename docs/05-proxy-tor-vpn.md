# Calyra — Proxy, Tor ve VPN Ayrımı

## Proxy (zorunlu, ilk sürümde var)

### Protokoller — exact semantik
- **HTTP CONNECT:** RFC 9110 9.3.6. TLS el sıkışması proxye kadar değil, hedefe kadar tünellenir. `Proxy-Authorization` yalnızca proxy’ye.
- **TLS-to-proxy:** `https://proxy` üzerinden CONNECT — sertifika doğrulaması proxy sertifikasına, hedef sertifikası ayrı doğrulanır. Karıştırma yok: “HTTPS proxy” ≠ “HTTPS hedefe tünel”.
- **SOCKS5:** RFC 1928/1929, `ATYP 0x03` domain, uzak DNS (socks-remote-dns).
- **Uzak DNS:** Tor/proxy modunda DNS asla sistem resolver’a düşmez.

Kimlik doğrulama ve sertifika doğrulaması güvenli; `Basic` parolalar vault’ta, URI/log/crash/process listesinde görünmez.

### PAC
- Opt-in, kaynak HTTPS + imza doğrulaması, execution limit 100ms + 1 MB, `FindProxyForURL` sandbox’ı. Güvenli fallback: proxy bozulursa **DIRECT değil, fail**.

### Fail-closed
- Gizlilik profilinde proxy bozulursa otomatik DIRECT **yasak**. UI “Bağlantı kesildi — proxy hatası” gösterir; kullanıcı explicit Normal profil açmadan direct’e dönmez.
- Ortam değişkenleri (`http_proxy`) ve sistem proxy ayarları explicit politikayı görünmeden ezmez — `calyra://net`’te gösterilir.

## Tor (isteğe bağlı)

- **Entegrasyon:** Resmî Tor binary (`torproject.org/dist/tor-*`, GPG imzalı). Lisans 3-clause BSD. Sürüm: Tor 0.4.8.12 (Nov 2025). İmza `tor-0.4.8.12.tar.gz.asc` ile doğrulandı.
- **Dağıtım:** `third_party/tor/` altında, `torrc` minimal, `ControlPort` unix socket, `SocksPort` 19050 (sistem 9050 ile çakışmaz).
- **SOCKS + DNS:** `Socks5Proxy=socks5://127.0.0.1:19050`, `HostResolverRules="MAP * ~NOTFOUND, EXCLUDE *"` — .onion DNS sızıntısı yok.
- **Stream isolation:** `IsolateSOCKSAuth` + `IsolateClientAddr` desteği varsa SOCKS auth ile origin izolasyonu; Tor’un garanti etmediği “her sekmeye ayrı devre” iddiası yok.
- **SOCKS üzerinden Chromium ≠ Tor Browser:** Fingerprint (canvas, font, WebGL) ve first-party isolation farkları `docs/08-tor-farklari.md`’de.
- **Bootstrap:** `BOOTSTRAP PROGRESS=...` kontrol kanalı, UI’da ilerleme çubuğu. Tor hazır olmadan sayfa doğrudan çıkmaz.
- **Düşerse fail-closed:** DIRECTe dönülmez.
- **Yeni Kimlik:** Control `SIGNAL NEWNYM` + tüm storage bağlamlarını kapatma (sekme, SW, auth, IndexedDB) — yalnızca sinyal gönderip “temizlendi” denmez. Rate limit (10 NEWNYM/60s) ve exit IP garantisi yok.
- **Uzaktan içerik kontrol kanalına erişemez.**

## VPN (gerçek sistem VPN’inden ayrılır)

- Arayüze VPN düğmesi koymak OS tüneli kurulduğu anlamına gelmez — açıkça yazıldı.
- Gerçek VPN gerekiyorsa: kullanıcı yetkilendirmesiyle WireGuard (`wireguard-tools`, `wg-quick`) entegrasyonu değerlendirildi; kendi kripto/driver yazılmadı.
- Sahte endpoint üretilmedi; sağlayıcı/sunucu/hesap yoksa “VPN varmış” gibi gösterilmedi.
- Mevcut tünel seçip durumu gösterme (Linux `nmcli`, Windows `RasEnumConnections`) mümkünse salt-okunur; sistem geneli etki için açık izin.
- Yetki yükseltme yalnızca tünel kurulum aşamasında.

### Kill switch vs fail-closed
- **Sistem geneli kill switch** OS seviyesidir; Calyra bunu vaat etmez.
- **Tarayıcıya ait fail-closed routing** desteklenir — `calyra://settings/network`’te hangisi olduğu açık.

## Test yarışları

Tünel kurulumu ↔ route değişimi ↔ DNS değişimi ↔ kapanış arasında yarışlar `tests/network/test_vpn_tor_race.py` ile. Crash sonrası makine interneti kalıcı bozulmaz; güvenli kurtarma ve kaldırma yolu belgelendi. VPN düşerken görünmez DIRECT, IPv6/QUIC sızıntısı yok. VPN+Tor zinciri varsayılan değil; sıra/güvenilen taraf/performans/korelasyon etkisi UI’da gösterilir.
