# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest functional fix head before governance commits: `999a10b1ec139e391ac888ed9c07e8556ca73fb6`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Full-app verification truth

Android CI #1623 passed debug source compilation and the complete unit-test suite. Android lint then failed on one minSdk compatibility issue: `InputStream.readNBytes` requires API 33, while Mayra supports API 26. The provider response reader has been replaced with a manual bounded loop that retains the configured maximum response size and is compatible with API 26+. R8, manifest audit and APK upload were skipped after lint failed; a newer full pipeline is required before a current-build green or installable-APK claim.

## Current product state

- Core routing, typed outcomes, action confirmation, idempotency and activity history foundations remain implemented.
- Document intelligence remains 16/18 implemented with TXT/PDF/DOCX support and Current-only evidence controls.
- Personal memory includes explicit approval, conflict review, protected storage, migration, expiry, Memory Center health/recovery UI and trusted provenance chips.
- Remote-provider foundation includes bounded HTTPS transport, owner settings, secret exclusion, default-off behavior and emergency disable.
- Remote transport remains uninstalled and no INTERNET permission has been added.

## Completed in this verification batch

- Continued full-app CI verification.
- Confirmed debug source compilation passed on CI #1623.
- Confirmed the complete unit-test suite passed on CI #1623.
- Downloaded and inspected the lint reports artifact.
- Located the exact API-33-only `readNBytes` compatibility failure.
- Replaced it with an API-26-compatible bounded response reader.
- Added a comprehensive owner-device acceptance checklist covering all major app surfaces and safety paths.
- Updated roadmap and rolling recovery snapshot.

## Full verification gate

The next governed CI must pass all of the following on one head before the current app build is treated as CI-verified:

1. Debug source compilation.
2. Complete unit-test suite.
3. Android lint.
4. Isolated minified document-test APK/R8 build.
5. Manifest, permission and component audit.
6. APK and reports artifact upload.

After that, owner-device checking must follow `docs/MAYRA_FULL_APP_ACCEPTANCE.md`, including launch, chat, voice, memory approval, protected storage, document search, action confirmation, provider settings, permissions and restart/recovery flows.

## Safety contract

- PR remains Draft and unmerged.
- No network permission or remote-provider activation was introduced.
- Credentials remain outside source, chat, memory and ordinary settings.
- Failed health checks never clear memory or reset Keystore keys.
- Response size remains strictly bounded on every supported Android version.
- No physical-device validation is claimed until the APK is actually installed and exercised.

## Recovery instructions

1. Confirm PR #12 remains Draft and unmerged.
2. Check the newest governed-head CI before claiming validation.
3. Use CI #1458 as authoritative green evidence until superseded by a complete newer run.
4. Download the newest APK only after compile, tests, lint, R8 and manifest audit all pass.
5. Keep remote provider disabled and permission-free until the separate audited network-release decision.
6. Use `docs/MAYRA_FULL_APP_ACCEPTANCE.md` for owner-device evidence.
7. Update this snapshot after each code or verification batch.
