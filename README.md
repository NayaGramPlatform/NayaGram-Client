𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦 
# 💎 NayaGram Messenger for Android (Open-Source Client)

<p align="center">
  <img src="https://i.postimg.cc/65K5k2WS/%F0%9D%90%8D%F0%9D%90%9A%F0%9D%90%B2%F0%9D%90%9A-%F0%9D%90%86%F0%9D%90%AB%F0%9D%90%9A%F0%9D%90%A6-%F0%9D%90%8F%F0%9D%90%A5%F0%9D%90%9A%F0%9D%90%AD%F0%9D%90%9F%F0%9D%90%A8%F0%9D%90%AB%F0%9D%90%A6.png" width="160" alt="𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦 Logo" />
</p>

<p align="center">
  <a href="https://t.me/NayaGramPro"><img src="https://img.shields.io/badge/Telegram-@NayaGramPro-2CA5E0?style=flat-square&logo=telegram&logoColor=white" alt="Telegram"/></a>
  <a href="https://x.com/NayaGramPro"><img src="https://img.shields.io/badge/Twitter%20%2F%20X-@NayaGramPro-000000?style=flat-square&logo=x&logoColor=white" alt="Twitter"/></a>
  <a href="https://youtube.com/@NayaGramPro"><img src="https://img.shields.io/badge/YouTube-@NayaGramPro-FF0000?style=flat-square&logo=youtube&logoColor=white" alt="YouTube"/></a>
  <a href="https://facebook.com/NayaGramPro"><img src="https://img.shields.io/badge/Facebook-@NayaGramPro-1877F2?style=flat-square&logo=facebook&logoColor=white" alt="Facebook"/></a>
  <a href="mailto:support.nayagram@gmail.com"><img src="https://img.shields.io/badge/Email-support.nayagram@gmail.com-D14836?style=flat-square&logo=gmail&logoColor=white" alt="Support Email"/></a>
</p>

