# Mayra AI — Motorola J2 Voice Acceptance

Status: CORE DEVICE ACCEPTANCE PASS — PRIVACY + OFFLINE TTS CI #136 GREEN; CONSOLIDATED MOTOROLA RETEST NEXT
Date updated: 2026-08-04
Target device: Motorola Edge 70 Fusion / Android 16

## Last device-proven candidate

- Package: `ai.mayra.app.j2`
- Application source: `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 #106 / J1 #210 / Android CI #2101 / Governance #282: success
- Protected baseline: `baseline/mayra-0.2.1-j2-speech-support-green-106`
- Permission boundary: exactly `RECORD_AUDIO`

## Device-proven PASS

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

## New authoritative CI-green candidate — device retest required

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

## One consolidated Motorola round

Test CI #136 in one round:

1. unlocked Hindi/Hinglish/English recognition;
2. audible Mayra reply and voice quality;
3. `open WhatsApp` safe confirmation without execution;
4. reminder request safe confirmation;
5. all dismissal methods and mic cleanup;
6. 20-cycle regression;
7. already-locked invocation with no private transcript or private spoken reply before unlock;
8. lock while listening;
9. no-speech and rapid open/close;
10. reboot recovery.

## Promotion rule

J2 core Assistant/on-device voice foundation is already accepted from device evidence. Full `DEVICE_VERIFIED` promotion waits only for this consolidated CI #136 privacy + spoken-reply Motorola round. J2 remains a temporary engineering package; the final product remains one Mayra app.
