# Windows Paketleme

- Format: MSIX/MSI (WiX Toolset)
- MSVC 2022 redist gereklidir (CEF 150+ için zorunlu).
- İmzalama: EV Kod İmzalama sertifikası + HSM, `signtool sign /fd SHA256`.
- CEF DLL’leri exe yanına: `libcef.dll`, `chrome_elf.dll`, `icudtl.dat` vb.
- Bu ortamda build edilmedi — artifact yoksa varmış gibi link yok.
