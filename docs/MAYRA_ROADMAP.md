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
| Personal memory | IN_PROGRESS | Protected storage, health/recovery UI and structured provenance implemented | Full CI and Motorola validation |
| Conversational provider | IN_PROGRESS | Reliability boundary plus concrete bounded HTTPS transport committed | Secure owner config, network permission decision and production composition |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe picker plus confirmation/idempotency/chat UX | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | Protected memory and provider credential boundaries | Wider privacy center and production release |

## Personal memory — implemented

1. Explicit proposal/approval, exclusions, conflict review and replay safety.
2. Protected approved/pending persistence with migration, expiry and process recovery.
3. Search, category filters, edit, pending management, health card and safe migration retry.
4. Structured memory-use metadata and Compose provenance chips.
5. No destructive key reset, silent clearing or hidden empty-history claim.

## Conversational provider — implemented foundation

1. Text-only provider boundary cannot execute actions or write memory.
2. Timeout, retry, cancellation and deterministic offline fallback are bounded.
3. Concrete HTTPS POST transport using runtime bearer credentials.
4. HTTPS-only endpoint validation and owner `enabled` gate.
5. Connect/read timeout and maximum response-size enforcement.
6. HTTP 408/429/5xx classify as temporary; other non-2xx classify as permanent.
7. Provider response must expose a JSON string field named `text`.
8. Provider health states: DISABLED, MISSING_CREDENTIAL, READY, TEMPORARY_FAILURE and PERMANENT_FAILURE.
9. Conversation history and message sizes are bounded before serialization.
10. Transport remains uninstalled by default; no INTERNET permission was added in this batch.
11. Deterministic tests cover disabled/missing credential, success, HTTP classification and oversized responses.

## Validation truth

Android CI #1589 failed during debug compilation because `this` inside a Compose `Column` was passed where Android `Context` was required. The exact line was replaced with `LocalContext.current`; this was a source regression, not a platform failure.

The newest governed head remains `IN_PROGRESS` until compile, complete unit tests, lint, R8 and permission/component audit all pass. No physical-device or live-provider claim has been made.

## Immediate next priority

Run and stabilize the newest governed CI. Then add secure owner-facing provider configuration and decide the audited INTERNET-permission/release-flavor strategy before production composition. PR #12 remains Draft and unmerged.
