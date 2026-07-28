# Blueprint, Roadmap and Backup Update Policy

Effective: 2026-07-27
Updated: 2026-07-28

## Mandatory files

- `docs/MAYRA_BLUEPRINT.md` — long-lived architecture, principles and scope.
- `docs/MAYRA_ROADMAP.md` — current execution status, ordering and next gates.
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` — rolling recovery record updated every coding batch.
- `docs/backups/MAYRA_SNAPSHOT_<date>_<milestone>.md` — immutable milestone snapshots.
- GitHub Issue #13 — canonical Jarvis North Star, idea intake and cross-module delivery track.

## Permanent owner instruction

Every accepted idea, feature, architecture decision, safety rule, scope change or implementation batch must be reflected in the project records. No important requirement may remain only in chat, commit messages or an individual pull request description.

The documentation update is part of the feature itself, not optional follow-up work.

## Required update for every coding batch

Before a batch is reported complete:

1. Update roadmap statuses for work completed, validation pending and next priority.
2. Update latest snapshot with branch/head, CI run, artifacts, completed work, unresolved risks and next step.
3. Update blueprint when architecture, privacy boundaries, module scope or non-negotiable behavior changes.
4. Update Issue #13 when a new idea changes the Jarvis North Star, delivery order, capability scope or permanent safety boundary.
5. Create an immutable dated snapshot for a milestone boundary, release candidate, migration, major module handoff or risky architectural transition.
6. Keep physical-device validation separate from automated validation.
7. Record superseded ideas and architecture explicitly instead of silently deleting history.
8. Verify that roadmap, blueprint, latest snapshot, capability registry, PR description and actual code do not contradict one another.

## New idea intake

When the owner adds a new idea:

1. Classify it under the appropriate Jarvis North Star delivery track.
2. Record its intended user outcome, dependencies, Android/platform constraints and risk class.
3. Add it to the roadmap as `PLANNED` or `IN_PROGRESS`.
4. Update the blueprint if it changes architecture, data flow, permissions, background behavior or trust boundaries.
5. Update the latest snapshot with the accepted direction and immediate next step.
6. Do not mark it `DONE` until implementation and relevant automated checks pass.
7. Do not mark a physical-device flow passed without owner-device evidence.

## Maximum useful batch rule

Each implementation pass should deliver the largest coherent, reviewable and safely testable scope available. A batch should normally include:

- production code;
- focused and regression tests;
- safety and privacy checks;
- runtime/UI integration where applicable;
- roadmap and blueprint synchronization;
- latest recovery snapshot;
- honest validation status and next gate.

Large scope must never justify bypassing confirmation, permissions, test gates, rollback safety or truthful status reporting.

## Status integrity

Use only:

- `DONE`: implementation and relevant automated checks passed.
- `DEVICE_VERIFY`: code and CI passed, physical test pending.
- `IN_PROGRESS`: actively being implemented or validated.
- `PLANNED`: accepted but not implemented.
- `DEFERRED`: intentionally postponed and not blocking the current module.

Never mark work DONE merely because code was written. Never mark a physical flow passed without owner/device evidence.

## Backup contents

Every snapshot must include:

- date;
- branch and PR;
- verified head SHA;
- authoritative CI run;
- artifacts and digests when available;
- completed capabilities;
- pending validation;
- known limitations and risks;
- newly accepted ideas or scope changes;
- explicit next step;
- merge/draft truth.

## Branch and PR safety

- PR #12 remains Draft and unmerged until explicit owner approval.
- Roadmap documentation does not authorize merging, marking ready, adding permissions or changing background behavior.
- A failed or pending latest-head CI must be recorded honestly; an earlier green run cannot be presented as proof for a newer head.
- New work must reference the relevant section of Issue #13 and preserve a clear rollback path.

## Drift prevention

The app-level `MayraCapabilityRegistry` is the machine-testable program status source. Tests enforce unique IDs, complete counts and critical document/governance states. Human roadmap documents provide context and sequence; neither source should silently contradict the other.

Before every completion report, compare:

1. actual code and tests;
2. capability registry state;
3. roadmap status;
4. blueprint architecture;
5. latest backup snapshot;
6. PR head and current CI evidence;
7. Issue #13 Jarvis North Star.

Any mismatch must be corrected or disclosed before the batch is called complete.
