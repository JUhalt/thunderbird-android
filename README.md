# ThunderWren 🐦⚡

**A Wear OS companion & standalone email client built on Thunderbird for Android.**

ThunderWren brings privacy-focused, open-source email management to your wrist. Designed specifically for smartwatches running Wear OS 3+, ThunderWren leverages the core engine of Thunderbird for Android (and K-9 Mail) while delivering a wrist-first UI built with **Wear OS Jetpack Compose Material 3**.

---

## 🎯 Vision & Goals

* **Wrist-First Experience**: Tailored interface for small, round smartwatch displays using Wear OS Compose Material 3 components (`AppScaffold`, `ScreenScaffold`, `ScalingLazyColumn`).
* **Companion & Standalone Modes**: Read and manage emails on your watch via Wi-Fi/Cellular, or seamlessly sync account data and notifications with Thunderbird on your phone.
* **Powered by Thunderbird Engine**: Reuses Thunderbird's robust core architecture (`core:api`, `feature:*:api`), account storage, IMAP/OAuth protocols, and cryptography foundations.
* **Privacy & Open Source**: Zero telemetry, zero tracking, and complete respect for user data privacy.

---

## 🗺️ Roadmap & Milestones

### Milestone 1: Foundation & Project Architecture 🏗️
- [ ] Create `:app-thunderwren` Wear OS Gradle module targeting Wear OS 3+.
- [ ] Add Wear OS dependencies (`androidx.wear.compose:compose-material3`, `androidx.wear.compose:compose-navigation`) to Version Catalog.
- [ ] Set up Wear OS launcher activity, theme scaffold, and verify emulator execution.

### Milestone 2: Wrist UI & Navigation ⌚
- [ ] Build `ThunderWrenTheme` using Wear OS Material 3 color schemes.
- [ ] Implement `InboxScreen` displaying email headers with subject, sender, and timestamp.
- [ ] Implement `MessageDetailScreen` optimized for small screen reading and rotary input.
- [ ] Build account/folder selection drawer.

### Milestone 3: Data Layer & Thunderbird Engine Integration 🔄
- [ ] Wire `:app-thunderwren` into Thunderbird's shared `:api` modules (`core:api`, `feature:account:api`, `feature:mail:api`).
- [ ] Connect account storage to access existing email accounts.
- [ ] Connect background mail synchronization & IMAP protocol fetching.
- [ ] Store and cache recent messages for offline wrist reading.

### Milestone 4: Phone Companion, Tiles & Complications 📱📲
- [ ] Implement Google Play Services Wearable DataLayer API for transferring credentials from Thunderbird (phone) to ThunderWren (watch).
- [ ] Create an Unread Messages Wear OS **Tile** for quick wrist status.
- [ ] Create an Unread Count **Complication** for watch faces.
- [ ] Add quick message actions (Mark Read, Archive, Quick Voice/Emoji Replies).

---

## 🧱 Repository Architecture

ThunderWren is built directly within the Thunderbird for Android multi-module workspace:

* **`:app-thunderwren`**: *(Planned)* Wear OS application module containing watch UI, launcher, tiles, and complications.
* **`:app-thunderbird` / `:app-k9mail`**: Phone application entry points.
* **`feature:*` & `core:*`**: Shared backend, database, mail protocol, and dependency injection modules (`app-common`).

For detailed architectural guidelines, see [`docs/architecture/`](docs/architecture/README.md) and [`AGENTS.md`](AGENTS.md). The original upstream Thunderbird README is preserved at [`README.upstream.md`](README.upstream.md).

---

## 🛠️ Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/JUhalt/thunderbird-android.git
   cd thunderbird-android
   ```
2. Open in **Android Studio** (2024.1+ recommended).
3. Create a **Wear OS Emulator** (Wear OS 3 or higher, Round display).
4. Build and run the `:app-thunderwren` module onto your Wear OS emulator or connected smartwatch.

---

## 📜 License & Credits

ThunderWren is an open-source community project based on [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android) and [K-9 Mail](https://k9mail.app/).

Licensed under the [Apache License, Version 2.0](LICENSE).
