# Mayra AI — Changelog

This changelog records meaningful user-visible and engineering milestones. It does not replace Git history.

## Unreleased — Simplified owner setup and stable updates

- Recorded the real Motorola install failure where Personal Alpha #1851 could not update an older `ai.mayra.app.alpha` signed by another temporary CI debug certificate.
- Added a stable secret-backed owner signing configuration for Personal Alpha.
- Added a dedicated `Stable Owner Alpha` GitHub workflow that requires protected signing secrets, verifies the certificate, records APK SHA-256 and uploads an update-compatible artifact.
- Added a one-time two-step Mayra setup screen:
  - request microphone, contacts and reminder-notification permissions together;
  - open Android's Assistant-role selector;
  - start Mayra with an explicit continue option if a permission/role is temporarily skipped.
- Renamed the main `Device` chip to `Setup` and simplified its wording.
- Added a permanent device test record for the package-signature conflict and a mandatory install-over-install data-retention test.

Validation state: source has been committed; Android CI, Project Governance and Motorola clean-install/upgrade tests are pending.

## Jarvis Mode foundation and recovery hardening

- Added Android Assistant-role architecture foundation.
- Added `VoiceInteractionService`, session service and assistant metadata foundation.
- Added animated Mayra voice-session orb foundation.
- Added RecognitionService shell for future offline wake-word/speech pipeline.
- Declared lock-screen assistant-session support.
- Kept Assistant-role components out of the low-permission Full Test variant.
- Added canonical project governance records and automated documentation-drift protection.
- Added `START_HERE.md`, pinpoint audit, test matrix and rollback playbook.
- Created protected pre-Jarvis and Jarvis J1 known-green baselines.
- Preserved CI #1833 failure evidence and repaired Assistant API/type incompatibilities.

Verified Jarvis J1 baseline: Android CI #1851 and Project Governance #32 green on `0d9435adb92b425bfb47a710d4f4516a6aaac398`.

## 0.2.1 — Owner Alpha hardening

- Scoped permission UX and removed misleading exact-reminder readiness.
- Provider changes rebuild Mayra live without restart.
- Confirmation expiry is surfaced before stale execution.
- Added minified/R8 final release audit and environment-only signing scaffold.
- Fixed reminder follow-up, reboot delay and app-opening routing.
- Version bumped to 0.2.1 / code 4.

Verified baseline: Android CI #1795.

## 0.2.0 — Personal Alpha

- Added owner-device Personal Alpha package and capability audit.
- Added Responses-compatible HTTPS provider and Android Keystore credential protection.
- Added review-first dialer/message handoffs.
- Added persistent reminders with Complete, Snooze, follow-up and reboot recovery.
- Added one user-facing launcher across Chat, Library, Memory, Provider and History.
- Added typed personal-memory attribution outside visible assistant text.

Verified milestone: Android CI #1753; superseded by 0.2.1.

## Document and memory foundation

- Added TXT/PDF/DOCX import, extraction, indexing, search, summaries and grounded answers.
- Added freshness/health tooling and protected personal-memory lifecycle.
- Added typed routing, capability gates, confirmation and activity history.

## Initial foundation

- Created Kotlin/Jetpack Compose Android application.
- Added stateful chat, replaceable assistant boundary, speech-to-text, text-to-speech and local deterministic assistant.
