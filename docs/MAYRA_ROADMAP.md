# Mayra AI — Execution Roadmap

Last updated: 2026-07-28
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest backup: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`

## Overall program view

| Track | Status | Progress signal | Next gate |
|---|---|---:|---|
| Blueprint and backup discipline | DONE | Canonical blueprint, roadmap and rolling snapshots | Keep updated every batch |
| Document intelligence | DEVICE_VERIFY | 16/18 implemented | Phone verification; OCR and legacy DOC deferred |
| Core routing and eligibility | DONE | Typed outcomes and capability gates | Keep regressions green |
| Audited runtime safety | DONE | Typed results, persistence, confirmation and idempotency | Physical validation |
| Concrete runtime integration | DEVICE_VERIFY | Full-app composition, history and action confirmation foundation | Physical-device validation |
| Personal memory | IN_PROGRESS | Durable approvals, conflict review, owner UI, expiry presets and protected migration implemented | Full CI and phone validation |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe picker plus confirmation/idempotency/chat UX | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | Protected memory records and local owner controls | Wider privacy center and production release |

## Personal memory — implemented

1. Explicit proposal/approval with prohibited-secret and sensitive-data exclusions.
2. Approved and pending persistence, TTL pruning, replay safety and process-death restoration.
3. Visual Save/Replace/Not now conflict review and stale-conflict rejection.
4. Approved-only relevant answer context with visible memory-key disclosure.
5. Search, category filters, direct value edit and pending proposal management.
6. Expiry controls for one day, seven days, thirty days or no expiry.
7. AES-GCM record protection backed by Android Keystore for approved memories.
8. Separate Keystore alias and protected records for pending proposals.
9. Backward-compatible plaintext-to-protected migration on successful read.
10. Failure-safe writes: protection failure occurs before SharedPreferences replacement, preserving the previous records.
11. Corrupt or undecryptable records are skipped instead of crashing startup.
12. Deterministic tests for protected writes, migration, unreadable records and rollback preservation.
13. No new Android permission, service, receiver or background component.

## Remaining personal-memory gates

1. Full compile, complete unit tests, lint, R8 and component audit on the newest governed head.
2. Motorola owner-device validation for migration, restart, Save/Replace, expiry and disclosure.
3. Dedicated structured answer provenance chip beyond appended disclosure text.
4. Key invalidation and device-lock edge-case diagnostics.
5. Export warning/optional protected export format; current readable export is an explicit owner action.

## Validation truth

Latest fully green authoritative validation remains Android CI #1458 on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af` until a newer complete governed-head pipeline succeeds.

The protected-storage and expiry batch is committed with tests but remains `IN_PROGRESS` until the newest governed head passes compile, complete tests, lint, R8 and permission/component audit. No physical-device claim has been made.

## Governance rules

1. Update blueprint, roadmap and latest snapshot every coding batch.
2. Never claim physical validation without owner evidence.
3. Never merge or mark ready without explicit approval.
4. Keep permissions, storage migrations and background components auditable.
5. A failed migration must preserve recoverable prior data rather than silently clearing it.

## Immediate next coding priority

Run and stabilize the governed CI chain. Then add storage-health diagnostics for Keystore/key invalidation and begin the production conversational-provider boundary. PR #12 remains Draft and unmerged.
