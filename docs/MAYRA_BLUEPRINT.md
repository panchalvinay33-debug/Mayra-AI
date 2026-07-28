# Mayra AI — Canonical Product Blueprint

Last updated: 2026-07-28
Status: Living source of truth

## Product vision

Mayra is a private, user-controlled Android AI assistant and future mobile operating intelligence layer. It should understand natural Hindi/Hinglish/English requests, reason over trusted personal context, search approved sources, work with local documents, understand device state, coordinate supported apps, and perform explicit user-approved actions without silently expanding permissions or background behavior.

## Non-negotiable principles

1. Privacy first: local data stays local unless a user-approved capability explicitly needs a remote service.
2. Grounded answers: Mayra must distinguish verified evidence, inference, stale data, and unsupported claims.
3. Explicit action boundaries: sending, deleting, booking, purchasing, changing settings, or other consequential actions require clear user intent.
4. Least privilege: every Android permission, service, receiver, worker, and integration must be justified and testable.
5. Offline-first where practical: core routing, local library, memory commands, health diagnostics, and safety checks should work without a network.
6. Hindi/Hinglish quality is a first-class product requirement, not a translation afterthought.
7. Every milestone must update the roadmap and backup snapshot before it is treated as complete.
8. Model-independent safety: memory writes and consequential device actions must be controlled by deterministic code and typed receipts, never by a model claim alone.

## System architecture

### 1. Interaction layer
- Text conversation UI
- Hindi/Hinglish/English understanding
- Voice intelligence milestone remains separately controlled and must not replace stable voice code without validation
- Clear action confirmations and result/error states
- Deterministic owner commands for memory, safety and recovery paths

### 2. Intent and reasoning layer
- Query routing
- Intent classification
- Tool/action selection
- Grounding policy
- Safety and permission checks
- Confidence-aware fallback
- Memory command interception before conversational model delegation

### 3. Knowledge layer
- Private local document library
- Current-only indexed evidence
- Personal memory with explicit proposal, approval, provenance, expiry and owner controls
- Approved web/search providers
- Connected account retrieval
- Source freshness and provenance

### 4. Action layer
- Device-safe actions
- Calendar, email, contacts, reminders, and future integrations
- Transactional execution where partial completion would be unsafe
- Auditable outcome records
- Exact confirmation for high-risk actions and personal-memory persistence

### 5. Reliability layer
- Health diagnostics
- CI compile, tests, lint, minified build, permission/component audits
- Isolated physical-test builds
- Migration, rollback, backup, and recovery paths
- Process-death persistence for pending approvals before Personal Alpha completion

## Major product modules

| Module | Objective | Current phase |
|---|---|---|
| Core assistant and query routing | Understand requests and route them safely | Foundation present; expansion planned |
| Document intelligence | Private local files, search, summaries, grounded Q&A | Foundation 16/18 implemented |
| Personal memory | Remember useful user-approved context with controls | Core, persistence, Memory Center and deterministic chat commands implemented; advanced UX/recovery pending |
| Search and knowledge tools | Fresh public and connected-source retrieval with citations | Planned/needs provider architecture |
| Actions and automations | Execute explicit tasks safely and report outcomes | Foundation/planned |
| Voice intelligence | Natural Hindi/Hinglish speech interaction | Separate controlled milestone |
| Integrations | Gmail, Calendar, Contacts, GitHub and future connectors | Incremental, permission-scoped |
| Privacy and safety center | Permissions, data controls, evidence provenance, deletion | Cross-cutting; ongoing |
| Release and recovery | Backups, migrations, diagnostics, reproducible builds | CI foundation present; broader release work planned |

## Personal memory boundary

Mayra may store a personal fact only after deterministic privacy classification and explicit owner confirmation. Conversation text is never silently promoted into memory. Prohibited secrets and sensitive categories are rejected before approval. Approved memories carry provenance, revision and optional expiry; retrieval excludes expired or unapproved records. Chat commands for remember, confirm, cancel, forget and list are handled locally before model delegation. Future model context injection may use only approved active relevant records and must make that use transparent to the owner.

## Document intelligence boundary

The document foundation currently tracks 18 features: 16 implemented, including local library, text/PDF/DOCX extraction, Unicode search, summaries, grounded Q&A, async indexing, safety limits, freshness, current-only evidence, health diagnostics, smart maintenance, and transactional commits. Remaining code milestones are on-device OCR and legacy binary DOC parsing. Physical validation remains required for several completed flows.

## Definition of milestone completion

A milestone is complete only when:

- implementation is committed;
- regression tests cover success and important failure paths;
- Android compile and lint pass;
- relevant minified APK/build audit passes;
- roadmap status is updated;
- a dated backup snapshot records head SHA, CI run, artifacts, completed work, remaining risks, and next step;
- physical-device claims are made only from actual user/device evidence.

## Change-control rule

Every future coding batch must update `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture or scope changes must also update this blueprint. A batch that changes code but leaves those records stale is not considered fully documented.
