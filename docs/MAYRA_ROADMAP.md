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
| Concrete runtime integration | IN_PROGRESS | Confirmed execution and answer/document/action adapters are CI-verified | App composition-root wiring and Activity History UI |
| Personal memory | PLANNED | No completion claim | Consent-first schema and controls |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | IN_PROGRESS | Safe confirmed execution foundation | Real calendar/email/reminder adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | CI audits and local activity history exist | User-facing controls and production release |

## Document intelligence

DONE: local library, TXT/PDF/DOCX extraction, Unicode search/snippets, summaries, grounded Q&A, async parsing limits, freshness, Current-only evidence, Library Health, smart/force maintenance, transactional index commits and isolated zero-permission CI APK.

DEVICE_VERIFY: PDF search, DOCX search, freshness UI and maintenance UX.

DEFERRED: on-device OCR and legacy binary `.doc` parsing.

## Core assistant and runtime

### DONE

1. Typed `ANSWER`, `RETRIEVE`, `ACT`, `CLARIFY`, `UNSUPPORTED` outcomes
2. Explicit reason, confidence, capability and confirmation metadata
3. Runtime capability eligibility and typed handler boundary
4. Deterministic action idempotency and duplicate prevention
5. Persistent bounded Unicode-safe activity history
6. Corrupt-history recovery, clear and export
7. One-time expiring confirmation tokens bound to exact actions
8. Runtime `confirmAndDispatch()` with replay/mismatch blocking
9. Concrete answer-provider adapter
10. Current-only document-retrieval adapter
11. Explicit optional device-action executor adapter
12. Automated routing, persistence, confirmation and adapter tests

### IN_PROGRESS

1. App composition-root wiring
2. User-visible Activity History screen
3. Clear/export controls in UI
4. Confirmed action UX on device
5. End-to-end physical-device validation

## Remaining product tracks

- Personal memory: consent, provenance, sensitive-memory exclusions, edit/delete/expiry and retrieval tests.
- Fresh search: provider interface, freshness, citations, privacy redaction, ranking and fallback.
- Actions: calendar, email, reminders and device integrations.
- Voice: controlled Hindi/Hinglish milestone; do not replace stable voice behavior casually.
- Privacy/release: user-facing controls, migrations, signed release and recovery checklist.

## Governance rules

1. Update blueprint/roadmap/latest snapshot every coding batch.
2. Record verified head, CI and artifact digest.
3. Never claim physical validation without owner evidence.
4. Never merge or mark ready without explicit approval.
5. Keep permissions and background components auditable.

## Latest validation

Android CI #1369 passed on `5ecbefc967ec6fe6f76f9f7ef1527484d53b2cd4`: compile, complete tests, lint, isolated R8 APK, zero-permission/component audit and artifact upload passed.

## Immediate next coding priority

Wire concrete adapters into the application composition root and add the Activity History screen. PR #12 remains Draft and unmerged.
