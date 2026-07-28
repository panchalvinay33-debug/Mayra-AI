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
| Concrete runtime integration | DEVICE_VERIFY | Full-app composition, Activity History and main-chat Confirm/Cancel UX are CI-verified | Physical-device validation |
| Personal memory | PLANNED | No completion claim | Consent-first schema, provenance and controls |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | IN_PROGRESS | Safe file-picker action plus confirmation/idempotency foundation | Calendar/email/reminder adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | CI audits, local history, clear/export and confirmation UX | Broader privacy center and production release |

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
18. Non-blocking typed chat bridge that preserves normal suspend-assistant answers
19. Main-chat exact-action Confirm/Cancel dialog
20. Pending confirmation retained in ViewModel state across configuration changes
21. Typed retrieval/action/blocked/failure results added to the chat conversation
22. Input, voice and duplicate sends locked while confirmation is pending
23. Automated bridge, replay, adapter, composition and runtime tests
24. Isolated APK zero-permission/component audit

### DEVICE VERIFY / REMAINING

1. Activity History screen on a physical phone
2. System file-picker action on a physical phone
3. Confirm/Cancel UX and action-result chat display on a physical phone
4. Rotation/configuration-change confirmation retention on a physical phone
5. Process-death persistence for pending confirmations is not implemented
6. End-to-end physical-device validation

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

## Latest authoritative validation

Android CI #1427 passed on `e9ab540a9b81e8d846a63d81890c839e6305b473`: compile, complete tests, Android lint, isolated R8 APK, zero-permission/component audit and artifact uploads passed.

Artifacts:

- `mayra-document-test-apk-1427` — `sha256:794aa8a8bf2fb75ef27a7c7a8181af1d7de97773ac21ff9885cf34e5b05f9138`
- `android-reports-1427` — `sha256:255a06851978858b322d58760189b620e61e897f80d080cebf87f541a8edcec2`

## Immediate next coding priority

Begin the consent-first personal-memory foundation: explicit user approval, provenance, sensitive-memory exclusions, edit/delete/expiry and deterministic retrieval tests. PR #12 remains Draft and unmerged.
