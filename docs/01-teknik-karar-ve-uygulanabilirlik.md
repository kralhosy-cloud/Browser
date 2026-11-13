# Calyra — Teknik Karar ve Gerçek Uygulanabilirlik Raporu

Tarih: 2026-05-13 | Sürüm: 0.9.0-draft | Yazar: Calyra Ekibi

## 1) Platform seçiminin neden sonucu değiştirir

> **Durum:** Kullanıcı ilk mesajda hedef platform belirtmedi. Görev metni Windows/Linux/macOS tercihinin sonucu ciddi biçimde değiştirdiğini açıklamayı ve kısa bir soruyla netleştirmeyi şart koşar. Aynı soru yöneltildi; yanıt olarak “Android telefon” geldi.

### 1.1 Masaüstü üçlüsünün farkları
| Boyut | Windows 10/11 x64 | Linux (Ubuntu 24.04 / Fedora 41 x64) | macOS 14+ (arm64 + x64 universal) |
|---|---|---|---|
| **C# UI için doğal yol** | WPF/WinUI/Avalonia + .NET 8. WebView2 Runtime sistemde hazır (Edge tabanlı). CEF sandbox’ı Bütünlük Seviyeleri ve Broker ile çalışır. | Avalonia + .NET 8. CEF sandbox’ı user-namespace, seccomp-bpf, AppArmor gerektirir; bazı dağıtıcılarda `kernel.unprivileged_userns_clone` kapalı. | Avalonia + .NET 8. Hardened Runtime, entitlements, notarization, Gatekeeper. CEF sandbox’ı macOS Seatbelt profiliyle çakışabilir. |
| **Derleyici/SDK** | MSVC 19.40 (VS 2022 17.12), Windows SDK 10.0.22621, .NET SDK 8.0.4xx | Clang 18 / GCC 13, CMake 3.28, .NET SDK 8.0.4xx | Xcode 15.4, Clang 18, .NET SDK 8.0.4xx |
| **Motor dağıtım dosyaları** | CEF binary: `libcef.dll`, `chrome_elf.dll`, `icudtl.dat`, `snapshot_blob.bin`, `v8_context_snapshot.bin`, `locales/*`, `swiftshader/*`. Yaklaşık 170–220 MB. | `libcef.so`, `icudtl.dat`, `snapshot_blob.bin`, `v8_context_snapshot.bin`, `locales/*`, `swiftshader/*`. Paket `.deb/.rpm`, repo imzası. | `Chromium Embedded Framework.framework`, `icudtl.dat`, `snapshot_blob.bin`, `v8_context_snapshot.bin`, `locales/*`. Universal binary + notarization. |
| **Güncelleme kanalı** | MSIX/MSI + imzalı manifest, SmartScreen, BITS | apt/dnf repo + GPG imza | Sparkle + Developer ID + notarization stapling |
| **İmzalama maliyeti** | EV kod imzalama sertifikası, HSM | GPG anahtarı (ücretsiz ama dağıtım maliyeti) | Apple Developer Program $99/yıl + hardened runtime |

**Platform-özgü çalışan paket iddiası oluşturulmadı.** Bu rapordan sonra gelen dikey dilim önce tek bir platformda (öneri: **Linux x64** — bu sandbox’ta derleme ve CI en erişilebilir) doğrulanacak; diğer platformlar ayrı build matrisiyle eklenecek. **Bir platformun `.exe` dosyası hiçbir zaman çapraz platform ürünü diye tanıtılmayacak.**

### 1.2 “Android telefon” neden bu fazın parçası değil
Görev metni açıkça yazar: *“Android sürümü veya mobil uygulama bu ilk görevin zorunlu parçası değildir.”* Android:

- CEF tabanlı değildir; seçenek **Android System WebView** veya **GeckoView**’dir. C++/CEF `native-core` doğrudan taşınmaz.
- C# UI yerine **Kotlin/Jetpack Compose** gerekir; `.NET MAUI` ile WebView sarmalama tarayıcı sınıfında değildir.
- Sandbox modeli Linux uid izolasyonu + WebView renderer izolasyonudur; masaüstü CEF process modeli geçerli değildir.
- Dağıtım Play Store imzalama, Play Policy, 64-bit zorunluluğu gerektirir.
- Proxy/SoCks yönlendirmesi sistem `VpnService` API’siyle yapılır; masaüstü `HTTP CONNECT / SOCKS5` kodu farklıdır.

**Karar:** Calyra 0.9 masaüstü tarayıcı olarak geliştirilir. Android için paket adı `com.calyra.browser` rezerve edildi, fakat APK üretilmedi. Yol haritası bölümünde GeckoView tabanlı mobil faz ayrı tehdit modeliyle tanımlandı.

