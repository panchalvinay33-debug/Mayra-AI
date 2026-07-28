# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest functional fix head before governance commits: `34e991966e5431e1d0c9fa9c751327ac297e1b33`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458

## Full-app verification truth

Android CI #1617 compiled application sources successfully and ran the complete unit-test suite. One test failed: `MayraHttpConversationalProviderTest.disabledProviderDoesNotOpenNetwork` expected health state `DISABLED`, but the disabled early return reused the generic permanent-failure helper and changed health to `PERMANENT_FAILURE`. The implementation now explicitly preserves `DISABLED` while still returning a non-success provider result. Lint, R8 and APK audit were skipped after the test failure; a newer full pipeline is required before a green or installable-current-build claim.

## Current product state

- Core routing, typed outcomes, action confirmation, idempotency and activity history foundations remain implemented.
- Document intelligence remains 16/18 implemented with PDF/DOCX/TXT support and Current-only evidence controls.
- Personal memory includes explicit approval, conflict review, protected storage, migration, expiry, Memory Center health/recovery UI and trusted provenance chips.
- Remote-provider foundation includes bounded HTTPS transport, owner settings, secret exclusion, default-off behavior and emergency disable.
- Remote transport remains uninstalled and no INTERNET permission has been added.

## Completed in this verification batch

- Inspected Android CI #1617 as a full-app verification run.
- Confirmed debug application source compilation passed.
- Confirmed the complete unit-test stage was reached.
- Downloaded and inspected the CI reports artifact.
- Located the single exact provider-health assertion failure.
- Fixed disabled-provider health-state preservation without enabling network access.
- Updated roadmap and rolling recovery snapshot.

## Full verification gate

The next governed CI must pass all of the following before the current app build is treated as verified:

1. Debug source compilation.
2. Complete unit-test suite.
3. Android lint.
4. Isolated minified document-test APK/R8 build.
5. Manifest, permission and component audit.
6. APK and reports artifact upload.

After that, owner-device checking should cover app launch, main chat, action confirmation, document library/search, Memory Center save/edit/expiry/restart, storage-health display, provider settings default-off behavior, voice entry and navigation between launcher activities.

## Safety contract

- PR remains Draft and unmerged.
- No network permission or remote-provider activation was introduced.
- Credentials remain outside source, chat, memory and ordinary settings.
- Failed health checks never clear memory or reset Keystore keys.
- No physical-device validation is claimed until the APK is actually installed and exercised.

## Recovery instructions

1. Confirm PR #12 remains Draft and unmerged.
2. Check the newest governed-head CI before claiming validation.
3. Use CI #1458 as authoritative green evidence until superseded by a complete newer run.
4. Download the newest APK only after compile, tests, lint, R8 and manifest audit all pass.
5. Keep remote provider disabled and permission-free until the separate audited network-release decision.
6. Update this snapshot after each code or verification batch.
