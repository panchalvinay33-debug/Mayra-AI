# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J4 CI recovery is green and protected; complete J4 quality/operability device gate, then start J5 AI-native launcher implementation from a known recovery point.

## Canonical product direction

Mayra is targeting a practical **Jarvis-style personal Android operating layer** combining:

- Android Digital Assistant / voice presence;
- local-first conversational brain with deterministic fallback;
- owner-approved memory and private documents;
- reminders, notification intelligence and people/context;
- AI-native default launcher/Home shell;
- trust-gated typed action engine;
- proactive My Day/pending-item assistance;
- later multimodal understanding and owner-defined routines through separate gates.

Canonical plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Launcher decision: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`
Room/KSP recovery decision: `docs/decisions/ADR_034_ROOM_SCHEMA_ISOLATION_FOR_MULTI_VARIANT_CI.md`
Jarvis planning snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_JARVIS_LAUNCHER_DIRECTION.md`
J4 recovery snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_J4_CI_RECOVERY_GREEN.md`

## Current protected recovery point

`baseline/mayra-0.2.1-j4-ci-recovery-green-134`

Exact source: `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`
Backup ref: `backup/j4-ci-recovery-2026-08-05`

Exact-head automated evidence:

- Android CI #2356 — SUCCESS;
- J1 Assistant Test #465 — SUCCESS;
- J2 Voice Test #361 — SUCCESS;
- J3 Neural TTS Test #183 — SUCCESS;
- J4 Local LLM Test #134 — SUCCESS;
- Project Governance #537 — SUCCESS.

Android CI #2356 passed serial governed variant compilation, complete debug unit tests, all governed lint variants, Personal Alpha package/audit, minified Release/R8 package/audit, FullTest package/audit and isolated DocumentTest package/audit.

J4 #134 passed pinned LiteRT-LM 0.15.0 provenance/hash/classfile checks, Maven-transitive isolation, isolated compile, generated Room JSON validation, focused local-model/learning tests, lint, J4 APK assembly, zero-permission/component/runtime audit and artifact upload.

J4 runtime APK artifact ID: `8917566340`.
J4 audit artifact ID: `8917567141`.

## Why the J4 red head happened and why it is considered repaired

The earlier failure was not a LiteRT-LM runtime incompatibility. Room/KSP variants shared `app/schemas`, and concurrent variant compilation could expose truncated or empty schema JSON to another Room processor. The same pattern reproduced under FullTest in Android CI.

Repair:

- J4 compile/test/lint/assemble split into isolated Gradle invocations;
- transient Room schema output reset before isolated variant work;
- explicit JSON parser validation after J4 compile;
- shared Android CI governed variants serialized to avoid concurrent Room schema writes.

The exact recovery source above passed all major CI regressions, so this failure class is closed at the automated-build level unless reproduced again.

## Physical Motorola evidence preserved

Motorola Edge 70 Fusion / Android 16 evidence already proves:

- Mayra selectable/invokable as Android Digital Assistant;
- offline Hindi/Hinglish/English recognition and lock/privacy foundation;
- J3 offline neural-TTS technical benchmark pass, with tested Priyamvada pack still production-license blocked;
- Gemma3-1B `.litertlm` import + private-storage SHA verification;
- LiteRT-LM 0.15.0 CPU engine initialization to Stage 5/5;
- fixed Hindi/Hinglish/English local generation;
- isolated `:localbrain` close/reload and launcher/test-activity survival.

The current local model is **not yet promoted as Mayra's production conversational brain**. Fixed prompts prove execution, not sufficient assistant quality. Hinglish quality in the first benchmark was weak.

## Active J4 quality/operability gate

Before J4 production-brain promotion or J5 dependency on local AI, complete:

1. longer useful Hindi/Hinglish/English prompts;
2. output character/approximate token metrics;
3. total generation timing and defensible decode estimate;
4. local-brain RAM before load / after load / during or after generation / after close;
5. explicit cancel-generation control and recovery;
6. 10 sequential prompts;
7. 5 close/reload cycles;
8. background, screen-lock and process-kill recovery;
9. Airplane-mode repeat;
10. owner-observed battery/thermal behavior.

J5 launcher work may begin only after the chosen J4 gate is recorded without weakening launcher reliability. The launcher must remain fully usable with local AI missing, corrupt, killed or disabled.

## Jarvis execution phases

- J5 — AI-native launcher/Home shell;
- J6 — provenance-aware typed context fabric;
- J7 — GREEN/AMBER/RED trust/action orchestration;
- J8 — proactive My Day/pending-item intelligence;
- J9 — multimodal Mayra;
- J10 — owner-defined routines.

## Non-negotiable trust boundaries

- launcher is the Home shell, not privileged action authority;
- heavy AI/model/provider failure never makes Home unusable;
- local LLM text never directly executes calls/messages/device actions;
- local LLM text never directly writes owner memory;
- trusted memory/document/context provenance stays structured;
- confirmations stay typed, action-bound and expiring;
- local mode never silently sends owner context to a network;
- previous/default launcher remains restorable;
- no Play Protect/security/signing bypass;
- no device-success claim without Motorola evidence;
- PR #12 remains Draft/open/unmerged until explicit owner approval.

## Baseline discipline

Material direction changes receive immutable planning snapshots. Exact-green major milestones receive immutable milestone snapshots plus protected `baseline/*` recovery branches. A pending/red head is never called stable. New risky work must preserve a known rollback point.

## Immediate next action

Instrument and validate the J4 quality/operability benchmark while preserving the exact green recovery baseline. After that, synchronize evidence and begin J5 launcher implementation with HOME selection, app drawer/search, app launch, switch-back and AI-failure survival as the first acceptance slice.
