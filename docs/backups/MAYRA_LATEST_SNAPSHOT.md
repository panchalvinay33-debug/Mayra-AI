# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: **J4 quality device verification + J5 launcher/Home foundation validation**.

## Canonical direction

Mayra targets a practical **Jarvis-style personal Android operating layer**: Digital Assistant voice presence, local-first conversational brain, owner-controlled memory/documents, typed context, AI-native Home, trust-gated actions, proactive assistance, later multimodal support and owner-defined routines.

The launcher is a resilient Home shell, not privileged authority. Basic Home/app access must survive model/provider/brain failure.

## Protected recovery baseline

`baseline/mayra-0.2.1-j4-ci-recovery-green-134`

Exact source: `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`
Backup ref: `backup/j4-ci-recovery-2026-08-05`

Exact automated evidence:
- Android CI #2356 — SUCCESS
- J1 #465 — SUCCESS
- J2 #361 — SUCCESS
- J3 #183 — SUCCESS
- J4 #134 — SUCCESS
- Governance #537 — SUCCESS

This remains the immutable rollback point while J4 quality and J5 launcher work advance.

## J4 quality engineering checkpoint

Source: `862450933da3700d4d1559e09ebde910a4185914`
Backup: `backup/j4-quality-harness-ci-green-2026-08-05`

Automated evidence:
- Android CI #2364 — SUCCESS
- J1 #473 — SUCCESS
- J2 #369 — SUCCESS
- J3 #191 — SUCCESS
- J4 #142 — SUCCESS
- Governance #545 — SUCCESS

J4 #142 adds longer Hindi/Hinglish/English prompts, safety/uncertainty prompts, 10-prompt sequential stress, response character/approximate-token metrics, total latency/rough throughput estimates, localbrain PSS + Java/native heap telemetry, manual metrics capture and process-bounded cancellation/rebind.

J4 runtime APK artifact ID: `8918003689`.
J4 audit artifact ID: `8918004266`.

Physical Motorola quality evidence is still pending. The model is not yet promoted as Mayra's production conversational brain.

## J5 launcher foundation now in source

The development branch now includes a separate `MayraLauncherActivity` and HOME intent surface.

First slice includes:
- `MAIN + HOME + DEFAULT` Home qualification;
- Android `ROLE_HOME` user-consent request where available;
- searchable launchable-app list;
- direct app launch;
- bridge into normal Mayra (`Ask Mayra`);
- explicit Android Home-settings switch/restore route;
- launcher rendering kept independent from local model, provider, memory and privileged action startup.

This is implementation evidence only until fresh CI and Motorola device acceptance pass. No J5 device-success claim exists yet.

## Physical Motorola evidence preserved

Already proven on Motorola Edge 70 Fusion / Android 16:
- Android Digital Assistant selection/invocation;
- offline Hindi/Hinglish/English recognition and privacy/lifecycle foundations;
- J3 neural-TTS technical benchmark pass, with tested voice license blocked for production;
- Gemma3-1B model import/private SHA verification;
- LiteRT-LM CPU Stage 5/5 initialization;
- fixed multilingual generation;
- isolated localbrain close/reload while outer UI survives.

## Immediate physical gates

### J4 #142
1. quality prompts;
2. RAM metrics before/after load and generation;
3. 10-prompt stress;
4. cancellation + fresh rebind + reinitialize;
5. five close/reload cycles;
6. background/lock/process recovery;
7. Airplane-mode repeat;
8. battery/thermal observations.

### J5
1. install fresh CI/owner APK;
2. select Mayra as default Home;
3. Home gesture/button returns to Mayra;
4. app drawer/search and app launching work;
5. reboot preserves usable Home behavior;
6. switch/restore previous Home works;
7. local-model/provider failure does not break Home/app access.

## Jarvis phases

- J5 Launcher/Home
- J6 Context Fabric
- J7 Trust/Action Orchestration
- J8 Proactive Mayra
- J9 Multimodal Mayra
- J10 Owner-defined Routines

## Trust boundaries

- no free-form LLM privileged execution;
- no direct LLM trusted-memory writes;
- structured provenance for memory/documents/context;
- typed/action-bound/expiring confirmations;
- no silent cloud use in local mode;
- no security/Play Protect/signing bypass;
- default Home remains restorable;
- no device claim without Motorola evidence;
- PR #12 stays Draft/open/unmerged until explicit owner approval.

## Immediate next action

Run fresh CI for the J5 launcher source. If green, preserve that exact source as an engineering checkpoint, then perform Motorola J4-quality and J5-HOME acceptance before deeper voice/context/action integration.
