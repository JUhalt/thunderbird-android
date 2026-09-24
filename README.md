# ThunderWren 🐦⚡

**A Wear OS email client prototype built on Thunderbird for Android.**

ThunderWren is an experiment in bringing privacy-focused, open-source email to smartwatches running Wear OS 3+. It lives inside a fork of the Thunderbird for Android repository so it can reuse the Thunderbird/K-9 Mail engine, and its wrist-first UI is built with **Wear OS Compose Material 3**.

> [!IMPORTANT]
> **Project status: early prototype.** The app builds and runs on a Wear OS emulator, and the Thunderbird mail engine starts up inside it. The screens, the Tile, and the complication still show **sample data**, though. The watch can't yet add an account, sync mail, or send anything. See [What works today](#-what-works-today) for the exact state.

---

## 🎯 Vision & Goals

* **Wrist-first experience**: an interface for small, round displays using Wear OS Compose Material 3 (`AppScaffold`, `ScreenScaffold`, `ScalingLazyColumn`, swipe-to-dismiss navigation).
* **Companion and standalone modes**: read and triage email on the watch over Wi-Fi/LTE, or pair with Thunderbird on the phone for account setup and notifications.
* **Powered by the Thunderbird engine**: reuse Thunderbird's account storage, IMAP/POP3 backends, OAuth, and OpenPGP foundations instead of reimplementing them.
* **Privacy and open source**: no telemetry (the watch app uses Thunderbird's no-op telemetry module) and no tracking.

---

## ✅ What works today

| Area | Status | Notes |
|---|---|---|
| Wear OS app module (`:app-thunderwren`) | ✅ Working | Builds, installs, and launches on Wear OS 3+ (API 30+). |
| Thunderbird engine and dependency injection | ✅ Working | Koin graph verified by `DependencyInjectionTest`; startup covered by the Robolectric `AppStartupTest`. |
| Inbox, message, folder, and quick-reply screens | 🟡 UI only | Real Wear M3 screens with rotary/scroll indicators and swipe-back, showing **sample messages**. |
| Reading configured accounts | 🟡 Partial | `InboxViewModel` reads accounts from Thunderbird's `Preferences`, but the watch has no way to add one yet. |
| Unread Tile and complication | 🟡 Placeholder | Registered with the system; always shows **"2"** unread. |
| Voice dictation and quick replies | 🟡 UI only | Launches speech recognition and shows canned replies; **nothing is sent**. |
| Archive / Delete / Mark read | 🟡 UI only | Buttons navigate back; no mail action is performed. |
| Folder switcher | 🟡 UI only | Static folder list; selecting a folder doesn't change the inbox. |
| OpenPGP | 🟡 Detection only | Detects ASCII-armored PGP messages and asks you to open them on the phone. No decryption on the watch. |
| Phone companion sync | 🔴 Not started | `PhoneSyncListenerService` only logs incoming Data Layer events. The phone app doesn't send anything yet. |

---

## 🗺️ Roadmap

### Done
- [x] `:app-thunderwren` Wear OS module targeting Wear OS 3+ (minSdk 30).
- [x] Wear Compose Material 3 theme with the Thunderbird palette and OLED-black background.
- [x] Inbox, message detail, folder list, and quick-reply screens with swipe-to-dismiss navigation.
- [x] Thunderbird engine (Koin, `K9`, `Core`) running in the watch app, with a verified dependency graph.
- [x] Tile, complication, and Data Layer listener registered (placeholder content).

### Next: make it a real mail client
- [ ] Show real messages: back the inbox, message, and folder screens with the local message store instead of sample data.
- [ ] Account setup: provision accounts from the paired phone over the Wearable Data Layer (plus a standalone setup path).
- [ ] Mail actions: wire Archive, Delete, Mark read, and Star to `MessagingController`.
- [ ] Sending: send quick and voice replies through the account's SMTP settings.
- [ ] Live Tile and complication: drive the unread count from the message store and refresh on sync.
- [ ] Battery-aware sync: WorkManager-based periodic sync tuned for watches.
- [ ] Notifications: bridge or generate watch notifications with inline actions.

### Upstream proposal
- [ ] Present the prototype on [thunderbird/thunderbird-android#6969](https://github.com/thunderbird/thunderbird-android/issues/6969) (the open Wear OS port request) once real mail flows end to end.

---

## 🛠️ Getting Started

### Requirements

* A recent **Android Studio** release that supports Android Gradle Plugin **9.4**. If Gradle sync says the AGP version is unsupported, update Android Studio.
* Gradle JDK **17 or newer**. Android Studio's bundled JDK works.
* Android SDK Platform **37** (the project's `compileSdk`). Android Studio offers to install it on first sync.
* A **Wear OS emulator**: in Device Manager, create a *Wear OS Small Round* (or Large Round) device with a **Wear OS 3 (API 30) or newer** system image.

### Run in Android Studio

1. Clone the repository and open the folder in Android Studio:
   ```bash
   git clone https://github.com/JUhalt/thunderbird-android.git
   ```
2. Let Gradle sync finish. The first sync of this multi-module project can take several minutes.
3. Start the Wear OS emulator.
4. Select the **`app-thunderwren`** run configuration and the watch emulator, then click **Run ▶️** (`Shift + F10`).

The debug build installs as **ThunderWren** with the application ID `net.thunderbird.wear.debug`.

### Command line

```bash
./gradlew :app-thunderwren:installDebug        # build and install on a running watch emulator
./gradlew :app-thunderwren:testDebugUnitTest   # unit, DI, and app-startup tests
./gradlew :app-thunderwren:detekt :app-thunderwren:spotlessCheck :app-thunderwren:lintDebug
```

### Try the Tile and complication

* **Tile**: long-press the watch face, or swipe to the tiles carousel and add **"Unread Emails"**.
* **Complication**: long-press the watch face → *Customize* → pick a complication slot → **ThunderWren / Unread Count**.

### Troubleshooting

* **Gradle sync fails with a version catalog error** (for example `InvalidUserDataException` while resolving `foojay-resolver-convention`): pull the latest `main`. An earlier commit corrupted `gradle/libs.versions.toml`, which is fixed now.
* **The app crashes at launch**: run `./gradlew :app-thunderwren:testDebugUnitTest`. `DependencyInjectionTest` and `AppStartupTest` name the missing dependency or failing component, which is faster than reading logcat.
* **The Gradle daemon runs out of memory**: the project reserves a 10 GB heap (`org.gradle.jvmargs` in `gradle.properties`). On machines with 16 GB of RAM or less, close other apps or lower `-Xmx`.

---

## 🧱 Repository Architecture

ThunderWren is built inside the Thunderbird for Android multi-module workspace:

* **`:app-thunderwren`**: the Wear OS app. It contains the watch UI (`ui/`), Tile (`tile/`), complication (`complication/`), Data Layer listener (`sync/`), and its Koin module (`di/ThunderWrenModule.kt`), which combines `appCommonModule` with watch-specific definitions.
* **`:app-thunderbird` / `:app-k9mail`**: the upstream phone apps. They are unchanged in this fork.
* **`feature:*`, `core:*`, `legacy:*`, `backend:*`**: the shared engine: accounts, storage, mail protocols, and DI modules.

For architectural guidelines, see [`docs/architecture/`](docs/architecture/README.md) and [`AGENTS.md`](AGENTS.md). The original upstream README is preserved in [`README.upstream.md`](README.upstream.md).

---

## 📜 License & Credits

ThunderWren is an open-source community project based on [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android) and [K-9 Mail](https://k9mail.app/). It is not an official Thunderbird or MZLA product.

Licensed under the [Apache License, Version 2.0](LICENSE).
