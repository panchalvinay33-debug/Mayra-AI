# Mayra AI — Canonical Product Blueprint

Last updated: 2026-08-03
Status: Living architecture source of truth
Entry point: `START_HERE.md`

## Product vision

Mayra is the owner’s personal Android AI companion: a local-first, voice-capable assistant that can converse naturally, remember approved personal context, reason over private documents, manage reminders, coordinate supported phone actions and remain available through official Android system roles.

Mayra is not intended to be merely an OpenAI client or a chat screen. A cloud provider is optional. The long-term identity, personality and essential capabilities belong to Mayra’s on-device runtime.

## Experience target

When the phone is unlocked, Mayra may appear as a compact animated presence that reacts while listening, thinking and speaking. When the display is off or locked, Mayra should use voice/background behavior without forcing the full app UI. The owner should be able to invoke Mayra, ask questions, create reminders, open apps, use memory/documents and—after optional system-role setup—control supported incoming-call operations.

The project targets the maximum reliable experience supported by Android and the owner’s Motorola device. It does not claim unrestricted movie-Jarvis control or protected platform capabilities without device evidence.

## Non-negotiable principles

1. **Local-first identity.** Core Mayra functions continue without an API key; a local conversational model is a required future milestone.
2. **Cloud optionality.** Remote providers may improve difficult answers but cannot become the only brain.
3. **One coherent app.** One user-facing launcher; engineering variants are not separate products.
4. **Owner control.** Privileged Android roles, memory, provider and background modes require explicit owner setup and remain disableable.
5. **Official roles before fragile hacks.** Android Assistant, Phone and Call Screening roles are preferred over unrestricted accessibility/root automation.
6. **Truthful actions.** Mayra never claims that a call connected, message sent, reminder fired or device test passed without evidence.
7. **Typed trust boundaries.** Remote/document text cannot directly execute actions, write memory or spoof memory provenance.
8. **Recoverable safeguards.** Routine owner-approved actions may be streamlined; broad destructive/irreversible operations retain clear guards.
9. **Secret isolation.** Credentials, private keys, signing material and owner-private data never enter Git history or project records.
10. **Documentation is implementation.** Blueprint, roadmap, latest snapshot, idea ledger, decisions and changelog stay synchronized through CI governance.
11. **Physical claims require physical evidence.** CI verification and Motorola acceptance are separate statuses.
12. **Draft/merge truth.** PR #12 remains Draft/unmerged until explicit owner authorization.

## System architecture

### 1. Interaction and presence layer

- Main Compose Chat surface.
- Animated Mayra presence for assistant voice sessions.
- Listening, thinking, speaking, alert and error states.
- Controlled microphone permission and speech recognition.
- Text-to-speech output.
- Lock-screen/background voice interaction through official Assistant role where supported.
- Main internal screens: Library, Memory, Provider, History and Device readiness.

### 2. Assistant-role / Jarvis layer

- Android `VoiceInteractionService` as the official system-assistant entry point.
- `VoiceInteractionSessionService` and session UI for overlay-like assistant presence without generic overlay permission.
- Recognition-service boundary for wake-word/speech integration.
- Owner-triggered Assistant-role selection; Mayra never silently becomes default.
- Future offline wake-word engine with explicit battery, thermal and privacy policy.

### 3. Local brain layer

Current:

- deterministic local intent/command engine;
- contextual offline fallback;
- local routing, memory, documents, reminders and actions.

Required future:

- benchmarked on-device language model suitable for Motorola RAM/thermal limits;
- local Hindi/Hinglish conversation and summarization;
- bounded context and memory retrieval;
- model download/update integrity and storage controls;
- graceful smaller-model/rule fallback.

### 4. Optional conversational-provider layer

- `MayraConversationalProvider` remains text-only.
- OpenAI Responses-compatible bounded HTTPS transport is implemented.
- Credentials are protected by Android Keystore-backed encryption.
- Owner can enable, disable, replace or remove provider configuration live.
- Cancellation, bounded retries, response limits and local fallback are mandatory.
- Provider output cannot execute actions or write personal memory directly.

### 5. Personal memory layer

- Memory proposals are explicit and locally approved.
- Owner can inspect, replace, edit, expire and delete memories.
- Storage failures are surfaced honestly rather than presented as empty data.
- Used memory keys travel in trusted structured metadata, not visible-text markers.
- Future Owner Mode may allow category-specific auto-save rules, but owner controls and delete/export remain available.

