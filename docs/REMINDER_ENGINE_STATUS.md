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
- Every reminder now carries a persisted monotonic `revision`.
- Every scheduled worker is bound to the exact reminder ID, revision and due time.
- Every notification Complete/Snooze/Cancel action is bound to the exact reminder revision.
- A stale worker exits without alerting or changing a snoozed/updated reminder.
- A stale notification action exits without mutating a newer reminder state.
- Completed and cancelled reminders cannot be revived by old actions.
- Snooze durations must be positive and no longer than 30 days.
- Legacy reminders without a revision load safely as revision zero.
- Reboot recovery is deterministic: future reminders reschedule, due reminders restore follow-up, overdue active reminders become missed once, already-missed reminders remain quiet, and terminal reminders are ignored.
- Reminder Center only reports Complete, Snooze, Cancel or Delete when the underlying transition actually succeeds.

## Privacy and safety

- Reminder content is stored locally in app-private preferences.
- No cloud provider is required for deterministic reminder parsing or scheduling.
- The engine does not claim an alert was displayed when Android notification permission/settings block it.
- Reminder notifications are user-visible and can be completed or snoozed directly.
- App-owned reminder actions are internal-only and do not expose exported receivers or activities.
- Global Stop blocks new autonomous phone actions, but owner-created reminders remain explicit commitments and continue to recover after reboot.
- Revision checks prevent an old notification or delayed WorkManager job from changing a newer reminder state.

## Deterministic tests

- relative-time, day/time and Hinglish parser cases;
- ambiguous-time clarification and unrelated-number protection;
- persistence and lifecycle transitions;
- revision increments;
- stale worker rejection;
- terminal-state protection;
- invalid snooze rejection;
- legacy revision migration;
- reboot recovery decisions for scheduled, overdue, due, missed and terminal reminders.

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

The revision, stale-worker, stale-action and reboot-recovery safeguards are coded with deterministic tests. GitHub Actions currently fails before Checkout with an empty step list and no logs, so this work is not claimed as compile-, test-, lint-, APK- or physical-device-verified.
