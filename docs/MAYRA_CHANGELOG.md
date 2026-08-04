# Mayra AI — Changelog

This changelog records meaningful user-visible and engineering milestones. It does not replace Git history.

## Unreleased — J2 lock-screen privacy + offline spoken reply

- Accepted the device-proven J2 core voice foundation: Digital assistant role, Motorola Power-button invocation, on-device Hindi/Hinglish/English recognition, direct dismissal, 20-cycle stability, already-locked invocation and owner-reported reboot/no-speech/rapid interaction checks.
- Implemented keyguard-aware privacy rendering so recognized/private transcript is not shown while the device is locked.
- Reworked lock-screen layout spacing to remove the observed Mayra-label/transcript overlap from CI #106.
- Added offline-first Android TTS with preferred voice order Hindi India → English India → English US → another offline voice.
- Added deterministic local spoken responses for greeting, time, capability, reminder and app-open intent.
- Preserved J2 safety boundary: app/reminder requests are understood but not executed and Mayra does not falsely claim success.
- Added recognizer/TTS lifecycle cleanup on hide/destroy.
- Added tests for greeting, deterministic time reply, no fake action success and private unknown-transcript classification.
- Exact application source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62` passed J2 #136, J1 #239, Android CI #2131 and Governance #312.
- Promoted protected CI baseline `baseline/mayra-0.2.1-j2-privacy-tts-green-136`.
- Artifact `mayra-j2-voice-apk-136`, ID `8868518898`, APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`.
- Device verification remains required for the repaired lock-screen privacy behavior and audible voice quality before full J2 `DEVICE_VERIFIED` promotion.

## Unreleased — J2 Motorola speech-locale repair

- Motorola device testing proved J2 can install, obtain microphone permission, report on-device recognition available, become the selected Digital assistant and launch a mic-active Mayra Assistant session.
- The first real transcript attempt returned `Speech language unavailable` instead of a transcript.
- Root cause: the CI #18 request relied on an implicit/default speech language even though on-device recognizer availability does not guarantee that language model is available.
- Added `MayraSpeechLocalePolicy` with finite locale order: device locale → `hi-IN` → `en-IN` → `en-US`.
- Added explicit recognition language/language-preference extras.
- Language-not-supported/language-unavailable errors now retry the next bounded locale only; there is no endless listener/retry loop and no silent cloud STT fallback.
- Added unit tests for locale order, duplicate removal and blank device locale.
- Device failure and repair are recorded in `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`.

Validation state: fresh J2/J1/Android/Governance CI pending; replacement owner APK must not be shared until green.

## Unreleased — J1 Motorola proof and J2 invocation-time voice

- Motorola Android 16 physically accepted `Mayra J1 Assistant Test` as the selected Digital assistant.
- Configuring the Motorola Power-button Digital assistant action successfully invoked Mayra’s `VoiceInteractionSession` while unlocked.
- Device screenshot proved the animated Mayra orb/session renders over the current screen.
- Back navigation and phone lock were confirmed to dismiss the J1 session.
- Device testing exposed that the first orb had no direct tap/outside dismiss listener.
- Added bounded session exits: orb tap, label tap, root/outside tap and Back call `hide()`; hide/destroy stop animation and keep-awake state.
- Hardened repeat-show behavior so the orb animation restarts cleanly after a previous hide.
- Added J2 voice feasibility preflight under Issue #14.
- Added isolated `j2VoiceTest` package `ai.mayra.app.j2`.
- J2 requests only `RECORD_AUDIO` and removes internet, contacts, notifications, reminders, WorkManager, Room and unrelated full-app/background components.
- Added a bounded voice-session state model for permission, on-device availability, preparing, listening, partial transcript, processing, heard text and errors.
- Added invocation-time `SpeechRecognizer.createOnDeviceSpeechRecognizer()` wrapper; no continuous recognizer loop.
- J2 stops recognition on hide/cancel/destroy and never claims a wake phrase is implemented.
- Added dedicated J2 CI to compile, unit-test, lint, assemble and audit the one-permission/component boundary.
- Added secret-backed owner-signing compatibility to J1/J2 where owner signing secrets are configured.

Verified application baseline: J2 #18, J1 #122, Android CI #2013 and Governance #194 on `ef809bbdaca80f3b953483499dc03de8e091339f`; protected as `baseline/mayra-0.2.1-j2-voice-green-18`.

## Unreleased — Play Protect recovery and zero-permission J1 test

- Recorded the Motorola Play Protect block against the full sideloaded Personal Alpha.
- Stopped treating the full debug-signed Personal Alpha as the correct J1 installation path.
- Added a dedicated `j1AssistantTest` package: `ai.mayra.app.j1`.
- The J1 package contains only Assistant activation/status and Android VoiceInteraction service/session/recognition foundations.
- Removed chat, provider, contacts, reminders, notification listener, boot recovery, document and memory screens from J1.
- Added dedicated CI that hard-fails on any requested permission, extra launcher or forbidden component.
- J1 run #16 fixed an API-29 onboarding lint guard.
- J1 run #22 exposed AndroidX-inherited WorkManager/Startup permissions/components and hardened the manifest boundary.
- J1 run #38 proved ProfileInstaller still survived final manifest merging; the final audit was tightened until #44 passed.
- J1 #44 became the first verified zero-permission package baseline.
- Activation repair #56 added visible diagnostics; Motorola-route #68 corrected J1 metadata and used the proven Default Apps → Digital assistant path.
- Full Mayra remains on the stable owner-signing / trusted-distribution track.

## Unreleased — Simplified owner setup and stable updates

- Recorded the Motorola install failure where Personal Alpha #1851 could not update an older `ai.mayra.app.alpha` signed by another temporary CI debug certificate.
- Added a stable secret-backed owner signing configuration for Personal Alpha and engineering variants where configured.
- Added a dedicated `Stable Owner Alpha` GitHub workflow that requires protected signing secrets, verifies certificate provenance and records APK SHA-256.
- Added a small first-launch setup for the full owner app.
- Added install-over-install data-retention testing requirements.

## Jarvis Mode foundation and recovery hardening

- Added Android Assistant-role architecture foundation.
- Added `VoiceInteractionService`, session service and assistant metadata foundation.
- Added animated Mayra voice-session orb foundation.
- Added RecognitionService shell for future wake-word/speech pipeline.
- Declared lock-screen assistant-session support.
- Added governance, pinpoint audit, test matrix, rollback playbook and protected baselines.

Verified Jarvis J1 code baseline: Android CI #1851 and Project Governance #32 green on `0d9435adb92b425bfb47a710d4f4516a6aaac398`.

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
