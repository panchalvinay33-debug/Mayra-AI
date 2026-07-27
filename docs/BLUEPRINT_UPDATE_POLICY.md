# Blueprint, Roadmap and Backup Update Policy

Effective: 2026-07-27

## Mandatory files

- `docs/MAYRA_BLUEPRINT.md` — long-lived architecture, principles and scope.
- `docs/MAYRA_ROADMAP.md` — current execution status, ordering and next gates.
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` — rolling recovery record updated every coding batch.
- `docs/backups/MAYRA_SNAPSHOT_<date>_<milestone>.md` — immutable milestone snapshots.

## Required update for every coding batch

Before a batch is reported complete:

1. Update roadmap statuses for work completed, validation pending and next priority.
2. Update latest snapshot with branch/head, CI run, artifacts, completed work, unresolved risks and next step.
3. Update blueprint when architecture, privacy boundaries, module scope or non-negotiable behavior changes.
4. Create an immutable dated snapshot for a milestone boundary, release candidate, migration, or major module handoff.
5. Keep physical-device validation separate from automated validation.

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
- explicit next step;
- merge/draft truth.

## Branch and PR safety

- PR #12 remains Draft and unmerged until explicit owner approval.
- Roadmap documentation does not authorize merging, marking ready, adding permissions, or changing background behavior.
- A failed or pending latest-head CI must be recorded honestly; an earlier green run cannot be presented as proof for a newer head.

## Drift prevention

The app-level `MayraCapabilityRegistry` is the machine-testable program status source. Tests enforce unique IDs, complete counts and critical document/governance states. Human roadmap documents provide context and sequence; neither source should silently contradict the other.