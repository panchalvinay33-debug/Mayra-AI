# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: J1 Assistant proof achieved; J2 invocation-time voice is exact-head green and ready for Motorola device acceptance
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

## Current authoritative code baseline

- Branch: `baseline/mayra-0.2.1-j2-voice-green-18`
- Commit: `ef809bbdaca80f3b953483499dc03de8e091339f`
- J2 Voice Test #18: success
- J1 Assistant Test #122: success
- Android CI #2013: success
- Project Governance #194: success
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_J2_VOICE_CI18.md`

Later documentation commits do not change the authoritative application artifact for this baseline.

## Other protected baselines

- `baseline/mayra-0.2.1-green-1795` at `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- `baseline/mayra-0.2.1-jarvis-j1-green-1851` at `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- `baseline/mayra-0.2.1-j1-zero-permission-green-44` at `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- `baseline/mayra-0.2.1-j1-activation-repair-green-56` at `ce96f8e83fe33b878d426c407715d4a3e1b0495a`

## J2 artifact provenance

- Package: `ai.mayra.app.j2`
- Label: `Mayra J2 Voice Test`
- Artifact: `mayra-j2-voice-apk-18`
- Artifact ID: `8863135214`
- APK size: `19,192,945` bytes
- APK SHA-256: `ef48c264f841efd7891e335848e90f38654a6dd25f70d10b0a3afd08b968ecbc`
- Artifact ZIP SHA-256: `ea6afeb03e9137614dd3c486b5905f8e482e53e37f39b771f7fd90fb2a60c743`

J2 CI proves exactly one requested Android permission: `RECORD_AUDIO`.

## Motorola evidence inherited from J1

PASS:

- Mayra installs/launches as the isolated J1 proof package.
- Motorola recognizes Mayra as a Digital assistant candidate.
- Owner can select Mayra as default Digital assistant.
- Motorola Power-button Digital assistant action can invoke Mayra while unlocked.
- Mayra VoiceInteractionSession/orb renders over the current screen.
- Back dismisses the session.
- Locking the phone dismisses the current session.

FAIL/repair:

- J1 #68 did not dismiss on direct orb/outside tap because click listeners were absent.
- The common session code in the J2 baseline now adds orb/root/label tap-to-hide, explicit Back-to-hide, recognition stop on hide/destroy and bounded animation lifecycle.
- Physical retest is still required.

## J2 engineering truth

Implemented and CI-verified:

- isolated `ai.mayra.app.j2` package;
- one launcher;
- exactly microphone permission;
- Assistant selection/status activity;
- bounded `MayraVoiceSessionState` state model;
- invocation-time Android on-device recognition capability detection;
- use of on-device SpeechRecognizer only when Android reports support;
- visible preparing/listening/result/error states;
- recognition stop on hide/destroy;
- no continuous SpeechRecognizer wake loop;
- no internet/provider, contacts, notifications, reminders, WorkManager/Room, memory, documents, full chat or call-control components.

Not yet device-proven:

- J2 installation/Play Protect behavior;
- microphone permission flow;
- on-device speech availability on the target Motorola;
- Hindi/Hinglish/English transcript quality;
- repaired direct tap dismissal;
- 20-cycle voice/session stability;
- invocation beginning from an already locked screen;
- reboot persistence/recovery.

## Current exact gate

Run `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md` on the Motorola using the recorded CI #18 artifact. Every device result must be recorded as PASS/FAIL/BLOCKED before J2 is promoted to device-verified.

## Next architecture gates

Allowed in parallel: feasibility/benchmark preparation only.

Still blocked from implementation until dedicated gates pass:

- production wake phrase / hotword engine;
- local conversational LLM;
- default Phone/InCallService incoming-call control;
- AI caller message-taking.

## Distribution truth

- Full Mayra still requires one stable private owner/release certificate and trusted distribution.
- J1/J2 use the owner signing path only after private signing secrets are configured.
- Temporary CI debug artifacts are not install-over-install stable across runner keys.
- Play Protect/signature checks must never be bypassed.

## Merge/secret truth

- PR #12 remains Draft/open/unmerged.
- No merge or ready transition is authorized.
- No API key, keystore, password or owner-private data belongs in GitHub records.
