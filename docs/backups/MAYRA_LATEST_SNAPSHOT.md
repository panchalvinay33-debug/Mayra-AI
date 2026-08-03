# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 physical voice acceptance; support-probe/single-recognizer repair is fully green and awaiting Motorola transcript retest
Canonical product issue: #13
Mandatory feasibility gate: #14
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`

## Canonical truth

- Final product remains one Mayra app; J1/J2 are temporary engineering packages only.
- PR #12 remains the only active implementation PR and is not authorized for merge/ready.
- Protected baselines are immutable recovery markers.
- Device capability claims require Motorola evidence.

## Latest protected J2 baseline

- `baseline/mayra-0.2.1-j2-speech-support-green-106`
- application source `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 #106 success
- J1 #210 success
- Android CI #2101 success
- Governance #282 success
- artifact `mayra-j2-voice-apk-106`, ID `8866441207`
- APK size `19,209,329` bytes
- APK SHA-256 `d0917d17b50429a843f3a5e688580df66f3eea678be4806b44ef9f1535adeb6e`
- ZIP SHA-256 `b2109366a0140a66f85fef3cd6a85a95263815643ae86076ba0a9f20194140db`
- package boundary remains exactly `RECORD_AUDIO`.

## Motorola evidence through 23:32 IST

PASS:

- J2 #90 clean install after removing only the conflicting engineering J2 package.
- J2 selected as Digital assistant.
- microphone allowed.
- Android reports on-device speech service available.
- Power-button invocation launches Mayra orb over Home.

FAIL history:

- CI #18: `Speech language unavailable`, no transcript.
- CI #90: `Speech recognizer unavailable`, no transcript.

No false transcript or crash was observed.

## J2 #106 repair now validated by CI

- one on-device recognizer instance per bounded attempt instead of destroy/recreate for each language;
- Android 13+ `checkRecognitionSupport()` queries actual installed on-device language support;
- Mayra prioritizes installed locales using device → Hindi India → English India → English US policy;
- if supported language is not installed, surface `On-device speech language pack needed`;
- if the OEM cannot report support, use bounded delayed locale trials;
- reuse recognizer with 450 ms fallback delay;
- no cloud STT fallback, no endless speech loop, no added permission;
- tests cover installed-language selection/canonicalization;
- exact-head J2/J1/full Android/Governance all pass.

## Current exact gate

1. Install J2 #106; if package signature conflicts, uninstall only engineering J2 then clean-install.
2. Re-select J2 as Digital assistant if required.
3. Invoke from Home and say `Mayra namaste`.
4. Record exact transcript/error.
5. If successful, continue Hindi/Hinglish/English transcripts, direct tap dismissal, 20-cycle stability, already-locked invocation and reboot recovery.
6. If a language pack is required, implement explicit model-download/user guidance rather than hidden cloud recognition.

## Future capability gates

Wake word, local LLM, Phone/InCallService, AI caller message-taking, notification intelligence, app workflow automation and trusted installation remain separately gated by issue #14. Preflight completion does not mean delivery.

## Distribution truth

Temporary hosted-runner debug signatures can conflict across J2 builds. Stable owner signing/trusted distribution is still required for seamless full Mayra upgrades. Play Protect/signature checks are never bypassed.
