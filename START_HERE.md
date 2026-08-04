# Mayra AI — START HERE

> **Read this first whenever Mayra work starts or resumes.**
>
> Canonical truth lives in the project records below, not in chat history or an old APK.

Last synchronized: **2026-08-04**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Current active phase: **J4 local-brain model lifecycle/integrity + LiteRT-LM compatibility benchmark**
Latest protected application baseline: **`baseline/mayra-0.2.1-j2-privacy-tts-green-136`** at `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`

## 1. Product north star

Mayra is the owner’s personal Android AI companion, not merely a chat screen.

Target experience:

- natural Hindi/Hinglish/English conversation;
- useful on-device local conversational brain without mandatory cloud/API access;
- optional cloud providers as boosters only;
- owner-controlled memory and private document intelligence;
- reminders, apps, contacts and supported device actions;
- Android Digital Assistant integration;
- listening/thinking/speaking presence;
- free/offline speech path with safe fallback;
- future wake phrase and optional supported Phone/Call Screening roles after dedicated gates;
- one final user-facing Mayra app and one launcher.

Engineering goal: maximum reliable behavior on the owner’s Motorola Edge 70 Fusion using supported Android roles/APIs. Unsupported protected capabilities are never claimed.

## 2. Read these records in this order

1. `START_HERE.md`
2. `docs/MAYRA_PINPOINT_AUDIT.md`
3. `docs/MAYRA_BLUEPRINT.md`
4. `docs/MAYRA_ROADMAP.md`
5. `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
6. `docs/MAYRA_TEST_MATRIX.md`
7. `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
8. `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`
9. `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
10. `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md`
11. `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md`
12. `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`
13. `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`
14. `docs/MAYRA_IDEA_LEDGER.md`
15. `docs/MAYRA_DECISIONS.md`
16. `docs/MAYRA_CHANGELOG.md`
17. `docs/MAYRA_FULL_APP_ACCEPTANCE.md`
18. `docs/BLUEPRINT_UPDATE_POLICY.md`

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

### J4 local brain — active phase

Green L0 source: `062894809bb8b0989f79ab99db644c9d0cbdfa2d`

Exact green evidence:

- J4 Local LLM Test #2 — success;
- Android CI #2224 — success;
- Project Governance #405 — success;
- J1 #333 / J2 #229 / J3 #51 — success.

L0 proves a dedicated zero-permission `ai.mayra.app.j4` package with owner-selected `.litertlm` import into app-private storage.

Fresh J4 source now additionally implements:

- reusable `MayraLocalModelIntegrity` + tests;
- `.litertlm` filename gate;
- storage-headroom and overflow guards;
- atomic `.partial` model import;
- selected/copied byte verification;
- SHA-256 during import and independent re-verification;
- saved model metadata;
- remove/re-import lifecycle;
- device ABI/RAM/app-heap/private-storage diagnostics;
- CI probe of the current LiteRT-LM Android Maven AAR version/hash/class-file Java level before runtime linking.

First runtime candidate: **Gemma3-1B chat-ready `.litertlm`**, upstream reference around 557 MB. It is a runtime-proof candidate, not yet the final Hindi/Hinglish brain.

## 4. Current ordered work

1. Settle the fresh hardened J4/J1/J2/J3/Android/Governance head.
2. Record exact J4 APK/audit/runtime-SDK provenance.
3. On Motorola, import exact Gemma3-1B `.litertlm` and prove SHA-256/reverify/reopen/remove/re-import lifecycle.
4. Pin exact LiteRT-LM Android SDK only from CI compatibility evidence.
5. Link LiteRT-LM to J4 only and initialize CPU engine off UI thread with exact load/error diagnostics.
6. Run fixed Hindi/Hinglish/English prompts in Airplane mode and record cold/warm load, first-token latency, decode speed, RAM and thermal behavior.
7. Only after technical stability compare GPU and/or a stronger multilingual model.
8. Only after a model passes device quality/performance, connect it behind Mayra’s deterministic routing/provider boundary.
9. Keep action execution, memory writes and document trust outside free-form model authority.
10. Continue wake-word, trusted-install and optional Phone-role tracks only through their dedicated feasibility/device gates.

## 5. Mandatory resume procedure

Before coding:

1. read this file;
2. read Pinpoint Audit + Latest Snapshot + active Roadmap section;
3. check PR #12 head/state and latest-head CI/Governance;
4. confirm source/docs/device evidence agree;
5. identify one coherent batch, tests and rollback point;
6. never expand a red head — repair/revert first;
7. never claim device success without owner evidence;
8. never merge or mark PR #12 ready without explicit owner approval.

## 6. Mandatory completion procedure

Synchronize applicable records after every meaningful batch:

- Roadmap;
- Latest Snapshot;
- Pinpoint Audit;
- Test Matrix;
- Blueprint for architecture/scope/privacy changes;
- Idea Ledger;
- Decisions;
- Changelog;
- START_HERE when entry truth changes;
- protected baseline + immutable snapshot after major exact-head green milestones;
- PR description when milestone truth materially changes.

Governance CI must remain green. Stale canonical records count as a real failure.

## 7. Safety / ownership rules

- official Android roles/APIs first;
- minimum permissions for the active capability;
- no secrets/private keys/owner-private data in GitHub;
- no false call/audio/device claims;
- local LLM text cannot directly execute privileged actions;
- memory/document trust remains structured and owner-controlled;
- owner can disable provider, memory and privileged roles;
- model/runtime failure must fall back cleanly instead of making Mayra unusable.

## 8. Backup model

- Git history is primary code/document backup;
- `baseline/*` branches are immutable recovery markers;
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is rolling recovery truth;
- immutable milestone snapshots live under `docs/backups/`;
- CI artifacts are temporary evidence and must have source/run/digest provenance recorded before promotion.

## 9. Immediate next action

Settle the hardened J4 head. If green, produce the exact Motorola J4 model-import artifact and complete `.litertlm` import/integrity evidence before linking LiteRT-LM inference.
