# CEF Binary Distribution — Lock

**Sürüm:** CEF 143.0.13+g30cb3bd+chromium-143.0.7499.170 (2025-12-20)
**Upstream:** https://bitbucket.org/chromiumembedded/cef — tag `143.0.13`
**Chromium:** https://chromium.googlesource.com/chromium/src.git @30cb3bd
**Mirror (resmi):** https://cef-builds.spotifycdn.com/cef_binary_143.0.13%2Bg30cb3bd%2Bchromium-143.0.7499.170_linux64.tar.bz2
**SHA256:** `PLACEHOLDER — indirme sonrası `sha256sum` ile doldurulacak, SBOM’a yazılacak`
**Gerekli dağıtım dosyaları:** `libcef.so`, `chrome_elf.dll` (Win), `icudtl.dat`, `snapshot_blob.bin`, `v8_context_snapshot.bin`, `locales/*`, `swiftshader/*`, `resources/*`

Bu klasör CI’da indirilir; repo’ya binary commit edilmez (boyut). Yer tutucu olarak `cef_version.h` ve `CHROMIUM_BUILD_COMPATIBILITY.txt` tutulur.

Upstream güvenlik güncelleme yolu: her Chromium stable (~6 hafta) sonrası CEF tag’i kontrol et, bu dosyayı güncelle, `SBOM.cdx.json`’u yenile.

Lisans: BSD-3-Clause (CEF) + Chromium BSD-style. `LICENSES/cef-BSD.txt` ve `LICENSES/chromium-BSD.txt`’e bak.
