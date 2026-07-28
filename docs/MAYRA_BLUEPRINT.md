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
7. Provider credentials never live in source control, conversation history, personal memory or ordinary provider settings persistence.
8. Remote endpoints must use HTTPS and be bounded by timeout, history, message and response-size limits.
9. Remote answers are owner-disabled by default and an emergency disable must remain available.
10. A concrete provider is not production-composed until network eligibility, secure credentials and full CI are complete.
11. Physical-device and live-network claims require actual evidence.

## Architecture

### Interaction layer
- Text and controlled voice interaction.
- Visual action and memory confirmations.
- Memory Center owner controls, storage health and safe migration retry.
- Structured personal-memory provenance chips.
- Remote-provider settings screen with explicit enable, save validation and emergency disable.

### Conversational-provider layer
- `MayraConversationalProvider` is text-only.
- `ResilientMayraProviderAssistant` owns timeout, retry, cancellation and offline fallback.
- `MayraHttpConversationalProvider` supplies bounded HTTPS transport but is not auto-installed.
- `AndroidMayraProviderSettingsStore` persists non-secret endpoint, model, enable state and limits only.
- Runtime authorization comes from a separate `MayraProviderCredentialSource`.
- Invalid settings cannot overwrite the previous valid configuration.
- Provider health is explicit: DISABLED, MISSING_CREDENTIAL, READY, TEMPORARY_FAILURE or PERMANENT_FAILURE.
- No network permission is silently introduced; release-flavor eligibility is a separate audited decision.

### Reliability layer
- Compile, complete tests, lint, R8, permission/component audits and isolated APK artifacts.
- CI failures are diagnosed from exact logs or reports before changes.
- Offline behavior remains available when remote intelligence is disabled or unavailable.

## Current module state

| Module | Current phase |
|---|---|
| Core assistant and routing | Foundation present; expansion planned |
| Document intelligence | Foundation 16/18 implemented |
| Personal memory | Protected storage, diagnostics, recovery UI and structured provenance implemented; validation pending |
| Conversational provider | Bounded HTTPS transport and owner settings implemented; network flavor/credentials/composition pending |
| Search and fresh knowledge | Provider architecture planned |
| Actions and automations | Safety foundation present; expansion planned |
| Voice intelligence | Separate controlled milestone |
| Privacy and release | Cross-cutting; ongoing |

## Milestone completion

A milestone is complete only when implementation and important failure-path tests are committed; compile, lint and relevant minified build audits pass; roadmap and snapshot are current; and device or live-network claims come only from actual evidence.

## Change-control rule

Every coding batch updates `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture, privacy or scope changes also update this blueprint.
