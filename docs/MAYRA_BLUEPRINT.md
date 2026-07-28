# Mayra AI — Canonical Product Blueprint

Last updated: 2026-07-28
Status: Living source of truth

## Product vision

Mayra is a private, user-controlled Android AI assistant and future mobile operating intelligence layer. It should understand Hindi/Hinglish/English requests, reason over trusted personal context, search approved sources, work with local documents, understand device state, coordinate supported apps, and perform explicit user-approved actions without silently expanding permissions or background behavior.

## Non-negotiable principles

1. Privacy first and least privilege.
2. Grounded answers must distinguish evidence, inference, stale data and unsupported claims.
3. Consequential actions and personal-memory writes require deterministic owner-controlled approval.
4. Conversation text is never silently promoted into personal memory.
5. Conflicting facts must show current and proposed values and must never silently overwrite.
6. Memory use must be visible to the owner.
7. Pending decisions may survive process death only in bounded, expiring and replay-safe storage.
8. Approved and pending personal-memory records must be protected at rest when platform capability is available.
9. Storage migration must be backward-compatible and failure-safe; inability to protect a new record must not destroy the previous recoverable set.
10. Physical-device claims require actual owner/device evidence.

## Architecture

### Interaction layer
- Text and controlled voice interaction.
- Visual action confirmation and Save/Replace/Not now memory review.
- Searchable/filterable Memory Center with edit, expiry, delete, clear, export and pending-review controls.
- Visible disclosure when approved personal memory influences an answer.

### Intent and reasoning layer
- Typed query routing, intent classification, safety policy and permission checks.
- Model-independent interception of owner memory commands.
- Typed distinction between ordinary reply, device confirmation and memory approval.

### Knowledge layer
- Private local document library and Current-only indexed evidence.
- Approved personal memory with provenance, revision and optional expiry.
- Bounded persistent pending proposals.
- Approved web/search providers and connected retrieval as separate future capabilities.

### Protected storage layer
- Approved memory records use per-record AES-GCM envelopes backed by an Android Keystore AES key.
- Pending proposals use a separate Keystore alias to isolate their protection lifecycle.
- Every encrypted record contains a versioned envelope, random GCM IV and authenticated ciphertext.
- Legacy plaintext codec records remain readable and are rewritten into protected form after successful decode.
- Encryption is completed before replacing the stored record set, so protection failure preserves the previous data.
- Corrupt, tampered, undecryptable or unsupported records are skipped without crashing application startup.
- Keystore invalidation must be surfaced by future storage-health diagnostics; it must never be misreported as an empty user history without explanation.

### Reliability layer
- Compile, unit tests, lint, R8, permission/component audits and isolated APK artifacts.
- Migration, rollback, backup and recovery records.
- Process-death restoration, stale-conflict rejection and bounded resource use.

## Personal memory boundary

Mayra may store, replace or change expiry for a personal fact only after deterministic privacy classification and an explicit owner action. Approved retrieval excludes expired and unapproved records. Normal answers may receive only approved, active and query-relevant memory as read-only context, and must disclose the memory keys used.

Readable export remains an explicit owner-triggered share-sheet action and is not equivalent to protected backup. A future protected export must define key portability, authentication and recovery separately.

## Major modules

| Module | Current phase |
|---|---|
| Core assistant and query routing | Foundation present; expansion planned |
| Document intelligence | Foundation 16/18 implemented |
| Personal memory | Durable approval, owner controls, expiry and protected migration implemented; CI/device validation pending |
| Search and fresh knowledge | Provider architecture planned |
| Actions and automations | Safety foundation present; expansion planned |
| Voice intelligence | Separate controlled milestone |
| Privacy and safety center | Cross-cutting; ongoing |
| Release and recovery | CI foundation present; broader release work planned |

## Milestone completion

A milestone is complete only when implementation and important failure-path tests are committed; compile, lint and relevant minified build audits pass; roadmap and snapshot are current; and device claims come only from actual owner evidence.

## Change-control rule

Every coding batch updates `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture, privacy or scope changes also update this blueprint. Code with stale governance records is not a fully documented batch.
