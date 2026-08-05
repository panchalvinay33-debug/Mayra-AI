# Mayra AI — START HERE

> **Read this first whenever Mayra work starts or resumes.**
>
> Canonical truth lives in project records and exact CI/device evidence, not chat history or an old APK.

Last synchronized: **2026-08-05**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Current active phase: **J5 unified Mayra permanent owner-signing migration + device verification; separate J4 quality device gate**
Latest protected recovery baseline: **`baseline/mayra-0.2.1-j4-ci-recovery-green-134`** at `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`
Latest unified J5 exact-green code: **`cc89a392a53fcb910166c92badaab3543b5520ff`**
J5 unified backup: **`backup/j5-unified-mayra-ci-green-2026-08-05`**
Owner signing migration record: **`docs/testing/MAYRA_OWNER_SIGNING_MIGRATION_2026-08-05.md`**
Immutable signing-migration snapshot: **`docs/backups/MAYRA_SNAPSHOT_2026-08-05_OWNER_SIGNING_MIGRATION.md`**

## 1. Product north star

Mayra is the owner's personal Android AI companion targeting a practical **Jarvis-style personal Android operating layer**. The phone's Home surface, quick assistant presence and full conversation should feel like one Mayra while remaining internally separated enough that AI/model failure cannot break basic Home/app access.

Canonical plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Architecture: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`
Launcher addendum: `docs/MAYRA_BLUEPRINT_JARVIS_LAUNCHER_ADDENDUM.md`

## 2. Read these records in this order

1. `START_HERE.md`
2. `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
3. `docs/MAYRA_PINPOINT_AUDIT.md`
4. `docs/MAYRA_BLUEPRINT.md`
5. `docs/MAYRA_BLUEPRINT_JARVIS_LAUNCHER_ADDENDUM.md`
6. `docs/MAYRA_ROADMAP.md`
7. `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
8. `docs/MAYRA_TEST_MATRIX.md`
9. `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
10. `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`
11. `docs/feasibility/MAYRA_J5_LAUNCHER_PREFLIGHT.md`
12. `docs/testing/MAYRA_J5_LAUNCHER_MOTOROLA_ACCEPTANCE.md`
13. `docs/testing/MAYRA_OWNER_SIGNING_MIGRATION_2026-08-05.md`
14. `docs/MAYRA_IDEA_LEDGER.md`
15. `docs/MAYRA_DECISIONS.md`
16. `docs/MAYRA_CHANGELOG.md`

## 3. Current proven foundation

### Core Mayra

Kotlin/Compose app, typed conversation/document/action routing, deterministic local commands, optional protected cloud provider, owner-controlled memory, document intelligence, reminders, app opening/contact resolution, review-first call/message handoffs and release/signing foundations exist.

### J1/J2 voice and Android Assistant

Motorola Edge 70 Fusion / Android 16 previously proved Digital Assistant selection/invocation, Power-button activation, on-device multilingual recognition, bounded dismissal/stability, lock/privacy paths and Android offline TTS fallback.

### J3 neural voice

Offline neural synthesis/playback was technically proven, but the benchmark Priyamvada voice pack remains production-license blocked. Android offline TTS remains the safe fallback until a license-clear voice is selected.

### J4 local brain

Runtime is proven and Room/KSP CI recovery is protected. Quality source `862450933da3700d4d1559e09ebde910a4185914` adds multilingual quality prompts, RAM metrics, stress and process-bounded cancel/rebind. Physical quality/RAM/thermal/background/lock/Airplane acceptance is still pending before production-brain promotion.

### J5 Home — real device core proven

On exact source `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a` / Android #2384, Motorola evidence proved default Home selection, app inventory/search/launch/Home return, lock/unlock, reboot, safe launcher switching, Airplane-mode core independence and the `Ask Mayra` bridge.

### J5 unified Mayra — automated green

Exact source `cc89a392a53fcb910166c92badaab3543b5520ff` adds shared Mayra entry routing, a central Home orb/card, orb/Open-Mayra handoff into full Mayra, and voice-session continuation into full Mayra while preserving lightweight Home independence.

Exact-head automated evidence:
- Android CI #2416 — SUCCESS
- J1 #525 — SUCCESS
- J2 #421 — SUCCESS
- J3 #243 — SUCCESS
- J4 #194 — SUCCESS
- Governance #597 — SUCCESS

### Owner signing migration — current delivery gate

The #2416 `ai.mayra.app.alpha` could not install over the physically proven #2384 `.alpha` because ordinary hosted CI used a different transient debug signing certificate. Android correctly blocks same-package replacement when the signer changes.

Stable Owner Alpha run #6 (`30984237319`) proved the dedicated signing workflow path but failed at the signing-material gate because `MAYRA_OWNER_KEYSTORE_BASE64` is not configured. A permanent owner key has been generated outside repository source/history for owner custody.

