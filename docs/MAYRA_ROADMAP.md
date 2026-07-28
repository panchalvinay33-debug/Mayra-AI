# Mayra AI — Execution Roadmap

Last updated: 2026-07-28
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest backup: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`

## Status vocabulary

- `DONE`: implemented and automated validation complete.
- `DEVICE_VERIFY`: implementation and CI complete; physical Android validation is still required.
- `IN_PROGRESS`: active implementation milestone.
- `PLANNED`: accepted roadmap scope, not yet implemented.
- `DEFERRED`: intentionally postponed and not a current blocker.

## Overall program view

| Track | Status | Progress signal | Next gate |
|---|---|---:|---|
| Product blueprint and backup discipline | DONE | Blueprint, roadmap, rolling/immutable snapshots and update policy | Keep updated every batch |
| Document intelligence foundation | DEVICE_VERIFY | 16/18 implemented (88%) | Phone verification; OCR and legacy DOC deferred |
| Core assistant routing | DONE | Typed outcomes, reasons, confidence, capability and confirmation policy | Keep compatibility regressions green |
| Provider/tool eligibility | DONE | Runtime capability gates and auditable execution dispositions | Concrete adapter integration |
| Audited routing runtime boundary | DONE | Typed results, handler isolation, idempotency, persistent audit and confirmation tokens | Wire real adapters |
| Concrete runtime integration | IN_PROGRESS | Contracts and safety gates exist | Answer/document/action adapters and end-to-end wiring |
| Personal memory | PLANNED | No completion claim | Consent-first schema and controls |
| Search and knowledge providers | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | IN_PROGRESS | Confirmation, idempotency, persistent audit model implemented | Confirmed action adapters and history UI |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy/safety center | IN_PROGRESS | Least-privilege CI audits exist | User-facing controls and audit history |
| Release/recovery | IN_PROGRESS | CI, isolated APKs and snapshots exist | Versioning and migration/release checklist |

## Track A — Document intelligence

### DONE

1. Persistable local document library metadata
2. Plain-text extraction
3. Text-based PDF extraction
4. DOCX extraction
5. Unicode-aware Hindi/Hinglish/English search
6. Search snippets
7. Local summaries
8. Grounded document Q&A
9. Async IO indexing
10. Parser safety limits
11. Index freshness and parser versioning
12. Current-only evidence policy
13. Library Health inventory
14. Smart refresh and force rebuild
15. Transactional content + fingerprint commit/rollback
16. Isolated zero-permission document-test APK and CI audit

### DEVICE_VERIFY

- PDF re-index and content search
- DOCX add/index/search
- freshness badge and changed-file behavior
- Smart refresh and transactional maintenance UX

### DEFERRED

17. On-device OCR for scanned PDFs/images
18. Legacy binary `.doc` parser

## Track B — Core assistant and routing

### DONE

1. Global capability registry and status snapshot
2. Typed outcomes: `ANSWER`, `RETRIEVE`, `ACT`, `CLARIFY`, `UNSUPPORTED`
3. Explicit reason, bounded confidence and required-capability metadata
4. Destructive-action confirmation flag
5. Backward-compatible `DOCUMENTS`/`DELEGATE` route
6. Hindi/English routing and unsupported OCR/legacy-DOC tests
7. Runtime availability model and execution dispositions
8. Capability-unavailable/deferred-feature blocking
9. Typed runtime result envelope and handler boundary
10. Confirmation/clarification/blocked plans never invoke handlers
11. Missing-handler, empty-output and exception conversion to typed failures
12. Deterministic action idempotency keys
13. Duplicate successful/in-progress action blocking
14. Failed-action reservation release for explicit retry
15. Immutable activity record value model
16. Thread-safe in-memory activity and idempotency stores
17. SharedPreferences-backed versioned activity persistence
18. Bounded retention, corrupt-row recovery, clear and export
19. One-time expiring confirmation tokens
20. Exact-action binding and replay protection
21. Runtime, idempotency, persistence and confirmation regression suites

### IN_PROGRESS — Concrete runtime integration

1. Concrete normal-answer adapter
2. Concrete Current-only document-retrieval adapter
3. Confirmed device-action adapter
4. User-visible activity/history screen
5. End-to-end assistant runtime wiring tests

## Track C — Personal memory

1. User-approved memory schema
2. Provenance and timestamps
3. Sensitive-memory exclusions
4. View/edit/delete controls
5. Expiry and correction
6. Retrieval relevance tests
7. Backup/export policy

## Track D — Search and knowledge

1. Provider-neutral search interface
2. Freshness requirements
3. Citations and provenance
4. Query privacy/redaction
5. Offline fallback
6. Ranking and deduplication
7. Connected-source boundaries
8. Search-to-answer tests

## Track E — Actions and automations

1. Typed action requests — IN_PROGRESS
2. Confirmation policy — DONE foundation
3. Permission/capability checks — DONE foundation
4. Persistent transaction/result record — DONE foundation
5. Partial-failure handling — DONE foundation
6. Idempotency and duplicate prevention — DONE foundation
7. Replay-safe confirmation tokens — DONE foundation
8. User-visible history — IN_PROGRESS
9. Calendar/email/reminder adapters — PLANNED

## Track F — Voice intelligence

Voice remains a separate controlled milestone. Do not add speech packages or replace stable voice behavior merely to advance this roadmap.

## Track G — Release, recovery and governance

1. Update blueprint/roadmap/latest snapshot every coding batch
2. Add immutable snapshot at significant milestones
3. Record verified head, CI and artifact digest
4. Never claim physical validation without owner evidence
5. Never merge or mark ready without explicit approval
6. Maintain persisted-data migration/rollback tests
7. Keep permissions/background components auditable

## Current batch pending authoritative CI

- Added versioned, bounded persistent runtime activity history.
- Added Unicode-safe field encoding, corrupt-row skipping, clear and export.
- Added one-time confirmation tokens with expiry, exact-action binding and replay protection.
- Added persistence/retention/corruption/Unicode/expiry/binding/replay tests.
- Updated capability registry and rolling backup.

## Immediate next coding priority

Run full Android CI. After green validation, connect concrete normal-answer and Current-only document-retrieval adapters, then confirmed device-action execution and a user-visible activity screen. PR #12 remains Draft and unmerged.
