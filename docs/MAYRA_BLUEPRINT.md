# Mayra AI — Canonical Product Blueprint

Last updated: 2026-08-03
Status: Living architecture source of truth
Entry point: `START_HERE.md`

## Product vision

Mayra is the owner’s personal Android AI companion: a local-first, voice-capable assistant that can converse naturally, remember approved personal context, reason over private documents, manage reminders, coordinate supported phone actions and remain available through official Android system roles.

Cloud intelligence is optional. The long-term identity, personality and essential capabilities belong to Mayra’s on-device runtime.

## Experience target

Mayra should feel present rather than like a conventional utility app. When invoked while unlocked she can appear as a compact animated assistant surface that reacts while listening, thinking and speaking. When locked, Android/Motorola assistant policy should control what can appear without exposing private content. Routine setup must stay small and understandable.

The target is the maximum reliable experience supported by Android and the owner’s Motorola Edge 70 Fusion, not an unsupported movie-Jarvis claim.

## Non-negotiable principles

1. Local-first identity; a real local conversational model remains future work.
2. Cloud providers are optional, never the sole brain.
3. One final user-facing Mayra app and one launcher.
4. Engineering-only J1/J2 packages may exist for isolated device proof, then disappear from the final product.
5. Official Android roles/APIs before fragile hacks.
6. Device claims require actual Motorola evidence.
7. Destructive/irreversible actions retain bounded guards even in Owner Mode.
8. Credentials, private signing material and owner-private data never enter Git history.
9. Documentation and recovery baselines are part of implementation.
10. Play Protect and Android signature checks are not bypassed.
11. Stable repeated owner updates require one private signing certificate.
12. PR #12 remains Draft/open/unmerged without explicit owner authorization.

## System architecture

### 1. Interaction and presence

- Main Compose chat surface for the final app.
- Framework-native `VoiceInteractionSession` assistant surface; no general overlay permission required.
- Animated Mayra orb/presence.
- Explicit listening/thinking/speaking state pipeline.
- Touch/Back/session-hide lifecycle must always remain recoverable.
- Internal Library, Memory, Provider, History and Setup screens.

### 2. Android Assistant / Jarvis layer

- `VoiceInteractionService` is the lightweight selected system-assistant service.
- `VoiceInteractionSessionService` owns visible interaction UI/heavier invocation work.
- `VoiceInteractionSession` provides the assistant surface over the current app.
- Motorola Digital Assistant selection is owner-controlled through Android Settings.
- Motorola Power-button assistant trigger is a separate device setting and must be configured by the owner.
- Lock-screen support is declared, but actual exposure remains governed by Android/Motorola policy and must be physically tested.

The selected `VoiceInteractionService` must remain lightweight. Continuous heavy model execution or a permanent `SpeechRecognizer` loop does not belong in that service.

### 3. J1 zero-permission Assistant proof

Engineering package: `ai.mayra.app.j1`.

Purpose: answer only whether the target Motorola can recognize, select and invoke Mayra as the Android Digital Assistant.

Included:

- one small Assistant activation/status activity;
- `VoiceInteractionService`;
- `VoiceInteractionSessionService` and animated orb;
- recognition-service metadata shell.

Excluded:

- every requested Android runtime permission;
- provider/internet;
- contacts;
- reminders/notifications;
- notification listener and boot receiver;
- documents, memory, history and full chat runtime.

Device evidence now proves:

- J1 installs and launches;
- Android 16 recognizes Mayra as a valid Digital assistant candidate;
- Mayra can be selected as the default Digital assistant;
- Motorola Power-button Digital assistant trigger can invoke Mayra;
- Mayra’s orb/session renders over the current screen while unlocked;
- Back and phone lock dismiss the session.

Observed J1 UX failure:

- the first orb implementation did not dismiss on orb/outside tap because no click listener existed.
- repair adds root/orb/label tap-to-hide, Back-to-hide, animation stop on hide and clean restart on the next show.
- lock-screen invocation, repeated invoke/dismiss stability and reboot recovery are still pending physical evidence.

### 4. J2 invocation-time voice proof

Engineering package: `ai.mayra.app.j2`.

Purpose: prove real short spoken input after explicit Mayra invocation without contaminating the zero-permission J1 baseline.

Boundary:

- exactly one runtime permission: `RECORD_AUDIO`;
- same Assistant/session foundation as J1;
- no internet, contacts, reminders, notifications, background listener, WorkManager, Room or full Mayra runtime;
- prefer `SpeechRecognizer.createOnDeviceSpeechRecognizer()` only when Android reports on-device recognition available;
- no continuous recognition loop;
- recognizer starts only for a visible/invoked J2 session and stops on hide/cancel/destroy;
- transcript proof displays only what was heard; J2 does not pretend a local conversational brain answered yet.

J2 preflight: `docs/feasibility/MAYRA_J2_VOICE_PREFLIGHT.md`.

### 5. Wake phrase architecture

A true offline wake phrase remains separate from J2 speech recognition.

Do not use Android `SpeechRecognizer` as an always-on hotword loop. A future dedicated wake-word detector needs its own Issue #14 review covering:

