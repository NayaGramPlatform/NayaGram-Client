# 🚀 NayaGram Client (Official Open Source Distribution)

> 📘 **Architecture & Pipeline Specification:** Detailed engineering pipeline, anti-delete engine specs, security isolation, and enterprise build architecture are fully documented in [**PLATFORM_PIPELINE.md**](./PLATFORM_PIPELINE.md).  
> 🔒 **Enterprise Core Pipeline:** Maintained at [NayaGramPlatform/NayaGramAndroid](https://github.com/NayaGramPlatform/NayaGramAndroid) for private release signing, keystore governance, and production deployments.

---

# 𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦™ for Android — Official Client Source Code

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![License: GPL v2/v3](https://img.shields.io/badge/License-GNU%20GPL%20v2%20%2F%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-2.0.html)
[![Telegram API](https://img.shields.io/badge/Powered%20By-Telegram%20API-0088cc.svg)](https://core.telegram.org/)
[![Status](https://img.shields.io/badge/Status-Official%20Public%20Source-brightgreen.svg)]()

> **NayaGram™** is an enhanced, high-performance, and secure Android messaging client powered by the official **Telegram MTProto API**.  
> Official Platform: [https://nayagram-privacy-fppvxkd4.agent.mira.tg/](https://nayagram-privacy-fppvxkd4.agent.mira.tg/)  
> Organization: **NayaGram Platform (𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦)**

---

## ⚖️ Legal Notice, Licensing & Trademark Compliance

### 1. Telegram Terms of Service Compliance
- This repository is an open-source client based on the official Telegram Android source code under the **GNU General Public License (GPL) v2 or later**.
- We strictly adhere to the [Telegram API Terms of Service](https://core.telegram.org/api/terms). 
- All MTProto protocol protocols, network infrastructures, and base messaging primitives remain the intellectual property of Telegram FZ-LLC / Telegram Messenger Inc.

### 2. Trademark & Identity Protection (Strictly Enforced)
- **Telegram Trademark:** "Telegram", the Telegram paper plane logo, and related marks are registered trademarks of Telegram FZ-LLC. This repository does not claim any endorsement or direct ownership of Telegram marks.
- **NayaGram™ Trademark:** The brand names **"NayaGram"**, **"𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦"**, official brand logos, icons, graphics, branding assets, and proprietary design identities are the exclusive intellectual property of **NayaGram Platform** and its founders.
- **Strict Prohibition:** Under international copyright, trademark, and intellectual property conventions, **no individual or entity is permitted to use the NayaGram™ name, logo, brand assets, or deceptive variations** in third-party forks, repackaged binaries, or commercial offerings without prior written authorization from NayaGram Platform Headquarters.

---

## 🔒 Security & API Key Guidelines (GPL Compliant)

In accordance with Section 2 of GNU GPL v2 and Telegram API Policy:
- **Proprietary Secrets Redacted:** This public repository intentionally **DOES NOT contain** production signing keystores (`.jks`), Google Cloud OAuth client secrets, Firebase private service accounts, or production API hashes.
- **Developer Instructions:** Developers building this repository locally must obtain their own credentials from [https://my.telegram.org](https://my.telegram.org) and generate their own signing keys.

---

## 🛠️ Building NayaGram-Client from Source

### Prerequisites
1. **Android Studio Ladybug | 2024.2.1** or newer
2. **JDK 17** (Temurin or OpenJDK)
3. **Android NDK** `27.2.12479018`
4. **CMake** `3.22.1`

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/NayaGramPlatform/NayaGram-Client.git
   cd NayaGram-Client
   git checkout master
   ```

2. Configure your API Credentials:
   Open `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java` and enter your credentials from [my.telegram.org](https://my.telegram.org):
   ```java
   public static int APP_ID = YOUR_APP_ID;
   public static String APP_HASH = "YOUR_APP_HASH";
   ```

3. Build via Gradle:
   ```bash
   ./gradlew assembleAfatDebug
   ```

---

## 🛡️ License

This project is distributed under the **GNU General Public License v2.0 or later (GPL-2.0-or-later)**.  
See the [LICENSE](LICENSE) file for full details.

Copyright (c) 2013-2024 Telegram FZ-LLC  
Copyright (c) 2026 NayaGram Platform (𝐍𝐚𝐲𝐚𝐆𝐫𝐚𝐦™). All rights reserved.
