# Calyra — SOCKS Üzerinden Chromium ≠ Tor Browser

Calyra Tor modunda resmi Tor binary’sini SOCKS5 proxy olarak kullanır. Bu **tam Tor Browser eşdeğeri değildir**. Eksikler açıkça belgelendi:

| Alan | Tor Browser (Firefox ESR tabanlı) | Calyra + CEF üzerinden Tor |
|---|---|---|
| Fingerprint direnç | Letterboxing, font kısıtlama, canvas/WebGL noise, timezone spoofing motor seviyesinde | CEF/Chromium’da aynı savunmalar yok; JS injection tam telafi değil |
| First-party isolation | FPI + state partitioning | Chromium Storage Partitioning var ama FPI eşdeğeri değil |
| Yazı tipi | Paketli font seti | Sistem fontları — ayırt edilebilirlik artar |
| Güncellemeler | Tor Project imzalı, otomatik | Calyra CEF bump + Tor binary ayrı güncellenir |
| Anonimlik kümesi | Büyük, homojen | Küçük, Calyra profili anonimlik kümesini büyütmez; benzersiz profil ayırt edilebilir kılar |

Kullanıcıya “Tor hazır” durumunda bu farklar `calyra://tor` sayfasında gösterilir.
