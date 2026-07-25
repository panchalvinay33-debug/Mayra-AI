# Mayra AI — Personal Alpha Device Check

This is the owner-first validation gate for the private sideloaded build. It is not a Play Store release checklist.

## Current decision

Mayra is ready to enter a controlled personal-device alpha as soon as the current branch successfully compiles into a debug APK. The latest source is not yet claimed build-verified because GitHub Actions is failing before Checkout with an empty step list.

Physical testing is necessary because voice, overlays, Accessibility, notifications, WorkManager, OEM battery management, contacts, app intents and phone actions cannot be proven by unit tests alone.

## Before installing

1. Build the exact branch `batch-12-runtime-control-center` with JDK 17 and Android SDK 35.
2. Run `:app:assembleDebug` and `:app:testDebugUnitTest` locally.
3. Install the resulting debug APK as a fresh install for the first pass.
4. Keep the previous APK available before later upgrade/migration tests.
5. Use only a personal test contact/message and a harmless short reminder during the first run.
6. Do not test payments, OTPs, destructive actions, emergency calls or important real-world messages.

## Mandatory first-alpha checks

The in-app **Start personal device check** screen records the core results. Floating and assistive checks are recorded manually until they are added to that screen.

1. Onboarding and settings persistence.
2. Living Home launcher and animated Mayra rendering.
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
15. Floating Mayra overlay: start, drag, edge dock, position restore, compact panel, minimize and stop.
16. Optional assistive context: explicit enablement, foreground-app detection, visible-text snapshot and password/OTP filtering.

## Floating Mayra device flow

1. Open `⋮ → Mayra Access → Floating Mayra`.
2. Grant **Display over other apps**.
3. Start Floating Mayra and return to the launcher or another harmless app.
4. Drag the Mayra face to both edges and confirm it docks without leaving the screen.
5. Tap it and confirm the panel shows Talk, Type, Living Home, Minimize and Stop.
6. Move it, stop it, restart it and confirm the saved position returns.
7. Reboot the phone and confirm it restores only when the user preference is on and overlay access still exists.
8. Revoke overlay access and confirm Mayra does not pretend the bubble is active.

## Assistive context safety flow

1. Open `⋮ → Mayra Access → Permissions & owner setup`.
2. Read the Assistive Screen Context disclosure before opening Android Accessibility settings.
3. Enable only **Mayra assistive context**.
4. Open a harmless app and confirm Mayra detects only current visible context.
5. Open a password/PIN field and confirm its content is not surfaced.
6. Confirm Mayra does not click, type, submit, send or navigate automatically in this foundation build.
7. Disable the service and confirm readiness returns to action-required.

## Alpha acceptance gate

Controlled personal alpha use is acceptable when:

- The app installs and starts without a crash.
- Onboarding, chat, voice and settings pass.
- At least 12 of 16 checks pass.
- Reminder create/alert, global action stop and Floating Mayra stop all pass.
- No wrong-contact, duplicate-send or false-success behavior is observed.
- No sensitive notification, password, PIN or OTP content leaks into Mayra screens, speech or audit history.
- At most two checks are blocked by known device/OEM setup.
- There are zero unresolved crash-causing failures.

Daily dependable use is **not** accepted until all critical flows pass on at least two clean test sessions, including one phone reboot and one app upgrade.

## Required reboot/upgrade regression

After the first pass:

1. Create a reminder at least 10 minutes in the future.
2. Enable Floating Mayra and place it on a known screen edge.
3. Reboot the phone.
4. Confirm the reminder remains visible and eventually alerts.
5. Confirm background runtime schedule is restored.
6. Confirm Floating Mayra restores only when its saved preference is enabled.
7. Install a newer debug APK over the existing app.
8. Confirm settings, identities, reminders, agenda events and floating position persist.
9. Confirm stale notification reply handles do not survive process death and are reported honestly.

## Known conditional features

- Notification direct reply requires the source app to expose Android `RemoteInput`.
- Full incoming-call answer/reject/speaker control is not implemented and requires default-dialer/InCallService work.
- WhatsApp universal background sending is not guaranteed; safe routes are intents, compose/handoff and supported notification reply.
- WorkManager reminder timing can be delayed by Android or Motorola battery policy.
- Online AI requires the owner's valid provider key and network access.
- Floating overlays require explicit Android special access and a visible foreground-service notification.
- Assistive context is read-only in this foundation: no automatic clicks, typing, sending or protected-field access.
- Android/OEM Accessibility output differs between apps; an empty snapshot must be reported honestly.

## Remaining work after the first alpha begins

The first alpha should generate the next coding priorities from real failures. Expected major remaining modules are:

1. Fresh compile/test/lint validation and APK packaging.
2. Crash and compatibility fixes discovered on the Motorola device.
3. Floating panel voice auto-start and richer state animation.
4. User-invoked screen-context summary and app-specific suggestions.
5. Deterministic, visible assistive actions with confirmations where legally and technically supported.
6. Recurring reminder/event execution and event notifications.
7. Conflict/free-time intelligence.
8. Notes, voice notes and file/PDF intelligence UI.
9. Default assistant invocation prototype.
10. Default dialer/InCallService prototype.
11. WhatsApp assisted messaging adapter.
12. Advanced memory management and proactive daily briefing.

## Honest readiness estimate

- **Ready for source-level feature development:** yes.
- **Ready to attempt the next APK/device check:** yes, after one successful local compile.
- **Ready for controlled personal alpha daily experiments:** expected after the mandatory gate passes.
- **Ready for dependable unrestricted personal use:** no; real-device regressions and several major modules remain.
- **Ready for Play Store:** no; distribution hardening, declarations, policy review, release signing and broad compatibility testing remain future work.
