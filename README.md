<p align="center">
  <img src="images/thunderwren/thunderwren-logo.svg" width="160" alt="ThunderWren logo: a round watch showing a wren with a lightning-bolt tail perched on an envelope">
</p>

# ThunderWren 🐦⚡

**A Wear OS companion for Thunderbird for Android.**

ThunderWren brings your Thunderbird inbox to smartwatches running Wear OS 3+. Thunderbird on your phone keeps doing the mail syncing. The watch shows your inboxes, lets you triage messages with a tap or a swipe, and sends quick replies through the phone. For anything longer it hands off to the phone. The watch never stores your passwords or connects to a mail server.

It lives in a fork of the Thunderbird for Android repository. The design is proposed upstream in [RFC 0010](docs/engineering/rfcs/0010-wear-os-companion.md).

> [!IMPORTANT]
> **Project status: working prototype, not yet tested on real hardware.** The phone and watch sides are implemented and covered by unit and UI tests, but haven't been run on a paired phone and watch yet. The watch needs the Thunderbird phone app **built from this fork**; the Thunderbird app from the Play Store doesn't include the companion.
>
> **Just curious?** You don't need a phone. Install only the watch app and tap **Try the demo**.

<p align="center">
  <img src="images/thunderwren/screenshots/2-inbox-all.png" width="200" alt="All inboxes, with account monograms on each message">
  <img src="images/thunderwren/screenshots/9-swipe.png" width="200" alt="A message swiped to the left, showing Delete and Archive">
  <img src="images/thunderwren/screenshots/5-message.png" width="200" alt="A message opened on the watch, with its account">
</p>
<p align="center">
  <img src="images/thunderwren/screenshots/7-reply.png" width="200" alt="Reply screen with Speak or type and ready-made replies">
  <img src="images/thunderwren/screenshots/8-reply-review.png" width="200" alt="Reviewing a reply before sending it">
  <img src="images/thunderwren/screenshots/6-inbox-starred.png" width="200" alt="The Starred view of all inboxes">
</p>
<p align="center"><em>The demo mailbox, rendered from the real watch UI.</em></p>

---

## ✅ What works today

|                     Area                     |      Status       |                                                                                                                       Notes                                                                                                                        |
|----------------------------------------------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Inbox on the watch                           | ✅                 | The newest 25 messages with sender, subject, preview, date, and unread/starred state.                                                                                                                                                              |
| All inboxes, Unread, Starred, or one account | ✅                 | Tap the mailbox name at the top of the inbox to switch. "Unread" and "Starred" filter all inboxes. The watch remembers your choice.                                                                                                                |
| Account monograms                            | ✅                 | In mixed views, each message shows its account's monogram and color, as in Thunderbird.                                                                                                                                                            |
| Swipe to archive or delete                   | ✅                 | Swipe a message to the left. A full swipe archives it. Screen readers get the same actions.                                                                                                                                                        |
| Mark read/unread, star, archive, delete      | ✅                 | Sent to the phone, which applies them like the phone app does. Opening a message marks it as read.                                                                                                                                                 |
| Mark all as read                             | ✅                 | At the end of any mailbox, after a confirmation.                                                                                                                                                                                                   |
| Reply from the watch                         | ✅                 | Speak, type, or pick a ready-made reply, review it, and send. Thunderbird on the phone sends it from the right account, with your signature and quoting settings, and marks the message as answered. Encrypted messages are answered on the phone. |
| Open on phone                                | ✅                 | Opens the message in Thunderbird on the phone, for reading in full.                                                                                                                                                                                |
| Tile                                         | ✅                 | Shows the unread count and the newest unread messages. Tap one to open it.                                                                                                                                                                         |
| Complication                                 | ✅                 | Shows the unread count on your watch face.                                                                                                                                                                                                         |
| Privacy                                      | ✅                 | The Tile and complication follow Thunderbird's **Lock Screen Notifications** setting (senders and subjects, senders only, count only, or nothing). The default is count only.                                                                      |
| Encrypted messages                           | ✅                 | Shown as encrypted, with a prompt to read them on the phone.                                                                                                                                                                                       |
| Works briefly offline                        | ✅                 | The watch keeps the last data the phone sent.                                                                                                                                                                                                      |
| Demo mailbox                                 | ✅                 | **Try the demo** on the watch shows a sample mailbox where everything above works, no phone needed. Real data from Thunderbird on the phone replaces it automatically.                                                                             |
| Phone notifications on the watch             | ✅ Improved        | Thunderbird's notifications offer **Reply** (voice, keyboard, or ready-made replies, sent by the phone) and **Star** on the watch, and their actions follow your notification action order.                                                        |
| Folders other than the inbox                 | ⬜ Not started     |                                                                                                                                                                                                                                                    |
| Standalone mode (no phone)                   | ⬜ Not planned yet | See the RFC for why the phone does the syncing.                                                                                                                                                                                                    |

---

## 🧱 How it works

```
Phone: Thunderbird (full/Play build)             Watch: ThunderWren
┌──────────────────────────────────┐             ┌──────────────────────────┐
│ :feature:wear:companion:internal │─ mailboxes ▶│ Inbox, mailbox picker,   │
│  • publishes each mailbox        │─ inboxes ──▶│ message and reply        │
│  • applies watch actions         │◀─ requests ─│ screens, Tile,           │
│  • sends replies                 │             │ complication             │
│  • "open on phone" entry point   │             │                          │
└──────────────────────────────────┘             └──────────────────────────┘
                both use :feature:wear:companion:api (protocol)
```

