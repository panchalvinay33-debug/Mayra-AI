# Mayra AI — Changelog

This changelog records meaningful user-visible and engineering milestones. It does not replace Git history.

## Unreleased — Play Protect recovery and zero-permission J1 test

- Recorded the Motorola Play Protect block against the full sideloaded Personal Alpha.
- Stopped treating the full debug-signed Personal Alpha as the correct J1 installation path.
- Added a dedicated `j1AssistantTest` package: `ai.mayra.app.j1`.
- The J1 package requests zero Android runtime permissions and contains only:
  - a small Assistant activation/status screen;
  - Android VoiceInteractionService/session/recognition foundations;
  - the animated Mayra assistant orb/session.
- Removed chat, provider, contacts, reminders, notification listener, boot recovery, document and memory screens from this test package.
- Added a dedicated CI workflow that hard-fails on any requested permission, extra launcher or forbidden component.
- Full Mayra remains on the stable owner-signing / Play Internal Testing track.

Validation state: J1 source and audit workflow committed; latest CI and Motorola installation evidence pending.

## Unreleased — Simplified owner setup and stable updates

- Recorded the real Motorola install failure where Personal Alpha #1851 could not update an older `ai.mayra.app.alpha` signed by another temporary CI debug certificate.
- Added a stable secret-backed owner signing configuration for Personal Alpha.
- Added a dedicated `Stable Owner Alpha` GitHub workflow that requires protected signing secrets, verifies the certificate, records APK SHA-256 and uploads an update-compatible artifact.
- Added a one-time two-step Mayra setup screen for the full owner app.
- Renamed the main `Device` chip to `Setup` and simplified its wording.
- Added install-over-install data-retention testing.

## Jarvis Mode foundation and recovery hardening

- Added Android Assistant-role architecture foundation.
- Added `VoiceInteractionService`, session service and assistant metadata foundation.
- Added animated Mayra voice-session orb foundation.
- Added RecognitionService shell for future offline wake-word/speech pipeline.
- Declared lock-screen assistant-session support.
- Added governance, pinpoint audit, test matrix, rollback playbook and protected baselines.

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
