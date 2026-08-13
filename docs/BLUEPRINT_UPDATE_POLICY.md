# Mayra Project Records and Update Policy

Effective: 2026-07-27
Updated: 2026-08-03
Enforced by: `.github/workflows/project-governance.yml`

## Purpose

Mayra is developed across multiple sessions, tools and agents. The repository must be sufficient to understand and resume the project without relying on private chat history.

Documentation synchronization is part of implementation—not optional follow-up work.

## Mandatory canonical files

- `START_HERE.md` — mandatory first read and resume/completion procedure.
- `README.md` — repository landing page that points to START_HERE.
- `docs/MAYRA_BLUEPRINT.md` — long-lived architecture, principles, system boundaries and module design.
- `docs/MAYRA_ROADMAP.md` — current status, ordering and next gates.
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` — exact rolling recovery state.
- `docs/backups/MAYRA_SNAPSHOT_<date>_<milestone>.md` — immutable milestone/transition snapshots.
- `docs/MAYRA_IDEA_LEDGER.md` — accepted, active, deferred, superseded and removed ideas.
- `docs/MAYRA_DECISIONS.md` — architecture/product decisions and supersession history.
- `docs/MAYRA_CHANGELOG.md` — meaningful delivery/milestone history.
- `docs/MAYRA_FULL_APP_ACCEPTANCE.md` — physical Motorola acceptance evidence.

## Permanent owner instruction

Every accepted idea, feature, architecture decision, scope change, removed idea or implementation batch must be represented in GitHub project records. Important requirements may not exist only in chat, commit messages, PR descriptions or personal memory.

When an idea changes or is removed, mark it `SUPERSEDED` or `REMOVED` with the reason. Do not silently erase history.

## Mandatory start-of-session procedure

Before coding or making project claims:

1. Read `START_HERE.md`.
2. Read `docs/backups/MAYRA_LATEST_SNAPSHOT.md`.
3. Read the active roadmap phase.
4. Check PR #12 head, Draft/open/unmerged state and latest-head CI.
5. Compare current code against blueprint, ideas and decisions.
6. Identify the coherent batch, tests and required record updates.
7. Separate automated verification from physical-device evidence.

## Mandatory update for every meaningful coding batch

Before a batch is reported complete:

1. Update `docs/MAYRA_ROADMAP.md` with completed work, current status and next ordered gate.
2. Update `docs/backups/MAYRA_LATEST_SNAPSHOT.md` with branch/head, CI truth, artifacts, risks and exact next action.
3. Update `docs/MAYRA_BLUEPRINT.md` for architecture, permission, background, data flow or trust-boundary changes.
4. Update `docs/MAYRA_DECISIONS.md` for significant design choices or supersessions.
5. Update `docs/MAYRA_IDEA_LEDGER.md` for feature-track additions, changes, deferrals, removals or deliveries.
6. Update `docs/MAYRA_CHANGELOG.md` for meaningful user-visible/release/build behavior.
7. Update `START_HERE.md` when the entry truth, document map, major current phase or resume procedure changes.
8. Create an immutable snapshot at a phase boundary, release candidate, migration, risky architecture change or major device milestone.
9. Update PR description when the overall PR scope or authoritative milestone changes.
10. Run Android CI and Project Governance CI.

## Automated enforcement

`scripts/verify_project_governance.sh` validates:

- mandatory records exist and are non-empty;
- START_HERE links the canonical project records and contains a resume procedure;
- key blueprint/roadmap/idea/decision sections exist;
- project records do not contain obvious API keys/private signing material;
- meaningful implementation/build/workflow changes also update Roadmap and Latest Snapshot;
- architecture/core/background changes also update Blueprint and Decisions;
- feature-track changes also update Idea Ledger;
- release/build/manifest changes also update Changelog.

`.github/workflows/project-governance.yml` runs this on relevant pushes and pull requests. A governance failure is a real project failure and must be fixed rather than bypassed.

## New idea intake

When the owner introduces a new idea:

1. assign/reuse an Idea ID;
2. record the desired user outcome;
3. record Android/device/API constraints honestly;
4. set status `ACCEPTED` or `IN_PROGRESS`;
5. place it in roadmap order;
6. update blueprint/decision log if architecture changes;
7. update rolling snapshot;
8. do not mark delivered until implementation and relevant checks pass;
9. do not mark device-verified without Motorola evidence.

## Idea removal or change

When an idea is abandoned or replaced:

1. retain the original Idea ID;
2. set `SUPERSEDED` or `REMOVED`;
3. record replacement/reason;
4. remove it from active roadmap work;
5. update blueprint if the system boundary changes;
6. add a decision record when the change is architectural;
7. note it in the latest snapshot.

## Maximum useful batch rule

Each implementation pass should deliver the largest coherent, reviewable and safely testable scope available, normally including:

- production code;
- focused/regression tests;
- runtime/UI integration;
- permission/privacy/reliability checks;
- release or manifest audits where applicable;
- synchronized project records;
- honest validation and next gate.

Large scope does not justify inventing status, ignoring Android platform boundaries, committing secrets, weakening rollback, or claiming device success without proof.

## Status vocabulary

Use only:

- `DONE`: implementation and relevant automated checks passed.
- `DEVICE_VERIFY`: code and CI passed; physical test pending.
- `IN_PROGRESS`: actively implemented or latest-head validation pending.
- `PLANNED`: accepted but not implemented.
- `DEFERRED`: intentionally postponed and non-blocking.
- `REMOVED`: intentionally dropped with reason retained.

A feature can be CI-verified without being device-verified.

## Snapshot requirements

Every rolling or immutable snapshot records:

- date/time when useful;
- branch and PR state;
- exact verified/current head SHA;
- authoritative Android CI and governance CI runs;
- artifacts and digests when available;
- completed capabilities;
- pending validation;
- known limitations/risks;
- ideas/decisions added, changed or removed;
- explicit next action;
- Draft/merge truth.

Never place credentials, signing secrets, tokens or private owner data in snapshots.

## Backup model

- Git commit history is the primary durable backup.
- Rolling snapshot is the quickest recovery point.
- Immutable snapshots preserve major milestones.
- CI artifacts are temporary evidence; record IDs/digests for authoritative candidates.
- Private keystores and credentials belong in owner-controlled secure storage outside GitHub.

## Branch and PR safety

- PR #12 remains Draft and unmerged until explicit owner approval.
- Documentation updates do not authorize merge, ready-for-review, new privileged roles or permissions.
- Failed/pending latest-head CI must be stated honestly; an older green run cannot prove a newer head.
- Privileged Android roles require explicit owner setup and physical acceptance.

## Completion integrity checklist

Before reporting completion, compare:

1. actual code and tests;
2. Android manifest/build variants;
3. latest-head Android CI;
4. Project Governance CI;
5. START_HERE;
6. blueprint;
7. roadmap;
8. latest snapshot;
9. idea ledger;
10. decisions;
11. changelog;
12. PR state;
13. physical-device evidence, if claimed.

Any mismatch must be corrected or disclosed before the batch is called complete.
