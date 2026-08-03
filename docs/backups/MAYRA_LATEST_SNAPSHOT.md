# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: Jarvis J1 zero-permission installation proof

## Protected baselines

### Pre-Jarvis

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795: success

### Jarvis J1 code baseline

- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851: success
- Project Governance #32: success

These remain known-green rollback points. Current zero-permission J1 work is not a baseline until all latest-head workflows pass.

## Motorola evidence received

### Signature conflict

An update attempt for `ai.mayra.app.alpha` failed because the old and new APKs used different temporary GitHub runner debug certificates.

Evidence: `docs/testing/MAYRA_J1_INSTALL_RESULT_2026-08-03.md`.

### Play Protect block

Google Play Protect blocked the full sideloaded Personal Alpha because it could request sensitive data. The app was not installed.

Evidence: `docs/testing/MAYRA_PLAY_PROTECT_BLOCK_2026-08-03.md`.

Instruction: do not disable or bypass Play Protect for this artifact.

## Current corrective implementation

Dedicated J1 build:

- build type: `j1AssistantTest`;
- package: `ai.mayra.app.j1`;
- label: `Mayra J1 Assistant Test`;
- requested Android permissions: zero;
- one launcher;
- included: small role activation/status activity, VoiceInteractionService, session service/orb and RecognitionService metadata shell;
- excluded: full chat, provider, internet, contacts, reminders, notifications, boot recovery, notification listener, documents and memory.

Dedicated workflow: `.github/workflows/j1-assistant-test.yml`.

## Failure history and current repair

J1 Assistant Test workflow run #16:

- Kotlin compilation passed;
- J1 lint failed on the full app's `MayraOwnerSetupGate` because `RoleManager.createRequestRoleIntent` requires API 29 and lint could not prove the call was guarded through derived Compose state;
- no APK was produced or promoted.

Repair:

- added a direct `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q` guard inside the click handler;
- retained nullable RoleManager handling;
- added no lint suppression and no baseline.

Current repair commit: `60ad83d3e09f5e4e55cc57094e4ee9b3a2d4f6f7`, followed by synchronized Roadmap/Snapshot commits. Fresh workflows are required on the final exact head.

## Full owner-app distribution truth

The complete Mayra app requires one stable private owner/release certificate, protected build secrets, signed APK/AAB provenance, trusted owner-controlled distribution (preferably Play Internal Testing), and install-over-install/local-data-retention proof.

Temporary CI debug-signed Personal Alpha APKs are not long-term owner releases and must not be used to bypass Play Protect.

## Latest validation gates

1. Android CI on the exact latest head.
2. Project Governance on the exact latest head.
3. J1 Assistant Test workflow on the exact latest head.
4. If all green, record artifact ID, source SHA, APK size and SHA-256.
5. Motorola clean install of only the zero-permission J1 APK.
6. Assistant role visibility, select/remove, unlocked/locked invocation and orb lifecycle.
7. Update device evidence before local LLM, wake-word or Phone-role coding.

## Current feature truth

- Core Mayra features remain code/CI mature but full-device acceptance is blocked by trusted installation/distribution.
- Jarvis J1 services/orb are code/CI verified at baseline #1851 but not device verified.
- The zero-permission J1 package is the next installation proof vehicle.
- Local LLM, always-listening wake phrase and default Phone/InCallService remain planned and blocked until J1 evidence is processed.

## Merge and secret truth

- PR #12 remains Draft/open/unmerged.
- No merge/ready transition is authorized.
- No API keys, keystores, passwords, tokens or owner-private data belong in GitHub records or chat.
- Device claims require actual Motorola evidence.
