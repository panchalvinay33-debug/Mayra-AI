# Mayra AI — Canonical Product Blueprint

Last updated: 2026-07-28
Status: Living source of truth

## Product vision

Mayra is a private, user-controlled Android AI assistant. It should understand Hindi/Hinglish/English requests, reason over trusted personal context, work with approved knowledge sources and local documents, understand device state, coordinate supported apps, and perform explicit user-approved actions without silently expanding permissions or background behavior.

## Non-negotiable principles

1. Privacy first and least privilege.
2. Consequential actions and personal-memory writes remain deterministic and owner-approved.
3. Memory use is visible through trusted structured metadata.
4. Protected-storage failure is never presented as empty history and keys are never automatically reset.
5. Remote conversational providers return text only and cannot execute actions or write memory.
6. Provider cancellation propagates; bounded failure may use deterministic offline fallback.
7. Provider configuration never comes from conversation history or personal memory.
8. Remote endpoints must use HTTPS and be bounded by timeout, history, message and response-size limits.
9. A concrete provider is not production-composed until owner configuration, network eligibility and full CI are complete.
10. Physical-device and live-network claims require actual evidence.

## Architecture

### Interaction layer
- Text and controlled voice interaction.
- Visual action and memory confirmations.
- Memory Center owner controls, storage health and safe migration retry.
- Structured personal-memory provenance chips.

### Knowledge and protected-storage layer
- Private document library and Current-only evidence.
- Approved personal memory and bounded pending proposals.
- AES-GCM Android Keystore protection, backward-compatible migration and non-destructive diagnostics.

### Conversational-provider layer
- `MayraConversationalProvider` is text-only.
- `ResilientMayraProviderAssistant` owns timeout, retry, cancellation and offline fallback.
- `MayraHttpConversationalProvider` supplies a concrete HTTPS POST transport but is not auto-installed.
- Runtime authorization comes from a dedicated configuration source.
- Owner configuration controls endpoint, model and enabled state.
- Connect/read timeout, request history, message lengths and response bytes are bounded.
- HTTP 408, 429 and 5xx are temporary; other non-success responses are permanent.
- Successful JSON responses must contain a nonblank string field named `text`.
- Provider health is explicit: DISABLED, MISSING_CREDENTIAL, READY, TEMPORARY_FAILURE or PERMANENT_FAILURE.
- No network permission is silently introduced; release-flavor eligibility is a separate audited decision.

### Reliability layer
- Compile, complete tests, lint, R8, permission/component audits and isolated APK artifacts.
- CI failures are diagnosed from exact logs or artifacts before code changes.
- Offline behavior remains available when the remote provider is disabled or unavailable.

## Current module state

| Module | Current phase |
|---|---|
| Core assistant and routing | Foundation present; expansion planned |
| Document intelligence | Foundation 16/18 implemented |
| Personal memory | Protected storage, diagnostics, recovery UI and structured provenance implemented; validation pending |
| Conversational provider | Concrete bounded HTTPS transport implemented but not production-enabled |
| Search and fresh knowledge | Provider architecture planned |
| Actions and automations | Safety foundation present; expansion planned |
| Voice intelligence | Separate controlled milestone |
| Privacy and release | Cross-cutting; ongoing |

## Milestone completion

A milestone is complete only when implementation and important failure-path tests are committed; compile, lint and relevant minified build audits pass; roadmap and snapshot are current; and device or live-network claims come only from actual evidence.

## Change-control rule

Every coding batch updates `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture, privacy or scope changes also update this blueprint.
