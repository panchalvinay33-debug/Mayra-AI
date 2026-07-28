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
| Personal memory | IN_PROGRESS | Consent-first core, Android persistence and Memory Center implemented | Chat Save/Not now proposal UX and transparent context injection |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe file-picker action plus confirmation/idempotency/chat UX implemented | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | CI audits, local history, confirmation UX, memory exclusions and Memory Center | Broader privacy center and production release |

## Document intelligence

DONE: local library, TXT/PDF/DOCX extraction, Unicode search/snippets, summaries, grounded Q&A, async limits, freshness, Current-only evidence, Library Health, smart/force maintenance, transactional commits and isolated zero-permission CI APK.

DEVICE_VERIFY: PDF search, DOCX search, freshness UI and maintenance UX.

DEFERRED: on-device OCR and legacy binary `.doc` parsing.

## Core assistant and runtime

DONE: typed outcomes, capability gates, idempotency, persistent activity history, confirmation tokens, answer/document/action adapters, Android composition, safe system picker, Activity History, main-chat typed bridge and Confirm/Cancel UX.

DEVICE_VERIFY: Activity History, file picker, Confirm/Cancel, rotation retention and end-to-end phone flow. Process-death persistence for pending confirmations remains unimplemented.

## Personal memory

### IMPLEMENTED

1. Candidate, approved-memory and provenance models
2. Explicit proposal and one-time approval; proposal creation never writes storage
3. Rejection and replay-safe proposal handling
4. Prohibited-secret and sensitive-personal-data exclusions
5. Source type/reference/capture timestamp provenance
6. Optional expiry and automatic pruning
7. Update, delete, clear, revision and stable identity
8. Deterministic key/value-only retrieval
9. Bounded versioned Android persistence
10. Unicode-safe codec, corruption recovery and readable export
11. Application-level persistent memory service installation
12. User-visible Memory Center
13. View key/value/category/source/revision/update time/expiry
14. Per-memory delete confirmation
15. Clear-all confirmation and Android share-sheet export
16. Direct launcher entry for current physical validation
17. Privacy, persistence and retrieval regressions

### REMAINING

1. Chat parser for explicit memory requests
2. `Save this memory?` / `Save` / `Not now` dialog
3. Edit-value and expiry controls in Memory Center
4. Transparent injection of approved relevant memories into answer context
5. Process-death persistence for pending memory proposals
6. Owner-device validation

## Latest authoritative validation

Android CI #1458 passed on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`: compile, complete tests, Android lint, isolated R8 APK, zero-permission/component audit and artifact uploads passed.

The first visible-memory run #1457 caught an incorrect Compose `setContent` import. The import was corrected and the entire pipeline passed on #1458.

Artifacts:

- `mayra-document-test-apk-1458` — `sha256:75751ce1848b73618adfcb10627420cb88756c0dd2125d164451d07d37d4b869`
- `android-reports-1458` — `sha256:931d86bcc5bfdf9f8037ba7e90c8c21d1ae15783f9e5f4b67ab1e327c69a7a69`

## Governance rules

1. Update blueprint/roadmap/latest snapshot every coding batch.
2. Record verified head, CI and artifact digest.
3. Never claim physical validation without owner evidence.
4. Never merge or mark ready without explicit approval.
5. Keep permissions and background components auditable.

## Immediate next coding priority

Add chat memory-proposal detection and explicit Save/Not now UX, then use only approved active relevant memories as transparent answer context. PR #12 remains Draft and unmerged.
