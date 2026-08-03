# Mayra AI — Canonical Product Blueprint

Last updated: 2026-08-03
Status: Living architecture source of truth
Entry point: `START_HERE.md`

## Product vision

Mayra is the owner’s personal Android AI companion: a local-first, voice-capable assistant that can converse naturally, remember approved personal context, reason over private documents, manage reminders, coordinate supported phone actions and remain available through official Android system roles.

Mayra is not merely an OpenAI client or chat screen. Cloud intelligence is optional. The long-term identity, personality and essential capabilities belong to Mayra’s on-device runtime.

## Experience target

When unlocked, Mayra may appear as a compact animated presence that reacts while listening, thinking and speaking. When locked, Mayra should use voice/background behavior without forcing the full app UI. Setup must remain simple: one first-launch screen, required runtime permissions, one Android Assistant-role step, then Mayra.

The project targets the maximum reliable experience supported by Android and the owner’s Motorola device. It does not claim unrestricted movie-Jarvis control or protected platform capabilities without evidence.

## Non-negotiable principles

1. Local-first identity; a local conversational model remains required future work.
2. Cloud providers are optional and text-only.
3. One coherent user-facing app and launcher.
4. Maximum owner control with explicit system-role approval.
5. Official Android roles before fragile hacks.
6. Truthful action and device-test claims.
7. Typed trust boundaries around memory and actions.
8. Recoverable safeguards for destructive/irreversible operations.
9. Credentials, signing keys and owner-private data never enter Git history or documentation.
10. Documentation is part of implementation and CI-governed.
11. Physical claims require Motorola evidence.
12. PR #12 remains Draft/unmerged without explicit owner authorization.
13. Owner setup should be as small and understandable as Android permits.
14. Owner APK upgrades require one stable signing certificate.

## System architecture

### 1. Interaction and presence

- Main Compose chat surface.
- Animated listening/thinking/speaking assistant presence.
- Speech recognition and text-to-speech.
- Lock-screen/background sessions through the official Assistant role.
- Internal Library, Memory, Provider, History and Setup screens.

### 2. Minimal owner setup

The first-start path is intentionally limited to two owner-facing steps:

1. request microphone, contacts and notification runtime permissions together;
2. open Android's Assistant-role selector.

The owner may continue temporarily if a permission or role is skipped. Internet and boot recovery do not require runtime dialogs. Android roles/special access cannot be silently granted, so Mayra presents the smallest possible explicit system step rather than a large settings maze.

### 3. Assistant-role / Jarvis layer

- Android `VoiceInteractionService`.
- `VoiceInteractionSessionService` and animated session UI.
- Recognition-service boundary for future wake-word/speech integration.
- Owner-triggered Assistant-role selection.
- Future offline wake-word engine with battery, thermal and privacy policy.

### 4. Local brain

Current:

- deterministic local intent/command engine;
- contextual offline fallback;
- local routing, memory, documents, reminders and actions.

Required future:

- benchmarked on-device language model suitable for Motorola RAM/thermal limits;
- local Hindi/Hinglish conversation and summarization;
- bounded context and memory retrieval;
- model integrity/storage/version controls;
- graceful smaller-model/rule fallback.

### 5. Optional provider

- Responses-compatible bounded HTTPS transport.
- Android Keystore-protected credentials.
- Live enable/disable/remove behavior.
- Cancellation, bounded retries and local fallback.
- Provider output cannot execute actions or write personal memory directly.

### 6. Personal memory

- Explicit local proposal/approval.
- Inspect, replace, edit, expire and delete.
- Honest storage-health behavior.
- Trusted structured memory-use metadata.
- Future category-specific Owner Mode rules remain owner-controlled.

### 7. Documents

- TXT, PDF and DOCX import/extraction/indexing.
- Current-index search, grounded answers, summary, freshness and health.
- OCR and legacy `.doc` deferred.
- Private content stays local unless a future bounded remote feature is explicitly enabled.

### 8. Actions and reminders

- Deterministic intent/capability gates.
- App opening and contact resolution.
- Review-first dialer/message composer.
- Exact-action expiring confirmations.
- Persistent WorkManager reminders with Complete, Snooze, follow-up and reboot recovery.
- Exact alarms deferred until device tests prove need.

### 9. Phone and call layer

Current: outgoing dialer/message handoffs.

Planned optional privileged role:

- default Phone/InCallService;
- caller announcement;
- answer, reject, silence, mute and speaker/audio endpoint where supported;
- Call Screening rules;
- complete fallback call UI.

Arbitrary protected cellular audio capture, secret recording and reliable TTS injection into SIM calls are not assumed. AI message-taking requires a documented voicemail/VoIP/device route.

### 10. Background and proactive layer

- Boot/update recovery for reminders.
- Notification listener only behind special-access consent.
- Owner-defined proactive rules with relevance/frequency controls.
- No unrestricted permanent microphone service.

### 11. Packaging, signing, testing and release

Packages:

- Personal Alpha: owner-capability package `ai.mayra.app.alpha`.
- Full Test: lower-permission UI regression package.
- Document Test: isolated zero-permission document regression package.
- Final release: `ai.mayra.app`, non-debuggable, minified/R8 and audited.

Signing policy:

- temporary hosted-runner debug certificates are acceptable only for disposable clean-install tests;
- repeated owner installs must use one stable owner certificate;
- stable keystore material and passwords are supplied only through protected environment/GitHub secrets;
- `.github/workflows/owner-alpha.yml` builds the stable owner artifact;
- every stable artifact records package, signing certificate and APK SHA-256;
- clean-install and install-over-install data-retention tests are required before promotion;
- production release signing remains distinct and private.

Both Android CI and Project Governance CI are required for promotion.

## Current module status

| Module | Status | Truth |
|---|---|---|
| Core routing/conversation | DEVICE_VERIFY | Local engine + optional provider implemented; local LLM pending |
| Personal memory | DEVICE_VERIFY | Controlled lifecycle implemented; Motorola checks pending |
| Documents | DEVICE_VERIFY | TXT/PDF/DOCX implemented; physical checks pending |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery implemented |
| App/contact actions | DEVICE_VERIFY | App open and review-first handoffs implemented |
| Provider | DEVICE_VERIFY | HTTPS/Keystore/live composition implemented |
| Minimal first-start setup | IN_PROGRESS | Two-step setup committed; CI/device proof pending |
| Stable owner updates | IN_PROGRESS | Secret-backed build path committed; secrets/update test pending |
| Animated Mayra presence | DEVICE_VERIFY | J1 CI foundation green; invocation pending |
| Android Assistant role | DEVICE_VERIFY | Services/metadata CI green; Motorola selection pending |
| Offline wake word | PLANNED | Recognition shell only |
| Local conversational model | PLANNED | Benchmark pending |
| Incoming-call control | PLANNED | Phone/Call Screening roles required |
| Production release | IN_PROGRESS | R8/signing scaffold implemented; private signing pending |

## Milestone completion rule

A milestone is complete only when implementation/failure tests, compile/unit/lint/R8/package audits, synchronized governance records and applicable physical evidence all pass. Major boundaries receive an immutable snapshot and protected baseline.

## Change-control rule

Every meaningful coding batch updates Roadmap and Latest Snapshot. Architecture/background/core changes update this blueprint and Decisions. Feature changes update Idea Ledger. Build/release/manifest changes update Changelog. Governance CI enforces the contract.
