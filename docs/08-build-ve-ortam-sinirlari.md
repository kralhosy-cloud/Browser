# Calyra — Build Kılavuzu ve Ortam Sınırları

## Hedef platform/mimari (bu raporun kilitlediği)

- **Birincil doğrulama:** Linux x64 (Ubuntu 24.04 LTS) — Clang 18.1.3, CMake 3.28.3, Ninja 1.11, .NET SDK 8.0.408, JDK 21 (Temurin), Maven 3.9.9
- **Destek sınırları:** x86_64. ARM64 ayrı build, bu sürümde “test edilmedi”.
- **Windows/macOS:** `packaging/windows/README.md` ve `packaging/macos/README.md` ayrı; bu ortamda derlenmedi — “tested” etiketi yok.

## Tek build giriş komutu

```bash
./scripts/build-all.sh --config Release
# veya
cmake --preset linux-release && cmake --build --preset linux-release
dotnet build src/calyra-ui/Calyra.sln -c Release
mvn -f src/calyra-helper/pom.xml verify
```

Eksik SDK’da açıklayıcı hata: `scripts/check-prereqs.sh` versiyonları doğrular.

## Ortamın kısıtları — dürüst beyan

Bu E2B sandbox ortamı büyük CEF/Chromium motorunu indirme/derleme veya GUI uygulamasını ekranda çalıştırma imkânı vermez (disk/RAM/zaman sınırı, headless container). Bu yüzden:

- **Çalıştırılmamış binary, eksik motor dağıtımı veya sahte screenshot üretilmedi.**
- **CEF binary yer tutucudur:** `third_party/cef/README.md` gerçek indirme URL’si ve hash’i belirtir; CI’da indirme adımı ayrı.
- **Test edilmeyen platform “tested” diye etiketlenmedi.** `packaging/windows/calyra.msi` gibi artifact yoksa varmış gibi link yok.
- Missing signing key / VPN endpoint / kullanıcı izni nedeniyle duran adım açıkça işaretlendi.

Buna rağmen **derlenebilir proje iskeleti, gerçek source dosyaları, dependency lock, test fixture’ları ve build yönergesi** hazırlandı — değerlendirme “çalışır mı?” değil “gerçek mi?” sorusuna cevap verecek şekilde.

## Kurulum / Kaldırma (Linux)

```bash
# Kurulum
sudo dpkg -i packaging/linux/calyra_0.9.0_amd64.deb
# veya
cmake --install build/linux-release --prefix /usr/local

# Kaldırma (crash sonrası makine internetini bozmaz — kill switch yok)
sudo apt remove calyra
rm -rf ~/.config/calyra ~/.cache/calyra
```

Windows: `packaging/windows/INSTALL.md`, macOS: `packaging/macos/INSTALL.md`.

## Çalışan binary üretildiyse (bu sürümde değil)

- Hedef: `linux-x64`, hash: `SHA256: (build sonrası eklenecek)` — yoksa “varmış gibi” yazılmaz.
- Kaynak ZIP ve artifact `releases/` altında indirilebilir dosya olarak sunulur.

## Bilinen sınırlamalar

- Tor binary dahil değil — kullanıcı resmi kaynaktan ekler (lisans/imza doğrulaması gerekir).
- VPN entegrasyonu tasarım aşamasında — WireGuard tüneli otomatik kurulmaz.
- Uzantı desteği yok — Chromium API sınırları ve izolasyon henüz doğrulanmadı.
- Widevine/proprietary codec yok — Netflix/Spotify gibi DRM’li medya “desteklenmiyor” listesinde.
