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
| Concrete runtime integration | DEVICE_VERIFY | Full-app composition, Activity History and main-chat action confirmation are CI-verified | Physical-device validation |
| Personal memory | IN_PROGRESS | Durable approvals, conflict review, context use, searchable/editable Memory Center and pending review UI implemented | Full CI, expiry editing, protected storage and phone validation |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe picker plus confirmation/idempotency/chat UX implemented | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | Local history, confirmation UX, memory exclusions and owner controls | Privacy center and production release |

## Document intelligence

DONE: local library, TXT/PDF/DOCX extraction, Unicode search/snippets, summaries, grounded Q&A, async limits, freshness, Current-only evidence, Library Health, smart/force maintenance, transactional commits and isolated zero-permission CI APK.

DEVICE_VERIFY: PDF search, DOCX search, freshness UI and maintenance UX.

DEFERRED: on-device OCR and legacy binary `.doc` parsing.

## Core assistant and runtime

DONE: typed outcomes, capability gates, idempotency, persistent activity history, confirmation tokens, answer/document/action adapters, Android composition, safe system picker, Activity History, main-chat typed bridge and action Confirm/Cancel UX.

DEVICE_VERIFY: Activity History, file picker, action confirmation, rotation retention and end-to-end phone flow. Process-death persistence for non-memory action confirmations remains unimplemented.

## Personal memory

### IMPLEMENTED

1. Candidate, approved-memory, provenance, revision and expiry models
2. Explicit proposal and one-time approval; proposal creation never writes approved storage
3. Prohibited-secret and sensitive-personal-data exclusions
4. Bounded versioned Android persistence for approved memories
5. Deterministic Hindi/Hinglish/English remember, confirm, cancel, forget and list routing
6. Bounded persistent pending-proposal store with corruption recovery and TTL pruning
7. Process-death restoration of pending memory approval
8. Visual Save/Not now and Replace/Not now review
9. Same-key contradiction comparison and stale-conflict rejection
10. Read-only injection of approved, active, query-relevant memories into normal answer context
11. Search across approved memory key/value text
12. Category filters covering every memory category
13. Direct owner edit with privacy-policy revalidation and revision increment
14. Pending proposal management in Memory Center with Save/Replace/Not now controls
15. Clear-all covers approved memories and pending proposals
16. Owner-visible disclosure appended when an answer used approved personal memory
17. No new Android permission, service, receiver or background component
18. Regression coverage for persistence, replay safety, conflicts, restoration, approved-only context and disclosure

### REMAINING

1. Direct expiry editing and custom expiry selection in Memory Center
2. Encrypted or Android Keystore-backed protected memory-at-rest migration
3. Backward-compatible migration and rollback tests for protected storage
4. Richer answer-level provenance surface beyond the compact memory-key disclosure
5. Owner-device validation of save/replace/restart/edit/filter/context flows

## Validation truth

Latest fully green authoritative validation remains Android CI #1458 on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af` until a newer complete governed-head pipeline succeeds.

The current Memory Center and disclosure batch is committed with tests but remains `IN_PROGRESS` pending compile, complete unit tests, lint, R8 and permission/component audit on the newest governed head.

## Governance rules

1. Update blueprint/roadmap/latest snapshot every coding batch.
2. Record verified head, CI and artifact digest.
3. Never claim physical validation without owner evidence.
4. Never merge or mark ready without explicit approval.
5. Keep permissions and background components auditable.

## Immediate next coding priority

Complete the governed CI chain and fix any regression. Then implement expiry editing plus protected at-rest storage with backward-compatible migration, followed by owner-device memory acceptance. PR #12 remains Draft and unmerged.
