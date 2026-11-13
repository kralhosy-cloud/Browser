# Linux Paketleme

- Format: `.deb` (Ubuntu/Debian) ve `.rpm` (Fedora)
- CEF dosyaları `/usr/lib/calyra/` altına, binary `/usr/bin/calyra`
- Repo GPG imzası: `apt` için `deb [signed-by=/usr/share/keyrings/calyra.gpg] https://repo.calyra.example stable main`
- Sandbox: `kernel.unprivileged_userns_clone=1` gereklidir — `postinst` kontrol eder, değilse uyarır.
- Profil: `~/.config/calyra/`, izin 0700
- Kaldırma: `sudo apt remove calyra && rm -rf ~/.config/calyra`
