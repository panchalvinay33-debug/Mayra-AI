# Mayra AI Source of Truth and Backup Map

**Status:** Active project-control document  
**Current delivery target:** Personal Alpha V0.1 — Stable Living Companion  
**Repository:** `panchalvinay33-debug/Mayra-AI`

---

## 1. Locked Product Documents

These documents must be read together. They are not competing plans.

1. [`MAYRA_AI_MASTER_BLUEPRINT.md`](MAYRA_AI_MASTER_BLUEPRINT.md)  
   Complete Android product and engineering blueprint.
2. [`MAYRA_LIVING_INTELLIGENCE_VISION.md`](MAYRA_LIVING_INTELLIGENCE_VISION.md)  
   Permanent one-brain/many-bodies strategic extension.
3. [`PERSONAL_ALPHA_DEVICE_CHECK.md`](PERSONAL_ALPHA_DEVICE_CHECK.md)  
   Physical-device acceptance gate.
4. [`WINDOWS_PERSONAL_ALPHA_BUILD.md`](WINDOWS_PERSONAL_ALPHA_BUILD.md)  
   Reproducible Windows build and install guidance.
5. [`BUILD_AND_ARTIFACT_PROVENANCE_STATUS.md`](BUILD_AND_ARTIFACT_PROVENANCE_STATUS.md)  
   Exact-source, gate, APK hash and installation-evidence contract.
6. Feature status documents in `docs/*_STATUS.md`  
   Honest implementation boundaries and remaining gaps.

No future batch may silently remove or contradict a locked product capability. Any deliberate owner-approved change must update the relevant source-of-truth documents in the same batch.

---

## 2. Branch Roles

### `main`

Stable integrated foundation. Do not write unrelated feature work directly to `main`.

### `batch-12-runtime-control-center`

Large active integration branch and PR #9 source. Despite its old name, it contains the broader Living Companion implementation.

### `backup/pr9-living-companion-2026-07-25`

Safety snapshot of PR #9 before Living Intelligence stabilisation work. Do not use for normal development.

### `stabilize/living-companion-v0.1`

Dedicated Personal Alpha V0.1 stabilisation track. Allowed changes are limited to build reproducibility, compile/test/lint fixes, lifecycle, safety, privacy, critical Living Home/voice/Floating Mayra integration, reminder/agenda/notification/action reliability, physical diagnostics, backup and low-memory stability.

### `backup/personal-alpha-safety-2026-07-25`

Immutable-style safety snapshot at commit `38626088db6f9fa586c3ef3b4e843f40e3fe5a41`. It includes the encrypted memory backup engine, startup crash containment and diagnostics, voice-loop hardening, twenty-check physical alpha gate, privacy manifest protections and persistent Global Stop with reboot enforcement.

### `backup/personal-alpha-provenance-2026-07-25`

Immutable-style provenance snapshot at commit `4a3498e550ba0c173ec3f3c4a3d308a7de9b92c4`. It adds review-first call/message permissions, conservative identity resolution, duplicate-action protection, strict build/CI provenance, artifact tamper verification and evidence-producing installation controls on top of the earlier safety work.

Unrelated major feature expansion remains deferred until the alpha gate is accepted.

---

## 3. Current Permanent Recovery Points

| Recovery point | Purpose |
|---|---|
| `main` at the pre-PR #9 stable foundation | Return to the last broadly merged architecture |
| `backup/pr9-living-companion-2026-07-25` | Recover the large Living Companion implementation snapshot |
| `3978c5b2db84e45ce887a358937e270f2e06a67d` | Living Intelligence vision locked on the integration branch |
| `backup/personal-alpha-safety-2026-07-25` at `38626088db6f9fa586c3ef3b4e843f40e3fe5a41` | Recover encrypted backup, startup, voice and persistent-stop safety work |
| `backup/personal-alpha-provenance-2026-07-25` at `4a3498e550ba0c173ec3f3c4a3d308a7de9b92c4` | Recover contact/action safety plus exact-source build, APK and install provenance controls |
| `stabilize/living-companion-v0.1` | Current moving stabilisation and alpha-delivery branch |

Before a risky merge, schema migration or broad refactor, create another dated backup branch and record its exact commit here.

---

## 4. Code and Artifact Backup Rules

