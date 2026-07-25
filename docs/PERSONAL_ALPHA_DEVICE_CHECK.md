# Mayra AI — Personal Alpha Device Check

This is the owner-first validation gate for the private sideloaded build. It is not a Play Store release checklist.

## Current decision

Mayra is ready to enter a controlled personal-device alpha as soon as the current branch successfully compiles into a debug APK. The latest source is not yet claimed build-verified because GitHub Actions is failing before Checkout with an empty step list.

Physical testing is necessary because voice, overlays, Accessibility, notifications, WorkManager, OEM battery management, contacts, app intents, phone actions, document providers and local-memory persistence cannot be proven by unit tests alone.

## Before installing

1. Build the exact branch `batch-12-runtime-control-center` with JDK 17 and Android SDK 35.
2. Run `:app:assembleDebug` and `:app:testDebugUnitTest` locally.
3. Install the resulting debug APK as a fresh install for the first pass.
4. Keep the previous APK available before later upgrade/migration tests.
5. Use only a personal test contact/message and a harmless short reminder during the first run.
6. Do not test payments, OTPs, destructive actions, emergency calls or important real-world messages.

## Mandatory first-alpha checks

The in-app **Start personal device check** screen records the core results. Floating, assistive, Memory V2, voice-note and document checks are recorded manually until they are added to that screen.

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
21. Voice Notes: capture speech, edit the transcript, save it and confirm it appears as a voice transcript in Memory.
22. Document Library: add a PDF/document, search it by name, reopen it after app restart and remove it cleanly.

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

## Voice Notes flow

1. Open `Memory & notes → Voice note`.
2. Record a harmless sentence and confirm Android speech recognition returns a transcript.
3. Edit the transcript before saving and confirm nothing is saved before pressing Save.
4. Save it and confirm the entry appears in Memory as `voice transcript` with the `voice-note` tag.
5. Dictate credential-like test wording and confirm the normal-memory privacy guard rejects it.

## Document Library flow

1. Open `Memory & notes → Documents`.
2. Add one harmless PDF or text document through Android's document picker.
3. Confirm the name, MIME type and size are indexed locally.
4. Search by part of the filename and open it in a compatible viewer.
5. Close and reopen Mayra; confirm persistent URI access still allows opening the file.
6. Remove the library entry and confirm Mayra releases its persisted read permission where Android permits.
7. Confirm Mayra does not claim full-text search, page extraction or AI summary in this foundation build.

## Alpha acceptance gate

Controlled personal alpha use is acceptable when:

- The app installs and starts without a crash.
- Onboarding, chat, voice and settings pass.
- At least 17 of 22 checks pass.
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
4. Add one document to the local library.
5. Reboot the phone.
6. Confirm the reminder remains visible and eventually alerts.
7. Confirm background runtime schedule is restored.
8. Confirm Floating Mayra restores only when its saved preference is enabled.
9. Confirm memory, pin, checklist and document-library state remain intact.
10. Install a newer debug APK over the existing app.
11. Confirm settings, identities, reminders, agenda events, memories, document entries and floating position persist.
12. Confirm stale notification reply handles do not survive process death and are reported honestly.

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
- Voice Notes depends on the Android speech-recognition service installed on the device.
- Document Library stores persistent URI access and metadata only; full document parsing is not implemented yet.

## Remaining work after the first alpha begins

1. Fresh compile/test/lint validation and APK packaging.
2. Crash and compatibility fixes discovered on the Motorola device.
3. Floating panel voice auto-start and richer state animation.
4. User-invoked screen-context summary and app-specific suggestions.
5. Deterministic, visible assistive actions with confirmations where legally and technically supported.
6. Recurring reminder/event execution and event notifications.
7. Conflict/free-time intelligence.
8. Full PDF/text extraction, page search and privacy-safe document summaries.
9. Default assistant invocation prototype.
10. Default dialer/InCallService prototype.
11. WhatsApp assisted messaging adapter.
12. Semantic memory ranking, edit/restore UI and encrypted secure-reference vault.

## Honest readiness estimate

- **Ready for source-level feature development:** yes.
- **Ready to attempt the next APK/device check:** yes, after one successful local compile.
- **Ready for controlled personal alpha daily experiments:** expected after the mandatory gate passes.
- **Ready for dependable unrestricted personal use:** no; real-device regressions and several major modules remain.
- **Ready for Play Store:** no; distribution hardening, declarations, policy review, release signing and broad compatibility testing remain future work.
