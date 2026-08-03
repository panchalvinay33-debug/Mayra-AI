# Mayra AI — Motorola J2 Voice Acceptance

Status: CORE DEVICE ACCEPTANCE PASS — CONSOLIDATED PRIVACY + SPOKEN-REPLY RETEST NEXT
Date updated: 2026-08-04
Target device: Motorola Edge 70 Fusion / Android 16

## Last device-proven candidate

- Package: `ai.mayra.app.j2`
- Application source: `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 #106 / J1 #210 / Android CI #2101 / Governance #282: success
- Artifact ID: `8866441207`
- APK SHA-256: `d0917d17b50429a843f3a5e688580df66f3eea678be4806b44ef9f1535adeb6e`
- Protected baseline: `baseline/mayra-0.2.1-j2-speech-support-green-106`
- Permission boundary: exactly `RECORD_AUDIO`

## Device-proven PASS

- clean install without Play Protect bypass;
- Digital assistant selection and Motorola Power-button invocation;
- on-device Hindi/India and English/India speech packs;
- unlocked Hindi/Hinglish/English transcription;
- `kal subah saat baje` → visible `कल सुबह 7:00`;
- `open WhatsApp` transcript-only, no command execution;
- orb, Mayra label, outside/root, Back and phone-lock dismissal;
- 20 invoke/speak-or-silence/dismiss cycles with no reported crash, duplicate orb, stuck mic, permanent recognizer-busy state or System UI restart;
- already-locked invocation and recognition;
- owner reported the remaining consolidated functional checks, including reboot recovery and rapid/no-speech behavior, as OK.

## Device-discovered repair item

CI #106 showed recognized/private transcript before unlock and label/transcript overlap on the lock screen. Functional locked invocation therefore passes, but privacy/layout requires repair.

## New consolidated source batch — NOT YET DEVICE VERIFIED

The next source batch adds:

- keyguard-aware rendering: locked screen shows only generic `Listening…` / `Heard you. Unlock to continue.`;
- no transcript-derived/private spoken reply while locked;
- separated orb/name/status spacing to prevent overlap;
- offline-first Android TTS voice selection: Hindi India → English India → English US → any offline voice;
- speech rate 0.95 and neutral pitch;
- deterministic local spoken replies for greeting, time, capability, reminder and app-open intent;
- app/reminder requests are understood but not executed in J2;
- TTS and recognition stop on hide/destroy;
- unit tests for natural greeting, deterministic time, no fake action execution and private unknown transcript classification.

This batch must pass fresh J2/J1/Android/Governance CI before a new APK is shared.

## One consolidated next device round

After the next green APK, test in one round:

1. unlocked Hindi/Hinglish/English recognition;
2. audible Mayra reply and voice quality;
3. `open WhatsApp` safe confirmation without execution;
4. reminder request safe confirmation;
5. all dismissal methods and mic cleanup;
6. 20-cycle regression;
7. already-locked invocation with no transcript/private reply before unlock;
8. lock while listening;
9. no-speech and rapid open/close;
10. reboot recovery.

## Promotion rule

J2 core Assistant/on-device voice foundation is accepted. Full `DEVICE_VERIFIED` promotion waits only for the consolidated privacy + spoken-reply APK round. J2 remains a temporary engineering package; final product remains one Mayra app.
