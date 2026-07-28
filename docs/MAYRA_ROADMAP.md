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
| Personal memory | IN_PROGRESS | Consent-first schema, privacy policy, approval, provenance, expiry, edit/delete, retrieval and Android persistence implemented | User-facing memory controls and chat proposal UX |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe file-picker action plus confirmation/idempotency/chat UX implemented | Physical validation and reviewed calendar/email/reminder adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | CI audits, local history, confirmation UX and memory exclusions | Broader privacy center and production release |

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

## Personal memory

### IMPLEMENTED FOUNDATION

1. Memory candidate, persisted-memory and provenance models
2. Explicit proposal and one-time approval flow; proposing never writes to storage
3. Rejection flow and replay-safe proposal handling
4. Prohibited-memory exclusions for passwords, PIN/OTP, cards/CVV, Aadhaar and cryptographic recovery secrets
5. Sensitive-memory exclusions for health, religion/caste/politics/sexual orientation and salary/bank-account data
6. Source type, source reference and capture timestamp provenance
7. Optional expiry with automatic pruning
8. User-controlled update, delete and clear operations
9. Revision counter and stable identity for corrected same-key memories
10. Deterministic key/value retrieval with relevance scoring and stable tie-breaking
11. Bounded Android SharedPreferences persistence with versioned codec
12. Unicode/Hindi/Hinglish-safe Base64 fields
13. Corrupt-record skipping, retention limit and readable local export
14. Pure JVM and Robolectric privacy/persistence regressions

### REMAINING

1. User-facing Memory Center with view/edit/delete/clear/export
2. Chat proposal parser and explicit Save/Not now dialog
3. User-visible source/provenance and expiry controls
4. Integration of relevant memories into answer context with transparent evidence
5. Process-death persistence for pending approval proposals
6. Owner-device validation

## Remaining product tracks

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

Android CI #1445 passed on `d4286a237a4be8d2f66577e2522ad8fa1fb52080`: compile, complete tests, Android lint, isolated R8 APK, zero-permission/component audit and artifact uploads passed.

The first memory run #1443 exposed category-label relevance inflation in one retrieval regression. Retrieval was corrected to score only user-authored key/value evidence, and the full pipeline passed on #1445.

Artifacts:

- `mayra-document-test-apk-1445` — `sha256:ad83a12d5f712ed7d216b7ff0ebe971dcf95ba3d3bbdd61c52390e585ef624e5`
- `android-reports-1445` — `sha256:afaf373e6b3e3f831fdff516bb3615f460eb67b1d6d9ef480ecc5094b503320c`

## Immediate next coding priority

Build the user-facing Memory Center and chat proposal/approval UX, then inject only approved, active and relevant memories into assistant context with visible provenance. PR #12 remains Draft and unmerged.
