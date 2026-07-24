# Mayra-Owned Reminders & Follow-ups — Implementation Status

## Implemented

- Reminder commands no longer depend on opening an external Android reminder screen in the production app path.
- Deterministic offline parsing for relative minutes/hours, today, tomorrow, day-after-tomorrow, clock times, AM/PM and common Hindi/Hinglish dayparts.
- Clarification instead of guessing when a due time is missing or ambiguous.
- Protection against treating unrelated quantities such as “2 tablets tomorrow” as a 2 AM reminder.
- Persistent bounded local reminder store.
- Reminder states: scheduled, due, snoozed, completed, cancelled and missed.
- WorkManager-backed one-time scheduling that survives ordinary process death.
- Boot and app-update rescheduling.
- High-importance Mayra reminder notification channel.
- Notification actions for Complete and Snooze 10 minutes.
- Reminder Center for creating, viewing, completing, snoozing, cancelling and deleting history.
- Notification deep link to the exact reminder.
- Missed-reminder follow-up after 30 minutes when the reminder remains unresolved.
- Follow-up cancellation when the reminder is completed, cancelled or snoozed.
- Notification-readiness handling that avoids an endless retry loop when notification permission is unavailable.
- Living Presence launcher entry.
- Deterministic parser, persistence, lifecycle and command-response tests.

## Privacy and safety

- Reminder content is stored locally in app-private preferences.
- No cloud provider is required for deterministic reminder parsing or scheduling.
- The engine does not claim an alert was displayed when Android notification permission/settings block it.
- Reminder notifications are user-visible and can be completed or snoozed directly.
- App-owned reminder actions are internal-only and do not expose exported receivers or activities.

## Android limitations

- WorkManager timing is reliable but not exact-to-the-second; Android and OEM battery management may delay execution.
- Exact-alarm delivery is not yet used. A future Owner Mode option may add AlarmManager exact alarms where Android permits and the user explicitly grants exact-alarm access.
- Notification delivery requires Android notification permission and an enabled reminder channel.
- A reboot/app-update broadcast restores active schedules, but some OEMs may delay background initialization.

## Remaining gaps

1. Calendar event store and Android Calendar provider sync.
2. Recurring reminders and recurrence rules.
3. Natural-language edit/cancel commands such as “move medicine reminder to 9 PM”.
4. Voice list/query commands: “What reminders do I have today?”
5. Location- and context-based reminders.
6. User-configurable follow-up delay and escalation policy.
7. Exact-alarm Owner Mode adapter and compatibility diagnostics.
8. Biometric/PIN protection for sensitive reminder content.
9. Physical-device validation for notification actions, reboot restore and Motorola battery management.
10. Fresh successful Gradle compile, tests and lint after the current GitHub Actions zero-step runner failure is resolved.

## Validation status

Source and deterministic tests are committed. GitHub Actions currently fails before Checkout with an empty step list and no logs, so this batch is not claimed as build-verified. The last fully green source validation remains CI #571, before later Action Safety, Notification Intelligence, Owner Mode, Identity and Reminder additions.
