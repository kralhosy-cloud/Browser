# Calyra — Tehdit Modeli

> “Tor Browser’dan 5-6 kat daha gizli” bir pazarlama arzusudur; matematiksel ölçüm veya sağlanmış garanti değildir. Gizlilik tek bir çarpanla sıralanamaz. Yeni bir motor profili Tor’un anonimlik kümesini büyütmez; benzersiz fingerprint ile kullanıcıyı ayırt edilebilir kılar. Aşağıdaki tehditler ayrı ele alınır ve tek bir “güvenli” rozeti altında birleştirilmez.

Versiyon: 0.9 | Tarih: 2026-05-13 | Durum: Draft

## 1) Varlıklar
- Kullanıcının IP adresi ve konumu (ISP, VPN/Proxy işletmecisi, yerel ağ gözlemcisi tarafından görülebilir)
- Ziyaret edilen origin’ler, zamanlama, trafik boyutu
- Çerezler, storage (IndexedDB, Cache API, localStorage, serviceWorker), HSTS, sertifika cache’i
- İndirilen dosyalar, yer imleri, geçmiş, açık sekmeler
- Kimlik bilgileri (proxy parolaları, istemci sertifikaları) — OS credential store’da
- Java helper’ın indirdiği filtre listeleri ve imza materyali

## 2) Tehdit aktörleri (ayrı değerlendirildi)

| Aktör | Yeteneği | Calyra’nın hedefi | Sınırı |
|---|---|---|---|
| **Ziyaret edilen site** | 1P script, çerez, fingerprint (canvas, WebGL, font) | 3P cookie varsayılan kapalı, storage partitioning, fingerprint standardizasyonu, izin prompt’u | Site kendi DOM değişimini gözleyebilir; %100 algılanamaz reklam engelleme garantisi yok |
| **3. taraf reklam/izleme ağı** | Çok sitede tracker, link decoration, CNAME cloaking | URL filtre + cosmetic, link decoration stripping, referrer trim, GPC sinyali | Tam CNAME uncloaking DNS erişimi gerektirir; ilk sürümde sınırlı |
| **Yerel ağ gözlemcisi / ISP** | Pasif trafik izleme, DNS sorguları, SNI (ECH yoksa) | HTTPS zorunlu, DoH/Proxy uzak DNS, Tor modunda yalnız Tor yolu | Global pasif gözlemci + zaman korelasyonu Calyra’nın dışındadır |
| **VPN/Proxy işletmecisi** | IP’yi görür, zamanlama bilgisi | Sağlayıcının ne gördüğünü UI’da açık metinle gösterir, no-log iddiası doğrulanmaz | İşletmeciye güven modeli kullanıcıya bırakılır |
| **Kötü amaçlı exit düğümü (Tor)** | Çıkışta düz HTTP/misconfigured TLS’i okuyabilir | Tor’da yalnız SOCKS + fail-closed, HSTS, .onion için DNS sızıntısı yok | Exit düğümü her zaman güvenilmez kabul edilir |
| **Cihazdaki başka süreç / malware** | Bellek/disk okuma, keylogger | Profil dizini 0700, secret’lar OS vault’ta, sandbox renderer | Ele geçirilmiş işletim sistemi dürüstçe kapsam dışı |
| **Kötü amaçlı uzantı** | Güçlü izleyici, webRequest ele geçirme | Güçlendirilmiş profilde varsayılan minimum yetenek, uzantı process izolasyonu | Uzantı desteği bu sürümde kapsam dışı — eklenirse ayrı tehdit analizi |

**Kapsam dışı (dürüstçe yazıldı):** Global pasif gözlemci, trafik zamanlama korelasyonu, işletim sistemi ele geçirilmesi, kullanıcının kendi hesabıyla kimliğini açıklaması, kötü amaçlı uzantı.

## 3) Güvenlik hedeflerinin ayrımı
- **HTTPS içerik gizliliği** ≠ **IP gizleme** ≠ **fingerprint azaltımı** ≠ **disk kalıntılarını azaltma** ≠ **hesaplar arası izolasyon**.
- Her profil açık politika kümesidir; UI toggle’ı yalnızca ikon değiştirmez.

## 4) Profiller

### Normal
- Doğrudan ağ (direct). Sistem proxy’si explicit politika yoksa kullanıcıya gösterilmeden override etmez.
- Reklam filtre aktif, 3P cookie engelli (dar istisna).

### Güçlendirilmiş Gizlilik
- Ek: WebRTC kısıtlı/kapalı (görüntülü konuşma kırılır — kullanıcıya anlatılır), referrer trim, strict fingerprint standardizasyonu, speculative loading kapalı.

### Tor Yönlendirmeli (isteğe bağlı)
- Yalnız tanımlı Tor yolu; Tor hazır değilse sayfa doğrudan çıkmaz (fail-closed).
- Control port üzerinden “Yeni Kimlik” rate-limit’e uyar; farklı exit IP garanti edilmez.
- SOCKS üzerinden Chromium trafiği ≠ tam Tor Browser; fingerprint ve motor değişikliklerinin eksikleri `docs/08-tor-farklari.md`’de belgeli.

**Private Window ≠ anonimlik:** Yalnızca yerel geçmiş/çerez kalıntısını azaltır; IP’yi gizlemez. Mod değişiminde mevcut sekmenin storage’ı sessizce taşınmaz — yeni izole bağlam veya açık yeniden yükleme istenir.

## 5) Gizlilik vs Güvenlik gerilimleri
- Güvenlik güncellemeleri (Safe Browsing, CRL, bileşen güncellemeleri) metadata sızdırabilir; tamamen kapatılmaz, doğrulanabilir ve rate-limitli tasarlanır.
- Phishing/malware koruması için uzak sorgu yapıldığında hangi verinin gittiği ayarlardadır.

## 6) Varsayımlar ve risk kabulü
- Kullanıcı Tor/VPN sağlayıcısının IP gördüğünü anlar.
- Fingerprint test skorları mutlak anonimlik ölçüsü değildir.
- İşletim sistemi swap/SSD wear-leveling, antivirüs, thumbnail cache Calyra kontrolü dışındadır — güvenli silme garantisi yok.
