# Mayra AI — Personal Alpha Device Check

This is the owner-first validation gate for the private sideloaded build. It is not a Play Store release checklist.

## Current decision

Mayra is ready to enter a controlled personal-device alpha as soon as the current branch successfully compiles into a debug APK. The latest source is not yet claimed build-verified because GitHub Actions is failing before Checkout with an empty step list.

Physical testing is necessary because voice, overlays, Accessibility, notifications, WorkManager, OEM battery management, contacts, app intents, phone actions and local-memory persistence cannot be proven by unit tests alone.

## Before installing

1. Build the exact branch `batch-12-runtime-control-center` with JDK 17 and Android SDK 35.
2. Run `:app:assembleDebug` and `:app:testDebugUnitTest` locally.
3. Install the resulting debug APK as a fresh install for the first pass.
4. Keep the previous APK available before later upgrade/migration tests.
5. Use only a personal test contact/message and a harmless short reminder during the first run.
6. Do not test payments, OTPs, destructive actions, emergency calls or important real-world messages.

## Mandatory first-alpha checks

The in-app **Start personal device check** screen records the core results. Floating, assistive and Memory V2 checks are recorded manually until they are added to that screen.

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
17. Memory & Notes: save/search/pin/archive a normal note and complete a checklist item.
18. Memory privacy: confirm OTP, password, card-like and secret-key content is rejected from normal memory.
19. Chat recall: ask a question matching a saved preference and confirm the relevant answer without exposing unrelated memory.
20. Personal briefing: confirm pinned/open items appear while sensitive entries remain excluded.

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

## Memory V2 safety flow

1. Open `⋮ → Memory → Memory & notes`.
2. Save one normal note, one pinned idea and one checklist with two items.
3. Search by a title, body word and tag; confirm the expected note appears.
4. Complete one checklist item, leave one open and confirm Memory health updates.
5. Archive a note and confirm it leaves the active list.
6. Attempt to save an OTP, password, card-like number and API key; each must be rejected.
7. Return to Living Home and confirm the My Day card reflects only non-sensitive pinned/open items.
8. Ask Mayra about the saved normal preference; relevant memory may inform the answer.
9. Ask an unrelated question and confirm unrelated private notes are not quoted or dumped.
10. Install a later APK over the existing one and confirm notes, checklist state and pins persist.

## Alpha acceptance gate

Controlled personal alpha use is acceptable when:

- The app installs and starts without a crash.
- Onboarding, chat, voice and settings pass.
- At least 15 of 20 checks pass.
- Reminder create/alert, global action stop, Floating Mayra stop and memory privacy rejection all pass.
- No wrong-contact, duplicate-send or false-success behavior is observed.
- No sensitive notification, password, PIN, OTP, card-like or secret-key content leaks into Mayra screens, speech, chat context or audit history.
- At most two checks are blocked by known device/OEM setup.
- There are zero unresolved crash-causing failures.

Daily dependable use is **not** accepted until all critical flows pass on at least two clean test sessions, including one phone reboot and one app upgrade.

## Required reboot/upgrade regression

After the first pass:

1. Create a reminder at least 10 minutes in the future.
2. Enable Floating Mayra and place it on a known screen edge.
3. Create a pinned memory and an incomplete checklist.
4. Reboot the phone.
5. Confirm the reminder remains visible and eventually alerts.
6. Confirm background runtime schedule is restored.
7. Confirm Floating Mayra restores only when its saved preference is enabled.
8. Confirm memory, pin and checklist state remain intact.
9. Install a newer debug APK over the existing app.
10. Confirm settings, identities, reminders, agenda events, memories and floating position persist.
11. Confirm stale notification reply handles do not survive process death and are reported honestly.

## Known conditional features

- Notification direct reply requires the source app to expose Android `RemoteInput`.
- Full incoming-call answer/reject/speaker control is not implemented and requires default-dialer/InCallService work.
- WhatsApp universal background sending is not guaranteed; safe routes are intents, compose/handoff and supported notification reply.
- WorkManager reminder timing can be delayed by Android or Motorola battery policy.
- Online AI requires the owner's valid provider key and network access.
- Floating overlays require explicit Android special access and a visible foreground-service notification.
- Assistive context is read-only in this foundation: no automatic clicks, typing, sending or protected-field access.
- Android/OEM Accessibility output differs between apps; an empty snapshot must be reported honestly.
- Memory recall is lexical and bounded in this build; it is not a perfect semantic-memory system.
- Normal memory intentionally rejects credential-like content rather than acting as a password manager.

## Remaining work after the first alpha begins

The first alpha should generate the next coding priorities from real failures. Expected major remaining modules are:

1. Fresh compile/test/lint validation and APK packaging.
2. Crash and compatibility fixes discovered on the Motorola device.
3. Floating panel voice auto-start and richer state animation.
4. User-invoked screen-context summary and app-specific suggestions.
5. Deterministic, visible assistive actions with confirmations where legally and technically supported.
6. Recurring reminder/event execution and event notifications.
7. Conflict/free-time intelligence.
8. Voice-note recording and transcription linked to Memory Center.
9. File/PDF intelligence UI.
10. Default assistant invocation prototype.
11. Default dialer/InCallService prototype.
12. WhatsApp assisted messaging adapter.
13. Semantic memory ranking, edit/restore UI and encrypted secure-reference vault.

## Honest readiness estimate

- **Ready for source-level feature development:** yes.
- **Ready to attempt the next APK/device check:** yes, after one successful local compile.
- **Ready for controlled personal alpha daily experiments:** expected after the mandatory gate passes.
- **Ready for dependable unrestricted personal use:** no; real-device regressions and several major modules remain.
- **Ready for Play Store:** no; distribution hardening, declarations, policy review, release signing and broad compatibility testing remain future work.