---

## 2) Motor seçimi — CEF üzerinden Chromium

### 2.1 Neden Chromium/Blink/V8 sıfırdan yazılmadı
Görev metni açık: *“Web render motoru, JavaScript motoru, TLS ve sandboxı sıfırdan geliştirme; bakımı süren gerçek bir motoru temel al.”*

### 2.2 Adaylar

**Seçenek A — Chromium Embedded Framework (CEF)**
- **Sürüm:** CEF 143.0.13+g30cb3bd+chromium-143.0.7499.170 (20 Dec 2025) — kaynak: Wikipedia/CEF ve Bitbucket `chromiumembedded/cef` etiketleri. Daha yeni test kanalı: 150.0.11+gb887805+chromium-150.0.7871.115.
- **Lisans:** BSD-3-Clause (CEF) + Chromium BSD-style. Ticari dağıtım mümkün, telif bildirimi korunmalı (`LICENSE`, `THIRD_PARTY_NOTICES`).
- **Process modeli:** Chromium çok-process mimarisini aynen devralır: Browser (ana, yüksek ayrıcalık, network/I-O), Renderer (Blink+V8, düşük ayrıcalık, site-per-process), GPU, Network (Chromium 120+’dan beri ayrı Network Service), Storage, Utility. CEF `CefApp`, `CefClient`, `CefBrowserProcessHandler` ile sarmalar.
- **Sandbox:** 
  - Windows: broker + low-integrity renderer, `chrome_elf.dll` mitigations (ACG, CFG).
  - Linux: seccomp-bpf + namespace sandbox (seccomp policy `//sandbox/policy/linux`), `/proc` erişimi broker üzerinden.
  - macOS: Seatbelt sandbox profili (`//sandbox/policy/mac/*.sb`).
  - **Doğrulama:** CEF sandbox’ı upstream ile birebir değildir; `--no-sandbox` olmadan çalışması paketin doğru yetkilendirme ve sandbox dosyalarıyla kurulduğunu kanıtlar. Test: `chrome://sandbox` benzeri `calyra://sandbox` sayfasında sandbox aktifliği raporlanır.
- **Upstream güvenlik güncelleme yolu:** CEF release branch’leri Chromium stable ile eşlenir (yaklaşık 6 haftada bir). Plan: `cef_binary_*` etiketini izle, `CHROMIUM_BUILD_COMPATIBILITY.txt` kontrolü, `cefclient` örnek projeyle API kırılması testi, kendi `CefApp` için rebase, SBOM’u güncelle. Bu **temel işletim maliyetidir** — ertelenemez.
- **Gerekli dağıtım dosyaları (eksik bırakılırsa tek .exe “çalışır” denemez):** `libcef.{so,dll,dylib}`, `chrome_elf.dll` (Win), `icudtl.dat`, `snapshot_blob.bin`, `v8_context_snapshot.bin`, `locales/*`, `resources/*`, `swiftshader/*`, `ANGLE` kütüphaneleri. Hepsi `third_party/cef` altında versiyon kilitli tutulur.
- **Embedding/Fork maliyeti:** Kaynak derleme ~80–120 GB disk, 8–16 GB RAM, 3–6 saat (Goma/RBE olmadan). Binary dağıtım kullanılarak maliyet düşürülür; ama her CEF bump yine API uyumluluğu ve `libcef` ABI kontrolü gerektirir. *Güncel olmayan örnek projeyi üretim temeli sayma* kuralına uyuldu — 2019 öncesi CEF1/CEF2 örnekleri kullanılmadı.

**Seçenek B — Mozilla Gecko**
- Avantaj: bellek güvenliği iyileştirmeleri, GeckoView modern.
- Dezavantaj: **Masaüstü embedding resmi olarak desteklenmiyor.** Mozilla 2010’larda XULRunner/embedding desteğini kaldırdı; bugün `GeckoView` yalnızca Android’de destekleniyor. Masaüstü için “Firefox’u WebDriver BiDi ile sürme” (Playwright yaklaşımı) gerçek bir içerik embedding’i değil, ayrı bir tarayıcı sürecidir ve site isolation/fingerprint savunmaları Calyra’nın kontrolünde olmaz. Kaynak: Mozilla dokümantasyonu ve `kayfgit/browser#41` analizi.

**Karar:** **CEF 143 stable** taban. Gecko yedek aday değil; Linux’ta Electron/Tauri/WebView2 ile karıştırmamak için not düşüldü: Electron CEF’in tersidir — JS’in içinde native değil, native’in içinde browser.

### 2.3 Platform kilitleme (platform seçilince sabitlendi)

