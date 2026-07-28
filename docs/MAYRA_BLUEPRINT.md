# Mayra AI — Canonical Product Blueprint

Last updated: 2026-07-28
Status: Living source of truth

## Product vision

Mayra is a private, user-controlled Android AI assistant and future mobile operating intelligence layer. It should understand Hindi/Hinglish/English requests, reason over trusted personal context, work with approved knowledge sources and local documents, understand device state, coordinate supported apps, and perform explicit user-approved actions without silently expanding permissions or background behavior.

## Non-negotiable principles

1. Privacy first and least privilege.
2. Grounded answers distinguish evidence, inference, stale data and unsupported claims.
3. Consequential actions and personal-memory writes require deterministic owner-controlled approval.
4. Conversation text is never silently promoted into memory.
5. Conflicting facts must never silently overwrite.
6. Memory use must be visible through trusted structured metadata, not model-written badge text.
7. Protected-storage failure must not be presented as empty history.
8. Keystore keys are never automatically reset as recovery.
9. Remote conversational providers cannot execute actions or write memory through the answer boundary.
10. Provider cancellation must propagate; timeout or provider failure may use deterministic offline fallback.
11. Provider credentials never live in source control, conversation history or personal memory.
12. Physical-device claims require actual owner/device evidence.

## Architecture

### Interaction layer
- Text and controlled voice interaction.
- Visual action and memory confirmations.
- Searchable/filterable Memory Center with edit, expiry, delete, export and pending-review controls.
- Storage-health card with protected, legacy and unreadable counters.
- Owner-triggered non-destructive migration retry.
- Structured personal-memory provenance chips on Mayra messages.

### Intent and reasoning layer
- Typed query routing, safety policy and permission checks.
- Model-independent memory commands.
- Typed distinction between ordinary reply, device confirmation and memory approval.

### Knowledge and protected-storage layer
- Private local document library and Current-only indexed evidence.
- Approved personal memory with provenance, revision and expiry.
- Bounded pending proposals.
- Per-record AES-GCM envelopes backed by Android Keystore.
- Separate aliases for approved and pending records.
- Backward-compatible legacy migration only after successful protected rewrite.
- Read-only health classification: EMPTY, HEALTHY, MIGRATION_NEEDED and DEGRADED.
- Degraded state is owner-visible and non-destructive.

### Conversational-provider layer
- `MayraConversationalProvider` returns text only.
- Requests are bounded by conversation count.
- Timeout, retry count and retry delay are strictly bounded.
- Temporary failures may retry; permanent failures do not.
- Exhausted or permanent failures use the offline assistant.
- Coroutine cancellation is never swallowed.
- Credentials come from a dedicated runtime credential source.
- A concrete network provider is not production-enabled until secure configuration, eligibility, diagnostics and full CI are complete.

### Reliability layer
- Compile, complete tests, lint, R8, component/permission audits and isolated APK artifacts.
- Migration, rollback and recovery records.
- No destructive automatic recovery from unreadable protected data.

## Current module state

| Module | Current phase |
|---|---|
| Core assistant and routing | Foundation present; expansion planned |
| Document intelligence | Foundation 16/18 implemented |
| Personal memory | Protected storage, diagnostics, owner recovery UI and structured provenance implemented; CI/device validation pending |
| Conversational provider | Audited reliability boundary implemented; concrete adapter pending |
| Search and fresh knowledge | Provider architecture planned |
| Actions and automations | Safety foundation present; expansion planned |
| Voice intelligence | Separate controlled milestone |
| Privacy and release | Cross-cutting; ongoing |

## Milestone completion

A milestone is complete only when implementation and important failure-path tests are committed; compile, lint and relevant minified build audits pass; roadmap and snapshot are current; and device claims come only from actual owner evidence.

## Change-control rule

Every coding batch updates `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture, privacy or scope changes also update this blueprint. Code with stale governance records is not a fully documented batch.
