# macOS Paketleme

- Format: `.dmg` + `Calyra.app` universal binary (x64+arm64)
- Notarization: `xcrun notarytool`, stapling, hardened runtime `com.apple.security.cs.allow-unsigned-executable-memory` (CEF V8 için gerekli)
- Sandbox: Seatbelt + CEF sandbox uyumu entitlements’de belirtilir.
- Bu ortamda build edilmedi.
