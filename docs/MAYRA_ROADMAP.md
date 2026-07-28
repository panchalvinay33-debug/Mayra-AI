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
| Personal memory | IN_PROGRESS | Durable approvals, protected migration, expiry, diagnostics and structured provenance foundation | Full CI, health UI and phone validation |
| Search and fresh knowledge | PLANNED | No completion claim | Production provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe picker plus confirmation/idempotency/chat UX | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | Protected memory records, diagnostics and owner controls | Wider privacy center and production release |

## Personal memory — implemented

1. Explicit proposal/approval with prohibited-secret and sensitive-data exclusions.
2. Approved and pending persistence, TTL pruning, replay safety and process-death restoration.
3. Visual Save/Replace/Not now conflict review and stale-conflict rejection.
4. Approved-only relevant answer context.
5. Search, category filters, direct value edit, expiry presets and pending proposal management.
6. AES-GCM Android Keystore protection for approved memories and pending proposals with separate aliases.
7. Backward-compatible plaintext migration and failure-safe writes that preserve the previous record set.
8. Read-only storage-health classification: `EMPTY`, `HEALTHY`, `MIGRATION_NEEDED` or `DEGRADED`.
9. Health counters distinguish protected, legacy and unreadable approved/pending records without deleting data.
10. Personal-memory usage now travels as machine-readable message metadata instead of visible marker text.
11. Metadata uses URL-safe Base64 for Unicode keys, is stripped from visible answer text and is ignored when malformed.
12. Regression tests cover protected storage health and structured provenance parsing.
13. No new Android permission, service, receiver or background component.

## Remaining personal-memory gates

1. Full compile, complete unit tests, lint, R8 and component audit on the newest governed head.
2. Memory Center health card and explicit non-destructive retry-migration control.
3. Motorola validation for Keystore creation, migration, restart, Save/Replace, expiry and provenance display.
4. Dedicated Compose provenance chip rendering from `usedPersonalMemoryKeys`.
5. Key invalidation/device-lock recovery policy; never silently reset a key or erase unreadable records.
6. Protected portable backup/export design; readable export remains an explicit owner action.

## Validation truth

Latest fully green authoritative validation remains Android CI #1458 on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af` until a newer complete governed-head pipeline succeeds.

Android CI #1545 was running for protected storage head `69292e7288c5a3b67c9d51050301cc0b3bfc5303`, but this diagnostics/provenance batch is newer and remains `IN_PROGRESS` until a full pipeline validates the newest governed head.

## Governance rules

1. Update blueprint, roadmap and latest snapshot every coding batch.
2. Never claim physical validation without owner evidence.
3. Never merge or mark ready without explicit approval.
4. Keep permissions, storage migrations and background components auditable.
5. A failed migration or invalid key must preserve recoverable prior data rather than silently clearing it.

## Immediate next coding priority

Run and stabilize CI, render storage health and structured provenance in Compose, then begin the production conversational-provider boundary. PR #12 remains Draft and unmerged.
