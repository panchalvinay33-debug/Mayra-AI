# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: Jarvis J1 installation/setup repair and Motorola verification

## Protected baselines

### Pre-Jarvis

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795: success

### Jarvis J1

- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851: success
- Project Governance #32: success
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_JARVIS_J1_CI1851.md`

These remain known-green rollback points. The current repair head is not a new baseline until both workflows pass.

## Motorola evidence received

The owner attempted to install Personal Alpha #1851 and Android displayed:

> App not installed as package conflicts with an existing package.

Evidence record: `docs/testing/MAYRA_J1_INSTALL_RESULT_2026-08-03.md`.

Root cause: an older `ai.mayra.app.alpha` was signed by a different temporary GitHub runner debug certificate. Android correctly rejected the new APK as an incompatible update.

Immediate recovery for the #1851 test:

1. uninstall the old Mayra AI Personal Alpha;
2. install #1851 as a clean install.

This removes the old test package's local data.

## Current corrective batch

Committed after the #1851 baseline:

- `MayraOwnerSetupGate` with one two-step first-launch screen;
- one permission request for microphone, contacts and notifications;
- one explicit Android Assistant-role activation step;
- simple Start Mayra/Continue for now path;
- main Device chip renamed to Setup and wording reduced;
- `personalAlpha` supports a stable secret-backed owner signing config;
- dedicated `.github/workflows/owner-alpha.yml` builds a stable owner APK, verifies package/certificate and records SHA-256;
- permanent install-failure evidence and upgrade-retention test contract;
- blueprint, roadmap, decisions, ideas and changelog synchronized.

Validation state: `IN_PROGRESS`. Compile, tests, lint, package audits and Governance are pending on the latest repair head.

## Stable owner signing setup still required

The repository now expects four protected GitHub secrets for the dedicated owner workflow:

- `MAYRA_OWNER_KEYSTORE_BASE64`
- `MAYRA_OWNER_STORE_PASSWORD`
- `MAYRA_OWNER_KEY_ALIAS`
- `MAYRA_OWNER_KEY_PASSWORD`

No secret value belongs in chat, commits or documentation. Until these are configured, ordinary Android CI may still produce disposable debug-signed Personal Alpha APKs that require clean install and are not upgrade-compatible.

## Next validation gates

1. Android CI latest-head compile, complete tests, lint and all package audits.
2. Project Governance latest-head success.
3. Clean-install test of the latest candidate.
4. First-launch setup test: permissions, role selector, Start Mayra.
5. Configure stable owner signing secrets once.
6. Build stable owner version A and version B.
7. Install B over A without uninstalling and verify local data remains.
8. Continue J1 Assistant visibility/invocation/lock-screen/orb/reboot testing.
9. Update evidence documents before J2 wake-word/local-model work.

## Current capability truth

The Jarvis J1 service/session/orb foundation remains CI-verified at baseline #1851 but not yet device-verified. Local LLM, always-listening wake phrase and Phone/InCallService remain blocked until the J1 installation/setup/device gate is processed.

## Merge and secret truth

- PR #12 remains Draft/open/unmerged.
- No merge/ready transition is authorized.
- No API keys, keystores, passwords, tokens or owner-private data belong in GitHub records.
- Device claims require actual Motorola evidence.
