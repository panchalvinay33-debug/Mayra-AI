# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-04
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 core device acceptance achieved; consolidated lock-screen privacy + offline spoken-reply batch in CI

## Canonical truth

- Final product remains one Mayra app; J1/J2 are temporary engineering packages only.
- PR #12 is not authorized for merge/ready.
- Protected baselines are immutable.
- Device claims require Motorola evidence.

## Last protected application baseline

`baseline/mayra-0.2.1-j2-speech-support-green-106`

- source `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 #106 / J1 #210 / Android CI #2101 / Governance #282: success
- artifact ID `8866441207`
- APK SHA-256 `d0917d17b50429a843f3a5e688580df66f3eea678be4806b44ef9f1535adeb6e`
- exactly `RECORD_AUDIO`

## Motorola acceptance achieved

- Assistant selection and Power-button invocation;
- offline Hindi/Hinglish/English transcription;
- all dismissal paths;
- 20-cycle stability;
- already-locked invocation;
- owner-reported remaining reboot/no-speech/rapid interaction checks OK;
- no reported crash, duplicate orb, stuck mic or permanent busy recognizer.

## Open device defect

CI #106 exposes transcript/private content and overlapping text before unlock.

## New source batch — awaiting fresh CI

- keyguard-safe generic locked text;
- no private transcript-derived TTS before unlock;
- layout spacing repair;
- offline-first Android TTS voice selection: Hindi India → English India → English US → offline fallback;
- 0.95 speech rate and neutral pitch;
- deterministic spoken replies for greetings, time, capability, reminder and app intent;
- app/reminder requests are understood but not executed in J2;
- TTS/recognizer cleanup on hide/destroy;
- tests prevent fake action-success claims and classify unknown transcript as private.

## Next gate

Fresh J2/J1/Android/Governance green → exact-head baseline/artifact → one consolidated Motorola round covering voice quality, privacy, lifecycle regression, locked state and reboot → then connect the proven bridge to full Mayra typed local brain and confirmation-safe actions.

## Distribution truth

Hosted CI debug signatures may conflict. Stable owner signing/trusted distribution remains required. Never bypass Play Protect or Android security.
