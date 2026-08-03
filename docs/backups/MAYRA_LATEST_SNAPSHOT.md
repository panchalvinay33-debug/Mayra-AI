# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 physical voice acceptance; CI #90 invocation/readiness passes but transcript remains blocked by OEM recognizer lifecycle/support discovery
Canonical product issue: #13
Mandatory feasibility gate: #14
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`

## Canonical truth

- Final product remains one Mayra app; J1/J2 are temporary engineering packages only.
- PR #12 remains the only active implementation PR and is not authorized for merge/ready.
- Protected baselines are immutable recovery markers.
- Device capability claims require Motorola evidence.

## Latest protected J2 baseline

- `baseline/mayra-0.2.1-j2-locale-repair-green-90`
- application source `e706bdfb8f53006825404db99a51f466aa251fc4`
- J2 #90 success
- J1 #194 success
- Android CI #2085 success
- Governance #266 success
- artifact `mayra-j2-voice-apk-90`, ID `8865632199`
- APK size `19,192,945` bytes
- APK SHA-256 `2c1e00db4a2bfd98993eb87fe091c5373931153eb3b5ac2252914d4441ac230c`
- ZIP SHA-256 `bbe4936bc5caec8a08d244ea28f82cf09daabceb49bde47b20ad1678933521b9`
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

## Current source repair after CI #90

The new source is not yet an owner candidate.

- one on-device recognizer instance per bounded attempt instead of destroy/recreate for each language;
- Android 13+ `checkRecognitionSupport()` queries actual installed on-device language support;
- Mayra prioritizes installed locales using device → Hindi India → English India → English US policy;
- if supported language is not installed, surface `On-device speech language pack needed`;
- if the OEM cannot report support, use bounded delayed locale trials;
- reuse recognizer with 450 ms fallback delay;
- no cloud STT fallback, no endless speech loop, no added permission;
- tests cover installed-language selection/canonicalization.

## Current exact gate

1. Fresh J2/J1/Android/Governance CI on the synchronized support-probe repair head.
2. No APK sharing until all gates are green.
3. Record exact artifact ID, APK hash and ZIP hash.
4. Motorola retest `Mayra namaste`.
5. If successful, continue Hindi/Hinglish/English transcripts, direct tap dismissal, 20-cycle stability, already-locked invocation and reboot recovery.
6. If a language pack is required, implement explicit model-download/user guidance rather than hidden cloud recognition.

## Future capability gates

Wake word, local LLM, Phone/InCallService, AI caller message-taking, notification intelligence, app workflow automation and trusted installation remain separately gated by issue #14. Preflight completion does not mean delivery.

## Distribution truth

Temporary hosted-runner debug signatures can conflict across J2 builds. Stable owner signing/trusted distribution is still required for seamless full Mayra upgrades. Play Protect/signature checks are never bypassed.
