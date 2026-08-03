# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: J2 physical voice acceptance active; locale-unavailable device failure repaired in source and awaiting fresh CI
Canonical product issue: #13
Mandatory feasibility gate: #14
Repository hygiene registry: #15
J2 preflight: `docs/feasibility/MAYRA_J2_VOICE_PREFLIGHT.md`
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`

## Canonical repository truth

- PR #12 is the only active implementation PR.
- PR #9 and #11 are closed as superseded.
- Issue #10 is closed as superseded.
- Final product remains one Mayra app; J1/J2 are temporary engineering proof packages only.
- Protected baselines/backups are recovery markers and must not be force-moved or deleted.

## Last protected J2 baseline

- Branch: `baseline/mayra-0.2.1-j2-voice-green-18`
- Commit: `ef809bbdaca80f3b953483499dc03de8e091339f`
- J2 Voice Test #18: success
- J1 Assistant Test #122: success
- Android CI #2013: success
- Project Governance #194: success
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_J2_VOICE_CI18.md`

This baseline remains immutable and is not moved to the new locale-retry repair until fresh exact-head CI passes.

## J2 tested artifact provenance

- Package: `ai.mayra.app.j2`
- Label: `Mayra J2 Voice Test`
- Artifact: `mayra-j2-voice-apk-18`
- Artifact ID: `8863135214`
- APK size: `19,192,945` bytes
- APK SHA-256: `ef48c264f841efd7891e335848e90f38654a6dd25f70d10b0a3afd08b968ecbc`
- Artifact ZIP SHA-256: `ea6afeb03e9137614dd3c486b5905f8e482e53e37f39b771f7fd90fb2a60c743`
- CI package boundary: exactly `RECORD_AUDIO` permission.

## Motorola evidence through 22:46 IST

PASS:

- J1 Assistant selection and unlocked invocation.
- J1/Mayra orb rendering over current screen.
- Back and phone-lock dismissal.
- J2 installs and opens.
- J2 microphone permission is granted and readiness shows `Microphone: allowed ✓`.
- Android reports `On-device speech: available ✓`.
- J2 can be selected as Digital assistant.
- Power-button invocation launches J2 on Home.
- Android microphone privacy indicator appears during the invoked session.

FAIL:

- First J2 speech attempt produced `Speech language unavailable` and no transcript.

This was a bounded failure: no crash and no false transcript.

## Root cause and repair

The CI #18 J2 recognition request did not explicitly set a speech language. Motorola proved that on-device recognizer availability does not imply the implicit/default language model is available.

Repair now in source:

- `MayraSpeechLocalePolicy` builds a finite candidate order: device locale → `hi-IN` → `en-IN` → `en-US`;
- duplicate tags are removed case-insensitively;
- J2 explicitly sets `EXTRA_LANGUAGE` and `EXTRA_LANGUAGE_PREFERENCE`;
- only language-not-supported/language-unavailable errors move to the next locale;
- no continuous retry loop and no cloud STT fallback;
- unit tests cover locale ordering, duplicate removal and blank device locale.

## Current exact gate

1. Settle fresh J2 Voice Test, J1 Assistant Test, Android CI and Project Governance on the synchronized locale-repair head.
2. Do not share a replacement APK until the required gates are green.
3. Record new source/run/artifact/hash provenance.
4. Motorola retest phrases: `Mayra namaste`, `kal subah saat baje`, `open WhatsApp`, one English phrase.
5. If transcript works, continue tap-dismiss, 20-cycle stability, already-locked invocation and reboot recovery.
6. If all locales still fail, record that device language pack limitation and evaluate an explicitly separate offline STT engine rather than silently using cloud recognition.

## Future capability gates

Preflights exist for wake word, local LLM, Phone/InCallService, AI caller message-taking, notification intelligence, app workflow automation and trusted installation. Preflight completion does not mean those features are delivered.

Production wake-word/local-LLM integration remains blocked until J2 physical voice acceptance is complete.

## Distribution truth

- Full Mayra still requires one stable private owner/release certificate and trusted distribution.
- Temporary CI debug artifacts are not install-over-install stable across runner keys.
- Play Protect/signature checks must never be bypassed.

## Merge/secret truth

- PR #12 remains Draft/open/unmerged.
- No merge or ready transition is authorized.
- No API key, keystore, password or owner-private data belongs in GitHub records.
