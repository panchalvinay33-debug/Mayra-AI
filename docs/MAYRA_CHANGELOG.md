# Mayra AI — Changelog

This changelog records meaningful user-visible and engineering milestones. It does not replace Git history.

## Unreleased — Jarvis Mode foundation

- Added Android Assistant-role architecture foundation.
- Added `VoiceInteractionService`, session service and assistant metadata foundation.
- Added animated Mayra voice-session orb foundation.
- Added RecognitionService shell for future offline wake-word/speech pipeline.
- Declared lock-screen assistant-session support.
- Kept Assistant-role components out of the low-permission Full Test variant.
- Added canonical project governance records and automated documentation-drift protection.

Validation state: latest Jarvis head requires complete CI and Motorola role-selection/invocation testing.

## 0.2.1 — Owner Alpha hardening

- Scoped permission UX: microphone on Voice use, Contacts and Notifications separately.
- Removed misleading exact-reminder readiness claim.
- Provider changes now rebuild Mayra live without app restart.
- Main-screen provider state refreshes on resume.
- Confirmation expiry is surfaced before stale execution.
- Added minified/R8 final `ai.mayra.app` release-candidate audit.
- Added environment-only production signing scaffold.
- Fixed reminder `DUE → MISSED` follow-up transition.
- Fixed reboot recovery to use remaining follow-up delay.
- Fixed app-opening commands being intercepted by the file-action router.
- Shared provider composition between startup and Settings.
- Version bumped to 0.2.1 / code 4.

Verified baseline: Android CI #1795 green before the Jarvis-role commits.

## 0.2.0 — Personal Alpha

- Added owner-device Personal Alpha package and automated capability audit.
- Added OpenAI Responses-compatible bounded HTTPS provider.
- Added Android Keystore-backed API credential protection.
- Added live offline fallback architecture.
- Added review-first dialer and message-composer handoffs.
- Added Mayra-owned persistent reminders with notifications, Complete, Snooze, follow-up and reboot recovery.
- Added one user-facing launcher architecture across Chat, Library, Memory, Provider and History.
- Added typed personal-memory attribution outside visible assistant text.

Verified milestone: Android CI #1753 green; later superseded by 0.2.1.

## Document and memory foundation

- Added TXT/PDF/DOCX document import, extraction, indexing, search, summaries and grounded answers.
- Added freshness/health and maintenance tooling.
- Added protected personal-memory storage, approval, replacement, edit, delete, expiry and recovery UI.
- Added structured memory provenance chips.
- Added typed routing, capability gates, action confirmation and activity history.

## Initial foundation

- Created Kotlin/Jetpack Compose Android application.
- Added stateful chat UI, replaceable assistant boundary, speech-to-text and text-to-speech foundations.
- Added local deterministic assistant and initial runtime permission handling.
