# Mayra AI — Execution Roadmap

Last updated: 2026-07-28
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest backup: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Overall program view

| Track | Status | Progress signal | Next gate |
|---|---|---|---|
| Blueprint and backup discipline | DONE | Canonical blueprint, roadmap and rolling snapshots | Keep updated every batch |
| Document intelligence | DEVICE_VERIFY | 16/18 implemented and isolated APK CI-verified | Full APK + Motorola verification; OCR and legacy DOC deferred |
| Core routing and eligibility | DONE | Typed outcomes and capability gates | Keep regressions green |
| Personal memory | DEVICE_VERIFY | Protected storage, health/recovery UI and provenance passed source/test/lint CI | Full APK packaging and Motorola acceptance |
| Conversational provider | FOUNDATION_VERIFIED | Bounded HTTPS transport and owner settings passed source/test/lint CI | Full APK audit, network flavor and secure credentials |
| Search and fresh knowledge | PLANNED | No completion claim | Provider interface, citations and freshness |
| Actions and automations | DEVICE_VERIFY | Confirmation/idempotency/chat UX passed source/test/lint CI | Full APK packaging and physical validation |
| Voice intelligence | DEVICE_VERIFY | Existing voice path compiles | Full APK packaging and Hindi/Hinglish physical evaluation |
| Privacy and release | IN_PROGRESS | Isolated APK audit passed; full APK audit added | Full package permission/component verification |

## Verification truth

Android CI #1631 passed compile, complete unit tests, lint, and the isolated `documentTest` R8 APK audit on governed head `edc349ac4870a832f3a8612683e3fd7ab584fb82`.

That artifact was **not the complete Mayra application**. It intentionally excluded `MainActivity`, `MayraApplication`, background services/receivers, personal-memory launcher surface, voice/runtime composition and normal application permissions.

The CI workflow now additionally:

1. Runs `:app:assembleDebug` for the complete `ai.mayra.app` package.
2. Audits the full APK label and package.
3. Requires Main Chat, Document Library, Memory Center, Provider Settings, Activity History, Notification Listener and Boot Receiver components.
4. Verifies the expected microphone, contacts, phone, SMS, notifications, exact-alarm and boot permissions.
5. Explicitly fails if `android.permission.INTERNET` appears.
6. Uploads a separate `mayra-full-debug-apk-<run>` artifact.
7. Retains the isolated document APK as a separate regression artifact.

## Immediate next priority

Stabilize the new full-APK CI run. Only the `mayra-full-debug-apk` artifact should be installed for complete Motorola acceptance testing. PR #12 remains Draft and unmerged.
