# Mayra AI — START HERE

> **Read this first whenever Mayra work starts or resumes.**
>
> Canonical truth lives in the project records below, not in chat history or an old APK.

Last synchronized: **2026-08-05**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Current active phase: **repair current J4 CI red head, then complete J4 recovery baseline before starting J5 AI-native launcher implementation**
Latest protected application baseline: **`baseline/mayra-0.2.1-j2-privacy-tts-green-136`** at `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`

## 1. Product north star

Mayra is the owner’s personal Android AI companion, not merely a chat screen.

The owner has now explicitly locked the long-term target as a practical **Jarvis-style personal Android operating layer** with an AI-native launcher/Home shell as the primary daily surface.

Canonical Jarvis/launcher plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Architecture decision: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`
Planning snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_JARVIS_LAUNCHER_DIRECTION.md`

Target experience:

- natural Hindi/Hinglish/English conversation;
- useful on-device local conversational brain without mandatory cloud/API access;
- optional cloud providers as boosters only;
- owner-controlled memory and private document intelligence;
- reminders, apps, contacts and supported device actions;
- Android Digital Assistant integration;
- AI-native default launcher/Home shell with app drawer/search and Mayra presence;
- typed context fabric from approved memory, reminders, notifications, people and documents;
- trust-gated action orchestration;
- proactive My Day/pending-item assistance with quiet/privacy limits;
- listening/thinking/speaking presence;
- free/offline speech path with safe fallback;
- future wake phrase and optional supported Phone/Call Screening roles after dedicated gates;
- future multimodal vision/screen understanding and owner-defined routines after their own device/privacy gates;
- one final user-facing Mayra app and one launcher.

Engineering goal: maximum reliable behavior on the owner’s Motorola Edge 70 Fusion using supported Android roles/APIs. Unsupported protected capabilities are never claimed.

## 2. Read these records in this order

1. `START_HERE.md`
2. `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
3. `docs/MAYRA_PINPOINT_AUDIT.md`
4. `docs/MAYRA_BLUEPRINT.md`
5. `docs/MAYRA_ROADMAP.md`
6. `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
7. `docs/MAYRA_TEST_MATRIX.md`
8. `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
9. `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`
10. `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
11. `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md`
12. `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md`
13. `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`
14. `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`
15. `docs/MAYRA_IDEA_LEDGER.md`
16. `docs/MAYRA_DECISIONS.md`
17. `docs/MAYRA_CHANGELOG.md`
18. `docs/MAYRA_FULL_APP_ACCEPTANCE.md`
19. `docs/BLUEPRINT_UPDATE_POLICY.md`

## 3. Current proven foundation

### Core Mayra

Implemented foundations include:

- Kotlin/Jetpack Compose app;
- typed conversation/document/action routing;
- deterministic Hindi/Hinglish/English local commands;
- optional cloud provider with protected credentials and fallback;
- owner-controlled personal memory;
- TXT/PDF/DOCX library intelligence;
- persistent reminders and recovery;
- app opening/contact resolution/review-first call-message handoffs;
- release/minification/signing scaffolds.

### J1 Assistant — device proven

Motorola evidence proves Android accepts/selects Mayra as Digital assistant and Power-button invocation opens the Mayra session. Dismissal/lifecycle foundations are physically exercised and preserved by later regressions.

### J2 recognition/privacy — device proven

Physical device evidence includes:

- Hindi/Hinglish/English on-device recognition;
- direct dismissal paths;
- 20-cycle stability;
- already-locked invocation;
- reboot/no-speech/rapid-interaction checks reported OK;
- lock-screen transcript/speech privacy foundation;
- Android offline TTS functional as fallback, though owner rates it robotic.

### J3 neural TTS — technical device benchmark pass, production voice license blocked

