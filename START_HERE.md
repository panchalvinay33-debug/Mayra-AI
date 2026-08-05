# Mayra AI — START HERE

> **Read this first whenever Mayra work starts or resumes.**
>
> Canonical truth lives in project records and exact CI/device evidence, not chat history or an old APK.

Last synchronized: **2026-08-05**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Current active phase: **J4 quality harness device verification + J5 AI-native launcher/Home foundation**
Latest protected recovery baseline: **`baseline/mayra-0.2.1-j4-ci-recovery-green-134`** at `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`
Latest J4 quality engineering checkpoint: **`862450933da3700d4d1559e09ebde910a4185914`**, all automated gates green; Motorola quality evidence pending.

## 1. Product north star

Mayra is the owner's personal Android AI companion and is targeting a practical **Jarvis-style personal Android operating layer** with an AI-native launcher/Home shell as the primary daily surface.

Canonical plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Architecture: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`
Launcher addendum: `docs/MAYRA_BLUEPRINT_JARVIS_LAUNCHER_ADDENDUM.md`

Target experience includes natural Hindi/Hinglish/English conversation, local-first brain, optional cloud boosters, owner-controlled memory/documents, reminders/notifications/people context, Android Digital Assistant integration, AI-native Home/app drawer/search, trust-gated actions, proactive My Day, later multimodal understanding and owner-defined routines.

The launcher is the Home shell, **not privileged authority**. Heavy AI/model/provider failure must never make basic Home/app access unusable.

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
13. `docs/MAYRA_IDEA_LEDGER.md`
14. `docs/MAYRA_DECISIONS.md`
15. `docs/MAYRA_CHANGELOG.md`

## 3. Current proven foundation

### Core Mayra

Kotlin/Compose app, typed conversation/document/action routing, deterministic local commands, optional protected cloud provider, owner-controlled memory, TXT/PDF/DOCX intelligence, reminders, app opening/contact resolution, review-first call/message handoffs and release/signing foundations exist.

### J1/J2 Assistant and voice — device-proven foundation

Motorola Edge 70 Fusion / Android 16 evidence covers Digital Assistant selection/invocation, Power-button activation, Hindi/Hinglish/English on-device recognition, bounded dismissal/stability, lock/privacy paths and Android offline TTS fallback.

### J3 neural voice — device benchmark pass / production license blocked

Neural model load/synthesis/playback and offline stability were proven, but the benchmark Priyamvada voice pack is not production-cleared. Android offline TTS remains the safe fallback until a license-clear voice is selected.

### J4 local brain — runtime proven, quality harness ready

The earlier Room/KSP truncated-schema race was repaired by isolating transient Room schema generation across variant builds. Exact recovery source `e72488a6f6dceb24950f9b0f574ae223d52bd8bb` passed Android CI #2356, J1 #465, J2 #361, J3 #183, J4 #134 and Governance #537 and is protected as `baseline/mayra-0.2.1-j4-ci-recovery-green-134`.

J4 quality source `862450933da3700d4d1559e09ebde910a4185914` adds longer Hindi/Hinglish/English quality prompts, safety/uncertainty prompts, 10-prompt stress benchmark, response metrics, runtime PSS/heap/native-memory telemetry and process-bounded cancellation/rebind. It passed Android CI #2364, J1 #473, J2 #369, J3 #191, J4 #142 and Governance #545. Physical Motorola quality/RAM/thermal/background/lock/Airplane evidence is still required before production-brain promotion.

### J5 launcher — implementation foundation started

The main source now contains a separate `MayraLauncherActivity` with Android HOME-role qualification, user-consent role request, searchable launchable-app list, app launch actions, bridge into normal Mayra, and an explicit Home-settings switch/restore path. The launcher rendering path deliberately does not initialize the local model, cloud provider, memory or privileged action engine.

J5 device success is **not yet claimed**. Current source must pass fresh CI and then Motorola HOME-selection/reboot/switch-back/crash-survival acceptance.

## 4. Jarvis execution phases

- **J5 Launcher:** default HOME shell, app drawer/search, Mayra presence, fallback/switch-back, reboot/crash/model-failure resilience.
- **J6 Context:** provenance-aware reminders, notifications, people, documents/media and bounded screen/app context.
- **J7 Actions:** GREEN/AMBER/RED trust policy, deterministic typed adapters and audit history.
- **J8 Proactive:** My Day, pending-item assistance, quiet/battery/privacy limits.
- **J9 Multimodal:** explicit camera/image/screen/document understanding after privacy/performance gates.
- **J10 Routines:** owner-defined typed reusable workflows.

## 5. Mandatory major-step baseline procedure

Before every major capability:

1. update Idea Ledger;
2. add/update ADR/decision;
3. update Blueprint;
4. update Roadmap gate;
5. add preflight/test contract where needed;
6. identify rollback point;
7. create immutable planning snapshot for material direction changes.

After implementation:

1. require applicable CI/lint/unit/package/permission/component checks;
2. require Motorola evidence for device claims;
3. synchronize Changelog + Latest Snapshot + test evidence;
4. create immutable milestone snapshot;
5. create protected `baseline/*` only from exact green source;
6. record next risky phase and rollback target.

A red/pending head is never called stable.

## 6. Current ordered work

1. Keep `baseline/mayra-0.2.1-j4-ci-recovery-green-134` immutable.
2. Physically run J4 #142 quality/RAM/cancel/stress/background/lock/Airplane/thermal acceptance on Motorola.
3. Validate the new J5 source through fresh Android/J1/J2/J3/J4/Governance CI.
4. Build/install a J5-capable owner APK and prove HOME-role selection, Home-button return, reboot persistence, app drawer/search/app launch and switch-back on Motorola.
5. Prove Home remains usable with AI/model/provider failure.
6. Only after J5 reliability evidence, connect Mayra voice/orb more deeply into Home.
7. Then start J6 context cards incrementally.

## 7. Mandatory resume procedure

Before coding:

1. read this file;
2. read Jarvis/Launcher Master Plan + Pinpoint Audit + Latest Snapshot + active Roadmap section;
3. check PR #12 head/state and latest-head CI/Governance;
4. confirm source/docs/device evidence agree;
5. identify one coherent batch, tests and rollback point;
6. never expand a red head with unrelated speculative features — repair/revert first;
7. never claim device success without owner evidence;
8. never merge or mark PR #12 ready without explicit owner approval.

## 8. Mandatory completion procedure

After every meaningful batch synchronize applicable Roadmap, Latest Snapshot, Pinpoint Audit, Test Matrix, Blueprint, Master Plan, Idea Ledger, Decisions/ADR, Changelog, START_HERE, immutable snapshot/baseline and PR description. Governance CI must remain green; stale canonical records count as a real failure.

## 9. Safety / ownership rules

- official Android roles/APIs first;
- minimum permissions for the active capability;
- no secrets/private keys/owner-private data in GitHub;
- no false call/audio/device claims;
- free-form LLM text cannot directly execute privileged actions or write trusted owner memory;
- context provenance stays structured and owner-controlled;
- missing/corrupt/killed AI must fall back cleanly;
- launcher must always preserve basic app access and a route to another Home app;
- no Play Protect/security/signing bypass.

## 10. Backup model

Git history is primary backup. `baseline/*` branches are immutable exact-green recovery markers. `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is rolling recovery truth. Immutable planning/milestone snapshots live under `docs/backups/`. CI artifacts are temporary evidence and must remain tied to source/run/digest provenance.

## 11. Immediate next action

Validate the new J5 launcher/Home foundation on fresh CI without disturbing the protected J4 recovery baseline; then perform Motorola J4-quality and J5-HOME device gates before any deeper context/action integration.
