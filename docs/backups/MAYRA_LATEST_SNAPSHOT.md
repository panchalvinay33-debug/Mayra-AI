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

After the owner retried installation, Google Play Protect blocked the full sideloaded Personal Alpha and displayed that the app could request sensitive data. The app was not installed.

Evidence: `docs/testing/MAYRA_PLAY_PROTECT_BLOCK_2026-08-03.md`.

Instruction: do not disable or bypass Play Protect for this artifact.

## Current corrective implementation

A dedicated build type now exists:

- name: `j1AssistantTest`;
- package: `ai.mayra.app.j1`;
- label: `Mayra J1 Assistant Test`;
- requested Android permissions: zero by design;
- launcher count: exactly one;
- included: small role activation/status activity, VoiceInteractionService, session service/orb and RecognitionService metadata shell;
- excluded: full chat, provider, internet, contacts, reminders, notifications, boot recovery, notification listener, documents and memory.

Dedicated workflow: `.github/workflows/j1-assistant-test.yml`.

The workflow compiles, lints, assembles and hard-audits the APK. It fails on any requested Android permission, extra launcher or forbidden feature/background component.

## Full owner-app distribution truth

The complete Mayra app still requires:

1. one stable private owner/release certificate;
2. protected build secrets;
3. signed APK/AAB provenance;
4. preferably Google Play Internal Testing or another trusted owner-controlled distribution channel;
5. install-over-install and local-data-retention proof.

Temporary CI debug-signed Personal Alpha APKs are not long-term owner releases and should not be used to bypass Play Protect.

## Latest validation gates

1. Android CI on the exact current head.
2. Project Governance on the exact current head.
3. J1 Assistant Test workflow on the exact current head.
4. If all green, record artifact ID, source SHA, APK size and SHA-256.
5. Motorola clean install of only the zero-permission J1 APK.
6. Assistant role visibility, select/remove, unlocked/locked invocation and orb lifecycle.
7. Update device evidence before local LLM, wake-word or Phone-role coding.

## Current feature truth

- Core Mayra features remain code/CI mature but full-device acceptance is blocked by trusted installation/distribution.
- Jarvis J1 services/orb are code/CI verified at baseline #1851 but not device verified.
- The new zero-permission J1 package is the next installation proof vehicle.
- Local LLM, always-listening wake phrase and default Phone/InCallService remain planned and blocked until J1 evidence is processed.

## Merge and secret truth

- PR #12 remains Draft/open/unmerged.
- No merge/ready transition is authorized.
- No API keys, keystores, passwords, tokens or owner-private data belong in GitHub records or chat.
- Device claims require actual Motorola evidence.
