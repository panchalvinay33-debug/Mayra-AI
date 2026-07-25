# Mayra AI Source of Truth and Backup Map

**Status:** Active project-control document  
**Current delivery target:** Personal Alpha V0.1 — Stable Living Companion  
**Repository:** `panchalvinay33-debug/Mayra-AI`

---

## 1. Locked Product Documents

These documents must be read together. They are not competing plans.

1. [`MAYRA_AI_MASTER_BLUEPRINT.md`](MAYRA_AI_MASTER_BLUEPRINT.md)  
   The complete Android product and engineering blueprint: Living Home, Floating Mayra, context assistance, permissions, intelligence, memory, actions, reminders, agenda, notifications, safety and development phases.

2. [`MAYRA_LIVING_INTELLIGENCE_VISION.md`](MAYRA_LIVING_INTELLIGENCE_VISION.md)  
   The permanent strategic extension: Mayra begins by making the Android phone feel alive and later becomes one continuous intelligence across smart devices, vehicles, wearables, displays and future holographic or robotic presence surfaces.

3. [`PERSONAL_ALPHA_DEVICE_CHECK.md`](PERSONAL_ALPHA_DEVICE_CHECK.md)  
   The physical-device acceptance gate and manual verification record.

4. [`WINDOWS_PERSONAL_ALPHA_BUILD.md`](WINDOWS_PERSONAL_ALPHA_BUILD.md)  
   Windows build and install guidance for the owner-first alpha.

5. Feature status documents in `docs/*_STATUS.md`  
   Honest implementation boundaries, tests and remaining gaps for each subsystem.

No future batch may silently remove or contradict a locked product capability. A deliberate owner-approved change must update the relevant source-of-truth documents in the same batch.

---

## 2. Branch Roles

### `main`

Stable integrated foundation. Do not write unrelated feature work directly to `main`.

### `batch-12-runtime-control-center`

Large active integration branch and PR #9 source. Despite its old branch name, it contains the broader Living Companion implementation.

### `backup/pr9-living-companion-2026-07-25`

Immutable-style safety snapshot of PR #9 before the Living Intelligence vision extension and stabilisation work. Do not use it for normal development.

### `stabilize/living-companion-v0.1`

Dedicated Personal Alpha V0.1 stabilisation track. Allowed changes are limited to:

- build reproducibility;
- compile/test/lint fixes;
- crashes and lifecycle;
- safety and privacy;
- critical Living Home, voice and Floating Mayra integration;
- reminder, agenda, notification and action reliability;
- physical-device diagnostics;
- backup foundations;
- low-memory and battery stability.

Unrelated major feature expansion is deferred until the alpha gate is accepted.

---

## 3. Current Permanent Recovery Points

| Recovery point | Purpose |
|---|---|
| `main` at the pre-PR #9 stable foundation | Return to last broadly merged architecture |
| `backup/pr9-living-companion-2026-07-25` | Recover the large Living Companion implementation snapshot |
| `3978c5b2db84e45ce887a358937e270f2e06a67d` | Living Intelligence vision locked on the integration branch |
| `stabilize/living-companion-v0.1` | Current stabilisation and alpha-delivery work |

Before a risky merge, schema migration or broad refactor, create another dated backup branch and record the exact commit here.

---

## 4. Code Backup Rules

- Every APK must record source branch, commit SHA, build time and SHA-256.
- Build and install reports go to `build/personal-alpha/` locally and must not contain secrets.
- API keys, signing passwords and user data never enter Git.
- Keep at least one rollback APK from the previous accepted physical-device build.
- Preserve Room schemas and document migrations.
- Never force-update a backup branch.
- Do not merge PR #9 into `main` merely because it is GitHub-mergeable; compile, tests, lint and physical acceptance remain separate gates.

---

## 5. User Data Backup Target

Encrypted Backup & Restore V1 must cover:

- profile and settings;
- people and relationships;
- reminders and agenda;
- notes, ideas, shopping lists and checklists;
- approved memories and preferences;
- notification privacy policies;
- schema version, checksums and app version.

Required restore behaviour:

- preview categories before restore;
- selective restore;
- duplicate and conflict handling;
- corruption detection;
- rollback when restore fails;
- no plaintext sensitive export by default.

Backup & Restore V1 is required before dependable personal beta, though the first physical alpha may precede it when a source/APK rollback exists.

---

## 6. Validation Vocabulary

Use only honest milestones:

1. **Coded** — source exists; no build claim.
2. **Compile verified** — production Kotlin compiles.
3. **Unit-test verified** — complete unit suite passes.
4. **Lint verified** — Android lint passes or a reviewed baseline is documented.
5. **APK verified** — APK assembled and hashed.
6. **Installed** — clean/update install verified on a named device.
7. **Physical-device verified** — the relevant manual checks pass.
8. **Personal-alpha accepted** — mandatory gate passes with no critical blocker.
9. **Production-ready** — release signing, policies, compatibility, privacy and distribution gates pass.

GitHub Actions failing before Checkout does not prove source failure, but it also does not prove success.

---

## 7. Personal Alpha V0.1 Execution Order

1. Preserve development snapshot.
2. Lock source-of-truth and vision.
3. Make Windows build self-bootstrapping and reproducible.
4. Compile production sources.
5. Run complete unit tests.
6. Run Android lint.
7. Assemble and hash APK.
8. Install on the owner's physical Android phone.
9. Complete Personal Device Test Center.
10. Fix critical and high-severity failures in coherent batches.
11. Create rollback APK/source snapshot.
12. Merge accepted stabilisation into the integration branch, then integrate safely into `main`.
13. Build encrypted Backup & Restore V1.
14. Continue deeper context assistance and multi-device platform work.

---

## 8. Fast-Batch Rule

Work should move quickly, but each response/batch must maximise **coherent verified value**, not raw file count.

A good large batch includes:

- one or more closely related fixes;
- tests or deterministic validation where possible;
- documentation of boundaries;
- exact commit references;
- no inflated completion claims;
- a clear next gate.

A batch must stop before it becomes an unreviewable mixture of unrelated systems.

---

## 9. Current Product Statement

> Mayra is one living personal intelligence with a portable brain, authorised memory, shared safety constitution, replaceable device adapters and multiple possible presence renderers. Android is the first body; stable daily usefulness is the first gate; smart-device and holographic continuity are future bodies of the same Mayra.
