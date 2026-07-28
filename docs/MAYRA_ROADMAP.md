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
| Blueprint and backup discipline | DONE | Canonical blueprint, roadmap and rolling snapshots | Keep updated every batch |
| Document intelligence | DEVICE_VERIFY | 16/18 implemented | Phone verification; OCR and legacy DOC deferred |
| Core routing and eligibility | DONE | Typed outcomes and capability gates | Keep regressions green |
| Audited runtime safety | DONE | Typed results, persistence, confirmation and idempotency | Physical/device validation |
| Concrete runtime integration | IN_PROGRESS | Answer/document/action adapters and confirmed execution exist | Composition-root wiring and Activity History UI |
| Personal memory | PLANNED | No completion claim | Consent-first schema and controls |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | IN_PROGRESS | Safe confirmed execution foundation | Real calendar/email/reminder adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | CI audits and local activity history exist | User-facing privacy controls and production release |

## Document intelligence

### DONE

1. Persistable local document metadata
2. Plain-text, text-based PDF and DOCX extraction
3. Unicode Hindi/Hinglish/English search and snippets
4. Local summaries and grounded Q&A
5. Async indexing and parser safety limits
6. Index freshness and parser versioning
7. Current-only evidence policy
8. Library Health inventory
9. Smart refresh and force rebuild
10. Transactional content/fingerprint commit and rollback
11. Isolated zero-permission APK and CI audit

### DEVICE_VERIFY

- PDF re-index and content search
- DOCX add/index/search
- freshness badge and changed-file behavior
- Smart refresh and transactional maintenance UX

### DEFERRED

- On-device OCR for scanned PDFs/images
- Legacy binary `.doc` parsing

## Core assistant and runtime

### DONE

1. Typed `ANSWER`, `RETRIEVE`, `ACT`, `CLARIFY`, `UNSUPPORTED` outcomes
2. Explicit reason, confidence, capability and confirmation metadata
3. Runtime capability eligibility and execution dispositions
4. Typed handler boundary and failure conversion
5. Deterministic action idempotency and duplicate prevention
6. Persistent bounded activity history with Unicode-safe codec
7. Corrupt-history recovery, clear and export
8. One-time expiring confirmation tokens bound to exact actions
9. Runtime `confirmAndDispatch()` with replay/mismatch blocking
10. Concrete answer-provider adapter
11. Current-only document-retrieval adapter
12. Explicit optional device-action executor adapter
13. Automated routing, persistence, confirmation and adapter tests

### IN_PROGRESS

1. App composition-root wiring
2. User-visible Activity History screen
3. Clear/export controls in UI
4. Confirmed action UX
5. End-to-end physical-device validation

## Personal memory — PLANNED

1. User-approved memory schema
2. Provenance and timestamps
3. Sensitive-memory exclusions
4. View/edit/delete and expiry controls
5. Retrieval relevance and correction tests
6. Backup/export policy

## Search and knowledge — PLANNED

1. Provider-neutral search interface
2. Freshness and citation contracts
3. Query privacy/redaction
4. Offline fallback
5. Ranking, deduplication and search-to-answer tests

## Actions and automations

- Typed requests, confirmation, capability checks, idempotency, persistent audit and partial-failure handling: DONE foundation
- User-visible history and confirmed action UX: IN_PROGRESS
- Calendar, email, reminders and app/device adapters: PLANNED

## Voice intelligence

Voice remains a separate controlled milestone. Do not add speech packages or replace stable voice behavior merely to advance this roadmap.

## Governance rules

1. Update blueprint/roadmap/latest snapshot every coding batch.
2. Record verified head, CI and artifact digest.
3. Never claim physical validation without owner evidence.
4. Never merge or mark ready without explicit approval.
5. Keep permissions and background components auditable.

## Current batch pending authoritative CI

- Integrated one-time confirmation tokens into runtime execution.
- Added replay-safe `confirmAndDispatch()` and exact-action enforcement.
- Added concrete answer, Current-only document and optional action adapters.
- Added confirmation execution/replay/mismatch and adapter regression tests.
- Updated rolling backup and this roadmap.

## Immediate next coding priority

Run full Android CI. After green validation, wire adapters into the application composition root and add a user-visible Activity History screen with clear/export controls. PR #12 remains Draft and unmerged.
