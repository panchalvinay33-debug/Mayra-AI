# Mayra AI — Decision Log

Last updated: 2026-08-04

This log preserves important product and architecture decisions. Newer entries may supersede older ones, but history is not deleted.

## Decision format

Each entry records: decision, reason, consequences, validation state and supersession status.

---

## ADR-001 — One user-facing application

**Status:** Accepted

Mayra must present one launcher icon and one coherent app. Library, Memory, Provider, History and Device controls remain internal screens. Separate packages may exist only for CI/device testing.

---

## ADR-002 — Local-first, cloud-optional intelligence

**Status:** Accepted; local LLM pending

Mayra must retain offline commands, memory, documents, reminders and device actions without an API key. Cloud providers are optional intelligence boosters.

---

## ADR-003 — Remote providers are text-only

**Status:** Implemented

A remote conversational provider may produce response text but cannot directly execute device actions or write personal memory.

---

## ADR-004 — Trusted memory attribution is structured metadata

**Status:** Implemented; supersedes visible marker design

Memory keys used in a response travel through `MayraAssistantResponse.usedPersonalMemoryKeys`, not hidden strings embedded in visible text.

---

## ADR-005 — Review-first call and message handoffs

**Status:** Implemented for current action layer

Outgoing call and message commands open Android’s dialer/composer after resolution and confirmation. Advanced incoming-call control will use official Phone roles.

---

## ADR-006 — Mayra-owned reminders use WorkManager

**Status:** Implemented

Mayra persists reminders locally and schedules through WorkManager, with Complete, Snooze, follow-up and reboot/update recovery.

---

## ADR-007 — Confirmation tokens are exact, expiring and process-local

**Status:** Implemented

Sensitive actions use one-time tokens bound to the exact action and short expiry. Raw tokens are not persisted across process death.

---

## ADR-008 — Provider credentials use Android Keystore protection

**Status:** Implemented

API credentials are encrypted with an Android Keystore-backed key and are never stored in source control or ordinary settings.

---

## ADR-009 — Jarvis availability uses official Android Assistant role

**Status:** Device-proven foundation

Always-available Mayra is built around `VoiceInteractionService` / Android Digital Assistant role and `VoiceInteractionSession`, not a general overlay or unrestricted permanently running microphone service.

Motorola evidence proves Mayra can be selected as the target device’s Digital assistant and invoked through the configured Power-button assistant trigger.

---

## ADR-010 — Advanced call control uses official Phone/Call Screening roles

**Status:** Accepted; implementation gated

Caller announce, answer, reject, silence, mute and speaker operations use a complete default Phone/InCallService runtime where supported. Call Screening is an optional fast screening/identification layer, not the mechanism for waiting on a spoken owner decision.

---

## ADR-011 — Cellular AI answering/recording is not assumed

**Status:** Accepted constraint

Mayra will not claim it can inject AI speech into, or secretly record/transcribe, arbitrary SIM-call audio through standard public Android APIs.

---

## ADR-012 — Personal Owner Mode does not mean zero safeguards

**Status:** Accepted

The app may streamline routine actions for its single owner, but broad destructive operations, credential handling and irreversible changes retain clear guards.

---

## ADR-013 — Documentation is part of every feature

**Status:** Accepted and CI-enforced

Meaningful code/architecture work must update the roadmap and rolling snapshot; affected blueprint/idea/decision/changelog records must also be synchronized.

---

## ADR-014 — Owner APKs require stable signing

**Status:** Implemented foundation; secret setup and upgrade test pending

Personal Alpha and repeated engineering APKs intended for owner-device upgrades should use one stable signing certificate supplied through protected secrets. Hosted-runner debug certificates are temporary and must not be presented as update-compatible.

---

## ADR-015 — First launch uses one minimal owner setup

**Status:** Implemented; trusted full-app device validation pending

The final Mayra app will present a small first-launch setup: essential runtime permissions, Android Assistant activation, then Start Mayra. Android may still show its own mandatory system dialogs.

---

## ADR-016 — J1 Assistant proof uses a zero-permission isolated APK

**Status:** Device-proven for selection/unlocked invocation; lifecycle completion pending

Assistant-role compatibility is tested separately from the full sensitive-capability app.

**Decision:** `ai.mayra.app.j1` contains only Assistant activation/status and VoiceInteraction service/session/orb foundations and requests zero Android permissions.

**Consequences:** dedicated CI rejects every permission, extra launcher and unrelated feature/background component; Play Protect/signature checks are never bypassed.

---

## ADR-017 — J2 voice proof is isolated from J1

**Status:** Core device accepted; privacy/TTS candidate CI-green

J1 remains the zero-permission proof baseline. Real invocation-time voice uses the separate engineering package `ai.mayra.app.j2`.

J2 requests exactly `RECORD_AUDIO` and excludes internet, contacts, notifications, reminders, WorkManager/Room/background listeners and the full Mayra runtime. The core voice path is physically proven on Motorola; CI #136 adds privacy/TTS progress without broadening the action boundary.

---

## ADR-018 — Android SpeechRecognizer is invocation-time STT, not the wake-word engine