* **`:feature:wear:companion:api`**: the shared protocol, covering Data Layer paths, message and mailbox models, and the JSON codec.
* **`:feature:wear:companion:internal`**: the phone side, included in Thunderbird's `full` (Play) flavor. It publishes the mailbox list and one snapshot per mailbox over the Wearable Data Layer. It also applies actions through `MessagingController`, sends replies with `QuickReplySender` (in `legacy:core`, shared with the notification Reply action), and opens messages on request.
* **`:feature:wear:companion:noop`**: included in the `foss` (F-Droid) flavor instead, because the Data Layer needs Google Play Services.
* **`:app-thunderwren`**: the Wear OS app. It depends only on the protocol module.

---

## 🛠️ Getting Started

### Requirements

* A recent **Android Studio** release that supports Android Gradle Plugin **9.4**. If Gradle sync says the AGP version is unsupported, update Android Studio.
* Gradle JDK **17 or newer**. Android Studio's bundled JDK works.
* Android SDK Platform **37** (the project's `compileSdk`). Android Studio offers to install it on first sync.
* A **phone emulator with Google Play** (or a real phone) and a **Wear OS 3+ (API 30+) emulator**, paired with each other.

### Quickest try: the demo (watch only)

1. Create and start a *Wear OS Small Round* emulator (API 30 or newer) in Device Manager.
2. Select the **`app-thunderwren`** run configuration and run it on the watch.
3. Tap **Try the demo**. You get "All inboxes", "Unread", "Starred", and a Work and a Personal account with sample messages. Open, star, reply to, swipe away, and mark them as read, switch mailboxes, and add the Tile and complication. Nothing is sent anywhere.

**Exit demo** at the bottom of the inbox returns to the "Connect your phone" screen. If Thunderbird on a paired phone connects, its real inbox replaces the demo automatically.

### Pair a phone and a watch emulator

1. In Device Manager, create a phone emulator using a system image **with Google Play**, and a *Wear OS Small Round* emulator (API 30 or newer).
2. Start both, then use **Tools → Device Manager → ⋮ → Pair Wearable** (Android Studio's pairing assistant) to pair them. It installs the Wear OS companion app on the phone.

### Install both apps

The phone app and the watch app must have the **same application ID and signing key**, or the Data Layer won't connect them. Debug builds from the same computer satisfy both (`net.thunderbird.android.debug`, signed with your debug key).

1. Select the **`app-thunderbird`** run configuration with the **`fullDebug`** build variant (*Build → Select Build Variant*), and run it on the **phone**. Set up a mail account in it.
2. Select the **`app-thunderwren`** run configuration and run it on the **watch**.

Or from the command line, with both emulators running:

```bash
./gradlew :app-thunderbird:installFullDebug      # installs on the phone (pick it with ANDROID_SERIAL if needed)
./gradlew :app-thunderwren:installDebug          # installs on the watch
```

### Tests and checks

```bash
./gradlew :feature:wear:companion:api:test :feature:wear:companion:internal:testDebugUnitTest
./gradlew :app-thunderwren:testDebugUnitTest     # view models, DI, app startup, and UI tests
./gradlew :app-thunderwren:detekt :app-thunderwren:spotlessCheck :app-thunderwren:lintDebug
```

### Try the Tile and complication

* **Tile**: swipe to the tiles carousel on the watch and add **"Unread Emails"**. To see senders and subjects on it with real mail, set **Settings → General settings → Notifications → Lock Screen Notifications** in Thunderbird on the phone to show them. The demo always shows them.
* **Complication**: long-press the watch face → *Customize* → pick a complication slot → **ThunderWren / Unread Count**.

### Troubleshooting

* **The watch says "Connect your phone"**: make sure the phone app is the **`fullDebug`** build from this repository (not `fossDebug` or the Play Store app), that it has at least one account, and that the emulators are paired. Opening Thunderbird on the phone once starts the companion.
* **CI doesn't run on your fork**: GitHub turns Actions off for forks. Enable it in the repository's **Actions** tab. The `Build - ThunderWren Wear OS application` job builds the watch app.
* **Gradle sync fails with a version catalog error**: pull the latest `main`. An earlier commit corrupted `gradle/libs.versions.toml`, which is fixed now.
* **The Gradle daemon runs out of memory**: the project reserves a 10 GB heap (`org.gradle.jvmargs` in `gradle.properties`). On machines with 16 GB of RAM or less, close other apps or lower `-Xmx`.

---

## 🗺️ Roadmap

- [x] Wear OS app with Compose for Wear OS Material 3.
- [x] Phone-first companion protocol and RFC ([0010](docs/engineering/rfcs/0010-wear-os-companion.md)).
- [x] Inboxes, mailbox switching, mail actions, open on phone, and a live Tile and complication.
- [x] Demo mailbox for trying the watch app without a phone.
- [x] Star and custom action order for Thunderbird's phone notifications on the watch.
- [x] Translatable strings, screen reader labels, and a CI build job.
- [x] Reply from the watch (voice, keyboard, or ready-made replies, sent by the phone), also from notifications.
- [x] Respect Thunderbird's notification privacy settings on the Tile and complication.
- [x] Swipe to archive or delete, Unread and Starred views, mark all as read, account monograms, and a richer Tile.
- [ ] Verify on a real phone and watch, then fix what that turns up.
- [ ] Present on [thunderbird/thunderbird-android#6969](https://github.com/thunderbird/thunderbird-android/issues/6969), the open Wear OS request.

For architectural guidelines, see [`docs/architecture/`](docs/architecture/README.md) and [`AGENTS.md`](AGENTS.md). The original upstream README is preserved in [`README.upstream.md`](README.upstream.md).

---

## 📜 License & Credits

ThunderWren is an open-source community project based on [Thunderbird for Android](https://github.com/thunderbird/thunderbird-android) and [K-9 Mail](https://k9mail.app/). It is not an official Thunderbird or MZLA product.

Licensed under the [Apache License, Version 2.0](LICENSE).
