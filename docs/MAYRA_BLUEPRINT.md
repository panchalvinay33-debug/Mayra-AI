# Mayra AI — Canonical Product Blueprint

Last updated: 2026-08-03
Status: Living architecture source of truth
Entry point: `START_HERE.md`

## Product vision

Mayra is the owner’s personal Android AI companion: a local-first, voice-capable assistant that can converse naturally, remember approved personal context, reason over private documents, manage reminders, coordinate supported phone actions and remain available through official Android system roles.

Cloud intelligence is optional. The long-term identity, personality and essential capabilities belong to Mayra’s on-device runtime.

## Experience target

Mayra should feel present rather than like a conventional utility app. When invoked while unlocked she can appear as a compact animated assistant surface that reacts while listening, thinking and speaking. When locked, Android/Motorola assistant policy controls what can appear without exposing private content. Routine setup stays small and understandable.

The engineering target is the maximum reliable experience supported by Android and the owner’s Motorola Edge 70 Fusion, not an unsupported movie-Jarvis claim.

## Non-negotiable principles

1. Local-first identity; a real local conversational model is benchmarked before selection.
2. Cloud providers are optional, never the sole brain.
3. One final user-facing Mayra app and one launcher.
4. J1/J2 or future engineering packages exist only for isolated proof and are not separate products.
5. Official Android roles/APIs before fragile hacks.
6. Device claims require actual Motorola evidence.
7. Destructive/irreversible actions retain bounded guards even in Owner Mode.
8. Credentials, private signing material and owner-private data never enter Git history.
9. Documentation, test evidence and recovery baselines are part of implementation.
10. Play Protect/signature checks are never bypassed.
11. Stable repeated owner updates require one private signing identity and proven update path.
12. PR #12 remains Draft/open/unmerged without explicit owner authorization.
13. Preflight completion means the safe path is documented; it never means the feature is delivered.

## System architecture

### 1. Interaction and presence

- Main Compose chat surface for the final app.
- Framework-native `VoiceInteractionSession` assistant surface; no general overlay permission required.
- Animated Mayra presence.
- Explicit listening/thinking/speaking state pipeline.
- Touch/Back/session-hide lifecycle always has a bounded exit.
- Internal Library, Memory, Provider, History and Setup screens.

### 2. Android Assistant / Jarvis layer

- `VoiceInteractionService` is the lightweight selected system-assistant service.
- `VoiceInteractionSessionService` owns visible invocation UI/heavier session work.
- `VoiceInteractionSession` renders over the current app.
- Motorola Digital Assistant selection is owner-controlled in Android Settings.
- Motorola Power-button assistant action is a separate device setting.
- Lock-screen support is declared, but actual behavior remains Android/Motorola policy and requires physical evidence.

The selected `VoiceInteractionService` stays lightweight. Continuous heavy model execution or a permanent Android SpeechRecognizer loop does not belong there.

### 3. J1 zero-permission Assistant proof

Engineering package: `ai.mayra.app.j1`.

Purpose: prove target-device Assistant recognition/selection/invocation independently of sensitive app capabilities.

Included:

- one small Assistant activation/status activity;
- VoiceInteractionService;
- VoiceInteractionSessionService and animated orb;
- recognition-service metadata shell.

Excluded: every requested Android permission and unrelated Mayra features/background infrastructure.

Motorola evidence proves selection, default-assistant state, configured Power-button invocation, unlocked orb rendering, Back dismissal and dismissal when the phone is locked.

J1 #68 exposed missing direct touch dismissal. The common session repair is now in the J2 green baseline; physical retest remains.

### 4. J2 invocation-time voice proof

Engineering package: `ai.mayra.app.j2`.

Purpose: prove short real spoken input after explicit Mayra invocation while keeping J1 as the clean zero-permission rollback proof.

Boundary:

- exactly `RECORD_AUDIO` permission;
- same Assistant/session foundation;
- no internet/provider, contacts, reminders, notifications, WorkManager, Room, documents, memory, full chat or call runtime;
- use Android on-device SpeechRecognizer only when Android reports it available;
- no continuous recognition loop;
- recognition starts only in an invoked session and stops on hide/cancel/destroy;
- transcript proof only; J2 does not pretend the local conversational brain is integrated.

Exact green application baseline:

