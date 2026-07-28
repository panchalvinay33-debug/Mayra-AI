# Mayra AI — Execution Roadmap

Last updated: 2026-07-28
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest backup: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Overall program view

| Track | Status | Progress signal | Next gate |
|---|---|---|---|
| Blueprint and backup discipline | DONE | Canonical blueprint, roadmap and rolling snapshots | Keep updated every batch |
| Document intelligence | DEVICE_VERIFY | 16/18 implemented and CI-verified | Motorola verification; OCR and legacy DOC deferred |
| Core routing and eligibility | DONE | Typed outcomes and capability gates | Keep regressions green |
| Personal memory | DEVICE_VERIFY | Protected storage, health/recovery UI and provenance passed full CI | Motorola acceptance |
| Conversational provider | FOUNDATION_VERIFIED | Bounded HTTPS transport and owner settings passed full CI | Audited network flavor and secure credential integration |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Confirmation/idempotency/chat UX passed full CI | Physical validation and reviewed adapters |
| Voice intelligence | DEVICE_VERIFY | Existing voice path compiles and packages | Hindi/Hinglish physical evaluation |
| Privacy and release | IN_PROGRESS | Permission/component audit passed for isolated APK | Wider privacy center and production release |

## Conversational provider — verified foundation

1. Text-only provider boundary cannot execute actions or write memory.
2. Timeout, retry, cancellation and deterministic offline fallback are bounded.
3. Concrete HTTPS POST transport enforces response-size and history limits.
4. HTTP failures classify into temporary and permanent outcomes.
5. Provider health exposes disabled, missing credential, ready and failure states.
6. Owner settings persist only non-secret endpoint/model/enable/limit configuration.
7. Bearer credentials are excluded from SharedPreferences and UI persistence.
8. Provider settings include default-off behavior, validation and emergency disable.
9. Plain HTTP settings are rejected without overwriting valid configuration.
10. No INTERNET permission or automatic production composition has been introduced.
11. Response reading uses an API-26-compatible bounded loop.

## Full-app verification truth

Android CI #1631 completed successfully on governed head `edc349ac4870a832f3a8612683e3fd7ab584fb82`.

Passed on the same head:

1. Debug source compilation.
2. Complete unit-test suite.
3. Android lint.
4. Isolated minified document-test APK/R8 build.
5. Manifest, permission and component audit.
6. Reports and APK artifact upload.

Artifacts:

- `mayra-document-test-apk-1631`
- `android-reports-1631`
- APK artifact ZIP SHA-256: `88d224c33c968c1311cebd34c471153f5bc4960e3aa4094c1961e179761ff0ee`
- Extracted APK SHA-256: `abe4b65073a32af823c39c45c5c8a1406279878d817cf8068f661a8965195b73`

CI verification does not equal physical-device acceptance. No live remote-provider or Motorola behavior claim is made yet.

## Immediate next priority

Install CI #1631's APK on the owner Motorola device and execute `docs/MAYRA_FULL_APP_ACCEPTANCE.md` screen by screen. Record screenshots and exact reproduction steps for any failure. PR #12 remains Draft and unmerged.