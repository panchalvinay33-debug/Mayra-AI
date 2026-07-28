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
| Personal memory | IN_PROGRESS | Consent-first core, persistence, Memory Center and deterministic chat commands implemented | Full CI, visual Save/Not now UX, process-death safety and context injection |
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
18. Model-independent chat routing for remember/confirm/cancel/forget/list commands
19. Hindi/Hinglish/English command aliases for core memory operations
20. Explicit text confirmation remains mandatory before persistence
21. Deterministic tests for save, cancellation, prohibited secrets, listing and forgetting

### REMAINING

1. Visual `Save this memory?` / `Save` / `Not now` dialog using the typed confirmation surface
2. Edit-value and expiry controls in Memory Center
3. Transparent injection of approved relevant memories into answer context
4. Process-death persistence for pending memory proposals
5. Contradiction/conflict review before replacing an existing approved fact
6. Encrypted or Keystore-backed protected memory-at-rest migration
7. Owner-device validation

## Validation truth

Latest fully green authoritative validation remains Android CI #1458 on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`.

The current memory-chat implementation head is `589d13ac731d5053be3668a4e60991550907ce05`. Full compile, complete unit tests, lint, R8 and artifact audit are pending for this newer head, so it is `IN_PROGRESS`, not `DONE`.

## Governance rules

1. Update blueprint/roadmap/latest snapshot every coding batch.
2. Record verified head, CI and artifact digest.
3. Never claim physical validation without owner evidence.
4. Never merge or mark ready without explicit approval.
5. Keep permissions and background components auditable.

## Immediate next coding priority

Run the full governed CI chain for memory chat commands, fix any regression, then add visual Save/Not now confirmation, persistent pending proposals and approved-memory context injection. PR #12 remains Draft and unmerged.