- engine/license/model size;
- microphone/background behavior;
- false-positive/false-negative threshold;
- screen-off/lock-screen behavior;
- battery and thermal budget;
- explicit owner stop/disable control.

### 6. Local brain

Current:

- deterministic local intent/command engine;
- contextual offline fallback;
- local routing, memory, documents, reminders and actions.

Required future:

- benchmarked on-device language model suitable for Edge 70 Fusion RAM/thermal limits;
- local Hindi/Hinglish conversation and summarization;
- bounded context/memory retrieval;
- model integrity/storage/version controls;
- graceful smaller-model/rule fallback.

No model is selected until the local-model feasibility gate records license, quantization, RAM, latency, storage, battery and thermal evidence.

### 7. Optional cloud provider

- Responses-compatible bounded HTTPS transport.
- Android Keystore-protected credentials.
- Live enable/disable/remove behavior.
- Cancellation, bounded retries and local fallback.
- Provider output cannot directly execute actions or write personal memory.

### 8. Personal memory and documents

- Explicit local memory proposal/approval; inspect, replace, edit, expire and delete.
- Trusted structured memory-use metadata.
- TXT, PDF and DOCX import/extraction/indexing, grounded answers, summary, freshness and health.
- OCR and legacy `.doc` remain deferred.

### 9. Actions and reminders

- Deterministic intent/capability gates.
- App opening and contact resolution.
- Review-first dialer/message composer.
- Exact-action expiring confirmations.
- Persistent WorkManager reminders with Complete, Snooze, follow-up and reboot recovery.
- Exact alarms deferred until device tests prove need.

### 10. Phone and call layer

Current: outgoing dialer/message handoffs only.

Planned after separate feasibility review:

- complete default Phone/Dialer UI contract;
- `InCallService` for supported answer/reject/disconnect/mute/audio-route control;
- caller announcement/contact lookup;
- optional Call Screening for supported identify/silence/reject logic;
- emergency-call fallback behavior.

Protected cellular audio capture, secret recording and arbitrary AI/TTS injection into SIM calls are not assumed. True AI caller message-taking requires a supported voicemail/VoIP/call-forwarding architecture.

### 11. Packaging, signing, testing and release

Packages:

- J1 Assistant Test: `ai.mayra.app.j1`, zero requested permissions.
- J2 Voice Test: `ai.mayra.app.j2`, only `RECORD_AUDIO`.
- Personal Alpha: `ai.mayra.app.alpha`, full owner-capability engineering candidate.
- Full Test: lower-permission UI regression package.
- Document Test: isolated document regression package.
- Final release: `ai.mayra.app`, non-debuggable, minified/R8 and audited.

Signing/distribution policy:

- hosted-runner debug certificates are disposable;
- J1/J2/Personal Alpha can use the secret-backed owner signing configuration when private secrets exist;
- every stable artifact records package/certificate/APK SHA-256 provenance;
- install-over-install/data-retention proof is mandatory before treating an owner channel as stable;
- full Mayra should use Play Internal Testing or another trusted owner-controlled signed channel when sideload trust is inadequate;
- no keystore/password/private key is committed.

Required CI surfaces now include Android CI, J1 Assistant Test, J2 Voice Test and Project Governance where applicable.

## Current module status

| Module | Status | Truth |
|---|---|---|
| Core routing/conversation | DEVICE_VERIFY | Local deterministic engine + optional provider implemented; local LLM pending |
| Personal memory | DEVICE_VERIFY | Controlled lifecycle implemented; Motorola checks pending |
| Documents | DEVICE_VERIFY | TXT/PDF/DOCX implemented; physical checks pending |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery implemented |
| App/contact actions | DEVICE_VERIFY | App open and review-first handoffs implemented |
| Provider | DEVICE_VERIFY | HTTPS/Keystore/live composition implemented |
| Stable owner updates | IN_PROGRESS | Secret-backed signing path exists; private secrets/update test pending |
| J1 Assistant role proof | DEVICE_VERIFY | Selection and unlocked invocation physically proven; lifecycle/lock-screen/reboot pending |
| Animated Mayra presence | DEVICE_VERIFY | Orb physically invoked on Motorola; touch-dismiss repair awaiting fresh CI/device retest |
| J2 invocation-time voice | IN_PROGRESS | Preflight, isolated variant, state pipeline and on-device recognizer foundation committed; CI/device proof pending |
| Offline wake phrase | PLANNED | Separate dedicated engine required; SpeechRecognizer loop explicitly rejected |
| Local conversational model | PLANNED | Benchmark/preflight pending |
| Incoming-call control | PLANNED | Default Phone/InCallService preflight required |
| Production release | IN_PROGRESS | R8/signing scaffold implemented; trusted signed distribution pending |

## Milestone completion rule

A milestone is complete only when implementation/failure tests, compile/unit/lint/R8/package audits, synchronized governance records and applicable Motorola evidence pass. Major boundaries receive immutable snapshots and protected baselines.

## Change-control rule

Every meaningful coding batch updates Roadmap and Latest Snapshot. Architecture/background/core changes update this blueprint and Decisions. Feature changes update Idea Ledger. Build/release/manifest changes update Changelog. Governance CI enforces the contract.
