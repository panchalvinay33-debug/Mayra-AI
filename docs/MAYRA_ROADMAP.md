# Mayra AI — Execution Roadmap

Last updated: 2026-07-28
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest backup: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`

## Overall program view

| Track | Status | Progress signal | Next gate |
|---|---|---|---|
| Blueprint and backup discipline | DONE | Canonical blueprint, roadmap and rolling snapshots | Keep updated every batch |
| Document intelligence | DEVICE_VERIFY | 16/18 implemented | Phone verification; OCR and legacy DOC deferred |
| Core routing and eligibility | DONE | Typed outcomes and capability gates | Keep regressions green |
| Audited runtime safety | DONE | Typed results, persistence, confirmation and idempotency | Physical validation |
| Personal memory | IN_PROGRESS | Protected storage, owner UI, health diagnostics and structured provenance implemented | Full CI and Motorola validation |
| Conversational provider | FOUNDATION | Timeout/retry/cancellation/offline-fallback boundary committed | Concrete provider adapter and secure runtime configuration |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe picker plus confirmation/idempotency/chat UX | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | Protected memory records, diagnostics and owner controls | Wider privacy center and production release |

## Personal memory — implemented

1. Explicit proposal/approval with prohibited-secret and sensitive-data exclusions.
2. Approved and pending persistence, TTL pruning, replay safety and process-death restoration.
3. Visual Save/Replace/Not now conflict review and stale-conflict rejection.
4. Search, category filters, direct edit, expiry presets and pending proposal management.
5. AES-GCM Android Keystore protection for approved and pending records with separate aliases.
6. Backward-compatible legacy migration and failure-safe write preservation.
7. Read-only storage-health classification: EMPTY, HEALTHY, MIGRATION_NEEDED and DEGRADED.
8. Memory Center health card with protected/legacy/unreadable counters.
9. Owner-triggered `Retry safe migration`; no automatic key reset or destructive clearing.
10. Structured memory-use metadata on `MayraMessage` and Compose provenance chips.
11. Malformed metadata cannot become trusted provenance.
12. Regression tests for storage health, metadata parsing, migration and rollback.
13. No new Android permission, service, receiver or background component.

## Conversational-provider foundation

1. `MayraConversationalProvider` is text-only and cannot execute actions or write memory.
2. Provider requests are bounded to 100 conversation messages.
3. Timeout is configurable and bounded to 1–60 seconds.
4. Temporary failures receive at most three total attempts.
5. Permanent failures immediately use the offline assistant.
6. Exhausted temporary failures use the offline assistant.
7. Coroutine cancellation propagates and is never converted into a fallback answer.
8. Provider credentials are supplied through a runtime credential-source contract, never personal memory or source control.
9. The remote provider is not enabled in production composition yet.

## Validation truth

Android CI #1571 compiled successfully but failed one legacy disclosure assertion in `PersonalMemoryAwareMayraAssistantTest`; the test expected old appended text after the implementation moved to structured metadata. The assertion was corrected to parse and verify trusted metadata.

The newest governed head remains `IN_PROGRESS` until compile, complete unit tests, lint, R8 and permission/component audit all pass. No physical-device claim has been made.

## Immediate next priority

Run and stabilize the latest governed CI. Then implement one concrete production provider adapter with secure runtime configuration, network eligibility checks, response-size limits and provider diagnostics. PR #12 remains Draft and unmerged.
