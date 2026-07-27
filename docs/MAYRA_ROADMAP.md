# Mayra AI — Execution Roadmap

Last updated: 2026-07-27
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
| Product blueprint and backup discipline | DONE | Canonical blueprint, roadmap, latest snapshot and update policy added | Keep updated every batch |
| Document intelligence foundation | DEVICE_VERIFY | 16/18 implemented (88%) | Phone verification; OCR and legacy DOC deferred to separate milestones |
| Core routing and capability registry | IN_PROGRESS | Existing router foundation plus global status registry | Expand deterministic routing/tool selection |
| Personal memory | PLANNED | No completion claim | Data model, consent, edit/delete, freshness |
| Search and knowledge providers | PLANNED | No completion claim | Provider interface, citations, freshness, failure policy |
| Actions and automations | PLANNED | Existing foundations require audited scope | Confirmation, transaction log, rollback/result model |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation plan and device validation |
| Privacy/safety center | IN_PROGRESS | Least-privilege CI and isolated APK audits exist | User-facing controls and audit history |
| Release/recovery | IN_PROGRESS | Android CI and isolated test APKs exist | Versioning, migration tests, reproducible release checklist |

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

- PDF re-index and content search on the owner’s phone
- DOCX add/index/search
- freshness badge and changed-file behavior
- Smart refresh and transactional maintenance UX

### DEFERRED TO SEPARATE MILESTONES

17. On-device OCR for scanned PDFs/images
18. Legacy binary `.doc` parser

Document foundation should not block progress on broader Mayra modules. Only device-found bugs should return to this PR unless scope is explicitly reopened.

## Track B — Core assistant and routing

### Existing foundation

- Intent engine and query router files exist
- Document-aware assistant integration exists
- Deterministic local tests exist for current routes

### Next implementation sequence

1. Global capability registry and module status snapshot
2. Typed routing outcome: answer, retrieve, act, clarify, unsupported
3. Provider/tool eligibility rules
4. Confidence and fallback reasons
5. Hindi/Hinglish routing regression corpus
6. Action confirmation boundary
7. End-to-end assistant runtime tests

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
3. Source citations and provenance
4. Query privacy/redaction
5. Offline/no-provider fallback
6. Result ranking and deduplication
7. Connected-source boundaries
8. Search-to-answer regression tests

## Track E — Actions and automations

1. Typed action requests
2. Confirmation policy
3. Permission and capability checks
4. Transaction/result record
5. Partial-failure handling
6. Idempotency and duplicate prevention
7. User-visible activity history
8. Calendar/email/reminder adapters

## Track F — Voice intelligence

This remains a separate controlled milestone. Do not add speech packages or replace stable voice behavior merely to advance the roadmap.

1. Hindi/Hinglish utterance corpus
2. Wake/listen state model
3. Speech-to-text provider boundary
4. Text-to-speech provider boundary
5. Interruption/cancellation behavior
6. Device and privacy evaluation

## Track G — Release, recovery and governance

1. Keep blueprint, roadmap and latest snapshot updated every coding batch
2. Add dated immutable snapshots at significant milestones
3. Record verified head SHA, CI run and artifact digest
4. Never mark physical tests passed without owner/device evidence
5. Never merge or mark PR ready without explicit approval
6. Maintain rollback/migration tests for persisted data
7. Keep permissions and background components auditable

## Immediate next coding priority

Move beyond documents into **Core assistant routing and capability governance** while retaining PR #12 as Draft. The first step is a machine-testable global capability registry and roadmap consistency tests. After that, typed routing outcomes should be implemented in a separate focused milestone/PR when branch strategy is approved.