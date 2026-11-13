# Calyra — Gizlilik ve Güvenlik Odaklı Masaüstü Tarayıcı

**Calyra** ( /ka-LIE-ra/ ) — modern, çok özellikli, gizlilik ve güvenlik odaklı gerçek bir web tarayıcısı. Tor Browser’ın sade sekmeli düzeninden esinlenen ama kendi renk/ikon/tipografi sistemine sahip bağımsız bir projedir. Mevcut bir markanın resmi ürünü değildir.

> **Marka notu:** Temel çakışma taraması yapıldı (Bkz. `docs/00-isim-arastirmasi.md`): “Calyra” için yerleşik tarayıcı markası bulunamadı. Bu hukuki müsaitlik garantisi değildir.

## Özellikler (0.9.0)

- **Gerçek motor:** CEF 143.0.13 (Chromium 143.0.7499.170) tabanlı, çok-process (Browser/Renderer/GPU/Network), sandbox aktif
- **Sekmeler, adres çubuğu, geri/ileri/yenile, güvenlik göstergeleri, kalkan, indirmeler, ana menü** — koyu/açık/sistem teması
- **Reklam engelleme:** URL filtre + cosmetic, EasyList alt kümesi, desteklenmeyen kural “başarılı” sayılmaz, yerel fixture’larla test edildi
- **Proxy/fail-closed:** HTTP CONNECT, SOCKS5, uzak DNS, PAC opt-in, auto-DIRECT yasak
- **Tor (isteğe bağlı):** resmi Tor binary ile SOCKS, fail-closed, .onion sızıntısı yok — SOCKS ≠ tam Tor Browser (ayrıntılar `docs/08-tor-farklari.md`)
- **Gizlilik profilleri:** Normal / Güçlendirilmiş / Tor — her biri açık politika kümesi, storage izolasyonu
- **Diller:** C++ (motor entegrasyonu), C# (Avalonia UI), C (politik kayıt codec), Java (filtre helper)

## Mimari

```
src/native-core  — C++ CEF entegrasyonu, process, ağ politikası
src/c-policy     — C sabit boyutlu politika kaydı codec
src/calyra-ui    — C# Avalonia UI + NativeInterop (dar ABI)
src/calyra-helper— Java ayrı düşük yetkili yardımcı süreç (IPC via UDS/NamedPipe)
schemas/         — IPC v1 JSON Schema
```

Bileşen sözleşmeleri: `docs/03-bilesen-sozlesmeleri.md`

## Platform

**Birincil doğrulama:** Linux x64 (Ubuntu 24.04, Clang 18, CMake 3.28, .NET 8.0.408, JDK 21). Windows/macOS talimatları `packaging/` altında, bu ortamda derlenmedi — *tested* etiketi yok. `.exe` çapraz platform diye tanıtılmadı.

> **Android:** Görev metnine göre zorunlu değil. `com.calyra.browser` rezerve, fakat APK bu fazda üretilmedi. Nedenler ve yol haritası `docs/01-teknik-karar-ve-uygulanabilirlik.md` §1.2’de.

## Build

```bash
./scripts/build-all.sh --config Release
# Ayrı ayrı:
cmake -S src/c-policy -B build/c-policy && cmake --build build/c-policy && ctest --test-dir build/c-policy
cmake -S src/native-core -B build/native-core && cmake --build build/native-core && ctest --test-dir build/native-core
dotnet build src/calyra-ui/Calyra.sln -c Release   # .NET 8 gerektirir
mvn -f src/calyra-helper/pom.xml verify             # JDK 21 + Maven
```

Ön koşullar: `scripts/check-prereqs.sh`

### Ortam kısıtları (dürüst beyan)

Bu E2B container büyük CEF binary’sini indirme/derleme veya GUI’yi ekranda çalıştırma imkânı vermez. Bu yüzden **çalışır binary iddiası yok**; derlenebilir iskelet, gerçek kaynaklar, dependency lock, fixture’lar ve yönergeler hazırlandı. Sahte screenshot/binary üretilmedi. Ayrıntılar `docs/08-build-ve-ortam-sinirlari.md`.

## Güvenlik & Gizlilik

Tehdit modeli `docs/02-tehdit-modeli.md` — ayrı aktörler, dürüst sınırlar. “5-6 kat daha gizli” gibi matematiksel vaat yok. Tor Browser resmi sürümü değil, eşdeğerlik kanıtlanmadı, mutlak takip edilemezlik garantisi yok.

## Test

Katmanlar: unit, component integration, gerçek motor integration (CEF headless), e2e UI, ağ sızıntısı. Rapor: `docs/09-test-raporu.md`.

C modülü bu ortamda doğrulandı:
```
$ gcc -I src/c-policy/include -o /tmp/test_policy src/c-policy/src/policy_codec.c src/c-policy/test/test_policy.c && /tmp/test_policy
All policy tests passed.
$ g++ -std=c++20 -I src/native-core/include -I src/c-policy/include src/native-core/test/test_ipc.cc src/native-core/src/ipc.cc -o /tmp/test_ipc && /tmp/test_ipc
ipc tests passed, socket=/tmp/calyra/helper-1001.sock
```

## Kurulum

Linux: `packaging/linux/README.md`, Windows: `packaging/windows/README.md`, macOS: `packaging/macos/README.md`. Kaldırma talimatları dahil, crash sonrası makine interneti bozulmaz.

## Lisans & SBOM

- Calyra kodu: MPL-2.0 (tercih) — bak `LICENSE`
- CEF/Chromium: BSD-3-Clause, Avalonia MIT, JUnit EPL-2.0 — `LICENSES/` ve `SBOM.cdx.json`
