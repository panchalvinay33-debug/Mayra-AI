# Mayra AI — Immutable Jarvis J1 CI Baseline

Snapshot date: 2026-08-03
Branch source: `agent/document-library-foundation`
Protected baseline branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
Exact baseline commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
PR: #12 — Draft/open/unmerged
Version: 0.2.1 / versionCode 4

## Authoritative validation

- Android CI #1851 — success
- Project Governance #32 — success

Android CI #1851 passed:

1. Debug, Personal Alpha and Full Test compilation.
2. Complete debug unit-test suite.
3. Android lint across Debug, Personal Alpha, Full Test, Release and Document Test.
4. Personal Alpha APK assembly and package/permission/component/one-launcher audit.
5. Minified non-debuggable final `ai.mayra.app` release/R8/manifest audit.
6. Safe Full Test assembly and permission/component audit.
7. Isolated Document Test assembly and zero-permission/component audit.
8. Reports and all governed APK artifacts upload.

## Artifacts

### Personal Alpha

- Name: `mayra-personal-alpha-apk-1851`
- Artifact ID: `8852147191`
- ZIP size: 18,722,590 bytes
- ZIP SHA-256: `ab7cb7d457ed9a034bab5ba394157cf263980b1444fd7c3cf178fc91186296af`
- Extracted APK: `app-personalAlpha.apk`
- APK size: 19,162,094 bytes
- APK SHA-256: `1459517f1aa375576afa353ba6683ceaf81ddbcb4e79fc6dd790a501f52307b8`

### Full Test

- Name: `mayra-full-test-apk-1851`
- Artifact ID: `8852148204`
- ZIP size: 18,721,631 bytes
- ZIP SHA-256: `d437181ac9c1b7c2c204a7cce66c29a53025b35a0e048b771d5e2b2990a0de0e`

### Document Test

- Name: `mayra-document-test-apk-1851`
- Artifact ID: `8852148923`
- ZIP size: 7,083,991 bytes
- ZIP SHA-256: `64c5c391c1f7876cc89616c0869e83a11c66969f33fc87c463068246a1d09fa7`

### Reports

- Name: `android-reports-1851`
- Artifact ID: `8852146266`
- ZIP size: 2,743,369 bytes
- ZIP SHA-256: `34a9f2e561349ff4f042801f5cf3b1d9b413132d71671be68eea77301156c2b3`

## Included Jarvis J1 foundation

- Android VoiceInteractionService foundation.
- VoiceInteractionSessionService.
- Native animated Mayra orb session surface.
- RecognitionService shell that fails honestly until a real recognizer is integrated.
- Assistant metadata and lock-screen session declaration.
- Assistant components excluded from the low-permission Full Test variant.
- Compile repairs after failed Android CI #1833.

## Included governance/recovery foundation

- START_HERE mandatory entry point.
- Full-project pinpoint audit.
- Canonical test matrix.
- Baseline and rollback playbook.
- Automated Project Governance workflow.
- Immutable pre-Jarvis baseline `baseline/mayra-0.2.1-green-1795` retained.

## Status boundary

This baseline proves source, tests, lint and governed packaging. It does **not** prove that the Motorola exposes Mayra in Assistant settings or that unlocked/locked invocation works.

Jarvis J1 remains `DEVICE_VERIFY` until physical evidence is recorded.

## Exact next phase

1. Install only Personal Alpha #1851 for owner testing.
2. Record phone Android/build version and APK SHA-256.
3. Verify one launcher and first launch.
4. Check whether Mayra appears in default Assistant selection.
5. Select/remove Mayra as Assistant.
6. Test unlocked and locked invocation.
7. Observe animated session lifecycle, crash behavior, reboot survival and battery impact.
8. Record PASS/FAIL/BLOCKED evidence in the acceptance checklist and pinpoint audit.
9. Do not start wake-word/local-model work until J1 device findings are documented and blocking defects repaired.
