# RFC 0010: Wear OS Companion (Phone-First)

- Issue: [#6969](https://github.com/thunderbird/thunderbird-android/issues/6969)
- Status: **Proposed**

## Summary

Add a Wear OS companion to Thunderbird for Android. The phone app keeps doing all mail syncing. It publishes a
compact snapshot of the unified inbox to a paired watch over the Wearable Data Layer. The watch app shows that
snapshot, sends simple actions back (mark read or unread, star, archive, delete), and can open a message on the phone
for full reading and replying.

The watch never stores account credentials and never talks to a mail server.

## Motivation

Users have asked for Wear OS support since 2023 (#6969). The main job on a watch is triage: glance at what arrived,
dismiss or archive it, and hand off to the phone for anything longer.

A prototype that ran the full Thunderbird engine on the watch showed the costs of the obvious approach:

- **Size**: The engine pulls in the phone UI through `app-common` and `legacy:common`. The shrunk release APK was
  ~15 MB, and ~150 MB for debug builds.
- **Credentials**: Every account's password or OAuth tokens would have to be copied to a second device.
- **Battery**: IMAP sync and IDLE connections on a watch drain a small battery quickly.
- **Setup**: There is no good way to type server settings or complete an OAuth flow on a watch.

## Proposal

### Modules

Following [ADR 0009](../adr/0009-api-internal-split.md):

| Module | Contents |
|---|---|
| `:feature:wear:companion:api` | Plain Kotlin (JVM). Data Layer paths, capability names, the serializable protocol models, and the JSON codec. Shared by the phone and the watch. |
| `:feature:wear:companion:internal` | Phone side (Android). Builds and publishes snapshots, handles watch requests, and hosts the "open on phone" entry point. |
| `:feature:wear:companion:noop` | Empty implementation for builds without Google Play Services, following the `feature:funding` pattern. |
| `:app-thunderwren` | The Wear OS app. Depends only on `:feature:wear:companion:api`. |

`:app-thunderbird` binds `internal` in the `full` flavor and `noop` in the `foss` flavor.

### Protocol (version 1)

All payloads are UTF-8 JSON produced by `kotlinx.serialization`. Each payload carries a `version` field, so either side
can ignore data it doesn't understand.

- **Capabilities**: The phone advertises `thunderbird_wear_companion` and the watch advertises `thunderwren_watch`. Each
  side uses the other's capability to find a peer and to skip work when none is connected.
- **Mailboxes**: This is a `DataItem` at `/thunderwren/v1/mailboxes` holding `WearMailboxList`. It lists the unified
  inbox first, then each account's inbox, with the account name, email address, color, and unread count.
- **Inbox snapshots**: There is one `DataItem` per mailbox, at `/thunderwren/v1/inbox/<mailbox>`, holding
  `WearInboxSnapshot`. `<mailbox>` is `unified` or the account UUID. Each snapshot contains the newest 25 messages of
  that inbox. Each message has an ID, sender, subject, preview, date, read/starred flags, attachment and encryption
  indicators, and the account color. Separate items keep each one well under the Data Layer's 100 KB limit. They also
  make switching mailboxes on the watch instant, even while it is disconnected.
- **Publishing**: The phone republishes (debounced) when the message list changes and a watch is paired. It deletes the
  items of accounts that no longer exist. The Data Layer keeps the last published items on the watch.
- **Requests**: The watch sends `MessageClient.sendRequest` to `/thunderwren/v1/request` with a `WearRequest`:
  `Refresh`, or `PerformAction(messageId, action)`. The phone answers with a `WearResponse`. An action updates the
  local store through `MessagingController`, and the resulting message-list change republishes the snapshots.
- **Message IDs**: `MessageReference.toIdentityString()` is used as-is. The watch treats it as opaque.
- **Open on phone**: The watch calls `RemoteActivityHelper` with `thunderwren://open?message=<id>`. A small exported
  trampoline activity in `internal` validates the ID and opens the message in the phone app.

### Watch app

- Uses Compose for Wear OS Material 3. It has an inbox, a mailbox picker (the unified inbox or a single account), a
  message screen with actions, and an "open on phone" action. The selected mailbox is remembered on the watch.
- The Tile and the complication show the unified inbox's unread count from the cached mailbox list. A `WearableListenerService` on the
  watch refreshes them when the snapshot changes, even when the app isn't open.
- When no phone with Thunderbird is reachable, the watch explains that instead of showing placeholder data.

## Alternatives Considered

- **Standalone engine on the watch**: The prototype did this. See Motivation for its size, credential, battery, and
  setup costs. It would also require splitting `legacy:common` and `app-common` into engine and phone UI modules first.
  A standalone mode could still be added later behind the same watch UI, since the UI only depends on the snapshot
  models.
- **Rely on bridged phone notifications only**: This needs no new code, but it only covers new mail. It doesn't give
  users an inbox to browse, and the actions available on bridged notifications are limited.
- **Custom Bluetooth or network transport**: This avoids Google Play Services but duplicates what the Data Layer
  already provides: pairing, encryption, offline caching, and delivery over the cloud when the devices are apart.

## Risks & Drawbacks

- **Google Play Services**: The Wearable Data Layer requires Play Services, so the companion exists only in the `full`
  flavor. F-Droid (`foss`) builds get the no-op module. In practice, Wear OS itself also requires Play Services on
  the phone.
- **Privacy**: Snapshots contain sender names, subjects, and previews. The Data Layer encrypts traffic between paired
  devices, and data stays within the user's Google account. The snapshot is capped in size and has no message bodies.
  Its contents should respect Thunderbird's existing notification privacy settings (see Open Questions). The mailbox
  list includes account email addresses.
- **Same app identity**: The Data Layer only connects apps with the same application ID and signing key. The watch
  app must therefore use Thunderbird's application ID for each build type (for example `net.thunderbird.android` and
  `net.thunderbird.android.debug`) and be signed with the same key. That is also how Play publishes a Wear OS app
  alongside its phone app.
- **Freshness**: If the phone process isn't running, the watch only gets updates when it asks or when Thunderbird
  syncs. This is acceptable for triage.
- **Maintenance**: A new app module and three feature modules to keep building.

## Open Questions

1. Should the snapshot respect the "lock screen notification visibility" privacy setting, for example by hiding
   previews?
2. Should replies from the watch (voice or canned responses) be sent by the phone in a later version, or stay
   "open on phone"?
3. Where should the Wear app live long term: this repository (as `:app-thunderwren` or a renamed module) or a
   separate one?
4. Naming: "ThunderWren" is a working title. Would the Thunderbird brand be allowed on the watch app?

## Outcome

Not decided yet.