- source `ef809bbdaca80f3b953483499dc03de8e091339f`;
- `baseline/mayra-0.2.1-j2-voice-green-18`;
- J2 #18, J1 #122, Android CI #2013 and Governance #194 all success.

Physical acceptance: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`.

### 5. Production wake phrase

Preflight: `docs/feasibility/MAYRA_WAKE_WORD_PREFLIGHT.md`.
Benchmark: `docs/testing/MAYRA_WAKE_WORD_BENCHMARK.md`.

Decision:

- Android SpeechRecognizer is not the production always-on hotword loop.
- A dedicated lightweight local keyword-spotting engine detects `Mayra`.
- First benchmark candidate is sherpa-onnx KWS; candidate status does not imply final selection.
- On confirmed wake, launch the existing bounded Assistant/J2-style voice path.
- Power-button Digital assistant invocation always remains the fallback.

Promotion requires Motorola false-accept/false-reject, screen-off/locked, reboot, RAM/thermal and long-idle battery evidence.

### 6. Local conversational brain

Preflight: `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`.
Benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`.

Current deterministic local engine remains the reliable fallback.

Initial benchmark direction:

- runtime: LiteRT-LM;
- first model candidate: Qwen3-1.7B;
- model is not final until actual Edge 70 Fusion storage/RAM/latency/thermal/quality tests pass;
- model asset should be separately downloadable/versioned/checksummed rather than blindly bloating the base APK.

Trust boundary:

`text/transcript → deterministic router → conversational model only where appropriate → typed result`

The free-form model does not directly execute calls/messages/device actions, write memory, mint confirmation tokens or replace structured document grounding.

If model load/generation fails or Android kills the heavy process, Mayra falls back cleanly to the deterministic local engine.

### 7. Optional cloud provider

- Responses-compatible bounded HTTPS transport.
- Android Keystore-protected credentials.
- Live enable/disable/remove behavior.
- Cancellation, bounded retries and local fallback.
- Provider output cannot directly execute actions or write personal memory.
- Local/private content is not silently uploaded merely because a provider exists.

### 8. Personal memory and documents

- Explicit local memory proposal/approval; inspect, replace, edit, expire and delete.
- Trusted structured memory-use metadata.
- TXT, PDF and DOCX import/extraction/indexing, grounded answers, summary, freshness and health.
- OCR and legacy `.doc` remain deferred.

### 9. Actions, app workflows and reminders

Preflight for cross-app automation: `docs/feasibility/MAYRA_APP_AUTOMATION_PREFLIGHT.md`.

Execution priority:

1. Android framework API/role;
2. documented app/provider API;
3. standard intent/deep link;
4. user-visible handoff;
5. only if separately justified, narrow deterministic Accessibility workflow.

A free-form LLM is never a generic tapping/swiping executor. Authentication, banking/payment, password/OTP and security-setting bypasses are excluded.

Existing actions/reminders:

- deterministic intent/capability gates;
- app opening/contact resolution;
- review-first dialer/message composer;
- exact-action expiring confirmations;
- WorkManager reminders with Complete/Snooze/follow-up/reboot recovery.

### 10. Notification intelligence

Preflight: `docs/feasibility/MAYRA_NOTIFICATION_INTELLIGENCE_PREFLIGHT.md`.

Architecture:

- explicit Android Notification Access / NotificationListenerService;
- local-first processing;
- owner app exclusions;
- sensitive OTP/banking/auth/security content excluded from autonomous action and broad cloud processing by default;
- no indefinite raw notification archive by default;
- notification-derived reminder/action remains a typed owner-controlled proposal.

### 11. Phone and call control

Preflight: `docs/feasibility/MAYRA_PHONE_ROLE_PREFLIGHT.md`.

Correct path:

- complete default Phone/Dialer runtime;
- Dialer activity plus incoming/ongoing call UI;
- `InCallService` for supported answer/reject/disconnect/mute/audio endpoint control;
- optional Call Screening for fast screening/ID/silence/reject rules;
- deterministic voice command state machine;
- emergency/lost-role/restore-previous-Phone behavior.

Hard gate: owner-facing `Make Mayra default Phone` does not appear until the complete call UI/runtime is already CI-green.

### 12. AI caller message-taking

Preflight: `docs/feasibility/MAYRA_AI_CALLER_MESSAGE_PREFLIGHT.md`.

Ordinary InCallService does not solve arbitrary remote SIM-call audio capture/injection. Mayra therefore does not claim hidden/direct cellular AI answering.