**Status:** Accepted and implemented in J2 foundation

`SpeechRecognizer.createOnDeviceSpeechRecognizer()` is used only for a short visible/invoked assistant session when Android reports on-device recognition available.

It stops on result/error/cancel/hide/destroy and is never looped continuously in the always-running `VoiceInteractionService`.

---

## ADR-019 — Assistant UI must always have a bounded exit

**Status:** Device-proven

Every visible Mayra assistant session must be dismissible through reliable Android navigation and direct surface interaction. Back, orb tap, label tap and outside/root tap call `hide()`. Session hide/destroy stops recognition, keep-awake state and animations.

---

## ADR-020 — Production wake phrase uses a dedicated local KWS engine

**Status:** Preflight complete; benchmark only

The production `Mayra` wake phrase will use a dedicated lightweight local keyword-spotting engine, not an endless Android SpeechRecognizer loop.

`sherpa-onnx` is the first benchmark candidate because it has Android/offline keyword-spotting support, but it is not selected until false-accept, false-reject, latency, memory, battery and lock-screen tests pass on the Motorola.

Power-button Digital assistant invocation remains the permanent fallback.

---

## ADR-021 — Local conversational brain is benchmarked before model selection

**Status:** Preflight complete; benchmark only

The first Android runtime direction to evaluate is LiteRT-LM. Qwen3-1.7B is an initial model candidate, not a final model decision.

The model stays outside privileged trust boundaries: deterministic Mayra routing continues to own actions, reminders, memory writes and document grounding. A local model failure must fall back to the deterministic local engine.

Model distribution should use a separately versioned/checksummed downloadable asset rather than blindly inflating the base APK.

---

## ADR-022 — Default Phone role is requested only after complete call runtime exists

**Status:** Accepted; owner-facing role request blocked

Mayra will not expose `Make Mayra default Phone` until a complete Dialer/Incoming/Ongoing call UI, InCallService lifecycle, answer/reject/hang-up, mute, audio endpoint routing, lost-role handling and emergency fallback are already CI-green.

Voice call commands are deterministic typed commands, not direct free-form LLM execution.

---

## ADR-023 — AI caller message-taking uses supported telephony routing

**Status:** Accepted constraint; architecture research only

For `Mayra talks to caller and takes a message`, the preferred architecture is carrier call forwarding to a Mayra-controlled VoIP/telephony endpoint, a Mayra-owned VoIP number, or another explicitly supported carrier voicemail path.

The app does not pretend ordinary InCallService grants remote SIM-call audio capture/injection. Privacy, disclosure, retention and backend reliability must be defined before real deployment.

---

## ADR-024 — Notification intelligence is explicit and local-first

**Status:** Preflight complete

Notification intelligence uses Android Notification Access / `NotificationListenerService`, not Accessibility scraping. Sensitive notifications such as OTP, banking, authentication and security content are excluded from autonomous actions and broad cloud processing by default.

Owner controls include enable/disable and app exclusions. Raw notification content is not kept indefinitely by default.

---

## ADR-025 — App automation uses supported APIs/intents before Accessibility

**Status:** Preflight complete

Workflow execution priority is official Android APIs/roles → documented app/provider API → standard intents/deep links → user-visible handoff.

Any future Accessibility workflow must be narrow, deterministic and individually reviewed. A free-form LLM cannot become a generic screen-tapping engine, and authentication/payment/security flows are excluded.

---

## ADR-026 — Trusted owner distribution uses stable signing plus a trusted update channel

**Status:** Architecture accepted; operational setup pending

Repeated owner installs require one stable private signing identity. The preferred full-app owner test channel is Google Play Internal Testing once Play Console/signing setup is complete; a controlled owner-signed APK remains a recovery/test option.

A→B install-over-install data-retention proof is mandatory. Play Protect is never disabled or bypassed. Ephemeral CI debug signing must never be labeled stable/update-compatible.

---

## ADR-027 — On-device speech support and language support are separate capabilities

**Status:** Device-proven and implemented

Android reporting `SpeechRecognizer.isOnDeviceRecognitionAvailable()` does not prove that the implicit/default speech language is installed or supported. Motorola J2 device testing proved this distinction.

Mayra uses explicit bounded locale negotiation and Android 13+ support probing; there is no silent cloud STT fallback or continuous retry loop.

---

## ADR-028 — Locked speech is private and J2 voice actions remain confirmation-only

**Status:** CI #136 green; Motorola privacy/TTS retest pending

When Android reports the device locked, Mayra may show a generic listening state but must not display or speak transcript-derived/private content before unlock. This uses normal keyguard state detection only.

The current speaking engine is offline-first Android TTS with preferred voice order Hindi India → English India → English US → another offline voice. A custom neural TTS model is a future benchmark rather than a dependency for the Assistant foundation.

J2 may understand app/reminder intent and speak confirmation-oriented wording, but it does not execute those actions or claim they succeeded. Execution will be connected only through the existing typed Mayra action/confirmation runtime after the voice bridge is device-proven.

Small physical checks should be consolidated into meaningful regression rounds when safe so owner testing overhead does not stall feature progress.
