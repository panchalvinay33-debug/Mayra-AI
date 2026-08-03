# Mayra AI — Motorola J2 Voice Acceptance

Status: DEVICE TEST ACTIVE — ASSISTANT/MIC/ON-DEVICE STT CAPABILITY PASS; LANGUAGE RETRY REPAIR IN CI
Date updated: 2026-08-03
Target device: Motorola Edge 70 Fusion / Android 16

## Authoritative tested candidate

- Label: `Mayra J2 Voice Test`
- Package: `ai.mayra.app.j2`
- Version: `0.2.1-j2`
- Source: `ef809bbdaca80f3b953483499dc03de8e091339f`
- J2 Voice Test: #18 — success
- J1 Assistant Test: #122 — success
- Android CI: #2013 — success
- Project Governance: #194 — success
- Artifact: `mayra-j2-voice-apk-18`
- Artifact ID: `8863135214`
- APK size: `19,192,945` bytes
- APK SHA-256: `ef48c264f841efd7891e335848e90f38654a6dd25f70d10b0a3afd08b968ecbc`
- Protected baseline: `baseline/mayra-0.2.1-j2-voice-green-18`

## Package boundary proven by CI

J2 requests exactly `android.permission.RECORD_AUDIO` and excludes internet/provider, contacts, notifications, reminders, boot recovery, notification listener, WorkManager, Room, documents, personal memory, full chat runtime and call control.

## Device evidence — 22:40 to 22:46 IST

### Installation/readiness

PASS:

- J2 installed and opened on the Motorola.
- Microphone permission was granted and J2 displayed `Microphone: allowed ✓`.
- Android reported `On-device speech: available ✓`.
- J2 readiness screen rendered normally.

### Assistant selection/invocation

PASS:

- J2 was selected as the Android Digital assistant.
- Motorola Power-button Assistant invocation launched the Mayra J2 VoiceInteractionSession on the Home screen.
- Android microphone privacy indicator appeared, proving the invoked J2 session obtained active microphone access.
- Mayra orb and label rendered over Home.

### First recognition result

FAIL — bounded and diagnosable:

- Visible session state reported `Speech language unavailable`.
- No transcript was produced.
- The app/session did not crash and remained dismissible.

Root cause found in source:

- J2 recognition request did not explicitly provide a speech locale.
- Android reported the on-device recognizer itself as available, but the implicit/default recognition language was unavailable for the on-device model.

Repair chain:

- `MayraSpeechLocalePolicy` introduces a bounded locale order: device locale → `hi-IN` → `en-IN` → `en-US`, with duplicates removed.
- `MayraOnDeviceSpeechRecognizer` now explicitly sets the recognition language.
- `ERROR_LANGUAGE_NOT_SUPPORTED` / `ERROR_LANGUAGE_UNAVAILABLE` automatically advance to the next locale rather than ending the session immediately.
- Maximum locale attempts are bounded; there is no endless retry loop.
- Unit tests cover locale ordering, duplicate removal and blank-device-locale fallback.

The repair must pass fresh J2/J1/Android/Governance CI before a replacement APK is shared.

## A. Installation and permission

- [x] App opens normally.
- [x] Granting microphone changes readiness correctly.
- [x] On-device speech recognition reports available on the Motorola.
- [ ] Denying microphone produces a clear bounded state and does not crash.

## B. Assistant selection

- [x] J2 appears in Digital assistant candidates.
- [x] J2 can be selected.
- [x] J2 operation does not require J1 after J2 is selected.

## C. Unlocked voice invocation

- [x] Mayra assistant surface appears over the current screen.
- [x] On-device speech capability is reported available.
- [x] Microphone becomes active during the invoked session.
- [ ] `Mayra namaste` transcript — blocked by tested build language-unavailable bug; repair in CI.
- [ ] `kal subah saat baje` transcript.
- [ ] `open WhatsApp` transcript only; J2 must not execute it.
- [ ] short English phrase transcript.

## D. Dismissal/lifecycle repair

- [ ] orb tap closes session.
- [ ] outside/root tap closes session.
- [ ] `Mayra` label tap closes session.
- [x] Back dismissal was already physically proven in J1 common session behavior.
- [x] Lock dismissal was already physically proven in J1 common session behavior.
- [ ] microphone indicator stops after every J2 dismissal.

## E. Repeated stability

20-cycle test remains pending after the language-retry repair passes CI and device transcription works.

## F. Locked-screen invocation

Pending after unlocked transcript proof.

## G. Reboot/recovery

Pending after unlocked transcript proof.

## H. Failure cases

- [ ] microphone denied.
- [ ] no speech.
- [x] language unavailable — observed on CI #18 candidate, bounded error, repair implemented.
- [ ] recognizer busy/error.
- [ ] rapid invoke/dismiss.
- [ ] screen lock during listening.

## Promotion rule

J2 becomes `DEVICE_VERIFIED` only after installation, permission handling, Assistant selection, successful unlocked recognition, dismissal, repeated lifecycle, locked-screen behavior and reboot recovery are recorded on Motorola.

J2 success does not prove production wake phrase, local LLM, full Mayra conversation or call control.
