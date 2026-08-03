# Mayra AI — J2 Voice Session Feasibility Preflight

Status: APPROVED FOR INVOCATION-TIME VOICE FOUNDATION ONLY
Date: 2026-08-03
Gate: Issue #14
Target device: Motorola Edge 70 Fusion / Android 16
Rollback baseline before implementation: `baseline/mayra-0.2.1-j1-zero-permission-green-44` plus device evidence from J1 #68 role/invocation tests

## Owner outcome

When Mayra is selected as the Android Digital Assistant and invoked from the configured Motorola assistant trigger, the assistant surface should:

1. appear immediately;
2. visibly indicate listening / thinking / speaking state;
3. capture a short spoken request only after the user invokes Mayra;
4. prefer on-device speech recognition when Android reports it available;
5. fall back honestly when on-device recognition is unavailable;
6. stop microphone/listening when the session is dismissed, hidden, locked out or cancelled;
7. never pretend that an always-on wake phrase exists before a dedicated wake-word engine is integrated and benchmarked.

## Official Android path

- Use the user-selected `VoiceInteractionService` as the lightweight always-available system assistant service.
- Keep UI and heavier work inside `VoiceInteractionSessionService` / `VoiceInteractionSession`.
- Use `SpeechRecognizer.isOnDeviceRecognitionAvailable()` and `createOnDeviceSpeechRecognizer()` for invocation-time local speech recognition when available.
- Keep a bounded fallback path to Android recognition or an explicit unavailable state rather than an endless listener.
- Continue using the Android Assistant role and Motorola Digital Assistant trigger already device-proven in J1.

## Hard platform boundaries

- `SpeechRecognizer` is not intended for continuous recognition and must not be used as an always-on hotword loop.
- A dedicated offline wake-word detector is a separate future capability with its own battery, privacy and lock-screen benchmark.
- Background microphone access remains constrained by Android while-in-use permission rules; Assistant service status helps with system-started assistant flows but does not remove the requirement for `RECORD_AUDIO` consent.
- No hidden OEM component, root, Accessibility hack or microphone-policy bypass is allowed.

## Motorola-specific risks

- Digital Assistant selection and power-key invocation must remain configured by the owner.
- Lock-screen invocation may be allowed, gated or visually different under Motorola/Android policy; no private content should be surfaced before unlock.
- Battery optimization and process recreation must be tested after J2 voice is connected.
- On-device speech recognition availability must be measured on the actual Motorola instead of assumed from Android version.

## Permission/setup burden

J2 voice requires exactly one new runtime capability compared with zero-permission J1:

- `RECORD_AUDIO` — requested only when the owner starts/uses real voice interaction.

No contacts, notifications, SMS, call, overlay, accessibility or exact-alarm permission is part of J2 voice proof.

## Distribution impact

- Stable owner signing remains required for install-over-install updates.
- CI debug-signed artifacts are test artifacts only until owner signing secrets are configured.
- Play Protect must not be bypassed.

## Offline behavior

Preferred path: on-device Android speech recognition when reported available.

If unavailable:

- J2 may expose an explicit `On-device speech unavailable` state and optionally allow a separately approved system recognizer fallback.
- The app must not claim offline speech when Android reports it unavailable.

## Performance/battery budget

Invocation-time recognition target:

- microphone/listener active only while the visible assistant session is listening;
- stop on result, error, cancel, hide or destroy;
- no continuous `SpeechRecognizer` loop;
- no foreground microphone service for J2 invocation-time proof;
- observe CPU/temperature and 20 repeated voice-session cycles on Motorola before promotion.

Wake-word budget is deferred to a dedicated preflight and benchmark.

## Privacy/data path

- Invocation-time audio is processed on-device when the Android on-device recognizer is used.
- No transcript/audio leaves the device unless a later owner-approved cloud fallback explicitly does so.
- J2 test does not persist raw audio.
- Transcript persistence follows Mayra's existing conversation/memory rules only after full-assistant integration.

## Failure/fallback UX

Every voice failure must result in a bounded visible state: microphone permission needed, on-device recognizer unavailable, recognizer busy/error, no speech, cancelled, or timed out. Session must remain dismissible.

## Evidence plan

Automated:

- compile/lint all governed variants;
- J1/J2 manifest audit;
- no unexpected permissions/components;
- state reducer/controller tests for listening/thinking/speaking/error/cancel;
- lifecycle tests where practical.

Motorola:

1. invoke while unlocked;
2. grant microphone only when prompted;
3. verify on-device recognizer availability result;
4. speak short Hindi, Hinglish and English phrases;
5. verify bounded success/error behavior;
6. dismiss by Back, touch and screen lock;
7. repeat 20 cycles;
8. test lock-screen behavior separately;
9. reboot and recheck Assistant role and invocation.

## Entry decision

APPROVED now:

- touch-dismiss lifecycle repair;
- assistant state model;
- invocation-time on-device recognizer capability detection;
- short bounded recognition session after explicit assistant invocation;
- listening/thinking/speaking visual state plumbing.

NOT APPROVED yet:

- continuous microphone listening;
- offline wake phrase / hotword loop;
- local LLM model integration;
- phone-call control;
- AI caller message-taking.

Each deferred capability needs its own Issue #14 preflight and Motorola benchmark before implementation.
