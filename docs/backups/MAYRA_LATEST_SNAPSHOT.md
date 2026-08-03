# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: Jarvis Mode Phase J1 — Android Assistant-role foundation
Pre-Jarvis verified baseline: Android CI #1795 — success
Governance/Jarvis documentation transition head before this rolling-snapshot commit: `86396f8027aa85154e6ca6074827b8d1f9dfdb78`

## Resume instruction

Before any new work:

1. read `START_HERE.md`;
2. read the active section of `docs/MAYRA_ROADMAP.md`;
3. check the latest PR #12 head and both Android CI and Project Governance CI;
4. repair failures before expanding the phase;
5. keep PR Draft/open/unmerged unless the owner explicitly approves otherwise.

## Current product truth

Mayra is a local-first personal Android AI companion. It already has substantial owner-assistant foundations: conversation, optional provider, voice, approved personal memory, private documents, reminders, app opening, contact resolution, review-first dialer/message handoff, activity history and governed release variants.

The active direction now extends Mayra into a system-supported mobile Jarvis through Android Assistant role, animated voice sessions, future offline wake phrase/local model, and later optional Phone/Call Screening roles.

Cloud AI remains optional. A true on-device conversational model is accepted and planned but not yet integrated.

## Last fully verified application baseline

Android CI **#1795** completed successfully for Mayra **0.2.1** before the later Assistant-role/Jarvis commits.

That baseline verified:

- Debug, Personal Alpha and Full Test compilation;
- complete debug unit-test suite;
- Android lint across governed variants;
- Personal Alpha APK assembly and package/label/permission/component/one-launcher audit;
- minified non-debuggable final `ai.mayra.app` release candidate and R8/manifest audit;
- low-permission Full Test audit;
- isolated zero-permission Document Test audit;
- report and APK artifact upload.

Included 0.2.1 fixes:

- scoped permission UX;
- provider live refresh and resume status refresh;
- confirmation expiry protection;
- final release/signing scaffold;
- reminder follow-up transition and reboot remaining-delay fixes;
- app-opening routing collision fix;
- shared provider composition.

## Jarvis foundation now committed

- Android `VoiceInteractionService` foundation;
- `VoiceInteractionSessionService` and session foundation;
- animated Mayra orb/session UI foundation;
- RecognitionService shell;
- assistant metadata;
- lock-screen assistant-session declaration;
- low-permission Full Test exclusion for assistant-role components.

Validation truth: these features are `IN_PROGRESS`. They require latest-head compile/tests/lint/R8/manifest audits and Motorola Assistant-role selection/invocation evidence.

## Governance and backup system now committed

### Canonical records

- `START_HERE.md` — first read and resume/completion procedures;
- `README.md` — repository landing/navigation;
- `docs/MAYRA_BLUEPRINT.md` — architecture source of truth;
- `docs/MAYRA_ROADMAP.md` — current status and ordered gates;
- this rolling snapshot — exact recovery state;
- `docs/MAYRA_IDEA_LEDGER.md` — idea lifecycle;
- `docs/MAYRA_DECISIONS.md` — decisions/supersessions;
- `docs/MAYRA_CHANGELOG.md` — milestone history;
- `docs/MAYRA_FULL_APP_ACCEPTANCE.md` — Motorola evidence;
- `docs/BLUEPRINT_UPDATE_POLICY.md` — update contract.

### Automated enforcement

- `scripts/verify_project_governance.sh` validates record presence, structure, secret leakage patterns and update coupling.
- `.github/workflows/project-governance.yml` runs the governance contract on pushes and pull requests.
- Meaningful implementation changes require Roadmap + Latest Snapshot updates.
- Architecture/core/background changes require Blueprint + Decisions updates.
- Feature-track changes require Idea Ledger updates.
- Release/build/manifest changes require Changelog updates.

### Immutable transition backup

`docs/backups/MAYRA_SNAPSHOT_2026-08-03_GOVERNANCE_JARVIS_FOUNDATION.md`

## Current capability boundary

### Implemented / awaiting Motorola validation

- one user-facing launcher;
- local deterministic conversation and optional online provider;
- Keystore-protected provider credentials;
- voice input/TTS foundation;
- personal memory lifecycle and provenance;
- TXT/PDF/DOCX intelligence;
- app opening and contact resolution;
- review-first dialer/message composer;
- persistent reminders with Complete/Snooze/follow-up/recovery;
- activity history and readiness UI;
- audited Personal Alpha, Full Test, Document Test and minified final release path.

### In progress

- Android Assistant-role setup/invocation;
- animated assistant presence connected to real listening/thinking/speaking state;
- lock-screen/background Assistant behavior.

### Planned

- offline wake phrase;
- on-device local language model;
- owner trust levels/Owner Mode;
- default Phone/InCallService module;
- Call Screening module;
- caller announce/answer/reject/silence/mute/speaker;
- proactive summaries and routines;
- private production signing and distribution.

### Deferred or constrained

- scanned OCR;
- legacy binary `.doc`;
- exact alarms until physical need is proven;
- unrestricted root/accessibility automation;
- hidden SIM-call recording;
- arbitrary AI/TTS injection into protected cellular call audio.

AI caller message-taking requires a supported voicemail/VoIP or documented device-specific route.

## Current risks and unknowns

1. Assistant-role service signatures/metadata may require compile or manifest repair on latest Android APIs.
2. Motorola may apply OEM battery/background restrictions requiring explicit owner settings and device-specific guidance.
3. A wake-word engine must be benchmarked for idle drain, false triggers and thermal behavior.
4. A local model must be selected by real device RAM/speed/quality measurements, not guessed.
5. Default Phone role requires a complete call UI/fallback and careful device testing.
6. CI-verified behavior is not yet evidence of real lock-screen, call or background operation.
7. Governance workflow itself requires its first green validation on the synchronized branch.

## Exact next actions

1. Wait for or inspect latest-head Android CI and Project Governance CI.
2. Repair any exact compile, test, lint, R8, manifest or governance failure.
3. Add/verify CI manifest checks for Assistant components in Personal Alpha/final and absence in Full Test.
4. Add owner-visible Android Assistant-role request/status UI.
5. Produce a new Personal Alpha only after latest-head green.
6. Test Assistant selection, unlocked invocation and locked invocation on the Motorola.
7. Record physical evidence in `docs/MAYRA_FULL_APP_ACCEPTANCE.md`.
8. Begin wake-word/local-model benchmarking only after Phase J1 is stable.

## Merge and secret truth

- PR #12 remains Draft, open and unmerged.
- No merge/ready transition is authorized.
- No API keys, keystores, passwords, tokens or private owner data belong in GitHub project records.
- Personal use permits deeper owner-granted Android roles, but does not remove Android platform boundaries or justify false capability claims.