- Every accepted APK must have `artifact-manifest.json` schema `mayra.personal-alpha.artifact.v1`.
- The manifest must record repository/ref, exact source SHA, toolchain, gate states, APK file name, exact size, APK SHA-256 and source-preflight SHA-256.
- Controlled local builds require a clean Git working tree and a valid exact source commit.
- CI artifacts must come from exact checkout with persisted checkout credentials disabled.
- The APK must be independently reverified against its manifest before upload or installation.
- A diagnostic artifact with skipped tests or lint is not an accepted Personal Alpha artifact.
- Installation must produce `install-manifest.json` with source SHA, APK hash, device identity and installed package/version evidence.
- Missing provenance requires an explicit diagnostic override and cannot count toward acceptance.
- Build and install reports go to `build/personal-alpha/` locally and must not contain secrets.
- API keys, signing passwords, backup passwords and user data never enter Git or artifact metadata.
- Keep at least one rollback APK from the previous accepted physical-device build.
- Preserve Room schemas and document migrations.
- Never force-update a backup branch.
- Do not merge merely because GitHub reports a PR as mergeable; compile, tests, lint, artifact verification and physical acceptance remain separate gates.

---

## 5. User Data Backup State and Target

### Implemented foundation

- versioned `.mayrabackup` envelope;
- PBKDF2-HMAC-SHA256 password derivation;
- AES-256-GCM authenticated encryption;
- wrong-password and tamper rejection;
- sensitive-marked memory exclusion;
- preview-first additive restore;
- duplicate ID skipping without silent overwrite or deletion;
- encryption and restore unit tests.

### Remaining Backup & Restore V1 scope

- profile and settings;
- people and relationships;
- reminders and agenda;
- notification privacy policies;
- selective category restore;
- schema/app version migration;
- rollback if a multi-category restore fails.

No plaintext sensitive export is allowed by default. Android automatic/full backup is disabled; explicit Mayra encrypted backup is authoritative.

---

## 6. Persistent Global Stop Rule

Global Stop is an owner safety boundary, not a temporary UI toggle.

- state survives process death, reboot and app update;
- new Mayra phone actions remain blocked until explicit owner resume;
- Floating Mayra is stopped and cannot automatically return after reboot while Global Stop is active;
- background automation and attention scheduling remain stopped;
- already-created reminders remain scheduled as owner commitments;
- state stores only bounded operational metadata, never personal content;
- source preflight must fail if persistence or reboot enforcement is removed.

---

## 7. Validation Vocabulary

1. **Coded** — source exists; no build claim.
2. **Source-preflight verified** — strict source and control checks execute successfully on an exact checkout.
3. **Compile verified** — production Kotlin compiles.
4. **Unit-test verified** — complete unit suite passes.
5. **Lint verified** — Android lint passes or a reviewed baseline is documented.
6. **APK assembled** — APK file exists from the exact source.
7. **Artifact verified** — manifest, gate states, exact size and APK SHA-256 independently match.
8. **Installed** — verified artifact installs on a named device and package/version evidence is recorded.
9. **Physical-device verified** — relevant manual checks pass.
10. **Personal-alpha accepted** — mandatory gate passes with no critical blocker and a rollback artifact exists.
11. **Production-ready** — release signing, policies, compatibility, privacy and distribution gates pass.

GitHub Actions failing before Checkout proves neither source failure nor source success.

---

## 8. Personal Alpha V0.1 Execution Order

1. Preserve development snapshots.
2. Lock source-of-truth and vision.
3. Make Windows build and CI self-checking and reproducible.
4. Run strict source preflight and provenance regression tests.
5. Compile production sources.
6. Run complete unit tests.
7. Run Android lint.
8. Assemble, describe and independently verify the APK.
9. Install the verified artifact and record package/device evidence.
10. Complete the twenty-check Personal Device Test Center.
11. Fix critical and high-severity failures in coherent batches.
12. Preserve rollback APK/source snapshot.
13. Merge accepted stabilisation into the integration branch, then integrate safely into `main`.
14. Complete multi-category encrypted Backup & Restore V1.
15. Continue deeper context assistance and multi-device platform work.

---

## 9. Fast-Batch Rule

Work should move quickly, but each batch must maximise coherent value rather than raw file count. A good large batch includes closely related fixes, deterministic tests where possible, updated boundaries, exact recovery references and no inflated completion claims.

A batch must stop before it becomes an unreviewable mixture of unrelated systems.

---

## 10. Current Product Statement

> Mayra is one living personal intelligence with a portable brain, authorised memory, shared safety constitution, replaceable device adapters and multiple possible presence renderers. Android is the first body; stable daily usefulness is the first gate; smart-device and holographic continuity are future bodies of the same Mayra.
