# Mayra AI — Motorola J2 Voice Acceptance

Status: LOCKED INVOCATION PASS — LOCK-SCREEN PRIVACY/LAYOUT REPAIR REQUIRED; REBOOT ACCEPTANCE PENDING
Date updated: 2026-08-04
Target device: Motorola Edge 70 Fusion / Android 16

## Authoritative device candidate

- Label: `Mayra J2 Voice Test`
- Package: `ai.mayra.app.j2`
- Version: `0.2.1-j2`
- Application source: `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 Voice Test: #106 — success
- J1 Assistant Test: #210 — success
- Android CI: #2101 — success
- Project Governance: #282 — success
- Artifact: `mayra-j2-voice-apk-106`
- Artifact ID: `8866441207`
- APK size: `19,209,329` bytes
- APK SHA-256: `d0917d17b50429a843f3a5e688580df66f3eea678be4806b44ef9f1535adeb6e`
- Artifact ZIP SHA-256: `b2109366a0140a66f85fef3cd6a85a95263815643ae86076ba0a9f20194140db`
- Protected baseline: `baseline/mayra-0.2.1-j2-speech-support-green-106`

J2 requests exactly `android.permission.RECORD_AUDIO` and excludes internet/provider, contacts, notifications, reminders, boot recovery, notification listener, WorkManager, Room, documents, personal memory, full chat runtime and call control.

## Physical evidence through 00:29 IST

PASS on Motorola:

- J2 clean-installed and opened.
- J2 selected as Android Digital assistant.
- microphone permission allowed.
- Android on-device speech service available.
- Motorola Power-button Assistant invocation launches Mayra over Home.
- Mayra orb/label renders.
- Android correctly surfaced `On-device speech language pack needed` before model availability was confirmed.
- On-device settings showed downloaded `Hindi (India)` and `English (India)` packs.
- After model availability, J2 produced unlocked on-device Hindi/Hinglish/English transcripts.
- Spoken `kal subah saat baje` was visibly transcribed as `कल सुबह 7:00`.
- Spoken `open WhatsApp` was visibly transcribed as `ओपन व्हाट्सएप`; J2 did not execute the command.
- Spoken `hello Mayra how are you` produced a visible Hindi-script phonetic transcript.
- Orb tap, `Mayra` label tap, outside/root tap, Back and phone lock dismiss the session.
- Owner completed 20 invoke/speak-or-silence/dismiss cycles with no observed problem.
- No crash, System UI restart, duplicate orb, permanently busy recognizer or stuck microphone indicator was reported during the 20-cycle test.
- Orb/animation returned normally on repeated invocation.
- Starting from an already locked phone, the Power-button Assistant trigger launches Mayra over the lock screen.
- Locked-screen orb rendering and recognition are physically proven.
- No cloud STT fallback was used.

Locked-screen failures discovered:

- recognized/private transcript is visible before device unlock;
- `Mayra` label and recognized text overlap/garble near the bottom of the lock screen;
- therefore locked invocation works, but the current locked presentation is not privacy-safe or visually acceptable for promotion.

Historical bounded failures:

- CI #18: `Speech language unavailable`.
- CI #90: `Speech recognizer unavailable`.
- CI #106 before usable model registration: `On-device speech language pack needed`.

## A. Installation/update — PASS

- [x] J2 #106 installs without Play Protect bypass after removing only a conflicting engineering J2 package if required.
- [x] J2 #106 opens normally.
- [x] microphone readiness remains correct.
- [x] on-device speech service reports available.

## B. Assistant selection — PASS

- [x] J2 appears as a Digital assistant candidate.
- [x] J2 is selected as default Digital assistant.
- [x] Power-button action invokes Mayra J2.

## C. Unlocked transcript — CORE PASS

- [x] assistant surface appears.
- [x] microphone/on-device speech path activates.
- [x] at least one short Hindi phrase produces a transcript.
- [x] `kal subah saat baje` produces visible `कल सुबह 7:00`.
- [x] `open WhatsApp` produces transcript only and does not execute the command.
- [x] short English phrase produces a visible transcript.
- [x] mixed English/Hindi input currently follows the Hindi model and can render English words phonetically in Devanagari.

J2 is intentionally transcript-only.

## D. Direct dismissal/lifecycle — PASS

- [x] orb tap closes session.
- [x] outside/root tap closes session.
- [x] `Mayra` label tap closes session.
- [x] Back closes session.
- [x] phone lock closes session.
- [x] microphone privacy indicator does not remain stuck after repeated dismissal.
- [x] no stuck/duplicate orb observed.

## E. Repeated stability — PASS

Owner completed 20 invoke → speak/no-speech → dismiss cycles.

- [x] no app crash.
- [x] no System UI restart.
- [x] no permanently busy recognizer.
- [x] no duplicate orb.
- [x] mic indicator does not remain active.
- [x] animation returns on every invocation.

## F. Already-locked invocation — FUNCTIONAL PASS / PRIVACY FAIL

- [x] lock phone first.
- [x] Power-button Assistant trigger launches Mayra while already locked.
- [x] orb renders and speech recognition runs.
- [ ] no private transcript before unlock — FAIL: recognized text was visible.
- [ ] clean non-overlapping lock-screen layout — FAIL: label/transcript overlap.
- [ ] locked presentation must be repaired to show only a generic state such as `Listening…` before unlock.
- [ ] after unlock, private transcript may be shown in the normal session UI.

No accessibility/root/OEM-private workaround is authorized. The repair must use normal keyguard state detection and privacy-safe rendering.

## G. Reboot/recovery — PENDING

- [ ] reboot phone.
- [ ] verify J2 remains selected as Digital assistant.
- [ ] invoke J2 and complete one short transcript.
- [ ] dismiss cleanly.

## H. Failure cases

- [ ] microphone denied.
- [ ] no speech.
- [x] language unavailable — CI #18 physical observation.
- [x] recognizer unavailable — CI #90 physical observation.
- [x] language pack needed — CI #106 physical observation before model availability.
- [x] rapid repeated invoke/dismiss — 20-cycle stability pass.
- [x] already-locked invocation — functional pass, privacy/layout fail.
- [ ] screen lock while actively listening.

## Promotion rule

J2 core on-device unlocked speech recognition, direct dismissal, 20-cycle stability and already-locked invocation are physically proven. Full `DEVICE_VERIFIED` status remains blocked until lock-screen transcript privacy/layout is repaired and reboot recovery passes.

J2 success does not prove production wake phrase, local LLM, full Mayra conversation or call control.
