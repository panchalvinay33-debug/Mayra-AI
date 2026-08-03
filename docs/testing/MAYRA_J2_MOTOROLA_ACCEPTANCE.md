# Mayra AI — Motorola J2 Voice Acceptance

Status: DEVICE REPAIR ACTIVE — CI #90 INVOKES/MIC PASS, TRANSCRIPT STILL BLOCKED
Date updated: 2026-08-03
Target device: Motorola Edge 70 Fusion / Android 16

## Last green retest candidate

- Label: `Mayra J2 Voice Test`
- Package: `ai.mayra.app.j2`
- Version: `0.2.1-j2`
- Application source: `e706bdfb8f53006825404db99a51f466aa251fc4`
- J2 Voice Test: #90 — success
- J1 Assistant Test: #194 — success
- Android CI: #2085 — success
- Project Governance: #266 — success
- Artifact: `mayra-j2-voice-apk-90`
- Artifact ID: `8865632199`
- APK size: `19,192,945` bytes
- APK SHA-256: `2c1e00db4a2bfd98993eb87fe091c5373931153eb3b5ac2252914d4441ac230c`
- Artifact ZIP SHA-256: `bbe4936bc5caec8a08d244ea28f82cf09daabceb49bde47b20ad1678933521b9`
- Protected baseline: `baseline/mayra-0.2.1-j2-locale-repair-green-90`
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_J2_LOCALE_REPAIR_CI90.md`

J2 #90 requests exactly `android.permission.RECORD_AUDIO` and excludes internet/provider, contacts, notifications, reminders, boot recovery, notification listener, WorkManager, Room, documents, personal memory, full chat runtime and call control.

## Physical evidence through 23:32 IST

PASS on Motorola:

- J2 #90 clean-installed after the expected CI-signature package conflict was resolved by uninstalling only the engineering J2 package.
- J2 opened normally.
- J2 displayed `Assistant: Mayra J2 selected ✓`.
- J2 displayed `Microphone: allowed ✓`.
- J2 displayed `On-device speech: available ✓`.
- Motorola Power-button Assistant invocation launched the Mayra J2 session over Home.
- Mayra orb/label rendered.

Historical CI #18 failure:

- visible state `Speech language unavailable`;
- no transcript;
- no crash and no false transcript.

New CI #90 failure:

- visible state changed to `Speech recognizer unavailable`;
- no transcript was produced;
- Assistant/orb invocation remained functional.

This proves the explicit-locale repair moved past the original immediate language error, but the retry path still churned/recreated the OEM on-device recognizer and reached an unavailable/server state. CI-green therefore did not equal Motorola transcript acceptance.

## Current source repair after CI #90 device failure

The next source repair is intentionally not an owner candidate until fresh exact-head CI passes.

Changes:

- keep one on-device `SpeechRecognizer` instance for the bounded recognition attempt instead of destroy/recreate on every locale retry;
- on Android 13+ use `SpeechRecognizer.checkRecognitionSupport()` to ask the OEM service which on-device languages are actually installed before listening;
- prefer installed languages using Mayra's device/Hindi/India/English policy;
- if no on-device language is installed but downloadable language support exists, surface `On-device speech language pack needed` instead of a vague recognizer error;
- if the OEM cannot report recognition support, fall back to bounded locale trials;
- reuse the recognizer and delay language retry by 450 ms to avoid immediate service churn;
- still no cloud STT fallback and no endless retry loop.

Official Android API basis:

- `isOnDeviceRecognitionAvailable()` only proves an on-device recognition service exists; it does not prove a requested language model is installed.
- Android 13+ `checkRecognitionSupport()` can report installed, downloadable/supported and pending on-device languages for a recognition request.

## A. Installation/update

- [x] CI #90 installs without Play Protect bypass after removing only conflicting engineering J2 package.
- [x] J2 #90 opens normally.
- [x] microphone permission/readiness remains correct.
- [x] on-device speech service reports available.

## B. Assistant selection

- [x] J2 appears as Digital assistant candidate.
- [x] J2 #90 selected as default Digital assistant.
- [x] Power-button action invokes Mayra J2.

## C. Unlocked transcript — CURRENT BLOCKER

- [x] assistant surface appears.
- [x] microphone/on-device readiness is present before invocation.
- [x] original CI #18 immediate `Speech language unavailable` failure reproduced and bounded.
- [x] CI #90 moved to `Speech recognizer unavailable`, exposing recognizer lifecycle/support-discovery problem.
- [ ] `Mayra namaste` transcript.
- [ ] `kal subah saat baje` transcript.
- [ ] `open WhatsApp` transcript only; J2 must not execute it.
- [ ] short English transcript.

Do not repeatedly retest CI #90; the device failure is now known and source-repaired. Wait for a new fully green artifact.

## D. Direct dismissal/lifecycle repair

After transcript proof:

- [ ] orb tap closes session;
- [ ] outside/root tap closes session;
- [ ] `Mayra` label tap closes session;
- [x] Back closes session in prior common-session device test.
- [x] phone lock closes current session in prior common-session device test.
- [ ] microphone privacy indicator disappears after dismissal;
- [ ] no stuck/duplicate orb.

## E. Repeated stability

Pending transcript proof. Run 20 cycles only after one successful transcript.

## F. Already-locked invocation

Pending transcript proof. Record Motorola policy; do not force with hacks.

## G. Reboot/recovery

Pending transcript proof.

## H. Failure cases

- [ ] microphone denied;
- [ ] no speech;
- [x] language unavailable — CI #18 physical observation;
- [x] recognizer unavailable — CI #90 physical observation;
- [ ] language pack needed support-state;
- [ ] recognizer busy/error;
- [ ] rapid invoke/dismiss;
- [ ] screen lock while listening.

## Promotion rule

J2 becomes `DEVICE_VERIFIED` only after successful unlocked recognition plus dismissal, repeated lifecycle, locked-screen behavior and reboot recovery are physically recorded.

J2 success does not prove production wake phrase, local LLM, full Mayra conversation or call control.
