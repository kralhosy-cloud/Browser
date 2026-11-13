# Calyra — Supply Chain ve Güncelleme

## Bağımlılık yönetimi

- **Native:** `CMake` + `vcpkg.json` / `conan.lock`, hash pinli. `third_party/cef` SHA256 kilitli.
- **.NET:** `nuget.lock.json` (lock file), `Central Package Management`.
- **Java:** `pom.xml` + `maven-dependency-plugin:go-offline`, checksum.
- **SBOM:** CycloneDX JSON (`SBOM.cdx.json`) — CEF, Avalonia, JUnit, RE2, libcurl.
- **Lisans envanteri:** `LICENSES/` altında BSD-3 (CEF/Chromium), MIT (Avalonia), Apache-2.0 (helper). Dağıtımda `THIRD_PARTY_NOTICES` ile telif bildirimi.

**İnternetten rastgele arşiv indirme yok.** Motor binary kaynağı `https://cef-builds.spotifycdn.com/index.html` (CEF resmi mirror) + `https://bitbucket.org/chromiumembedded/cef` tag; checksum + GPG doğrulaması.

**Upstream duyuruları:** Chromium security advisories (`chromereleases.googleblog.com`) RSS’i, CEF forum, `cef-announce` mail listesi. Her CEF bump için ayrı PR + security label.

## Derleyici hardening

- C/C++: `-Wall -Wextra -Werror -fstack-protector-strong -D_FORTIFY_SOURCE=2 -Wformat -Wformat-security -fPIE -pie -Wl,-z,RELRO -Wl,-z,now`
- Sanitizers: ASAN/UBSAN CI’da, MSAN nightly.
- Statik analiz: `clang-tidy`, `cppcheck`, `spotbugs` (Java), `Roslyn analyzers` (C#).

## Parser fuzz

- C policy codec, IPC framing, filter list parser için `libFuzzer` / `AFL++` hedeşleri: `src/c-policy/test/fuzz_policy`, `src/native-core/test/fuzz_ipc`. Corpus CI’da saklanır.

## Kendi kripto yasağı

Kendi TLS/AES/RNG/parola hash’i yazılmadı. Kullanılanlar: BoringSSL (Chromium içinde), libsodium (helper imza doğrulama), OS RNG.

## Günlükler ve telemetri

- Varsayılan: ziyaret edilen URL/sorgu/çerez/sayfa içeriği saklanmaz. Error log minimal, döndürmeli (5×10 MB).
- Crash report opt-in; bellek dökümü parola sayfa verisi içerebilir — göndermeden içerik gösterilir, secret redaction.
- Telemetri kapalıysa başka adla veri gönderilmez. Ölçüm eklenirse amaç/saklama/silme politikası açık; gizlilik ürünü diye tanımlayıcı üretilmez.

## Otomatik güncelleme (tasarım — ilk sürümde manuel)

- İmzalı manifest (`manifest.json` + Ed25519 detatched sig), artifact SHA256, downgrade/replay koruması (monotonik `versionCode`), atomik kurulum (`rename` + rollback), bozuk güncelleme kurtarma.
- Code signing sertifikası / dağıtım altyapısı yoksa imzalı üretim ürünü varmış gibi davranılmaz — `buildtype=dev` vs `release` ayrı.
- Platform gerekleri:
  - Windows: Authenticode + SmartScreen EV.
  - macOS: Developer ID + notarization + stapled ticket.
  - Linux: apt repo GPG + `debsig-verify`.
- Updater route’u gizlenmez; `calyra://settings/updates`’te görünür.

## Profil ve secret yönetimi

- Profil dizini `~/.config/calyra` (Linux) / `%AppData%\Calyra` (Win) 0700.
- Secret’lar düz JSON’a yazılmaz — OS vault (Win Credential Manager, libsecret, Keychain).
- Export format sürümlemesi + schema validation (JSON Schema v1).
