# Android — Yerel APK Build Kılavuzu (Çalışma Ortamı Kısıtı Nedeniyle Yerelde Yapılır)

**Neden burada APK üretilemedi?** Bu E2B sandbox’ta giden internet yalnızca `github.com`, `files.pythonhosted.org` vb. ile sınırlı; `dl.google.com` (Android SDK) ve `deb.debian.org` (JDK apt) engelli. `curl https://dl.google.com` → `SSL_ERROR_SYSCALL`. Bu yüzden **çalıştırılmamış binary / sahte APK üretilmedi** — dürüst beyan `docs/08-build-ve-ortam-sinirlari.md`.

## Kendi makinenizde APK

```bash
# 1) Ön koşullar (Ubuntu 24.04 örnek)
sudo apt update && sudo apt install -y openjdk-21-jdk wget unzip

# 2) Android SDK cmdline-tools
mkdir -p ~/Android/Sdk/cmdline-tools && cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*_latest.zip
mv cmdline-tools latest
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 3) Platform + NDK + Build-Tools
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;26.1.10909125"

# 4) Build
cd /path/to/Browser/src/calyra-android
./gradlew assembleDebug   # Java 21 gerektirir
# Çıktı: app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Alternatif: Android Studio
- Android Studio Hedgehog+ ile `src/calyra-android`’yi aç, SDK Manager’dan API 34 + NDK 26.1 kur, Run.

## Doğrulama
- APK hash: `sha256sum app/build/outputs/apk/debug/app-debug.apk` → kendi makinenizde üretin, bu dokümanda sahte hash yok.
- İmzalama: debug keystore otomatik; release için `packaging/android/signing.md`’deki (ayrıca oluşturulacak) kendi keystore’unuzla imzalayın — üretim sertifikası yoksa varmış gibi davranılmaz.

