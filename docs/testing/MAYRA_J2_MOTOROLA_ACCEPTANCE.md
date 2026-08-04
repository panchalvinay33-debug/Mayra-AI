# Mayra AI — Motorola J2 Voice Acceptance

Status: CI #136 DEVICE RETEST IN PROGRESS — UNLOCKED INTENT/TRANSCRIPT + LAYOUT PASS; LOCK PRIVACY/TTS AUDIBLE PROOF PENDING
Date updated: 2026-08-04
Target device: Motorola Edge 70 Fusion / Android 16

## Last device-proven candidate

- Package: `ai.mayra.app.j2`
- Application source: `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 #106 / J1 #210 / Android CI #2101 / Governance #282: success
- Protected baseline: `baseline/mayra-0.2.1-j2-speech-support-green-106`
- Permission boundary: exactly `RECORD_AUDIO`

## Device-proven PASS from CI #106

- Digital assistant selection and Motorola Power-button invocation;
- downloaded Hindi (India) and English (India) on-device speech models;
- unlocked Hindi/Hinglish/English transcription;
- `kal subah saat baje` → visible `कल सुबह 7:00`;
- `open WhatsApp` transcript-only, no execution;
- orb, Mayra label, outside/root, Back and phone-lock dismissal;
- 20 invoke/speak-or-silence/dismiss cycles with no reported crash, duplicate orb, stuck mic, permanent recognizer-busy state or System UI restart;
- already-locked invocation and recognition;
- owner-reported reboot recovery, no-speech and rapid open/close checks OK.

## Device-discovered defect in CI #106

Locked invocation worked, but recognized/private transcript was visible before unlock and label/transcript overlapped near the bottom of the lock screen.

## Authoritative CI-green candidate under device retest

- Application source: `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 Voice Test #136: success
- J1 Assistant Test #239: success
- Android CI #2131: success
- Project Governance #312: success
- Artifact: `mayra-j2-voice-apk-136`
- Artifact ID: `8868518898`
- APK size: `19,209,329` bytes
- APK SHA-256: `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`
- Artifact ZIP SHA-256: `405406128d1f44a7bd9c90b71cc173d4bb352ba5d9e8c2863c1100c9a4d13b36`
- Protected baseline: `baseline/mayra-0.2.1-j2-privacy-tts-green-136`

This candidate keeps the same J2 engineering boundary and does not add action execution.

## What CI #136 adds

- keyguard-aware privacy rendering: locked screen shows only generic state rather than recognized transcript;
- no transcript-derived/private spoken reply while locked;
- separated orb/name/status layout to eliminate the observed overlap;
- offline-first Android TTS voice selection: Hindi India → English India → English US → another offline voice;
- speech rate 0.95 and neutral pitch;
- deterministic local spoken replies for greeting, time, capability, reminder and app-open intent;
- app/reminder requests are understood but explicitly not executed in J2;
- TTS and recognition stop on hide/destroy;
- unit tests cover greeting, deterministic time, no fake action-success claim and private unknown transcript classification.

## CI #136 Motorola evidence captured 2026-08-04

PASS from screenshots:

- clean-installed CI #136 after the expected engineering-package signing conflict was resolved by removing only the old J2 package;
- Mayra J2 is present alongside J1 and the full app as a temporary engineering package;
- unlocked Power-button Assistant invocation renders the orb over the app drawer;
- spoken `hello aaj ke mausam ki jankari do` was visibly captured as `Maine suna: हेलो आज के मौसम की जानकारी दो`;
- spoken `kya aaj tum mere liye reminder set kar sakti ho` was visibly captured as `Maine suna: क्या आज तुम मेरे लिए रिमाइंडर सेट कर सकती हो`;
- reminder/weather intent phrasing is therefore reaching the deterministic voice-response layer without falsely executing an action;
- orb/name/status spacing is visibly cleaner in this unlocked UI and the prior bottom-label garble is not present in these screenshots;
- no crash, duplicate orb or stuck recognizer is visible in this evidence.

Not yet proven by screenshot alone:

- whether TTS audio was actually audible and which installed voice was selected;
- whether the spoken reply wording after those intents is correct;
- whether the lock-screen privacy repair hides private transcript and suppresses private TTS before unlock;
- lock-while-listening behavior;
- full 20-cycle regression on CI #136;
- reboot recovery specifically on CI #136.

## Remaining consolidated Motorola checks

1. confirm Mayra audibly speaks after an unlocked recognized phrase and record whether the voice sounds acceptable;
2. say `open WhatsApp` and confirm J2 only gives a safe confirmation/handoff message and does not open WhatsApp;
3. say a reminder request and confirm J2 does not falsely claim the reminder was created;
4. invoke while already locked and confirm only generic `Listening…`/status is shown, with no private transcript or private spoken reply;
5. lock the phone while Mayra is listening and confirm clean privacy/lifecycle behavior;
6. do a short regression sweep (dismiss paths, mic cleanup, rapid invoke/dismiss, no-speech, repeated cycles);
7. reboot once and verify J2 remains selectable/functional.

## Promotion rule

J2 core Assistant/on-device voice foundation is already accepted from device evidence. Full `DEVICE_VERIFIED` promotion waits for the remaining CI #136 audible-TTS and lock-screen privacy checks. J2 remains a temporary engineering package; the final product remains one Mayra app.
