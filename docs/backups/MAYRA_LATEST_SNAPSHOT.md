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

- Personal Alpha update failed because old/new APKs used different temporary CI debug certificates.
- Clean-install retry was blocked by Google Play Protect because the full sideloaded debug app could request sensitive access.
- Play Protect must not be disabled or bypassed for that artifact.

Evidence:

- `docs/testing/MAYRA_J1_INSTALL_RESULT_2026-08-03.md`
- `docs/testing/MAYRA_PLAY_PROTECT_BLOCK_2026-08-03.md`

## Dedicated J1 package

- build type: `j1AssistantTest`;
- package: `ai.mayra.app.j1`;
- label: `Mayra J1 Assistant Test`;
- intended requested Android permissions: zero;
- exactly one launcher;
- included: Assistant activation/status activity, VoiceInteractionService, session service/orb and RecognitionService metadata shell;
- excluded: full chat, provider, contacts, reminders, notifications, boot recovery, notification listener, documents and memory.

Dedicated workflow: `.github/workflows/j1-assistant-test.yml`.

## Failure history and repairs

### J1 run #16

Lint caught the API-29 Assistant role request without a directly visible SDK guard. Fixed with an explicit Android Q runtime check and no suppression/baseline.

### J1 run #22

Compilation, lint and assembly passed, but the hard audit rejected inherited AndroidX permissions/components: Wake Lock, Network State, Foreground Service, app-private dynamic receiver permission, Startup, WorkManager, Room and ProfileInstaller infrastructure. No APK was promoted.

### J1 run #32

Lint rejected the explicit removal declaration for `androidx.profileinstaller.ProfileInstallReceiver` because that class was absent from the actual dependency graph. The invalid removal declaration was removed. All real inherited permission/component removals and strict workflow audits remain.

Current source repair commit:

- `2e35d1ff44fbcfa582e2621bbc28bd2830b48d3c`

Roadmap and rolling snapshot were synchronized afterward. Fresh exact-head workflows are required before an APK is shared.

## Full owner-app distribution truth

The complete Mayra app requires one stable private owner/release certificate, protected build secrets, signed APK/AAB provenance, trusted owner-controlled distribution (preferably Play Internal Testing), and install-over-install/local-data-retention proof.

Temporary CI debug-signed Personal Alpha APKs are not long-term owner releases and must not be used to bypass Play Protect.

## Latest validation gates

1. Android CI on the exact latest head.
2. Project Governance on the exact latest head.
3. J1 Assistant Test on the exact latest head.
4. If all green, record artifact ID, source SHA, APK size and SHA-256.
5. Motorola clean install of only the verified J1 APK.
6. Assistant role visibility, select/remove, unlocked/locked invocation and orb lifecycle.
7. Update physical evidence before local LLM, wake-word or Phone-role coding.

## Current feature truth

- Core Mayra features remain code/CI mature, but full-device acceptance is blocked by trusted installation/distribution.
- Jarvis J1 services/orb are code/CI verified at baseline #1851 but not device verified.
- The zero-permission J1 package is the next installation proof vehicle.
- Local LLM, always-listening wake phrase and default Phone/InCallService remain planned and blocked until J1 evidence is processed.

## Merge and secret truth

- PR #12 remains Draft/open/unmerged.
- No merge/ready transition is authorized.
- No API keys, keystores, passwords, tokens or owner-private data belong in GitHub records or chat.
- Device claims require actual Motorola evidence.
