# Mayra AI — Secure Baseline and Rollback Playbook

Last updated: 2026-08-03
Entry point: `START_HERE.md`

## Purpose

Mayra development must be ambitious without making a known-good state disposable. This playbook defines how green baselines are created, protected, promoted and recovered.

## Current protected baseline

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Version: `0.2.1` / versionCode `4`
- Authoritative CI: Android CI `#1795` — success
- Scope: last fully green pre-Jarvis build, including provider, memory, documents, actions, reminders, Personal Alpha, minified final release audit, Full Test and Document Test.

This branch is a recovery marker. Do not develop directly on it, force-move it, merge into it or rewrite its history.

## Baseline promotion requirements

Create a new `baseline/<milestone>-green-<run>` branch only when all applicable checks pass on the exact source commit:

1. Project Governance workflow green.
2. Debug, Personal Alpha and Full Test compilation green.
3. Complete unit-test suite green.
4. All governed lint variants green.
5. Personal Alpha package/permission/component/launcher audit green.
6. Minified final release/R8 audit green.
7. Safe Full Test audit green.
8. Isolated Document Test audit green.
9. Roadmap and latest snapshot synchronized.
10. Immutable milestone snapshot created for major architecture/release transitions.
11. Any claimed device behavior has dated Motorola evidence.

A pending or failed head can never become a baseline.

## Development rule

- Continue work only on the active development branch.
- Keep commits small enough to identify the breaking batch.
- Do not stack unrelated speculative features on a red head.
- On failure, inspect exact CI logs and repair or revert the smallest responsible change.
- Never weaken tests, permission audits or governance merely to make CI green.

## Recovery levels

### Level 1 — Repair forward

Use when the failure is isolated and the intended architecture remains correct.

1. Record failing head and CI run in latest snapshot.
2. Inspect exact failing step/log.
3. Add or repair a focused regression test.
4. Fix the smallest responsible code/configuration.
5. Update roadmap/snapshot.
6. Wait for full latest-head green.

### Level 2 — Revert one batch

Use when a coherent recent batch is flawed or unclear.

1. Identify the last green commit before the batch.
2. Preserve failure evidence in changelog/snapshot.
3. Revert only the batch commits on the active branch.
4. Do not delete the original commits or rewrite shared history.
5. Run all workflows again.

### Level 3 — Restore from protected baseline

Use for widespread breakage, corrupted direction, major data-loss/signing/package regression or an unrecoverable sequence of speculative commits.

1. Do not move/delete the protected baseline branch.
2. Create a new recovery branch from the baseline commit.
3. Reapply only reviewed commits in coherent groups.
4. Run governance and Android CI after every group.
5. Re-establish a new green baseline before resuming risky work.
6. PR #12 remains Draft/unmerged unless owner explicitly decides otherwise.

## Device rollback

For owner-device testing:

- Record package, version, source commit, CI run and APK SHA-256 before installation.
- Do not install Debug/Full Test/Document Test over the Personal Alpha/final package by accident.
- Use only the newest explicitly shared owner candidate.
- Never bypass Play Protect; use lower-risk variants or signed/internal distribution when needed.
- Before testing migrations/upgrades, preserve expected local data state and record whether uninstall will erase it.
- If a new candidate crashes or corrupts behavior, stop testing that candidate and return to the last recorded installable baseline package where signature/package compatibility permits.

## Data and secret recovery

Git baselines do not include:

- API keys;
- release keystores/passwords;
- owner memories/documents;
- private phone data.

These must never be committed. A code rollback must not pretend to restore private device data. Storage migrations require their own backup/restore and failure-path tests.

## Baseline record template

Every promoted baseline must be recorded in the latest snapshot and immutable milestone snapshot with:

- branch name;
- commit SHA;
- version/versionCode;
- CI and Governance run numbers;
- test count;
- artifact IDs, sizes and SHA-256 digests;
- package/permission/component boundary;
- device verification status;
- known limitations;
- exact next risky phase.

## Forbidden actions

- Force-moving a protected baseline branch.
- Calling a failed/pending commit “stable”.
- Deleting failed history to hide a problem.
- Reusing an artifact without source/run/digest provenance.
- Merging PR #12 or marking ready without owner approval.
- Disabling checks because a new capability is difficult to package or test.
