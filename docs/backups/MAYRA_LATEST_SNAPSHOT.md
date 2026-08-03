# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: Jarvis Mode Phase J1 — Android Assistant-role foundation
Current synchronized head after this snapshot commit: see PR #12 latest head

## Protected recovery baseline

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI: #1795 — success
- Scope: last fully green pre-Jarvis application state.

This branch is immutable and is not used for development. Recovery rules are in `docs/MAYRA_BASELINE_AND_ROLLBACK.md`.

## Resume instruction

Before new work:

1. read `START_HERE.md`;
2. read `docs/MAYRA_PINPOINT_AUDIT.md`;
3. read this snapshot and the active roadmap section;
4. check exact PR head plus Android CI and Project Governance;
5. repair/revert a red head before expanding scope;
6. keep PR Draft/open/unmerged unless owner explicitly approves otherwise.

## Canonical control documents

- `docs/MAYRA_PINPOINT_AUDIT.md` — whole-project gap/evidence register;
- `docs/MAYRA_TEST_MATRIX.md` — evidence levels and mandatory test matrix;
- `docs/MAYRA_BASELINE_AND_ROLLBACK.md` — baseline promotion and recovery;
- `docs/MAYRA_BLUEPRINT.md` — architecture;
- `docs/MAYRA_ROADMAP.md` — ordered execution;
- `docs/MAYRA_IDEA_LEDGER.md` — idea lifecycle;
- `docs/MAYRA_DECISIONS.md` — decisions/supersessions;
- `docs/MAYRA_CHANGELOG.md` — milestone history;
- `docs/MAYRA_FULL_APP_ACCEPTANCE.md` — Motorola evidence.

## Last fully verified application truth

Android CI #1795 passed version 0.2.1 before Jarvis commits. It verified:

- Debug, Personal Alpha and Full Test compilation;
- complete unit tests;
- lint across governed variants;
- Personal Alpha APK package/label/permission/component/one-launcher audit;
- minified non-debuggable final `ai.mayra.app` R8/manifest audit;
- safe Full Test audit;
- isolated zero-permission Document Test audit;
- artifact/report upload.

Included features/fixes:

- optional HTTPS provider and Keystore credentials;
- scoped permission UX and live provider refresh;
- memory/document/action foundations;
- persistent reminders, Complete/Snooze/follow-up/reboot recovery;
- confirmation expiry;
- app-opening routing repair;
- final release/signing scaffold.

## Current Jarvis foundation

Committed:

- `VoiceInteractionService` foundation;
- `VoiceInteractionSessionService` and animated native orb;
- RecognitionService shell;
- assistant metadata and lock-screen declaration;
- low-permission Full Test exclusion for assistant components.

### Failed validation retained

Android CI #1833 failed at compile on the synchronized governance/Jarvis head:

1. `MayraRecognitionService.onCheckRecognitionSupport` overrode no available method;
2. `repeatCount` was referenced through the base Animator type.

### Repair applied

- RecognitionService now implements only mandatory callbacks and returns a deterministic unavailable error until the real recognizer exists.
- Orb repeat behavior is configured directly on each ObjectAnimator.

This repaired head is `IN_PROGRESS`; it is not a baseline until full latest-head Android CI and Project Governance both pass.

## Governance truth

Project Governance workflow has already completed a successful run after the initial governance rollout. It validates required documents, structure, update coupling and basic secret-leak patterns.

New audit/recovery controls added in this batch:

- canonical full-project pinpoint audit;
- canonical test matrix with design→compile→automated→package→device→release evidence levels;
- protected green baseline branch;
- baseline promotion/repair/revert/restore playbook.

## Current capability status

### Implemented / awaiting Motorola validation

- one launcher and internal feature screens;
- local deterministic chat and optional online provider;
- Keystore credentials and offline fallback;
- voice input/TTS foundation;
- memory lifecycle/provenance;
- TXT/PDF/DOCX intelligence;
- app opening/contact resolution;
- dialer/message-composer handoff;
- persistent reminders and recovery;
- Activity History/Device Readiness;
- governed engineering/release variants.

### In progress

- Assistant role packaging and API compatibility;
- animated Assistant session;
- unlocked/locked invocation;
- Assistant component presence/absence hard CI audit.

### Planned after J1 stable baseline

- offline wake phrase and battery/thermal benchmark;
- on-device local LLM benchmark/integration;
- Owner Mode trust levels;
- default Phone/InCallService and Call Screening;
- incoming caller announce/answer/reject/silence/mute/speaker;
- proactive summaries/routines;
- private production signing/distribution.

### Deferred/constrained

- scanned OCR;
- legacy binary `.doc`;
- exact alarms until physical need;
- unrestricted root/accessibility automation;
- hidden cellular recording;
- arbitrary protected call-audio injection.

## Current risks

1. Assistant manifest/service contracts still require exact-head full CI and device role visibility proof.
2. Full Test must remain free of Assistant-role/background components.
3. Motorola battery management may affect background/lock-screen behavior.
4. Wake word and local LLM must be selected through measured battery/thermal/RAM/quality evidence.
5. Phone role requires a complete fallback call UI and emergency/lost-role testing.
6. Core code is mature but physical acceptance is still the largest evidence gap.

## Exact next actions

1. Run latest-head Android CI and Project Governance after compile repair/docs synchronization.
2. Repair any exact remaining tests, lint, R8 or manifest audit failures.
3. Enforce Assistant components present in Personal Alpha/final and absent in Full Test.
4. Record exact green head/run/artifact IDs/digests.
5. Create immutable J1 milestone snapshot and a new protected baseline only after dual-green.
6. Generate Personal Alpha and perform Motorola Assistant-role tests.
7. Update acceptance, audit, roadmap and snapshot from real results before J2.

## Merge and secret truth

- PR #12 remains Draft/open/unmerged.
- No ready/merge transition is authorized.
- No API keys, keystores, passwords, tokens or private owner data belong in GitHub.
- A failed/pending head is never presented as stable.
