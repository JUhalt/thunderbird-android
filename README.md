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

## 🗺️ Roadmap & Milestone Progress

### Milestone 1: Foundation & Project Architecture 🏗️
- [x] Create `:app-thunderwren` Wear OS Gradle module targeting Wear OS 3+.
- [x] Add Wear OS dependencies (`androidx.wear.compose:compose-material3`, `androidx.wear.compose:compose-navigation`) to Version Catalog.
- [x] Set up Wear OS launcher activity, theme scaffold, and verify execution.

### Milestone 2: Wrist UI & Navigation ⌚
- [x] Build `ThunderWrenTheme` using Wear OS Material 3 color schemes.
- [x] Implement `InboxScreen` displaying email headers with subject, sender, and timestamp.
- [x] Implement `MessageDetailScreen` optimized for small screen reading and rotary input.
- [x] Implement `SwipeDismissableNavHost` supporting native Wear OS swipe-to-back gestures.

### Milestone 3: Data Layer & Thunderbird Engine Integration 🔄
- [x] Wire `:app-thunderwren` into Thunderbird's shared `:api` modules (`core:api`, `feature:account:api`, `feature:mail:api`).
- [x] Initialize Koin Dependency Injection via `ThunderWrenApplication` extending `BaseApplication`.
- [x] Create `InboxViewModel` querying user email accounts from `Preferences`.
- [x] Apply Thunderbird official brand palette (`#0A84FF` / `#72A3FF`) with OLED true-black backgrounds.

### Milestone 4: Phone Companion, Tiles & Complications 📱📲
- [x] Implement Play Services Wearable DataLayer listener (`PhoneSyncListenerService`) for receiving account credentials from phone.
- [x] Create an Unread Messages Wear OS **Tile** (`UnreadTileService`) for quick wrist status.
- [x] Create an Unread Count **Complication** (`UnreadComplicationService`) for watch faces.
- [x] Add quick message actions (Mark Read, Archive, Quick Replies).

---

## 🔮 Future Horizon & Next Steps

Now that all foundational milestones are complete, potential future directions for ThunderWren include:

1. **Voice & Emoji Quick Replies**: Add a Wear OS speech-to-text input prompt allowing users to dictate quick email replies directly from their watch.
2. **On-Watch OpenPGP Decryption**: Investigate lightweight PGP message decryption on watch using Thunderbird's existing `:legacy:crypto-openpgp` module.
3. **Upstream Thunderbird Proposal**: Present this functional Wear OS prototype to the upstream Thunderbird for Android maintainers ([Issue #6969](https://github.com/thunderbird/thunderbird-android/issues/6969)) for potential upstream integration!

---

## 🧱 Repository Architecture

ThunderWren is built directly within the Thunderbird for Android multi-module workspace:

* **`:app-thunderwren`**: *(Implemented)* Wear OS application module containing watch UI, launcher, tiles, complications, and sync listener.
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
4. Select **`app-thunderwren`** run configuration and click **Run** ▶️ (`Shift + F10`).

---

## 📜 License & Credits

ThunderWren is an open-source community project based on [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android) and [K-9 Mail](https://k9mail.app/).

Licensed under the [Apache License, Version 2.0](LICENSE).
