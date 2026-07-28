# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest CI-verified source head: `edc349ac4870a832f3a8612683e3fd7ab584fb82`
Authoritative green CI: Android CI #1631
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Full-app verification truth

Android CI #1631 completed successfully on the same governed head.

Passed:

1. Debug application source compilation.
2. Complete unit-test suite.
3. Android lint.
4. Isolated minified document-test APK/R8 build.
5. Manifest, permission and component audit.
6. Reports artifact upload.
7. APK artifact upload.

Artifacts:

- APK artifact: `mayra-document-test-apk-1631`
- Reports artifact: `android-reports-1631`
- APK artifact ZIP SHA-256: `88d224c33c968c1311cebd34c471153f5bc4960e3aa4094c1961e179761ff0ee`
- Extracted `app-documentTest.apk` size: `7,488,314` bytes
- Extracted APK SHA-256: `abe4b65073a32af823c39c45c5c8a1406279878d817cf8068f661a8965195b73`

## Current product state

- Core routing, typed outcomes, action confirmation, idempotency and activity history foundations are CI-verified.
- Document intelligence remains 16/18 implemented with TXT/PDF/DOCX support and Current-only evidence controls.
- Personal memory includes explicit approval, protected storage, migration, expiry, Memory Center health/recovery UI and provenance chips.
- Remote-provider foundation includes bounded HTTPS transport, owner settings, secret exclusion, default-off behavior and emergency disable.
- Remote provider remains uninstalled and no INTERNET permission has been added.
- The isolated APK is now suitable for owner-device smoke and acceptance testing.

## Safety contract

- PR remains Draft and unmerged.
- CI success is not a physical-device claim.
- No live remote-provider claim is made.
- Credentials remain outside source, chat, memory and ordinary settings.
- Failed memory-health checks never clear records or reset Keystore keys.
- Response size remains bounded on every supported Android version.

## Owner-device gate

Install the CI #1631 APK and execute `docs/MAYRA_FULL_APP_ACCEPTANCE.md` in order. The physical check must cover launch, navigation, chat, Hindi/Hinglish, voice, memory approval and persistence, Memory Center, document library, action confirmation, provider settings default-off behavior, permissions and restart/recovery.

For every failure record:

- screen name,
- exact steps,
- expected result,
- actual result,
- screenshot or screen recording,
- whether the issue reproduces after force-stop/restart.

## Recovery instructions

1. Confirm PR #12 remains Draft and unmerged.
2. Treat CI #1631 as the authoritative automated validation for head `edc349ac4870a832f3a8612683e3fd7ab584fb82`.
3. Verify the downloaded APK SHA-256 before installation when possible.
4. Keep remote provider disabled and permission-free until the separate audited network-release decision.
5. Use `docs/MAYRA_FULL_APP_ACCEPTANCE.md` for physical-device evidence.
6. Update this snapshot after device findings or the next code batch.