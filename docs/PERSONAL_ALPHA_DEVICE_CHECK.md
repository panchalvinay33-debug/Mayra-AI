# Mayra AI — Personal Alpha Device Check

This is the owner-first validation gate for the private sideloaded build. It is not a Play Store release checklist.

## Current decision

Mayra is ready to enter a controlled personal-device alpha as soon as the current branch successfully compiles into a debug APK. The latest source is not yet claimed build-verified because GitHub Actions is failing before Checkout with an empty step list.

Do not wait for every Master Blueprint feature before taking the first device check. Physical testing is now necessary because voice, notifications, WorkManager, OEM battery management, contacts, app intents and phone actions cannot be proven by unit tests alone.

## Before installing

1. Build the exact branch `batch-12-runtime-control-center` with JDK 17 and Android SDK 35.
2. Run `:app:assembleDebug` and `:app:testDebugUnitTest` locally.
3. Install the resulting debug APK as a fresh install for the first pass.
4. Keep the previous APK available before later upgrade/migration tests.
5. Use only a personal test contact/message and a harmless short reminder during the first run.
6. Do not test payments, OTPs, destructive actions, emergency calls or important real-world messages.

## Mandatory first-alpha checks

The in-app **Start personal device check** screen records these results:

1. Onboarding and settings persistence.
2. Living Presence launcher rendering.
3. Offline text chat.
4. Microphone speech recognition and spoken reply.
5. Phone Pulse device-state readings.
6. Open-app action.
7. Relationship/contact resolution and safe call handoff.
8. Message draft handoff without false delivery claims.
9. Mayra-owned reminder creation, due notification, Complete and Snooze.
10. Personal Agenda creation and management.
11. Notification Access capture, privacy redaction and grouped summary.
12. Supported notification RemoteInput reply and duplicate-send protection.
13. Background runtime immediate scan and diagnostics.
14. Optional online AI provider connection and offline fallback.

## Alpha acceptance gate

Controlled personal alpha use is acceptable when:

- The app installs and starts without a crash.
- Onboarding, chat, voice and settings pass.
- At least 10 of 14 feature checks pass.
- Reminder create/alert and global action stop both pass.
- No wrong-contact, duplicate-send or false-success behavior is observed.
- No sensitive notification or OTP content leaks into Mayra screens, speech or audit history.
- At most two checks are blocked by known device/OEM setup.
- There are zero unresolved crash-causing failures.

Daily dependable use is **not** accepted until all critical flows pass on at least two clean test sessions, including one phone reboot and one app upgrade.

## Required reboot/upgrade regression

After the first pass:

1. Create a reminder at least 10 minutes in the future.
2. Reboot the phone.
3. Confirm the reminder remains visible and eventually alerts.
4. Confirm background runtime schedule is restored.
5. Install a newer debug APK over the existing app.
6. Confirm settings, identities, reminders and agenda events persist.
7. Confirm stale notification reply handles do not survive process death and are reported honestly.

## Known conditional features

- Notification direct reply requires the source app to expose Android `RemoteInput`.
- Full incoming-call answer/reject/speaker control is not implemented and requires default-dialer/InCallService work.
- WhatsApp universal background sending is not guaranteed; current safe routes are intents, compose/handoff and supported notification reply.
- WorkManager reminder timing can be delayed by Android or Motorola battery policy.
- Online AI requires the owner's valid provider key and network access.
- Accessibility-based app control is not yet implemented and would require explicit manual enablement and visible deterministic behavior.

## Remaining work after the first alpha begins

The first alpha should generate the next coding priorities from real failures. Expected major remaining modules are:

1. Fresh compile/test/lint validation and APK packaging.
2. Crash and compatibility fixes discovered on the Motorola device.
3. Recurring reminder/event instance execution and event notifications.
4. Conflict/free-time intelligence.
5. Notes and voice-notes user experience.
6. File/PDF search and document intelligence UI.
7. Default assistant invocation prototype.
8. Default dialer/InCallService prototype.
9. WhatsApp assisted messaging adapter.
10. Advanced memory management and proactive daily briefing.

## Honest readiness estimate

- **Ready for source-level feature development:** yes.
- **Ready to attempt the first APK/device check:** yes, after one successful local compile.
- **Ready for controlled personal alpha daily experiments:** expected after the mandatory gate passes.
- **Ready for dependable unrestricted personal use:** no; real-device regressions and several major modules remain.
- **Ready for Play Store:** no; distribution hardening, declarations, policy review, release signing and broad compatibility testing remain future work.
