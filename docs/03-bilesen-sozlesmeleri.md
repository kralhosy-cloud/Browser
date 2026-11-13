# Calyra — Bileşen Sözleşmeleri ve Dil Sınırları

Versiyon: 1.0 | Tarih: 2026-05-13

Bu belge C/C++ ↔ .NET ve Java helper ↔ native arasındaki tüm dar, sürümlenmiş, açık sözleşmeleri tanımlar. Native pointer gelişigüzel managed tarafa taşınmaz.

## 1) C/C++ ↔ .NET ABI sözleşmesi (v1)

**Kaynak:** `src/native-core/include/calyra/ipc.h` ve `src/c-policy/include/calyra/policy_record.h`

| Konu | Karar |
|---|---|
| **ABI** | C ABI üzerinden `extern "C"`; C++ sınıfları doğrudan expose edilmez. |
| **Calling convention** | Windows: `__stdcall` değil, `__cdecl` (x64’te tek convention). Linux/macOS: System V AMD64. `CALYRA_CALL` makrosuyla sabitlendi. |
| **Integer width** | `<stdint.h>` sabit genişlik: `uint32_t`, `int32_t`, `uint64_t`. `int`/`long` kullanılmaz. |
| **Unicode** | UTF-8 (`char*` + `size_t len` + NUL terminatör). UTF-16 (Windows wchar) sınırı geçerken dönüştürülür; .NET tarafı `Marshal.PtrToStringUTF8`. |
| **Buffer length** | Her pointer yanında `size_t len` ve `size_t capacity`. Sınır dışı yazma yok; `CALYRA_E_OVERFLOW` döner. |
| **Ownership** | Çağrılan taraf allocate ederse `calyra_free()` ile serbest bırakılır. .NET `SafeHandle` kullanır. |
| **Lifetime** | Opaque handle (`CalyraEngine*`, `CalyraTab*`) `create`/`destroy` çiftinde yaşar. Double-free `CALYRA_E_USE_AFTER_FREE`. |
| **Hata kodları** | `CalyraStatus` enum: `OK=0`, `INVALID_ARG=1`, `OVERFLOW=2`, `NOT_FOUND=3`, `DENIED=4`, `TIMEOUT=5`, `INTERNAL=6`. Negatif değer yok. |
| **Sürümleme** | `CALYRA_ABI_VERSION = 1`. Major bump’ta eski .NET binding reddedilir. |

**Yasaklar:** `IntPtr` ile rastgele native pointer taşıma, `GCHandle` ile pinlenmiş buffer’ı C’ye ödünç verme, CLI argümanında parola/token taşıma.

### Örnek C ABI (özet)
```c
#define CALYRA_CALL
#define CALYRA_ABI_VERSION 1

typedef enum { CALYRA_OK=0, CALYRA_E_INVALID_ARG=1, CALYRA_E_OVERFLOW=2, CALYRA_E_DENIED=4 } CalyraStatus;
typedef struct CalyraEngine CalyraEngine;

CalyraStatus CALYRA_CALL calyra_engine_create(const CalyraEngineConfig* cfg, CalyraEngine** out);
CalyraStatus CALYRA_CALL calyra_engine_navigate(CalyraEngine* e, const char* url_utf8, size_t len);
CalyraStatus CALYRA_CALL calyra_engine_destroy(CalyraEngine* e);
void         CALYRA_CALL calyra_free(void* p);
```

## 2) C politika modülü — sabit boyutlu kayıt

**Dosya:** `src/c-policy/include/calyra/policy_record.h`

- `CalyraPolicyRecord` 64 byte sabit boyut, `__attribute__((packed))` yok, doğal hizalama.
- `magic[4] = "CLRA"`, `version=1`, `route` enum (DIRECT=0, PROXY=1, TOR=2), `flags` bitmask, `domain_hash` FNV-1a 32-bit.
- Codec `calyra_policy_encode/decode` length/overflow/ownership testleri zorunlu; fuzz testi `src/c-policy/test/fuzz_parser.cc`.
- Kernel driver/kripto yok; yalnızca validasyon.

## 3) Java helper ↔ Native IPC

**Taşıma:** 
- Windows: Named Pipe `\\.\pipe\calyra-helper-{uid}-{rand}` (kullanıcı kapsamlı, `SECURITY_ATTRIBUTES` DACL)
- Linux/macOS: Unix Domain Socket `$XDG_RUNTIME_DIR/calyra/helper-{uid}.sock` (0700 dizin, 0600 socket)

**Neden TCP değil:** `localhost TCP` kimlik doğrulamasız açılamaz — portu başka kullanıcı dinleyebilir. User-scoped pipe/socket + SO_PEERCRED / Token doğrulaması.

| Özellik | Değer |
|---|---|
| **Handshake** | HELLO (v1, pid, nonce) → CHALLENGE (HMAC-SHA256, 32 byte) → HELLO_ACK. Başarısızda pipe kapatılır. |
| **Mesaj boyutu** | Header 16 byte: `magic, version, type, length`. Body ≤ 1 MiB. Büyükse `CALYRA_E_OVERFLOW`. |
| **İzinler** | Pipe/socket yalnızca aynı UID. `FileOwner` kontrolü. |
| **Cancellation** | Her request’ta `request_id` + 30s timeout + client-side cancel frame. |
| **Yetki** | Helper düşük yetkili (`nice`, `no-new-privs`, seccomp). Renderer asla helper’a doğrudan konuşmaz. Web sayfası → helper bridge’i **yasak**. |

**JVM argümanlarında parola yok;** proxy parolaları OS vault’ta (Windows Credential Manager, libsecret, Keychain). IPC’de parolalar `mlock` + zeroize ile taşınır, log’a yazılmaz.

## 4) JNI/FFI kuralı
- **Bu projede JNI yok.** Java helper saf IPC ile konuşur. JNI yalnızca performans ölçümüyle gerekçelendirilirse eklenir; şu an FFI “dil kullanmak için dil” olur.
- Native pointer Java’ya `long` olarak taşınmaz.

## 5) IPC şemaları (ortak)

`schemas/ipc-v1.json` (JSON Schema) ve `schemas/filter-subscription-v1.json`:

- Tüm mesajlar `version:1`, `type` enum, `request_id: uint32`.
- `UpdateFiltersRequest { source_url, if_etag, if_modified_since }`
- `FilterList { version, hash_sha256, rule_count, rules[] }`
- `ReportRequest { since, anonymized: true }` — URL’ler asla düz gitmez.

## 6) Web → Native bridge yasağı
Web içeriği `window.calyra.*`, `external.*`, `chrome.*` priv bridge’lerine erişemez. Adres çubuğunu kontrol eden native bridge web origin’lerine kapalıdır. Test: `tests/integration/bridge_isolation.test.js` — sayfadan `fetch("pipe://...")` reddedilir.
