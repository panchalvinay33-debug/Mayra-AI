# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Idea lifecycle: `docs/MAYRA_IDEA_LEDGER.md`
Decision log: `docs/MAYRA_DECISIONS.md`

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Project governance and backups | IN_PROGRESS | START_HERE, blueprint, roadmap, rolling snapshot, idea/decision/changelog ledgers and governance CI added | Complete latest-head CI/governance validation |
| One-app packaging | DEVICE_VERIFY | One launcher; Chat/Library/Memory/Provider/History internal; engineering variants separated | Motorola one-icon acceptance |
| Core conversation | DEVICE_VERIFY | Hindi/Hinglish/English local foundation + optional online provider | Long conversation and physical voice validation |
| Local conversational brain | PLANNED | Deterministic offline engine exists; no integrated local LLM yet | Benchmark suitable on-device model on Motorola |
| Optional cloud provider | DEVICE_VERIFY | HTTPS Responses-compatible provider, Keystore credentials, live composition and fallback | Real owner-key/network failure test |
| Personal memory | DEVICE_VERIFY | Approval, provenance, edit/replace/delete/expiry/recovery implemented | Motorola lifecycle and protected-storage checks |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX extraction/index/search/summary/grounding implemented | Physical PDF/DOCX acceptance |
| Mayra reminders | DEVICE_VERIFY | Persistent WorkManager reminders, Complete/Snooze/follow-up/reboot recovery | Doze/timing/reboot device acceptance |
| App and contact actions | DEVICE_VERIFY | App opening, contacts, dialer/composer handoff and expiring confirmations | Motorola end-to-end action checks |
| Animated Mayra presence | IN_PROGRESS | VoiceInteractionSession animated orb foundation committed | Compile/lint/R8 + device session state wiring |
| Android Assistant role | IN_PROGRESS | VoiceInteractionService/session/metadata/recognition shell committed | Select Mayra as Assistant and invoke on Motorola |
| Offline wake phrase | PLANNED | Recognition shell only | Choose engine; battery/thermal benchmark |
| Lock-screen/background voice | IN_PROGRESS | Assistant lock-screen declaration foundation | Assistant-role device proof |
| Incoming-call control | PLANNED | No default Phone/InCallService module yet | Build optional role request, call UI and controller |
| Call screening | PLANNED | Notification/call ideas recorded only | Add CallScreeningService owner rules |
| AI caller message-taking | PLANNED_WITH_CONSTRAINTS | Cellular audio injection/recording not assumed | Voicemail/VoIP or documented device route design |
| Production release | IN_PROGRESS | Non-debuggable minified release audit + secret-only signing scaffold | Private signing, provenance, distribution and acceptance |

## Verified baseline

### Mayra 0.2.1 pre-Jarvis baseline

Android CI **#1795** completed successfully for version **0.2.1 / versionCode 4** before the Assistant-role/Jarvis commits.

It covered:

- Debug, Personal Alpha and Full Test compilation;
- complete unit-test suite;
- Android lint across governed variants;
- Personal Alpha APK and permission/component/one-launcher audit;
- minified final `ai.mayra.app` release candidate and R8/manifest audit;
- safe Full Test audit;
- isolated zero-permission Document Test audit;
- artifact/report upload.

This baseline includes provider live refresh, scoped permission UX, confirmation expiry, app-opening routing repair, reminder follow-up repair and reboot remaining-delay recovery.

### Current latest-head truth

After CI #1795, the branch added the Android Assistant-role/Jarvis foundation and the complete project-governance system. These newer commits require their own latest-head Android CI and Project Governance green evidence. Until that completes, Jarvis features remain `IN_PROGRESS`, not `DONE`.

## Delivered capability groups

### Conversation and provider

- local deterministic commands and contextual offline fallback;
- voice input and TTS foundation;
- optional OpenAI Responses-compatible transport;
- encrypted API-key storage;
- live provider enable/disable/removal without restart;
- bounded retries, cancellation and offline fallback.

### Memory and documents

- approval-first personal memory lifecycle;
- trusted typed memory provenance;
- TXT/PDF/DOCX import and extraction;
- current-index search, summaries and grounded answers;
- document freshness/health tooling.

### Actions and reminders

- app opening;
- contact resolution;
- review-first dialer/composer;
- exact-action expiring confirmations;
- Mayra-owned persistent reminders;
- Complete, Snooze, follow-up and reboot/update recovery.

### Release engineering

- Personal Alpha owner candidate;
- low-permission Full Test;
- isolated Document Test;
- minified final release audit;
- environment-only signing scaffold;
- automated permission/component/launcher audits.

## Active Jarvis Mode plan

### Phase J1 — Assistant presence foundation

Status: `IN_PROGRESS`

- compile and lint VoiceInteractionService/session/metadata/recognition shell;
- audit Personal Alpha/final manifest components;
- keep those components absent from Full Test;
- add owner-visible Assistant-role setup/status;
- test invocation while unlocked and locked;
- connect animated orb to real listening/thinking/speaking state.

### Phase J2 — Local wake phrase and local brain

Status: `PLANNED`

- benchmark wake-word engines under screen-off conditions;
- measure idle battery, false triggers, thermal behavior and restart recovery;
- benchmark small quantized local language models on the Motorola target;
- add model storage/download integrity and fallback policy;
- route privacy-sensitive/basic conversation to local brain;
- keep cloud provider optional.

### Phase J3 — Advanced phone role

Status: `PLANNED`

- request optional default Phone role;
- implement required incoming/ongoing call UI fallback;
- announce caller;
- support answer, reject, silence, mute and speaker/audio endpoint where Android exposes it;
- add optional Call Screening role and owner rules;
- test emergency/default-dialer failure boundaries.

### Phase J4 — Proactive owner assistant

Status: `PLANNED`

- notification/call summaries;
- owner-defined trusted routines;
- missed-task and reminder follow-ups;
- context relevance/frequency controls;
- Owner Mode trust policy for routine low-risk actions.

### Phase J5 — Final release

Status: `PLANNED`

- complete Motorola acceptance matrix;
- fix OEM-specific background/battery issues;
- configure private release signing;
- produce signed APK/AAB with provenance;
- owner-controlled distribution/Play Internal Testing;
- final branding, onboarding, accessibility and performance polish.

## Deferred or constrained work

- OCR for scanned images: `DEFERRED`;
- legacy binary `.doc`: `DEFERRED`;
- exact alarm access: `DEFERRED` pending device need;
- unrestricted root/accessibility automation: `DEFERRED`;
- hidden cellular recording or arbitrary AI audio injection: not an assumed capability;
- caller message-taking requires a supported voicemail/VoIP/device route.

## Immediate ordered next actions

1. Let latest Android CI and Project Governance CI run on the synchronized branch head.
2. Repair exact compile/lint/manifest/governance failures without weakening the architecture.
3. Record authoritative run IDs and head in the rolling snapshot.
4. Add Assistant-role setup/status UI and manifest audits if not already covered by CI.
5. Generate the next Personal Alpha only after latest-head green.
6. Test role selection, unlocked invocation and locked invocation on the Motorola.
7. Begin local wake-word/model benchmarking only after J1 is stable.
8. Keep PR #12 Draft/open/unmerged until explicit owner approval.
