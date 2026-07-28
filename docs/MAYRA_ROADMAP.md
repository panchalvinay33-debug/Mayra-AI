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

## Full-app verification truth

Android CI #1617 compiled application sources and executed the complete unit-test suite, but one assertion failed: a disabled remote provider correctly avoided opening a connection yet changed its health state from `DISABLED` to `PERMANENT_FAILURE`. The provider now preserves `DISABLED` while still returning a non-success outcome to the resilient offline-fallback layer.

The newest governed head remains `IN_PROGRESS` until compile, complete unit tests, lint, R8 and permission/component audit all pass. A downloadable CI APK is authoritative only after that full chain succeeds. No physical-device or live-provider claim has been made.

## Immediate next priority

Run the full governed app verification chain on the repaired head. If green, use its isolated APK for owner-device smoke testing of launch, chat, documents, Memory Center, provider settings and safety controls. PR #12 remains Draft and unmerged.
