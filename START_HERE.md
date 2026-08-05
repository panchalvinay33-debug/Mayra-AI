# Mayra AI — START HERE

> **Read this first whenever Mayra work starts or resumes.**
>
> Canonical truth lives in project records and exact CI/device evidence, not chat history or an old APK.

Last synchronized: **2026-08-05**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Current active phase: **J4 quality device verification + J5 AI-native launcher/Home DEVICE_VERIFY**
Latest protected recovery baseline: **`baseline/mayra-0.2.1-j4-ci-recovery-green-134`** at `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`
Latest J5 engineering checkpoint: **`6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`**, all automated gates green; Motorola HOME proof pending.
J5 backup: **`backup/j5-home-contract-ci-green-2026-08-05`**.

## 1. Product north star

Mayra is the owner's personal Android AI companion targeting a practical **Jarvis-style personal Android operating layer** with an AI-native launcher/Home shell as the primary daily surface.

Canonical plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Architecture: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`
Launcher addendum: `docs/MAYRA_BLUEPRINT_JARVIS_LAUNCHER_ADDENDUM.md`

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

### J4 local brain — runtime proven, quality device gate pending

The Room/KSP truncated-schema race is repaired and protected at `baseline/mayra-0.2.1-j4-ci-recovery-green-134`.

J4 quality source `862450933da3700d4d1559e09ebde910a4185914` adds longer multilingual prompts, safety/uncertainty prompts, 10-prompt stress, response metrics, RAM telemetry and process-bounded cancellation/rebind. It passed Android #2364, J1 #473, J2 #369, J3 #191, J4 #142 and Governance #545. Physical Motorola quality/RAM/thermal/background/lock/Airplane evidence remains required before production-brain promotion.

### J5 launcher — automated DEVICE_VERIFY-ready foundation

Exact source `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a` contains:

- separate `MayraLauncherActivity`;
- `MAIN + HOME + DEFAULT` Home qualification;
- user-consent `ROLE_HOME` request;
- searchable installed launchable-app list;
- direct app launching;
- bridge to normal Mayra;
- explicit Android Home-settings switch/restore route;
- launcher rendering independent of local model/cloud/memory/privileged-action startup;
- deterministic app-search tests;
- APK audits that explicitly require the Home component/categories while preserving exactly one normal LAUNCHER entry.

Exact-head automated evidence:
- Android CI #2384 — SUCCESS
- J1 #493 — SUCCESS
- J2 #389 — SUCCESS
- J3 #211 — SUCCESS
- J4 #162 — SUCCESS
- Governance #565 — SUCCESS

Motorola device success is **not yet claimed**. J5 is now ready for the physical HOME-role/reboot/switch-back/AI-failure acceptance gate.

## 4. Jarvis execution phases

- **J5 Launcher:** current DEVICE_VERIFY gate.
- **J6 Context:** provenance-aware reminders, notifications, people, documents/media and bounded screen/app context.
- **J7 Actions:** GREEN/AMBER/RED trust policy, deterministic typed adapters and audit history.
- **J8 Proactive:** My Day, pending-item assistance, quiet/battery/privacy limits.
- **J9 Multimodal:** explicit camera/image/screen/document understanding after privacy/performance gates.
- **J10 Routines:** owner-defined typed reusable workflows.

## 5. Mandatory major-step baseline procedure

Before every major capability: Idea Ledger → ADR/decision → Blueprint → Roadmap gate → preflight/test contract → rollback point → planning snapshot when direction materially changes.

After implementation: applicable CI/lint/unit/package/permission/component checks → Motorola evidence for device claims → Changelog/Latest Snapshot/test evidence → immutable milestone snapshot → protected `baseline/*` only from exact promoted green source → next-risky-phase rollback target.

A red/pending head is never called stable.

## 6. Current ordered work

1. Keep the J4 protected recovery baseline immutable.
2. Physically run J4 quality/RAM/cancel/stress/background/lock/Airplane/thermal acceptance.
3. Install exact J5 Personal Alpha from Android #2384 and verify its recorded SHA-256.
4. Prove Mayra appears/selects as default Home, Home returns 20/20, apps/search/open work, lock/unlock and reboot are sane, and switch-back works.
5. Prove Home/app access survives local-model/provider/voice/permission failures.
6. If J5 physical gate passes, synchronize evidence, immutable J5 milestone snapshot and protected J5 baseline.
7. Only then deepen voice/orb Home integration and start J6 context cards.

## 7. Mandatory resume procedure

Before coding: read canonical records; inspect PR #12 exact head and latest CI; reconcile source/docs/device evidence; identify one coherent batch and rollback point; repair/revert red heads before unrelated expansion; never claim device success without owner evidence; never merge/ready PR #12 without explicit owner approval.

## 8. Mandatory completion procedure

After every meaningful batch synchronize applicable Roadmap, Latest Snapshot, Pinpoint Audit, Test Matrix, Blueprint, Master Plan, Idea Ledger, Decisions/ADR, Changelog, START_HERE, immutable snapshot/baseline and PR description. Governance CI must remain green; stale canonical records count as a real failure.

## 9. Safety / ownership rules

- official Android roles/APIs first;
- minimum permissions for active capability;
- no secrets/private keys/owner-private data in GitHub;
- no false call/audio/device claims;
- free-form LLM text cannot directly execute privileged actions or write trusted owner memory;
- context provenance stays structured and owner-controlled;
- missing/corrupt/killed AI must fall back cleanly;
- launcher must preserve basic app access and a route to another Home app;
- no Play Protect/security/signing bypass.

## 10. Backup model

Git history is primary backup. `baseline/*` branches are immutable promoted recovery markers. `backup/*` branches preserve engineering checkpoints. `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is rolling recovery truth. CI artifacts are temporary evidence and must remain tied to source/run/digest provenance.

## 11. Immediate next action

Perform Motorola J5 acceptance on exact source `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a` / Android CI #2384 Personal Alpha artifact `8919388343`, APK SHA-256 `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`, while separately completing the J4 #142 physical quality gate.