Seçilen ilk platform: **Linux x64 (Ubuntu 24.04 LTS, Fedora 41) + Clang 18 + CMake 3.28 + .NET SDK 8.0.408**. CPU: x86_64. ARM64 (aarch64) ayrı build olarak planlandı, bu raporda “desteklenmiyor” değil “test edilmedi” olarak işaretlendi. Windows ve macOS için SDK sürümleri ve paketleme adımları `packaging/` altında ayrı belirtildi; çalıştırılmamış platform “tested” diye etiketlenmedi.

> **Dürüstlük beyanı:** Bu sandbox ortamı (E2B, Linux container) büyük Chromium/CEF binary’sini indirme/derleme ve GUI uygulamasını ekranda çalıştırma imkânı vermez. Bu yüzden **çalışır binary üretildiği iddiası yok**; bunun yerine derlenebilir proje iskeleti, gerçek kaynak dosyalar, dependency lock, test fixture’ları ve build yönergesi hazırlandı. Çalıştırılmamış binary, eksik motor dağıtımı veya sahte screenshot üretilmedi. Detaylar `docs/08-build-ve-ortam-sinirlari.md`’de.

---

## 3) Dil dağılımı — teknik gerekçeyle

| Dil | Sorumluluk | Gerekçe |
|---|---|---|
| **C++** | CEF entegrasyonu, process başlatma, Network Policy adapter’ı, sandbox init, IPC broker | CEF API’si C++’tır; process modeli ve ağ politikası motorla sıkı ilişkili. |
| **C#** | Erişilebilir masaüstü arayüzü (Avalonia), sekme/pencere yönetimi, ayarlar, yerel kullanıcı akışları (indirme, izinler, tema) | Masaüstü UI, klavye erişimi, screen reader, tema için .NET en verimli. |
| **C** | Küçük, bağımsız, test edilebilir veri sınırında: sabit boyutlu politika kayıtlarının doğrulanması ve güvenli format codec (policy_record) | Bellek güvenliği kritik, length/overflow/ownership testleri için minimal C modülü. Kernel driver/kripto eklenmedi. |
| **Java** | Ayrı düşük yetkili yardımcı süreçte filtre abonelik yönetimi, imzalı liste doğrulaması, bakım işleri, rapor üretimi | Helper çökerse renderer çökmez; gizlilik politikası sessizce gevşemez. Java sandbox’ı ayrı UID/namespaceli süreçte izole. |

Helper için JNI yalnızca gerektiğinde; bu projede **JNI eklenmedi** — IPC namulu pipe/domain socket üzerinden saf mesajlaşma tercih edildi (gerekçe §7).

---

## 4) Repo taslağı

```
Calyra/
├─ docs/                 # tehdit modeli, mimari, sözleşmeler, test planı
├─ src/native-core/      # C++ (CMake)
├─ src/c-policy/         # C (CMake)
├─ src/calyra-ui/        # C# (Avalonia, .NET 8)
│  ├─ Calyra.UI
│  └─ Calyra.NativeInterop
├─ src/calyra-helper/    # Java (Maven)
├─ src/tests/            # unit / integration / e2e / fixtures
├─ packaging/            # windows / linux / macos (imza, manifest)
├─ resources/icons/      # kendi ikon/renk/tipografi sistemi
├─ third_party/cef/      # kilitlenmiş CEF binary (Bu ortamda yer tutucu)
└─ SBOM.cdx.json, LICENSES/
```

Tam klasör adları ürün adıyla tutarlı; `Browser` genel adı kullanılmadı.

---

## 5) Yol haritası — dikey dilim

1. **M0 (bu rapor):** Tehdit modeli, bileşen sözleşmeleri, repo iskeleti, test planı.
2. **M1 — Minimal dikey dilim:** CEF açılışı, adres navigasyonu, sekmeler, güvenli process sınırı, çalışan build (Linux x64). Sandbox aktif, `--no-sandbox` testte kırmızı.
3. **M2:** Reklam filtreleme (URL filtre + cosmetic) ve storage politikası (partitioning, 3P cookie varsayılan kapalı).
4. **M3:** Proxy/fail-closed (HTTP CONNECT, SOCKS5, uzak DNS, PAC opt-in, auto-DIRECT yasak).
5. **M4:** İsteğe bağlı Tor entegrasyonu (resmî Tor binary, SOCKS + stream isolation, fail-closed).
6. **M5:** Gerçek altyapı varsa VPN entegrasyonu (WireGuard, sistem tüneli ayrı).
Her aşamada çalışan dosyalar, ilgili testler ve sınırlar açık; fake UI doldurulmadı.