[NayaGram](https://t.me/NayaGramPro) is an independent, privacy-focused Telegram API client for Android built on the official open-source Telegram Android codebase.

This repository (`NayaGramPlatform/NayaGram-Client`) contains the official public open-source code corresponding to the NayaGram Android release:
- **Application ID (Google Play):** `org.nayagram.platform`
- **Current Synced Version:** `v4.26.75 NG` (Internal Build Code: `4097`)
- **License:** [GNU General Public License v2.0 or later](LICENSE)

---

## Notice for Telegram & Play Store Reviewers / Contributors
- **Unofficial Client Disclosure:** NayaGram is an independent client application that uses the Telegram API under Telegram's Terms of Service and is not affiliated with Telegram FZ-LLC / Telegram Messenger Inc.
- **Open-Source Security Hygiene:** Private API keys, `TELEGRAM_API_ID` / `TELEGRAM_API_HASH`, production `google-services.json`, Google Maps API keys, and signing keystores/passwords are **intentionally excluded or replaced with placeholders** in this public repository.
- **Source Parity:** All application source code (`TMessagesProj`, `TMessagesProj_App`, native C++ submodules, and resources) matches the production `4.26.75 NG` (`4097`) build.

---

## Creating & Building Your NayaGram Application

Developers using this source code must comply with Telegram's developer guidelines and GPLv2+:

1. [**Obtain your own `api_id` and `api_hash`**](https://core.telegram.org/api/obtaining_api_id) from `https://my.telegram.org`.
2. Please **do not** use the name Telegram for your app — and clearly inform users that your app is unofficial.
3. Please **do not** use Telegram's standard logo (white paper plane in a blue circle) as your app's logo.
4. Study Telegram's [**security guidelines**](https://core.telegram.org/mtproto/security_guidelines) and protect your users' data and privacy.
5. Publish **your** modified source code under GNU GPLv2+ to comply with the license.

### API & Protocol Documentation
- Telegram API manuals: https://core.telegram.org/api  
- MTProto protocol manuals: https://core.telegram.org/mtproto  

### Compilation Guide
You will require **JDK 17**, **Android SDK 36**, **CMake 3.22.1**, and **Android NDK `27.2.12479018`**.

1. **Clone the source code with all native submodules:**
   ```bash
   git clone --recursive --shallow-submodules https://github.com/NayaGramPlatform/NayaGram-Client.git NayaGram
   cd NayaGram
   ```
   If you forgot `--recursive`, run:
   ```bash
   git submodule sync --recursive
   git submodule update --init --recursive --depth=1
   ```

2. **Configure Telegram API credentials** (injected at build time via `TMessagesProj/build.gradle` into `BuildConfig.TELEGRAM_APP_ID` / `BuildConfig.TELEGRAM_APP_HASH`):
   Pass `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` as environment variables or Gradle `-P` flags:
   ```bash
   export TELEGRAM_API_ID=YOUR_API_ID
   export TELEGRAM_API_HASH=YOUR_API_HASH
   ```

3. **Configure Firebase & Maps placeholders (optional for production push/maps):**
   - Replace the placeholder `google-services.json` files (e.g. `TMessagesProj_App/google-services.json`, `TMessagesProj/google-services.json`) with your own Firebase Console configuration for `org.nayagram.platform`.
   - Replace `YOUR_GOOGLE_MAPS_API_KEY` inside `TMessagesProj/config/*/AndroidManifest*.xml` with your own Google Maps Android API key if map previews are needed.

4. **Configure Release Signing (optional for signed release builds):**
   Provide your own keystore path and credentials via environment variables or Gradle `-P` flags (never commit keystores or passwords):
   ```bash
   export RELEASE_KEYSTORE_PATH=/path/to/your-release.jks
   export RELEASE_STORE_PASSWORD=your_store_password
   export RELEASE_KEY_PASSWORD=your_key_password
   export RELEASE_KEY_ALIAS=your_key_alias
   ```

5. **Open in Android Studio** (use **Open**, not Import) or build from the terminal.

---

### Terminal Build Tasks

- **Debug APK:** `./gradlew :TMessagesProj_App:assembleAfatDebug`
- **Release APK:** `./gradlew :TMessagesProj_App:assembleAfatRelease`
- **Play Store Release AAB (SDK 24+):** `./gradlew :TMessagesProj_App:bundleBundleAfat_SDK23Release`
- **Play Store Bundle (Default):** `./gradlew :TMessagesProj_App:bundleBundleAfatRelease`

---

### Localization
We welcome community contributions for localization and translations. Please submit pull requests or reach out at [support.nayagram@gmail.com](mailto:support.nayagram@gmail.com).

### Community & Channels
- **Telegram:** [@NayaGramPro](https://t.me/NayaGramPro)
- **Twitter / X:** [@NayaGramPro](https://x.com/NayaGramPro)
- **YouTube:** [@NayaGramPro](https://youtube.com/@NayaGramPro)
- **Facebook:** [@NayaGramPro](https://facebook.com/NayaGramPro)
- **Email:** support.nayagram@gmail.com

---

### License
NayaGram for Android is licensed under the **GNU General Public License v2.0 or later**.  
*For original Telegram documentation, see [README_TELEGRAM.md](README_TELEGRAM.md).*

---

## 🏛️ About 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦

**𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦** is an independent technology initiative dedicated to delivering next-generation, high-performance, and privacy-respecting communication tools.

### ⚖️ Copyright & Trademarks
- **Copyright © 2026 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦.** All rights reserved.
- **Telegram** is a registered trademark of Telegram FZ-LLC / Telegram Messenger Inc.
- **NayaGram** is an independent client application built on the official open-source Telegram Android codebase under the terms of the **GNU General Public License (GPL) v2.0 or later**.
- Inquiries: [support.nayagram@gmail.com](mailto:support.nayagram@gmail.com).

<p align="center">
  <sub>Built with dedication, passion, and integrity by <b>𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦 𝐏𝐥𝐚𝐭𝐟𝐨𝐫𝐦</b>.</sub>
</p>