Preferred supported architecture to investigate:

- carrier forwarding to a Mayra-controlled telephony/VoIP endpoint;
- or a Mayra-owned VoIP number/account;
- or another proven carrier voicemail integration.

That endpoint can provide greeting/disclosure, capture a caller message, transcribe/summarize and deliver it securely to the owner. Privacy, disclosure, retention, backend cost/reliability and deletion controls must be defined first.

### 13. Trusted installation, signing and updates

Preflight: `docs/feasibility/MAYRA_TRUSTED_INSTALL_PREFLIGHT.md`.

Packages:

- J1: `ai.mayra.app.j1`, zero permissions;
- J2: `ai.mayra.app.j2`, RECORD_AUDIO only;
- Personal Alpha: `ai.mayra.app.alpha`, full owner engineering candidate;
- Full Test: engineering regression package;
- Document Test: isolated document regression package;
- Final: `ai.mayra.app`.

Policy:

- hosted-runner debug certificates are disposable only;
- repeated owner candidates use one stable private owner certificate supplied only through protected secrets;
- every promoted artifact records source/package/version/hash and certificate provenance where available;
- A→B install-over-install data-retention proof is mandatory;
- preferred full owner testing path is Google Play Internal Testing once Play Console/signing setup is ready;
- owner-signed APK remains a controlled recovery/test option;
- Play Protect is never disabled/bypassed.

### 14. Engineering governance and recovery

- `START_HERE.md` is mandatory entry point.
- Roadmap, rolling snapshot, Idea Ledger, Decisions and Changelog are synchronized with meaningful work.
- Issue #14 is a permanent preflight gate for every new major capability.
- Major exact-green transitions get protected `baseline/*` branches and immutable snapshots.
- Failed heads remain history/evidence; they are never promoted or hidden.
- PR #12 stays Draft/unmerged until explicit owner authorization.

## Current module status

| Module | Status | Truth |
|---|---|---|
| Core routing/conversation | DEVICE_VERIFY | Deterministic local engine + optional provider implemented; local LLM benchmark pending |
| Personal memory | DEVICE_VERIFY | Controlled lifecycle implemented; Motorola checks pending |
| Documents | DEVICE_VERIFY | TXT/PDF/DOCX implemented; physical checks pending |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery implemented |
| App/contact actions | DEVICE_VERIFY | App open and review-first handoffs implemented |
| Provider | DEVICE_VERIFY | HTTPS/Keystore/live composition implemented |
| Stable owner updates | IN_PROGRESS | Secret-backed signing path + trusted-install preflight exist; private secrets/A→B test pending |
| J1 Assistant role proof | DEVICE_VERIFY | Selection/unlocked invocation physically proven; touch/locked-start/reboot completion pending |
| Animated Mayra presence | DEVICE_VERIFY | Orb physically invoked; touch-dismiss repair is J2 CI-green, device retest pending |
| J2 invocation-time voice | DEVICE_VERIFY | Exact-source J2/J1/Android/Governance green; Motorola speech/lifecycle proof next |
| Wake phrase | BENCHMARK | Dedicated KWS architecture documented; sherpa-onnx first candidate only |
| Local conversational model | BENCHMARK | LiteRT-LM/Qwen3-1.7B initial direction; Motorola benchmark required |
| Notification intelligence | ACCEPTED | Local-first Notification Access architecture documented; device/privacy validation pending |
| App workflow automation | ACCEPTED | APIs/intents-first boundary documented; typed adapters added per workflow |
| Incoming-call control | ACCEPTED | Complete Phone/InCallService runtime required before role request |
| AI caller message-taking | ACCEPTED_WITH_CONSTRAINTS | Supported forwarding/VoIP route required; direct arbitrary SIM audio rejected |
| Production release | IN_PROGRESS | R8/signing scaffold implemented; trusted signed distribution pending |

## Milestone completion rule

A milestone is complete only when implementation/failure tests, compile/unit/lint/R8/package audits, synchronized governance records and applicable Motorola evidence pass. Major boundaries receive immutable snapshots and protected baselines.

## Change-control rule

Every meaningful coding batch updates Roadmap and Latest Snapshot. Architecture/background/core changes update this blueprint and Decisions. Feature changes update Idea Ledger. Build/release/manifest changes update Changelog. Governance CI enforces the contract.
