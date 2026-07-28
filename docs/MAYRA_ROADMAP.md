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
| Conversational provider | IN_PROGRESS | Bounded HTTPS transport plus owner settings UI/store implemented | Full CI, audited network flavor and secure credential integration |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Safe picker plus confirmation/idempotency/chat UX | Physical validation and reviewed adapters |
| Voice intelligence | PLANNED | Controlled separate milestone | Hindi/Hinglish evaluation and device validation |
| Privacy and release | IN_PROGRESS | Protected memory and provider credential boundaries | Wider privacy center and production release |

## Conversational provider — implemented foundation

1. Text-only provider boundary cannot execute actions or write memory.
2. Timeout, retry, cancellation and deterministic offline fallback are bounded.
3. Concrete HTTPS POST transport with response-size and history limits.
4. HTTP failures classify into temporary and permanent outcomes.
5. Provider health states expose disabled, missing credential, ready and failure states.
6. Owner settings store persists only non-secret endpoint/model/enable/limit configuration.
7. Bearer credentials are intentionally excluded from SharedPreferences and UI persistence.
8. Provider settings screen includes default-off toggle, validation, save status and emergency disable.
9. Plain HTTP settings are rejected without overwriting the previous valid configuration.
10. No INTERNET permission or automatic production composition has been introduced.

## Validation truth

Android CI #1601 compiled debug sources successfully but failed while compiling unit tests because `MayraProviderCredentialSource` was a normal interface while tests used Kotlin SAM lambdas. It is now a `fun interface`; runtime behavior is unchanged.

The newest governed head remains `IN_PROGRESS` until compile, complete unit tests, lint, R8 and permission/component audit all pass. No physical-device or live-provider claim has been made.

## Immediate next priority

Stabilize the newest governed CI. Then implement an audited network-enabled release flavor, secure runtime credential integration, provider composition/health diagnostics and Hindi/Hinglish evaluation. PR #12 remains Draft and unmerged.
