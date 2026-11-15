# 🚀 NayaGram Android (Open-Source Build Guide)

This guide explains how open-source contributors, security researchers, and reviewers can build **NayaGram Android** (`org.nayagram.platform`, currently synced at `v4.26.75 NG` / Build `4097`) from `NayaGramPlatform/NayaGram-Client`.

---

## 🛠️ 1. System Requirements & Prerequisites
* **Operating System:** Linux, macOS, or Windows (WSL2 recommended for Windows)
* **JDK:** OpenJDK 17 or Temurin JDK 17 (`JAVA_HOME` set to JDK 17)
* **Android Studio:** Android Studio 2024.2+ (Ladybug / Meerkat / 2025.1+)
* **Android SDK & NDK:**
  - Android SDK Platform `36`
  - Android SDK Build-Tools: `36.0.0`
  - CMake: `3.22.1`
  - Android NDK: `27.2.12479018`

---

## 📥 2. Cloning the Repository (Submodules Required)
NayaGram relies on native C++ libraries (BoringSSL, FFmpeg, Libvpx, WebRTC, tgcalls, tdlib, Media3). Clone with `--recursive`:

```bash
git clone --recursive https://github.com/NayaGramPlatform/NayaGram-Client.git
cd NayaGram-Client
```

*(If you already cloned without submodules, run:)*
```bash
git submodule sync --recursive
git submodule update --init --recursive
```

---

## ⚙️ 3. Environment & Credentials Configuration
For security, no private API keys, Firebase credentials, or signing keystores are stored in this repository.

1. **Create `local.properties`** in the project root:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ndk.dir=/path/to/your/Android/Sdk/ndk/27.2.12479018
   ```
2. **Supply Telegram API Credentials (`my.telegram.org`):**
   `TMessagesProj/build.gradle` reads `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` from environment variables or Gradle `-P` properties:
   ```bash
   export TELEGRAM_API_ID=YOUR_API_ID
   export TELEGRAM_API_HASH=YOUR_API_HASH
   ```
3. **Firebase & Maps Placeholders:**
   - Public placeholder `google-services.json` files are included so Gradle configures cleanly out of the box. Replace them with your own Firebase project config for FCM push notifications.
   - Replace `YOUR_GOOGLE_MAPS_API_KEY` in `TMessagesProj/config/*/AndroidManifest*.xml` with your own Maps key if needed.
4. **Signing Keystore (for Release builds):**
   Provide your keystore via environment variables or `-P` flags:
   ```bash
   export RELEASE_KEYSTORE_PATH=/path/to/your-release.jks
   export RELEASE_STORE_PASSWORD=your_store_password
   export RELEASE_KEY_PASSWORD=your_key_password
   export RELEASE_KEY_ALIAS=your_key_alias
   ```

---

## 🔨 4. Building via Command Line

### A. Build Universal Release APK:
```bash
./gradlew :TMessagesProj_App:assembleAfatRelease
```

### B. Build Google Play Release AAB Bundle:
```bash
./gradlew :TMessagesProj_App:bundleBundleAfat_SDK23Release
```

---

## 💻 5. Opening in Android Studio
1. Open Android Studio.
2. Select **"Open"** (do **NOT** choose "Import").
3. Select the `NayaGram-Client` root folder.
4. Wait for Gradle Sync to complete (ensure `TELEGRAM_API_ID` & `TELEGRAM_API_HASH` are set in environment or `gradle.properties` locally without committing them).
5. In the Build Variants tool window, select module `:TMessagesProj_App` and variant `afatDebug` or `afatRelease`.
