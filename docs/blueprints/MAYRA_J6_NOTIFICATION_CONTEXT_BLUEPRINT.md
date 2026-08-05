# J6 Notification Context Blueprint

## Goal

Add a privacy-first notification context layer that can help Mayra surface useful attention signals without turning Home into a notification mirror and without exposing private message content by default.

## First slice

The first implementation must be contract-only and deterministic:

- explicit opt-in/access state
- total active notification count
- high-attention count derived only from safe metadata
- coarse category buckets where Android exposes them
- captured-at timestamp and source provenance
- explicit `NotGranted` and `Unavailable` states
- no notification title, text, sender, conversation body, OTP, banking text, or message preview stored or rendered on Home

## Trust boundaries

1. Notification access is never implied by launcher/default-Home status.
2. User must enable Android Notification Access separately.
3. Home remains fully usable when access is absent, revoked, or service binding fails.
4. Raw notification objects must remain in the collector/service boundary and must not enter model prompts by default.
5. Aggregate context must be ephemeral unless a later ADR explicitly approves persistence.
6. Sensitive categories must not be inferred from notification text in this slice.
7. Any future deep-link/action requires an explicit user tap and a separate action contract.

## Typed model

Proposed fields:

- `access: ContextValue<NotificationAccessState>`
- `activeCount: Int`
- `attentionCount: Int`
- `categoryCounts: Map<NotificationCategory, Int>`
- `capturedAt`
- `source: NOTIFICATION_LISTENER`

## Home presentation

Home may show one compact line such as:

- `Notifications not enabled`
- `3 active notifications`
- `3 active · 1 may need attention`

Home must not show private content in this milestone.

## Tests

- access missing -> `NotGranted`
- service unavailable -> `Unavailable`
- empty list -> zero counts
- deterministic aggregation from synthetic metadata
- no title/text/sender fields in exported snapshot model
- launcher renders safely with all states
- J1/J2/J3/J4 and document-test isolation remain green

## Device gate

On the permanent Owner package:

1. Home remains usable before notification access is enabled.
2. Enabling access updates only aggregate context.
3. Revoking access returns to `NotGranted` without crash.
4. Airplane mode and provider/model failure do not affect the notification aggregate card.
