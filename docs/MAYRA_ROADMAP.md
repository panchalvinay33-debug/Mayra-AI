# Mayra AI — Execution Roadmap

Last updated: 2026-08-05
Entry point: `START_HERE.md`
Jarvis/launcher master plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Local LLM benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`
Self-learning decision: `docs/decisions/ADR_031_SAFE_SELF_LEARNING.md`

## Product direction locked on 2026-08-05

Mayra is now explicitly targeting a practical **Jarvis-style personal Android operating layer**. The final experience will combine the existing Digital Assistant/voice foundation with an AI-native launcher/Home shell, context fabric, trust-gated action engine, proactive assistance, multimodal support and owner-defined routines.

The launcher is the primary Home shell, not the privileged brain. Heavy AI, memory and action execution stay modular so Home remains usable if local model/provider/AI runtime fails.

New major phases after J4 stabilization:

- **J5 — AI-native launcher shell**: HOME/default launcher, app drawer/search, Mayra Home/orb, safe fallback/switch-back, crash/reboot resilience.
- **J6 — Context fabric**: reminders, notifications, contacts/people, documents/media and bounded current-screen/app context.
- **J7 — Trust/action orchestration**: GREEN/AMBER/RED action classification, deterministic adapters, audit history and no direct LLM execution.
- **J8 — Proactive Mayra**: My Day, important notifications, follow-ups and quiet limits.
- **J9 — Multimodal Mayra**: explicit camera/image/screen/document understanding after privacy/performance gates.
- **J10 — Personal routines**: owner-defined typed workflows and narrowly reviewed automation.

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every meaningful batch and major-step checkpoint |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; power trigger invokes orb | Preserve regression baseline |
| J2 recognition/privacy | DEVICE VERIFIED FOUNDATION | Hindi/Hinglish/English, dismissal, lock/reboot/privacy cycles pass | Preserve regressions |
| Android system TTS | FALLBACK PASS | Offline speech works but owner finds it robotic | Keep as safe fallback |
| J3 neural TTS | DEVICE BENCHMARK PASS / LICENSE BLOCKED | Offline model load/synthesis/playback pass, RTF 0.72 | Find production-license-clear voice |
| J4 local LLM | ACTIVE / CI REPAIR GATE | Local-brain architecture and LiteRT-LM provenance work exist; current head has a Room/KSP schema failure that must be repaired before expansion | Restore exact-head J4 + shared CI green, then finish device quality/RAM/thermal/cancel gate |
| Self-learning | POLICY FOUNDATION IN SOURCE | Deterministic candidate policy, secret rejection and confirmation gates added | Auditable local memory store + owner review UI |
| Voice actions | SAFE FOUNDATION | Intent understanding exists without false execution claims | Connect only through deterministic confirmation-safe router |
| Trusted install | IN_PROGRESS | Stable owner signing/trusted distribution required | Private certificate + upgrade proof |
| J5 launcher shell | ACCEPTED / BLOCKED ON J4 GREEN | Canonical master plan + ADR accepted; no launcher implementation should stack on red J4 head | J4 green baseline, then launcher preflight/isolated proof |
| J6 context fabric | ACCEPTED | Existing reminders/memory/documents/notifications foundations can feed typed context later | Start only after J5 Home reliability proof |
| J7 trust/action orchestration | ACCEPTED | Existing typed actions/confirmations are foundation | Formal GREEN/AMBER/RED policy + audit tests |
| J8 proactive Mayra | ACCEPTED | Briefing scheduler/notification foundations exist | Context-quality, privacy and battery gates |
| J9 multimodal Mayra | ACCEPTED / LATER | Document/image path can be extended | Local/device privacy/RAM/thermal benchmark |
| J10 personal routines | ACCEPTED / LATER | Typed workflow philosophy established | Add owner-defined routines after action trust layer |

## Protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

J3/J4 remain engineering evidence packages and do not replace the protected production baseline until their own exact-green promotion requirements are satisfied.

## Major-step baseline discipline

Every major phase now follows this mandatory lifecycle:

1. record/update Idea Ledger;
2. record architecture decision;
3. update Blueprint;
4. define Roadmap gate;
5. add preflight/test matrix where applicable;
6. implement one coherent batch;
7. require relevant CI/package/permission/component audits;
8. obtain Motorola evidence for device claims;
9. synchronize Changelog + Latest Snapshot;
10. create immutable milestone snapshot;
11. create protected `baseline/*` branch only from the exact green commit;
12. start the next risky phase only from a known recovery point.

A red/pending head is never a stable baseline. Failure analysis/documentation may continue, but unrelated speculative features do not stack on top.

## J4 Motorola/device evidence record

Earlier J4 records contain local CPU inference evidence and model/runtime measurements. Current canonical execution must nevertheless honor the newest exact-head CI state: the observed current PR head has a J4 Room/KSP schema failure, so no newer code state is promoted until the failure is repaired and all applicable workflows are green again.

## Safe self-learning architecture

Self-learning means Mayra may become more useful from owner corrections and repeated preferences, but the model never gets authority to silently write trusted memory.

Source foundation:

- `MayraSelfLearningPolicy` evaluates bounded `LearningCandidate` objects;
- credential-like keys such as password/PIN/OTP/CVV/API keys are rejected;
- sensitive identity, health, finance, relationships, locations and contacts require confirmation;
- permanent memory always requires confirmation;
- uncertain model inference is rejected;
- only reversible low-risk response/language/UI preferences may be accepted without a blocking confirmation;
- every future memory item must remain visible, editable, forgettable and resettable.

Next implementation slice:

1. add a local Room-backed learned-memory record with source, confidence, timestamps and lifecycle state;
2. add candidate → pending review → approved/rejected/forgotten transitions;
3. add `Remember this`, `Forget this` and `What have you learned?` deterministic commands;
4. add owner review UI with edit/delete/reset;
5. inject only approved memory into local/cloud prompts through a bounded structured context;
6. add expiry/decay for repeated-behavior guesses;
7. add export/import without secrets;
8. later evaluate opt-in adapter/LoRA training only after RAM, battery, rollback and privacy gates.

## Immediate next actions

1. Repair the current J4 Room/KSP truncated-schema failure.
2. Re-run J4 and preserve J1/J2/J3/Android/Governance regressions.
3. Finish J4 longer-output, cancellation, RAM, thermal, Airplane/background/lock rounds.
4. Synchronize exact evidence and promote a protected J4 recovery baseline only from exact green.
5. Create J5 launcher feasibility/preflight and isolated launcher test surface.
6. Prove default-HOME selection, Home-button return, reboot persistence, app drawer/search, fallback launcher and crash/model-failure survival on Motorola.
7. Connect Mayra orb/voice to Home without moving heavy AI into critical launcher rendering.
8. Add context cards incrementally: reminders → notifications → people → documents/media.
9. Formalize J7 trust levels and auditable action history.
10. Add proactive briefing/routines only after context quality, privacy and battery evidence.
11. Keep PR #12 Draft/open/unmerged until explicit owner approval.