### 6. Document intelligence layer

- TXT, PDF and DOCX import/extraction/indexing.
- Current-index-only search and grounded answers.
- Summary, snippets, health, freshness and maintenance tools.
- Scanned OCR and legacy `.doc` remain deferred until promoted in the idea ledger.
- Private document content stays on device unless the owner explicitly enables a future bounded remote-document feature.

### 7. Action and automation layer

- Deterministic intent parsing and capability gates.
- Installed-app opening.
- Contact resolution.
- Review-first dialer and message-composer handoffs.
- One-time exact-action confirmation tokens with expiry.
- Idempotency and activity recording.
- Owner Mode may reduce confirmations for specifically trusted low-risk actions; broad destructive actions remain guarded.

### 8. Reminder layer

- Persistent Mayra-owned reminder store.
- Hindi/Hinglish/English parsing and clarification.
- WorkManager scheduling with revision-safe workers.
- Complete, Snooze, follow-up and missed states.
- Reboot/app-update recovery with remaining follow-up delay.
- Exact-alarm privilege remains deferred unless Motorola testing demonstrates a real owner need.

### 9. Phone and call layer

Current:

- outgoing contact resolution and dialer handoff;
- message composer handoff.

Planned optional privileged-role module:

- default Phone/InCallService role;
- incoming caller announcement;
- answer, reject, silence, mute and speaker/audio-endpoint control;
- Call Screening role for owner-defined screening rules;
- complete fallback call UI required by Android role expectations.

Not assumed:

- arbitrary cellular-call audio capture;
- secret recording;
- reliable third-party TTS injection into SIM call audio.

AI message-taking should use a documented supported device route or owner-controlled voicemail/VoIP architecture.

### 10. Background and proactive layer

- Boot/update recovery for reminders and approved schedules.
- Notification listener only behind Android special-access consent.
- Owner-defined briefing/proactive rules with relevance and frequency controls.
- No permanent unrestricted microphone service; system-assistant/hotword path must respect Android background restrictions and device battery.

### 11. Packaging, testing and release layer

- Personal Alpha: owner-capability debug-signed test package.
- Full Test: lower-permission UI regression package.
- Document Test: isolated zero-permission document regression package.
- Final `ai.mayra.app` release: non-debuggable, minified/R8, resource-shrunk and permission/component audited.
- Production signing values come only from secure environment/secrets.
- Android CI and Project Governance CI are both required.

## Current module status

| Module | Status | Truth |
|---|---|---|
| Core routing/conversation | DEVICE_VERIFY | Local engine + optional provider implemented; broad local LLM pending |
| Personal memory | DEVICE_VERIFY | Full controlled lifecycle implemented; Motorola checks pending |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX foundation implemented; physical checks pending |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery implemented; OEM timing pending |
| App/contact actions | DEVICE_VERIFY | App open and review-first call/message handoff implemented |
| Provider | DEVICE_VERIFY | HTTPS/Keystore/live composition implemented; real owner key test pending |
| Animated Mayra presence | IN_PROGRESS | Assistant-session UI foundation added |
| Android Assistant role | IN_PROGRESS | Services/metadata foundation added; CI/device role test pending |
| Offline wake word | PLANNED | Recognition shell only |
| Local conversational model | PLANNED | Model selection/benchmark pending |
| Incoming-call control | PLANNED | Requires optional Phone/Call Screening roles |
| AI caller message-taking | PLANNED_WITH_CONSTRAINTS | Voicemail/VoIP or documented device path required |
| Production release | IN_PROGRESS | R8/signing scaffold implemented; private signing and distribution pending |

## Milestone completion rule

A milestone is complete only when:

1. implementation and important failure-path tests are committed;
2. relevant compile, unit tests, lint, R8 and manifest/permission/component audits pass;
3. roadmap, latest snapshot, idea ledger, decisions and changelog are synchronized;
4. governance CI passes;
5. device/live-network claims are backed by actual evidence;
6. an immutable snapshot is created for major phase/release boundaries.

## Change-control rule

Every meaningful coding batch updates `docs/MAYRA_ROADMAP.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`. Architecture/background/core changes update this blueprint and the decision log. Feature-track changes update the idea ledger. Build/release/manifest changes update the changelog. `scripts/verify_project_governance.sh` and `.github/workflows/project-governance.yml` enforce this contract.
