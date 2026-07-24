# Mayra Notification Intelligence — Implementation Status

This document maps the Notification Intelligence implementation to the locked Mayra AI Master Blueprint.

## Implemented foundation

- Explicit Notification Access service; no notification is read until the user enables Android Notification Access.
- Self-notification filtering so Mayra does not ingest its own alerts.
- Per-app privacy policies: full content with built-in redaction, hide content, or ignore app.
- Per-app reply and private read-aloud preferences.
- Privacy-first processing order: policy check, sensitivity classification, sanitization, then bounded local storage.
- OTP and verification-code detection with plain code suppression.
- Sensitive financial, credential, medical and legal hints with numeric redaction.
- Bounded in-memory notification record store with update/removal handling.
- Conversation/app grouping and protected spoken unread brief.
- Voice/text intent for notification summary.
- Reply availability only when the source notification exposes an Android `RemoteInput` free-form action.
- Reply PendingIntent and RemoteInput handles remain memory-only.
- Mandatory short-lived confirmation before every notification reply.
- Global Mayra action kill switch takes priority over reply preparation and confirmation.
- OTP notification replies are always blocked.
- Ignored apps and apps with replies disabled cannot enter the reply flow.
- Sensitive reply previews hide draft content.
- Exact confirmation expiry and stale reply-handle expiry.
- Duplicate reply suppression for identical content on the same notification.
- Honest send result: Mayra reports that the reply action was handed to the source app, not guaranteed delivered.
- Bounded visible reply audit with prepared, confirmed, sent, blocked, failed, cancelled and duplicate-blocked states.
- Internal Notification Center accessible from Living Presence.
- Notification Access settings shortcut and local notification/audit clear controls.

## Privacy guarantees

- Raw OTP codes are not stored in Mayra notification records or Ambient history.
- Ignored apps are dropped before local notification or Ambient history storage.
- Notification reply PendingIntents are never serialized or written to disk.
- Passwords, OTPs and raw notification reply text are not written to the reply audit.
- Read-aloud defaults to off per app.
- Notification Center activity is internal-only (`android:exported=false`).

## Technically conditional behavior

- Direct reply works only when the source app exposes a valid Android notification `RemoteInput` action.
- A successful PendingIntent handoff is not proof of network delivery or recipient receipt.
- Notification access may be disabled by the user or restricted by OEM behavior.
- Reply handles disappear after process death, notification removal, source-app cancellation or expiry.

## Remaining gaps

1. Contact/relationship-aware sender resolution across notifications.
2. AI-generated reply suggestions with sensitive-context policy gates.
3. User-visible per-app policy list even when no notification is currently captured.
4. Lock-screen authentication gate for reading normal private content aloud.
5. Create-reminder-from-notification flow through the Action Safety Engine.
6. Mark-as-read/dismiss support only where Android notification APIs and user policy allow it.
7. Physical-device compatibility testing across WhatsApp, Messages, Telegram and OEM notification implementations.
8. Fresh successful Gradle compile, unit-test and lint validation after the current GitHub Actions runner issue is resolved.

## Validation status

The source and deterministic tests are committed, but GitHub Actions currently fails before Checkout with an empty step list and no logs. Therefore this batch is not claimed as build-verified. The last fully green source state remains Android CI #571, before the Action Safety and Notification Intelligence additions.