Passing J3 source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`

Physical evidence:

- app-private neural model materialization PASS;
- sherpa native constructor/model load PASS;
- speech generation/playback PASS;
- sample RTF 0.72;
- all six phrases reported fine; phrase #1 preferred;
- Airplane mode / Stop cleanup / 20-reply stability reported pass.

The tested Priyamvada voice pack remains benchmark-only because its dataset terms are not production-cleared. Android offline TTS therefore remains the safe production fallback while a license-clear neural voice is researched independently.

### J4 local brain — active repair/stabilization phase

The J4 track has model import/integrity, LiteRT-LM provenance and isolated local-brain work in source/history. However, the newest observed PR head has one red workflow: **J4 Local LLM Test**.

Observed failure:

- LiteRT-LM 0.15.0 AAR download/hash/classfile probe passed;
- J4 compile stage failed in `kspJ4LocalLlmTestKotlin`;
- Room/KSP attempted to deserialize a truncated schema JSON and reported `Expected colon ':', but had 'EOF'`;
- Android CI, J1, J2, J3 and Project Governance were green on that observed head.

This red head is not a baseline. Repair it before any unrelated J5 launcher implementation is stacked.

## 4. Jarvis execution phases

After J4 exact-head green and recovery promotion:

- **J5 Launcher:** default HOME shell, app drawer/search, Mayra Home/orb, fallback/switch-back, reboot/crash/model-failure resilience.
- **J6 Context:** typed/provenance-aware reminders, notifications, people, documents/media and bounded screen/app context.
- **J7 Actions:** GREEN/AMBER/RED trust policy, deterministic typed adapters and auditable history.
- **J8 Proactive:** My Day, important-notification/pending-task suggestions, quiet mode and battery/privacy limits.
- **J9 Multimodal:** explicit camera/image/screen/document understanding after device gates.
- **J10 Routines:** owner-defined reusable workflows; narrow reviewed automation only.

The launcher is Mayra's Home shell, not the authority for privileged actions. Heavy AI failure must never make basic Home unusable.

## 5. Mandatory major-step baseline procedure

Before every major capability:

1. update Idea Ledger;
2. add/update architecture decision;
3. update Blueprint;
4. update Roadmap with explicit gate;
5. add preflight/test matrix where needed;
6. identify rollback point;
7. create immutable planning/direction snapshot when the direction materially changes.

After implementation:

1. require all applicable CI/lint/unit/package/permission/component checks;
2. require Motorola evidence for device claims;
3. update Changelog + Latest Snapshot + test evidence;
4. create immutable milestone snapshot;
5. create protected `baseline/*` branch only from exact green source;
6. record next risky phase and rollback target.

Planning/failure snapshots are not stable application baselines. No failed/pending head is promoted.

## 6. Current ordered work

1. Repair current J4 Room/KSP truncated-schema failure.
2. Restore fresh J4/J1/J2/J3/Android/Governance exact-head green.
3. Complete J4 local-brain longer-output/cancel/RAM/thermal/background/lock/Airplane regression gate.
4. Synchronize J4 evidence and promote a protected J4 recovery baseline only if exact green.
5. Create J5 launcher feasibility/preflight and isolated engineering implementation.
6. On Motorola prove default HOME selection, Home-button return, reboot persistence, app drawer/search, switch-back and crash/model-failure survival.
7. Connect existing Mayra orb/voice to Home without placing heavy AI on critical launcher rendering.
8. Add J6 context cards incrementally: reminders → notifications → people → documents/media.
9. Add J7 trust/action orchestration.
10. Add J8 proactive behavior, J9 multimodal and J10 routines only through their own gates.

## 7. Mandatory resume procedure

Before coding:

1. read this file;
2. read Jarvis/Launcher Master Plan + Pinpoint Audit + Latest Snapshot + active Roadmap section;
3. check PR #12 head/state and latest-head CI/Governance;
4. confirm source/docs/device evidence agree;
5. identify one coherent batch, tests and rollback point;
6. never expand a red head with unrelated speculative features — repair/revert first;
7. never claim device success without owner evidence;
8. never merge or mark PR #12 ready without explicit owner approval.

## 8. Mandatory completion procedure

Synchronize applicable records after every meaningful batch:

- Roadmap;
- Latest Snapshot;
- Pinpoint Audit;
- Test Matrix;
- Blueprint for architecture/scope/privacy changes;
- Jarvis/Launcher Master Plan when the product-shell direction changes;
- Idea Ledger;
- Decisions/ADR;
- Changelog;
- START_HERE when entry truth changes;
- protected baseline + immutable snapshot after major exact-head green milestones;
- PR description when milestone truth materially changes.

Governance CI must remain green. Stale canonical records count as a real failure.

## 9. Safety / ownership rules

- official Android roles/APIs first;
- minimum permissions for the active capability;
- no secrets/private keys/owner-private data in GitHub;
- no false call/audio/device claims;
- local LLM text cannot directly execute privileged actions;
- memory/document/context trust remains structured and owner-controlled;
- owner can disable provider, memory and privileged roles;
- model/runtime failure must fall back cleanly instead of making Mayra or the launcher unusable;
- launcher must always preserve a route to switch back to another Home app;
- no Play Protect/security/signing bypass.

## 10. Backup model

- Git history is primary code/document backup;
- `baseline/*` branches are immutable green recovery markers;
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is rolling recovery truth;
- immutable planning and milestone snapshots live under `docs/backups/`;
- planning snapshots are explicitly separate from green code baselines;
- CI artifacts are temporary evidence and must have source/run/digest provenance recorded before promotion.

## 11. Immediate next action

Repair the current J4 Room/KSP schema failure and restore exact-head green. Do not begin J5 launcher implementation until J4 recovery evidence is green and recorded.
