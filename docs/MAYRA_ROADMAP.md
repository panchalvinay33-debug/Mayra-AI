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
| Routing runtime boundary | IN_PROGRESS | Typed results, handler isolation and controlled failures implemented | Idempotency, activity records and real adapters |
| Personal memory | PLANNED | No completion claim | Consent-first schema and controls |
| Search and knowledge providers | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | PLANNED | No completion claim | Typed execution, confirmation and history |
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
2. Registry integrity regression tests
3. Typed outcomes: `ANSWER`, `RETRIEVE`, `ACT`, `CLARIFY`, `UNSUPPORTED`
4. Explicit reason and bounded confidence
5. Required-capability metadata
6. Destructive-action confirmation flag
7. Backward-compatible `DOCUMENTS`/`DELEGATE` route
8. Hindi/English routing and unsupported OCR/legacy-DOC tests
9. Runtime capability availability model
10. Execution dispositions: `EXECUTE`, `CONFIRM`, `CLARIFY`, `FALLBACK`, `BLOCK`
11. Capability-unavailable and deferred-feature blocking
12. Confirmation gate for destructive actions
13. Typed runtime result envelope
14. Provider/action handler boundary
15. Confirmation, clarification and blocked plans never invoke handlers
16. Missing-handler, empty-result and thrown-handler failures become typed failures
17. Runtime dispatch regression suite

### IN_PROGRESS — Runtime integration

1. Idempotency key and duplicate-action prevention
2. User-visible activity/audit record model
3. Concrete normal-answer adapter
4. Concrete document-retrieval adapter
5. Confirmed device-action adapter
6. End-to-end assistant runtime wiring tests

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

1. Typed action requests
2. Confirmation policy
3. Permission/capability checks
4. Transaction/result record
5. Partial-failure handling
6. Idempotency
7. User-visible history
8. Calendar/email/reminder adapters

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

- Removed a conflicting duplicate typed-router implementation after detecting a newer integrated branch version.
- Added `MayraRoutingRuntime` typed dispatch boundary.
- Added `Executed`, `ConfirmationRequired`, `ClarificationRequired`, `Blocked` and `Failed` runtime results.
- Added answer/retrieve/action handler registry.
- Added controlled missing-handler, empty-output and exception handling.
- Added ten runtime safety and failure regression tests.

## Immediate next coding priority

Run full Android CI on the latest documentation head. After green validation, add action idempotency and immutable activity records, then connect concrete assistant/document adapters. PR #12 remains Draft and unmerged.
