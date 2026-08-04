# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-04
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 core device acceptance achieved; lock-screen privacy + offline spoken-reply candidate CI #136 fully green and ready for one consolidated Motorola retest

## Canonical truth

- Final product remains one Mayra app; J1/J2 are temporary engineering packages only.
- PR #12 is not authorized for merge/ready.
- Protected baselines are immutable.
- Device claims require Motorola evidence.

## Latest protected CI-green application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136 / J1 #239 / Android CI #2131 / Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK size `19,209,329` bytes
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`
- ZIP SHA-256 `405406128d1f44a7bd9c90b71cc173d4bb352ba5d9e8c2863c1100c9a4d13b36`

The previous `baseline/mayra-0.2.1-j2-speech-support-green-106` remains the last physically device-proven application source until CI #136 is retested on Motorola.

## Motorola acceptance already achieved

- Assistant selection and Power-button invocation;
- offline Hindi/Hinglish/English transcription;
- all dismissal paths;
- 20-cycle stability;
- already-locked invocation;
- owner-reported reboot/no-speech/rapid interaction checks OK;
- no reported crash, duplicate orb, stuck mic or permanent busy recognizer.

## Device defect found in #106

- private transcript visible before unlock;
- status/name overlap on lock screen.

## CI #136 repair and forward progress

- keyguard-safe generic locked text;
- no private transcript-derived TTS before unlock;
- layout spacing repair;
- offline-first Android TTS voice selection: Hindi India → English India → English US → offline fallback;
- 0.95 speech rate and neutral pitch;
- deterministic spoken replies for greetings, time, capability, reminder and app intent;
- app/reminder requests understood but not executed in J2;
- TTS/recognizer cleanup on hide/destroy;
- tests prevent fake action-success claims and classify unknown transcript as private.

## Next gate

Install CI #136 and do one consolidated Motorola round covering voice quality, privacy, action-confirmation wording, lifecycle regression, already-locked state, lock-while-listening, no-speech, rapid open/close and reboot. If green, mark the J2 Assistant/on-device speech/offline TTS foundation `DEVICE_VERIFIED`, then connect it to the full Mayra typed local brain and confirmation-safe actions.

## Distribution truth

Hosted CI debug signatures may conflict. Stable owner signing/trusted distribution remains required. Never bypass Play Protect or Android security.
