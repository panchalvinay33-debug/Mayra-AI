# Mayra AI — Canonical Product Blueprint

Last updated: 2026-07-28
Status: Living source of truth

## Product vision

Mayra is a private, user-controlled Android AI assistant and future mobile operating intelligence layer. It should understand natural Hindi/Hinglish/English requests, reason over trusted personal context, search approved sources, work with local documents, understand device state, coordinate supported apps, and perform explicit user-approved actions without silently expanding permissions or background behavior.

## Non-negotiable principles

1. Privacy first: local data stays local unless a user-approved capability explicitly needs a remote service.
2. Grounded answers: Mayra must distinguish verified evidence, inference, stale data, and unsupported claims.
3. Explicit action boundaries: sending, deleting, booking, purchasing, changing settings, replacing memories, or other consequential actions require clear user intent.
4. Least privilege: every Android permission, service, receiver, worker, and integration must be justified and testable.
5. Offline-first where practical: core routing, local library, memory commands, health diagnostics, and safety checks should work without a network.
6. Hindi/Hinglish quality is a first-class product requirement, not a translation afterthought.
7. Every milestone must update the roadmap and backup snapshot before it is treated as complete.
8. Model-independent safety: memory writes and consequential device actions must be controlled by deterministic code and typed receipts, never by a model claim alone.
9. Durable approval: a pending owner decision may survive process death only in bounded local storage, with expiry, replay protection and visible review.
10. Conflict honesty: a new fact that contradicts an approved fact must show both values and must never silently overwrite the existing record.
11. Memory transparency: whenever approved personal memory influences an answer, the owner must receive a visible indication of which memory keys were used.
12. Owner control: approved and pending memories must be searchable, reviewable, editable or rejectable without relying on the conversational model.

## System architecture

### 1. Interaction layer
- Text conversation UI
- Hindi/Hinglish/English understanding
- Voice intelligence remains separately controlled
- Clear action and memory confirmations
- Visual Save/Not now and Replace/Not now review surfaces
- Searchable/filterable Memory Center with direct owner controls
- Visible disclosure when personal memory influenced an answer

### 2. Intent and reasoning layer
- Query routing and intent classification
- Tool/action selection
- Grounding, safety and permission checks
- Confidence-aware fallback
- Memory command interception before conversational model delegation
- Typed distinction between ordinary reply, device confirmation and memory approval

### 3. Knowledge layer
- Private local document library and Current-only indexed evidence
- Approved personal memory with proposal, provenance, revision, expiry and owner controls
- Bounded persistent pending-memory proposals
- Deterministic search and category filtering over approved records
- Approved web/search providers and connected account retrieval
- Source freshness and provenance

### 4. Action layer
- Device-safe actions
- Calendar, email, contacts, reminders, and future integrations
- Transactional execution where partial completion would be unsafe
- Auditable outcome records
- Exact confirmation for high-risk actions and personal-memory persistence

### 5. Reliability layer
- Health diagnostics
- CI compile, tests, lint, minified build and permission/component audits
- Isolated physical-test builds
- Migration, rollback, backup and recovery paths
- Process-death restoration for pending memory approvals
- Stale-conflict rejection when approved state changes before confirmation
- Protected-storage migration must preserve backward-compatible recovery

## Major product modules

| Module | Objective | Current phase |
|---|---|---|
| Core assistant and query routing | Understand requests and route them safely | Foundation present; expansion planned |
| Document intelligence | Private local files, search, summaries, grounded Q&A | Foundation 16/18 implemented |
| Personal memory | Remember useful user-approved context with controls | Durable approval, conflict review, context transparency and owner-management foundation implemented; validation/protected storage pending |
| Search and knowledge tools | Fresh public and connected-source retrieval with citations | Planned/needs provider architecture |
| Actions and automations | Execute explicit tasks safely and report outcomes | Foundation/planned |
| Voice intelligence | Natural Hindi/Hinglish speech interaction | Separate controlled milestone |
| Integrations | Gmail, Calendar, Contacts, GitHub and future connectors | Incremental, permission-scoped |
| Privacy and safety center | Permissions, data controls, evidence provenance, deletion | Cross-cutting; ongoing |
| Release and recovery | Backups, migrations, diagnostics, reproducible builds | CI foundation present; broader release work planned |

## Personal memory boundary

Mayra may store or replace a personal fact only after deterministic privacy classification and explicit owner approval. Conversation text is never silently promoted into memory. Prohibited secrets and sensitive categories are rejected before approval. Pending proposals are bounded, local, expiring and replay-safe; they may survive process death so the owner can finish an interrupted decision. Same-key contradictory values require a visual comparison of current and proposed values. If the existing memory changes before approval, the stale proposal is rejected.

Approved memories carry provenance, revision and optional expiry. Retrieval excludes expired or unapproved records. Chat commands are handled locally before model delegation. Normal answers may receive only approved, active and query-relevant memory as read-only context. The assistant must not treat injected memory as proof beyond the stored value. Answers influenced by memory must visibly disclose the relevant memory keys. Memory Center operations must reapply privacy classification before accepting edited values.

## Document intelligence boundary

The document foundation currently tracks 18 features: 16 implemented, including local library, text/PDF/DOCX extraction, Unicode search, summaries, grounded Q&A, async indexing, safety limits, freshness, current-only evidence, health diagnostics, smart maintenance, and transactional commits. Remaining code milestones are on-device OCR and legacy binary DOC parsing. Physical validation remains required for several completed flows.

## Definition of milestone completion

A milestone is complete only when implementation is committed; regression tests cover success and important failure paths; Android compile, lint and relevant minified build audits pass; roadmap and snapshot are updated; and physical-device claims come only from actual owner/device evidence.

## Change-control rule

Every future coding batch must update `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture or scope changes must also update this blueprint. A batch that changes code but leaves those records stale is not considered fully documented.
