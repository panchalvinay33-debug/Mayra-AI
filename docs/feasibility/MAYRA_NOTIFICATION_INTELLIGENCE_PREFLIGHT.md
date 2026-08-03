# Mayra AI — Notification Intelligence Feasibility Preflight

Status: APPROVED FOR OWNER-CONTROLLED LOCAL PROCESSING — DEVICE/PRIVACY ACCEPTANCE REQUIRED
Date: 2026-08-03
Gate: Issue #14
Target: Motorola Edge 70 Fusion / Android 16

## Owner outcome

Mayra may optionally read selected posted notifications to provide useful local summaries, identify urgent items and support owner-defined reminders/follow-up without forcing the owner to open every app.

Notification intelligence is optional and must remain easy to disable.

## Official Android path

Use `NotificationListenerService` with Android's explicit Notification Access settings. The owner grants/revokes this special access through the system UI.

Do not simulate notification reading through Accessibility.

## Privacy boundary

Notifications can contain OTPs, financial information, private messages, health information and work data. Default architecture therefore is:

- local processing first;
- no raw notification content sent to cloud merely because a provider is enabled;
- explicit filtering before storage/summarization;
- no long-term raw notification archive by default;
- owner can exclude apps/categories;
- sensitive-pattern filtering before any optional provider use.

## Initial safe scope

Allowed first capabilities:

- app/package/title/category metadata;
- conversation/alerting notifications selected by owner;
- local urgency/relevance scoring using deterministic rules;
- short ephemeral local summary;
- owner-approved creation of a reminder/follow-up from a notification.

Do not auto-act on OTP, bank/payment, authentication or security notifications.

## Owner controls

Setup should expose only simple controls:

- notification intelligence on/off;
- Android Notification Access status;
- excluded apps;
- optional `private/sensitive notifications stay local only` status, enabled by default;
- clear recent-processing history/delete option if summaries are retained.

No complex per-notification rules are required for first release.

## Cloud boundary

If a future cloud summarizer is used:

- owner explicitly enables notification cloud processing separately;
- financial/auth/OTP/security notifications remain excluded;
- content is minimized/redacted;
- provider failure falls back to local/no-summary behavior;
- no provider output directly executes an action.

## Failure behavior

- Notification Access revoked → listener stops and Setup shows not ready.
- Process killed/reboot → service reconnects only through Android's normal lifecycle.
- Unsupported/malformed notification → ignore safely.
- Duplicate notification update → avoid duplicate reminder/action.
- No network → local filtering remains available.

## Evidence plan

Automated:

- inclusion/exclusion policy;
- sensitive-pattern filters;
- duplicate/update handling;
- no notification listener in J1/J2 safe proof packages;
- provider boundary tests;
- explicit reminder proposal rather than silent action.

Motorola:

- grant/revoke Notification Access;
- WhatsApp/SMS/email-style sample notifications;
- OTP/bank-style sample exclusion;
- app exclusion control;
- reboot/reconnect;
- battery observation;
- no duplicate alerts/summaries.

## Entry decision

APPROVED:

- local policy/filter tests;
- simple owner controls;
- local-only summary/proposal path.

BLOCKED until physical/privacy acceptance:

- broad cloud upload of notification contents;
- autonomous actions from notification text;
- financial/authentication workflows;
- hidden collection/retention.

## Source reviewed

- Android NotificationListenerService API documentation.
