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
| Concrete runtime integration | DEVICE_VERIFY | Android composition root is installed in the full app; Activity History is reachable from chat | Confirmation dialog and typed chat bridge |
| Personal memory | PLANNED | No completion claim | Consent-first schema and controls |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | IN_PROGRESS | Safe file-picker action plus confirmation/idempotency foundation | Calendar/email/reminder adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | CI audits, local history, clear and export UI | Broader privacy center and production release |

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
12. Android runtime composition root
13. Full-app `MayraApplication` installation of the typed runtime
14. Limited permission-free system file-picker action executor
15. User-visible Activity History screen
16. Activity clear and Android share-sheet export controls
17. Main-chat History navigation
18. Isolated APK manifest audit for the history component
19. Automated routing, persistence, confirmation, adapter and composition tests

### DEVICE_VERIFY / IN_PROGRESS

1. Activity History screen on a physical phone
2. System file-picker action on a physical phone
3. Confirmation dialog/token UX in the main chat surface
4. Non-blocking typed chat bridge while preserving the existing suspend assistant and voice path
5. End-to-end physical-device validation

## Remaining product tracks

- Personal memory: consent, provenance, sensitive-memory exclusions, edit/delete/expiry and retrieval tests.
- Fresh search: provider interface, freshness, citations, privacy redaction, ranking and fallback.
- Actions: calendar, email, reminders and reviewed device integrations.
- Voice: controlled Hindi/Hinglish milestone; do not replace stable voice behavior casually.
- Privacy/release: broader user-facing controls, migrations, signed release and recovery checklist.

## Governance rules

1. Update blueprint/roadmap/latest snapshot every coding batch.
2. Record verified head, CI and artifact digest.
3. Never claim physical validation without owner evidence.
4. Never merge or mark ready without explicit approval.
5. Keep permissions and background components auditable.

## Current batch pending authoritative validation

- Confirmed Android CI #1407 was fully green for the previous visible-runtime head.
- Installed `MayraAndroidRuntimeComposition` in the full application container.
- Reused the existing deterministic local command engine as the synchronous typed answer provider.
- Preserved the existing suspend assistant, document wrapper and voice behavior.
- Added a History chip to the main chat header.
- Kept the first device action limited to Android's permission-free system file picker.

## Immediate next coding priority

Run full CI on the latest governed head. After green validation, add confirmation-dialog state and a non-blocking typed runtime bridge to the main chat. PR #12 remains Draft and unmerged.
