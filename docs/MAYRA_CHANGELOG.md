# Mayra AI — Changelog

This changelog records meaningful user-visible and engineering milestones. It does not replace Git history.

## Unreleased — J5 AI-native Home foundation

- Added a separate `MayraLauncherActivity` so Android Home rendering is independent from the normal chat/activity surface.
- Declared `MAIN + HOME + DEFAULT` qualification for the Mayra Home surface while keeping exactly one normal `CATEGORY_LAUNCHER` app entry.
- Added user-consent `ROLE_HOME` request where the Android role is available.
- Added searchable launchable-app discovery and direct app launching.
- Added deterministic case-insensitive app-search tests by label/package.
- Added an `Ask Mayra` bridge into the existing normal Mayra experience.
- Added an explicit Android Home-settings switch/restore path so the owner can return to another launcher.
- The J5 Home rendering path intentionally does not initialize the local LLM, cloud provider, memory or privileged action engine.
- Hardened Android package audits so Personal Alpha and minified Release must contain `MayraLauncherActivity` plus HOME/DEFAULT categories while retaining exactly one normal app LAUNCHER entry.
- Exact J5 source `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a` passed Android #2384, J1 #493, J2 #389, J3 #211, J4 #162 and Governance #565.
- Preserved the exact CI-green checkpoint as `backup/j5-home-contract-ci-green-2026-08-05` and immutable snapshot `docs/backups/MAYRA_SNAPSHOT_2026-08-05_J5_HOME_CONTRACT_CI_GREEN.md`.
- Motorola test artifact: Android #2384 Personal Alpha ID `8919388343`; extracted APK SHA-256 `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`.
- J5 is now `DEVICE_VERIFY`, not yet a protected promoted baseline. Physical default-HOME, reboot, app access, switch-back, AI-failure independence and resource evidence remain required.

## Unreleased — J4 quality/operability harness

- Repaired the Room/KSP multi-variant schema race by isolating transient Room schema generation and serializing governed variant builds.
- Protected exact recovery source `e72488a6f6dceb24950f9b0f574ae223d52bd8bb` as `baseline/mayra-0.2.1-j4-ci-recovery-green-134` after Android #2356, J1 #465, J2 #361, J3 #183, J4 #134 and Governance #537 all passed.
- Added longer Hindi/Hinglish/English quality prompts plus explicit safety-boundary and uncertainty prompts.
- Added 10-prompt sequential local-brain stress benchmarking.
- Added response character count, approximate-token count, total latency and clearly labelled rough throughput estimates.
- Added isolated `:localbrain` process PSS, Java heap and native heap telemetry.
- Added explicit process-bounded generation cancellation: only the isolated localbrain process is killed/rebound so the outer UI survives.
- Quality engineering source `862450933da3700d4d1559e09ebde910a4185914` passed Android #2364, J1 #473, J2 #369, J3 #191, J4 #142 and Governance #545.
- J4 #142 runtime APK artifact ID `8918003689`; audit artifact ID `8918004266`.
- Motorola quality/RAM/thermal/background/lock/Airplane evidence remains required before production conversational-brain promotion.

## Unreleased — Free offline neural voice benchmark foundation

- Motorola CI #136 physically speaks Mayra replies through Android offline TTS; owner reports the voice is understandable but too robotic for the desired final Mayra experience.
- Added `MayraSpeechOutput` so the Assistant can swap speech engines without rewriting the voice-session logic.
- `MayraOfflineTtsSpeaker` now implements that contract and remains the zero-cost fallback.
- Added `MayraVoicePackPolicy` with explicit `APPROVED`, `BENCHMARK_ONLY` and `BLOCKED` license gates; a freely downloadable model is not automatically production-eligible.
- Added unit tests preventing the first Hindi Piper/VITS benchmark model from being treated as production-approved while its model card cites a non-commercial dataset.
- Added `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md` covering free/offline requirements, sherpa-onnx runtime choice, model/license cautions and Motorola performance gates.
- Added `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md` with fixed Hindi/Hinglish phrases and latency/RAM/storage/thermal/privacy/stability measurements.
- Sherpa-ONNX is the preferred first Android neural runtime candidate; no neural binary/dependency has yet been promoted into the normal J2 path.
- Hindi Priyamvada Medium (~63.5 MB) is benchmark-only; Indic Parler-TTS and IndicF5 remain later quality/research candidates.
- No paid TTS API, subscription or per-character billing is accepted as the primary Mayra voice architecture.

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
