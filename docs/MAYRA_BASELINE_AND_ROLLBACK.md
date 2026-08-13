# Mayra AI — Secure Baseline and Rollback Playbook

Last updated: 2026-08-05
Entry point: `START_HERE.md`
Jarvis/launcher direction: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`

## Purpose

Mayra development must be ambitious without making known-good states disposable. This playbook defines how green baselines are created, protected, promoted and recovered.

## Major-step checkpoint rule

Every major capability now has **two different checkpoint types** and they must never be confused:

1. **Planning/direction snapshot** — immutable record of the accepted idea, architecture, risks, validation gate and rollback path. It may be created even while the active head is red, but it is explicitly NOT a stable application baseline.
2. **Green recovery baseline** — protected `baseline/*` branch pointing to the exact implementation commit only after every applicable automated/device gate passes.

Before a major step begins, synchronize applicable Idea Ledger, Decision/ADR, Blueprint, Roadmap and preflight/test records. After a major step becomes exact-green, synchronize Changelog, Latest Snapshot, test/device evidence and create an immutable milestone snapshot before moving to the next risky phase.

This lifecycle is mandatory for J5 launcher, J6 context, J7 action trust, J8 proactive Mayra, J9 multimodal and J10 routines as well as future major tracks.

## Current protected baselines

### Full-app pre-Jarvis

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795 — success
- Scope: last fully green pre-Jarvis owner app foundation.

### Jarvis Assistant foundation

- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851 — success

### Zero-permission Assistant proof

- Branch: `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- Commit: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 #44 / Android CI #1935 / Governance #116 — success

### J1 activation repair

- Branch: `baseline/mayra-0.2.1-j1-activation-repair-green-56`
- Commit: `ce96f8e83fe33b878d426c407715d4a3e1b0495a`
- J1 #56 / Android CI #1947 / Governance #128 — success

### Current invocation-time voice baseline

- Branch: `baseline/mayra-0.2.1-j2-voice-green-18`
- Commit: `ef809bbdaca80f3b953483499dc03de8e091339f`
- J2 Voice Test #18 — success
- J1 Assistant Test #122 — success
- Android CI #2013 — success
- Project Governance #194 — success
- J2 APK SHA-256: `ef48c264f841efd7891e335848e90f38654a6dd25f70d10b0a3afd08b968ecbc`
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_J2_VOICE_CI18.md`

All baseline branches are recovery markers. Do not develop directly on them, force-move them, merge into them or rewrite their history.

## Baseline promotion requirements

Create a new `baseline/<milestone>-green-<run>` branch only when all applicable checks pass on the exact source commit:

1. Project Governance green.
2. Governed application variants compile.
3. Complete applicable unit-test suite green.
4. Governed lint variants green.
5. Relevant APK package/permission/component/launcher audits green.
6. Minified final release/R8 audit green when the shared/full-app code changed.
7. Safe Full Test and isolated Document Test remain green where applicable.
8. Roadmap and rolling snapshot are synchronized.
9. Major architecture/release transitions receive an immutable snapshot.
10. Any claimed device behavior has dated Motorola evidence.
11. For launcher milestones: HOME-role/default selection, app reachability, crash/model-failure fallback and switch-back behavior are physically validated where applicable.
12. The exact next risky phase and rollback point are recorded.

A pending or failed head can never become a baseline.

Documentation-only commits after a validated application commit do not move the application baseline. The exact application source SHA remains the authoritative artifact source until code changes and all gates pass again.

## Development rule

- Continue work only on the active development branch.
- Keep coherent changes isolated enough to identify a breaking batch.
- Do not stack speculative features on a red head.
- On failure, inspect the exact CI/device evidence and repair or revert the smallest responsible change.
- Never weaken tests, permission audits or governance merely to force green.
- Never use a baseline branch as an experimental branch.
- A planning snapshot may document a future direction while the head is red, but implementation of unrelated major features waits for recovery to green.
- Each major step must identify the last known recovery point before implementation begins.

## Recovery levels

### Level 1 — Repair forward

Use when a failure is isolated and the intended architecture remains correct.

1. Record failing source/device result.
2. Inspect the exact failing step/log/evidence.
3. Add or repair focused regression coverage when practical.
4. Fix the smallest responsible code/configuration.
5. Update roadmap/snapshot.
6. Wait for full latest-head green before promotion.

### Level 2 — Revert one batch

Use when a coherent recent batch is flawed or unclear.

1. Identify the last green commit before the batch.
2. Preserve failure evidence.
3. Revert only the responsible batch on the active branch.
4. Do not delete original commits or rewrite shared history.
5. Run required workflows again.

### Level 3 — Restore from protected baseline

Use for widespread breakage, corrupted direction, major data-loss/signing/package regression or an unrecoverable speculative sequence.

1. Do not move/delete the protected baseline branch.
2. Create a new recovery branch from the selected baseline commit.
3. Reapply only reviewed commits in coherent groups.
4. Run governance and relevant CI after every group.
5. Re-establish a new green baseline before resuming risky work.
6. PR #12 remains Draft/unmerged unless the owner explicitly decides otherwise.

## Device rollback

For owner-device testing:

- record package, version, source commit, CI run and APK SHA-256 before installation;
- never install a test package over a different package identity accidentally;
- use only the newest explicitly promoted candidate for that engineering track;
- never bypass Play Protect;
- understand whether uninstall will erase local data before testing an update path;
- if a candidate crashes or corrupts behavior, stop that candidate and return to the last recorded compatible package where signing/package identity permits;
- J1/J2/J3/J4 and future isolated J5 engineering packages are disposable engineering packages and must not be treated as the final personal-data container;
- launcher testing must always preserve a documented route to select the previous/default launcher again.

## Data and secret recovery

Git baselines do not include API keys, signing keystores/passwords, owner memories/documents or private phone data. These must never be committed. A code rollback must not pretend to restore private device data. Storage migrations require separate backup/restore/failure-path tests.

## Baseline record template

Every promoted baseline must record:

- branch name;
- commit SHA;
- version/versionCode;
- CI/Governance run numbers;
- artifact IDs, sizes and SHA-256 digests;
- package/permission/component/launcher boundary;
- device verification state;
- known limitations;
- exact next risky phase;
- rollback target and recovery instructions.

## Forbidden actions

- Force-moving a protected baseline branch.
- Calling a failed/pending commit stable.
- Calling a planning snapshot a green application baseline.
- Deleting failed history to hide a problem.
- Reusing an artifact without source/run/digest provenance.
- Merging PR #12 or marking it ready without explicit owner approval.
- Disabling checks because a capability is difficult to package or test.