To avoid uninstall-first risk, Mayra now has a dedicated permanent **side-by-side owner package**:

- build type: `ownerAlpha`
- package: `ai.mayra.app.owner`
- visible label: `Mayra AI Owner`
- signing: one persistent owner key through Stable Owner Alpha only
- preflight: `Owner Alpha Preflight` compiles/lints the owner variant without exposing private signing material

The working `.alpha` remains installed while `.owner` is installed and tested. Android isolates their private data, so no silent cross-package data migration is claimed.

## 4. Jarvis execution phases

- **J5 Launcher/Unified Presence:** current permanent owner-signing + device-verification gate.
- **J6 Context:** provenance-aware reminders, notifications, people, documents/media and bounded app/screen context.
- **J7 Actions:** GREEN/AMBER/RED trust policy, deterministic typed adapters and audit history.
- **J8 Proactive:** My Day, pending-item assistance, quiet/battery/privacy limits.
- **J9 Multimodal:** explicit camera/image/screen/document understanding after privacy/performance gates.
- **J10 Routines:** owner-defined typed reusable workflows.

## 5. Mandatory major-step baseline procedure

Before every major capability: Idea Ledger → ADR/decision → Blueprint → Roadmap gate → preflight/test contract → rollback point → planning snapshot when direction materially changes.

After implementation: applicable CI/lint/unit/package/permission/component checks → Motorola evidence for device claims → Changelog/Latest Snapshot/test evidence → immutable milestone snapshot → protected `baseline/*` only from exact promoted green source → next-risky-phase rollback target.

A red/pending head is never called stable.

## 6. Current ordered work

1. Keep the protected J4 recovery baseline immutable.
2. Keep the physically working `ai.mayra.app.alpha` installed as rollback/reference.
3. Securely preserve the permanent Mayra owner signing bundle outside GitHub source/history.
4. Configure GitHub Actions Secrets `MAYRA_OWNER_KEYSTORE_BASE64`, `MAYRA_OWNER_STORE_PASSWORD`, `MAYRA_OWNER_KEY_ALIAS`, `MAYRA_OWNER_KEY_PASSWORD`.
5. Run Stable Owner Alpha to build `ai.mayra.app.owner`; record APK SHA-256 + signer SHA-256.
6. Install `Mayra AI Owner` side-by-side; do not uninstall `.alpha` first.
7. Test `.owner` as Home and, where desired, Digital Assistant: orb/full-Mayra handoff, app search/open/Home return, lock/unlock, reboot, switch-back, Airplane mode and failure independence.
8. Build a second stable-owner APK and prove direct install-over-install without uninstall.
9. Keep `.alpha` until owner-package behavior and any needed owner data transfer/recreation are accepted.
10. If unified J5 physical acceptance is accepted, create immutable J5 milestone snapshot + protected J5 baseline.
11. Only then begin J6 context integration.
12. Separately complete J4 quality/RAM/cancel/stress/background/lock/Airplane/thermal acceptance.

## 7. Mandatory resume procedure

Before coding: read canonical records; inspect PR #12 exact head and latest CI; reconcile source/docs/device evidence; identify one coherent batch and rollback point; repair/revert red heads before unrelated expansion; never claim device success without owner evidence; never merge/ready PR #12 without explicit owner approval.

## 8. Mandatory completion procedure

After every meaningful batch synchronize applicable Roadmap, Latest Snapshot, Pinpoint Audit, Test Matrix, Blueprint, Master Plan, Idea Ledger, Decisions/ADR, Changelog, START_HERE, immutable snapshot/baseline and PR description. Governance CI must remain green; stale canonical records count as a real failure.

## 9. Safety / ownership rules

- official Android roles/APIs first;
- minimum permissions for active capability;
- no secrets/private keys/owner-private data in GitHub;
- stable owner-device updates must use one persistent owner signer;
- ordinary transient CI APKs are not assumed update-compatible;
- permanent owner package is `ai.mayra.app.owner` unless a later explicit migration changes it;
- no false device/audio/action claims;
- free-form LLM text cannot directly execute privileged actions or write trusted owner memory;
- context provenance stays structured and owner-controlled;
- missing/corrupt/killed AI must fall back cleanly;
- launcher must preserve basic app access and a route to another Home app;
- no Play Protect/security/signing bypass.

## 10. Backup model

Git history is primary backup. `baseline/*` branches are immutable promoted recovery markers. `backup/*` branches preserve engineering checkpoints. `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is rolling recovery truth. Immutable planning/milestone snapshots live under `docs/backups/`. CI artifacts are temporary evidence and remain tied to source/run/digest provenance. Private signing keys remain outside GitHub source/history.

## 11. Immediate next action

Finish Owner Alpha preflight/shared regressions, configure the four encrypted signing Secrets from the private owner bundle, produce the first `ai.mayra.app.owner` APK, install it side-by-side with the working `.alpha`, then prove a second stable-owner build updates it in place before J5 protected promotion.
