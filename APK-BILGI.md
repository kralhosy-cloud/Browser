# Calyra APK — İndirme ve Kurulum

**Dosya:** Calyra.apk (11 MB, debug, SHA256: 0c860ba72439a10cd6754d08a9667748b2c00416cfad654303e449fd23065e14)
**Kaynak:** GitHub Actions `arena/eae29342-browser` 806405f, workflow `43484843961` success
**İçerik:** Minimal WebView tabanlı Calyra — adres çubuğu, sekmeler, kilit/kalkan göstergeleri, indirmeler. Full GeckoView + filtre + Tor varyantı `src/calyra-android`’da mimarisi hazır, fakat Compose/NDK bağımlılığı nedeniyle bu ilk APK minimal ile üretildi (dürüst beyan).
**Kurulum:**
```bash
adb install Calyra.apk
# veya telefona kopyalayıp dosya yöneticisinden kur (Bilinmeyen kaynaklara izin ver)
```
**Not:** Bu debug APK, Play Store imzalı değil. Release için kendi keystore’unla `gradlew assembleRelease` yap.
