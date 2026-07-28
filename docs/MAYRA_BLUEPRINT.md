# Mayra AI — Canonical Product Blueprint

Last updated: 2026-07-28
Status: Living source of truth

## Product vision

Mayra is a private, user-controlled Android AI assistant and future mobile operating intelligence layer. It should understand Hindi/Hinglish/English requests, reason over trusted personal context, search approved sources, work with local documents, understand device state, coordinate supported apps, and perform explicit user-approved actions without silently expanding permissions or background behavior.

## Non-negotiable principles

1. Privacy first and least privilege.
2. Grounded answers distinguish evidence, inference, stale data and unsupported claims.
3. Consequential actions and personal-memory writes require deterministic owner-controlled approval.
4. Conversation text is never silently promoted into personal memory.
5. Conflicting facts show current and proposed values and never silently overwrite.
6. Memory use is visible to the owner through structured message provenance.
7. Pending decisions survive process death only in bounded, expiring and replay-safe storage.
8. Approved and pending personal-memory records are protected at rest when platform capability is available.
9. Storage migration is backward-compatible and failure-safe.
10. Unreadable or invalid-key records are never silently treated as an empty history or automatically erased.
11. Physical-device claims require actual owner/device evidence.

## Architecture

### Interaction layer
- Text and controlled voice interaction.
- Visual action confirmation and Save/Replace/Not now memory review.
- Searchable/filterable Memory Center with edit, expiry, delete, clear, export and pending-review controls.
- Structured personal-memory provenance attached to Mayra messages for dedicated UI rendering.

### Intent and reasoning layer
- Typed query routing, intent classification, safety policy and permission checks.
- Model-independent interception of owner memory commands.
- Typed distinction between ordinary reply, device confirmation and memory approval.

### Knowledge layer
- Private local document library and Current-only indexed evidence.
- Approved personal memory with provenance, revision and optional expiry.
- Bounded persistent pending proposals.
- Approved web/search providers and connected retrieval as separate future capabilities.

### Protected storage and health layer
- Approved and pending records use separate Android Keystore-backed AES-GCM protection aliases.
- Versioned encrypted envelopes contain random GCM IV and authenticated ciphertext.
- Legacy plaintext records remain readable and migrate only after successful protection.
- Encryption failure preserves the previous record set.
- Read-only health diagnostics classify storage as `EMPTY`, `HEALTHY`, `MIGRATION_NEEDED` or `DEGRADED`.
- Diagnostics count protected, legacy and unreadable approved/pending records without deleting or resetting anything.
- Key invalidation recovery must require an explicit owner-visible decision; automatic destructive reset is prohibited.

### Answer provenance layer
- The memory-aware assistant emits machine-readable metadata separately from visible answer content.
- Memory keys are URL-safe Base64 encoded to preserve Unicode and prevent delimiter ambiguity.
- Chat parsing removes valid metadata from visible text and attaches decoded keys to `MayraMessage.usedPersonalMemoryKeys`.
- Malformed metadata never becomes trusted provenance.
- Compose may render chips from structured keys; model-generated plain text cannot impersonate trusted provenance unless it passes the internal marker protocol.

### Reliability layer
- Compile, unit tests, lint, R8, permission/component audits and isolated APK artifacts.
- Migration, rollback, backup and recovery records.
- Process-death restoration, stale-conflict rejection and bounded resource use.

## Personal memory boundary

Mayra may store, replace or change expiry for a personal fact only after deterministic privacy classification and an explicit owner action. Approved retrieval excludes expired and unapproved records. Normal answers receive only approved, active and query-relevant memory as read-only context. Provenance is carried as structured message metadata rather than model-visible disclosure text.

Readable export remains an explicit owner-triggered share-sheet action and is not equivalent to protected backup. A future protected export must define key portability, authentication and recovery separately.

## Major modules

| Module | Current phase |
|---|---|
| Core assistant and query routing | Foundation present; expansion planned |
| Document intelligence | Foundation 16/18 implemented |
| Personal memory | Durable approval, protected migration, diagnostics and structured provenance foundation implemented; CI/device/UI validation pending |
| Search and fresh knowledge | Provider architecture planned |
| Actions and automations | Safety foundation present; expansion planned |
| Voice intelligence | Separate controlled milestone |
| Privacy and safety center | Cross-cutting; ongoing |
| Release and recovery | CI foundation present; broader release work planned |

## Milestone completion

A milestone is complete only when implementation and important failure-path tests are committed; compile, lint and relevant minified build audits pass; roadmap and snapshot are current; and device claims come only from actual owner evidence.

## Change-control rule

Every coding batch updates `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture, privacy or scope changes also update this blueprint. Code with stale governance records is not a fully documented batch.
