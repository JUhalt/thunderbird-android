# RFC 0010: Wear OS Companion (Phone-First)

- Issue: [#6969](https://github.com/thunderbird/thunderbird-android/issues/6969)
- Status: **Proposed**

## Summary

Add a Wear OS companion to Thunderbird for Android. The phone app keeps doing all mail syncing. It publishes a
compact snapshot of the unified inbox to a paired watch over the Wearable Data Layer. The watch app shows that
snapshot, sends simple actions back (mark read or unread, star, archive, delete, mark all as read), sends short
replies that the phone writes and sends, and can open a message on the phone for full reading.

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

|               Module               |                                                                     Contents                                                                     |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `:feature:wear:companion:api`      | Plain Kotlin (JVM). Data Layer paths, capability names, the serializable protocol models, and the JSON codec. Shared by the phone and the watch. |
| `:feature:wear:companion:internal` | Phone side (Android). Builds and publishes snapshots, handles watch requests, and hosts the "open on phone" entry point.                         |
| `:feature:wear:companion:noop`     | Empty implementation for builds without Google Play Services, following the `feature:funding` pattern.                                           |
| `:app-thunderwren`                 | The Wear OS app. Depends only on `:feature:wear:companion:api`.                                                                                  |

`:app-thunderbird` binds `internal` in the `full` flavor and `noop` in the `foss` flavor.

### Protocol (version 1)

All payloads are UTF-8 JSON produced by `kotlinx.serialization`. Each payload carries a `version` field, so either side
can ignore data it doesn't understand.

- **Capabilities**: The phone advertises `thunderbird_wear_companion` and the watch advertises `thunderwren_watch`. Each
  side uses the other's capability to find a peer and to skip work when none is connected.
- **Mailboxes**: This is a `DataItem` at `/thunderwren/v1/mailboxes` holding `WearMailboxList`. It lists the unified
  inbox and its "Unread" and "Starred" views first, then each account's inbox, with the account name, email address,
  color, monogram, and unread count. It also carries the glance visibility (see Privacy below).
- **Inbox snapshots**: There is one `DataItem` per mailbox, at `/thunderwren/v1/inbox/<mailbox>`, holding
  `WearInboxSnapshot`. `<mailbox>` is `unified`, `unread`, `starred`, or the account UUID. Each snapshot contains the
  newest 25 messages of that mailbox. Each message has an ID, sender, subject, preview, date, read/starred flags,
  attachment and encryption indicators, and its account's ID and color. Separate items keep each one well under the
  Data Layer's 100 KB limit. They also make switching mailboxes on the watch instant, even while it is disconnected.
- **Publishing**: The phone republishes (debounced) when the message list changes and a watch is paired. It deletes the
  items of accounts that no longer exist. The Data Layer keeps the last published items on the watch.
- **Requests**: The watch sends `MessageClient.sendRequest` to `/thunderwren/v1/request` with a `WearRequest`:
  `Refresh`, `PerformAction(messageId, action)`, `MarkAllRead(mailboxId)`, or `Reply(messageId, text)`. The phone
  answers with a `WearResponse`. Actions update the local store through `MessagingController`, and the resulting
  message-list change republishes the snapshots. A phone that doesn't know a request answers `UNSUPPORTED_REQUEST`,
  which the watch explains as "update Thunderbird on your phone".
- **Replies**: `QuickReplySender` in `legacy:core` builds the reply the way the compose screen would: to the sender or
  Reply-To address, from the identity the message was sent to, with the account's quoting, signature, Bcc, and read
  receipt settings, threaded with `In-Reply-To` and `References`. It puts the reply in the Outbox and marks the
  original as answered. Replies are plain text. Encrypted messages are refused, because the reply would go out
  unencrypted; the watch offers "open on phone" for them instead. Nothing is sent without a review step on the watch.
- **Message IDs**: `MessageReference.toIdentityString()` is used as-is. The watch treats it as opaque.
- **Open on phone**: The watch calls `RemoteActivityHelper` with `thunderwren://open?message=<id>`. A small exported
  trampoline activity in `internal` validates the ID and opens the message in the phone app.

### Watch app

- Uses Compose for Wear OS Material 3. It has an inbox, a mailbox picker (the unified inbox, "Unread", "Starred", or a
  single account), a message screen with actions, a reply screen (voice, keyboard, or ready-made replies, then a
  review step), and an "open on phone" action. Messages can be swiped to archive or delete them, and a mailbox can be
  marked as read after a confirmation. Account monograms show which account a message belongs to. The selected
  mailbox is remembered on the watch.
- The Tile shows the unified inbox's unread count and the newest unread messages, which open when tapped. The
  complication shows the unread count. A `WearableListenerService` on the watch refreshes them when the phone
  publishes, even when the app isn't open.
- When no phone with Thunderbird is reachable, the watch explains that instead of showing placeholder data. It also
  offers a clearly labeled demo mailbox that lives only on the watch, so people can try the app without a phone. Real
  data from a phone always replaces the demo.
- Screens follow the MVI pattern of the [UI architecture](../../architecture/ui-architecture.md): each has a contract
  with its state, events, and effects, and a view model built on `BaseViewModel`. The watch uses Wear Compose
  Material 3 instead of the phone's design system, whose components are made for phone screens.
- The watch app has the same build types as `app-thunderbird` (debug, daily, beta, and release), with the same
  application ID suffixes and signing configurations, so each watch build pairs with the matching phone build.

### Rollout

Both phone-side parts are behind feature flags in `thunderbird_mobile_featureflag.catalog.json`, off by default and on
in Thunderbird debug builds. In debug builds, they can be toggled in the secret debug settings.

- `wear_companion`: publishing to the watch and answering its requests. While it is off, the phone publishes nothing
  and answers every request with `UNSUPPORTED_REQUEST`.
- `wear_notification_quick_reply`: sending the Reply action of Wear notifications with `QuickReplySender`. While it is
  off, that action opens the compose screen on the phone, as before.

This allows turning the companion on in Daily first, then Beta, then Release, and turning it off again without an
update if something goes wrong.

## Alternatives Considered

- **Standalone engine on the watch**: The prototype did this. See Motivation for its size, credential, battery, and
  setup costs. It would also require splitting `legacy:common` and `app-common` into engine and phone UI modules first.
  A standalone mode could still be added later behind the same watch UI, since the UI only depends on the snapshot
  models.
- **Rely on bridged phone notifications only**: This needs no new code, but it only covers new mail. It doesn't give
  users an inbox to browse, and the actions available on bridged notifications are limited. The companion still
  improves them: their Wear actions follow the user's notification action order, include Star, and the Reply action
  takes a voice, keyboard, or ready-made reply that `QuickReplySender` sends.
- **Custom Bluetooth or network transport**: This avoids Google Play Services but duplicates what the Data Layer
  already provides: pairing, encryption, offline caching, and delivery over the cloud when the devices are apart.

## Risks & Drawbacks

- **Google Play Services**: The Wearable Data Layer requires Play Services, so the companion exists only in the `full`
  flavor. F-Droid (`foss`) builds get the no-op module. In practice, Wear OS itself also requires Play Services on
  the phone.
- **Privacy**: Snapshots contain sender names, subjects, and previews. The Data Layer encrypts traffic between paired
  devices, and data stays within the user's Google account. The snapshot is capped in size and has no message bodies.
  The mailbox list includes account email addresses. The Tile and complication can be seen by people nearby, like a
  phone's lock screen, so they follow Thunderbird's "lock screen notifications" setting, which the phone publishes as
  the glance visibility: senders and subjects, senders only, the unread count only (the default), or nothing. Inside
  the app, which the wearer opens on purpose, everything is shown, as in Thunderbird on an unlocked phone.
- **Same app identity**: The Data Layer only connects apps with the same application ID and signing key. The watch
  app must therefore use Thunderbird's application ID for each build type (for example `net.thunderbird.android` and
  `net.thunderbird.android.debug`) and be signed with the same key. That is also how Play publishes a Wear OS app
  alongside its phone app.
- **Freshness**: If the phone process isn't running, the watch only gets updates when it asks or when Thunderbird
  syncs. This is acceptable for triage.
- **Maintenance**: A new app module and three feature modules to keep building.

## Open Questions

1. Where should the Wear app live long term: this repository (as `:app-thunderwren` or a renamed module) or a
   separate one?
2. Naming: "ThunderWren" is a working title. Would the Thunderbird brand be allowed on the watch app?
3. Should quick replies (from the watch and from Wear notifications) quote the original message? They currently
   follow the account's "quote original message when replying" setting, like the compose screen.
4. Version codes: Play needs the phone and watch APKs of one release to have different version codes. Should the watch
   app derive its code from the phone's (for example with a fixed offset), or be released on its own schedule?

Answered in this proposal: the Tile and complication follow the lock screen notification setting (see Privacy), and
replies from the watch are sent by the phone, as plain text, never for encrypted messages.

## Outcome

Not decided yet.
