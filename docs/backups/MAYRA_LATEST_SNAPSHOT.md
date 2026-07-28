# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest isolated-APK CI-verified source head: `edc349ac4870a832f3a8612683e3fd7ab584fb82`
Authoritative isolated green CI: Android CI #1631
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Important correction

Android CI #1631 produced and audited `app-documentTest.apk`, an intentionally isolated document-testing package. It was **not the complete Mayra application APK**.

The isolated package excluded normal full-app surfaces and runtime components including Main Chat, `MayraApplication`, Memory Center launcher access, provider settings, notification listener, boot receiver and normal application permissions. It must not be used as evidence that the complete app was packaged or physically tested.

## What CI #1631 actually verified

1. Debug source compilation.
2. Complete debug unit-test suite.
3. Debug and document-test lint.
4. Isolated minified document-test R8 build.
5. Isolated zero-permission/component audit.
6. Isolated APK and reports artifacts.

Artifact: `mayra-document-test-apk-1631`

## Full Mayra APK pipeline now added

The Android CI workflow now also:

1. Builds `:app:assembleDebug` for package `ai.mayra.app`.
2. Audits the label `Mayra AI`.
3. Requires Main Chat, Document Library, Memory Center, Provider Settings, Activity History, Notification Listener and Boot Receiver.
4. Verifies expected microphone, contacts, call, SMS, notification, exact-alarm and boot permissions.
5. Explicitly rejects unexpected `android.permission.INTERNET`.
6. Uploads a separate `mayra-full-debug-apk-<run>` artifact.
7. Continues building the isolated document artifact separately.

## Current truthful status

- Source compilation, complete tests and lint were green on CI #1631.
- Isolated document APK/R8/audit was green.
- Complete Mayra debug APK packaging and audit are pending the new CI run.
- No complete-app Motorola testing has started.
- Remote provider remains uninstalled and INTERNET permission remains absent.
- PR remains Draft and unmerged.

## Next gate

Wait for the new full-APK workflow run to pass. Download and install only the artifact named `mayra-full-debug-apk-<run>` for complete Motorola acceptance testing with `docs/MAYRA_FULL_APP_ACCEPTANCE.md`.
