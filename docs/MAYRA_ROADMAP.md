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
| Concrete runtime integration | DEVICE_VERIFY | Full-app composition, Activity History and main-chat action confirmation are CI-verified | Physical-device validation |
| Personal memory | IN_PROGRESS | Consent core, persistent approvals, visual review, conflicts and answer context implemented | Full CI, edit/expiry UX, protected storage and phone validation |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe picker plus confirmation/idempotency/chat UX implemented | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | Local history, confirmation UX, memory exclusions and owner controls | Privacy center and production release |

## Document intelligence

DONE: local library, TXT/PDF/DOCX extraction, Unicode search/snippets, summaries, grounded Q&A, async limits, freshness, Current-only evidence, Library Health, smart/force maintenance, transactional commits and isolated zero-permission CI APK.

DEVICE_VERIFY: PDF search, DOCX search, freshness UI and maintenance UX.

DEFERRED: on-device OCR and legacy binary `.doc` parsing.

## Core assistant and runtime

DONE: typed outcomes, capability gates, idempotency, persistent activity history, confirmation tokens, answer/document/action adapters, Android composition, safe system picker, Activity History, main-chat typed bridge and action Confirm/Cancel UX.

DEVICE_VERIFY: Activity History, file picker, action confirmation, rotation retention and end-to-end phone flow. Process-death persistence for non-memory action confirmations remains unimplemented.

## Personal memory

### IMPLEMENTED

1. Candidate, approved-memory, provenance, revision and expiry models
2. Explicit proposal and one-time approval; proposal creation never writes approved storage
3. Prohibited-secret and sensitive-personal-data exclusions
4. Bounded versioned Android persistence for approved memories
5. Memory Center view, delete, clear and export controls
6. Deterministic Hindi/Hinglish/English remember, confirm, cancel, forget and list routing
7. Bounded persistent pending-proposal store with corruption recovery
8. Process-death restoration of the latest pending memory approval
9. Visual `Save this memory?` / `Save` / `Not now` dialog
10. Same-key contradiction review showing current and proposed values
11. Stale-conflict rejection if the existing fact changes before approval
12. Read-only injection of approved, active, query-relevant memories into normal answer context
13. No new Android permission, service, receiver or background component
14. Regression coverage for persistence, replay safety, conflicts, restoration and approved-only context

### REMAINING

1. Edit-value and expiry controls in Memory Center
2. Search and category filters in Memory Center
3. Encrypted or Keystore-backed protected memory-at-rest migration
4. Multiple-pending-proposal management rather than restoring only the newest proposal
5. Owner-visible indication when an answer used personal memory
6. Owner-device validation of save/replace/restart/context flows

## Validation truth

Latest fully green authoritative validation remains Android CI #1458 on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`.

Android CI #1505 started for source head `d098930a60b39f4e47333df4deaae37edf24bd94` and was still in progress when this record was written. The latest memory work remains `IN_PROGRESS`, not `DONE`, until a full compile, complete unit tests, lint, R8 and component audit pass on the governed head or a later equivalent head.

## Governance rules

1. Update blueprint/roadmap/latest snapshot every coding batch.
2. Record verified head, CI and artifact digest.
3. Never claim physical validation without owner evidence.
4. Never merge or mark ready without explicit approval.
5. Keep permissions and background components auditable.

## Immediate next coding priority

Complete the full CI chain and fix any regression. Then add Memory Center edit/expiry/search controls and protected at-rest storage migration while preserving backward-compatible recovery. PR #12 remains Draft and unmerged.
