# Mayra AI — Immutable Planning Snapshot: Jarvis / Launcher Direction

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
Type: PLANNING / DIRECTION SNAPSHOT — NOT A GREEN APPLICATION BASELINE

## Owner direction locked

Mayra is to evolve into a practical Jarvis-style personal Android AI companion with an AI-native launcher/Home shell as the primary daily interaction surface.

The launcher is not the brain. Mayra Brain, Memory, Voice, Context and Action layers remain modular so phone Home remains usable when a model, provider or AI runtime fails.

Canonical plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Architecture decision: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`

## Major capability pillars accepted

- J5 AI-native launcher shell;
- J6 context fabric;
- J7 trust/action orchestration;
- J8 proactive Mayra;
- J9 multimodal Mayra;
- J10 personal routines;
- existing J1/J2/J3/J4 assistant/voice/local-brain foundations remain preserved and reused.

## Baseline discipline accepted

Every major step must update applicable idea/decision/blueprint/roadmap/test records before implementation, then synchronize changelog/latest snapshot/evidence afterward. Exact-green milestones receive immutable snapshots and protected `baseline/*` recovery branches.

Planning/failure snapshots are allowed but may never be called stable code baselines.

## Current technical blocker at time of snapshot

Current PR head observed before this documentation batch had one failing workflow: J4 Local LLM Test. Android CI, J1, J2, J3 and Governance were green.

The J4 failure was localized to Room/KSP reading a truncated schema JSON (`Expected colon ':', but had 'EOF'` while deserializing Room schema). LiteRT-LM AAR download/hash/classfile probing had already passed in that run.

Therefore no J5 launcher implementation should be stacked until J4 is repaired and the exact head is green again.

## Immediate next gate

1. repair J4 Room/KSP schema failure;
2. rerun J4 + shared regressions;
3. synchronize evidence;
4. promote protected J4 recovery baseline only if exact-head green;
5. then begin isolated J5 launcher preflight and implementation.
