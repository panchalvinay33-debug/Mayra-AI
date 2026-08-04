# Mayra AI — Execution Roadmap

Last updated: 2026-08-04
Entry point: `START_HERE.md`
Local LLM benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`
Self-learning decision: `docs/decisions/ADR_031_SAFE_SELF_LEARNING.md`

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; power trigger invokes orb | Preserve regression baseline |
| J2 recognition/privacy | DEVICE VERIFIED FOUNDATION | Hindi/Hinglish/English, dismissal, lock/reboot/privacy cycles pass | Preserve regressions |
| Android system TTS | FALLBACK PASS | Offline speech works but owner finds it robotic | Keep as safe fallback |
| J3 neural TTS | DEVICE BENCHMARK PASS / LICENSE BLOCKED | Offline model load/synthesis/playback pass, RTF 0.72 | Find production-license-clear voice |
| J4 local LLM | DEVICE RUNTIME PASS / QUALITY BENCHMARK | Gemma3-1B runs fully local on Motorola CPU; init, generation, close and reload pass | Longer outputs, RAM/thermal/cancel/context testing |
| Self-learning | POLICY FOUNDATION IN SOURCE | Deterministic candidate policy, secret rejection and confirmation gates added | Auditable local memory store + owner review UI |
| Voice actions | SAFE FOUNDATION | Intent understanding exists without false execution claims | Connect only through deterministic confirmation-safe router |
| Trusted install | IN_PROGRESS | Stable owner signing/trusted distribution required | Private certificate + upgrade proof |

## Protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

J3/J4 remain engineering evidence packages and do not replace the protected production baseline.

## J4 Motorola device evidence

Device: Motorola Edge 70 Fusion / Android 16 / arm64-v8a / 7.30 GB RAM.

Runtime/model:

- LiteRT-LM Android `0.15.0`;
- AAR SHA-256 `b398c4745934a6035d192ffce5fdaf4f72a0009830a97b73c017c21f2a92b5bd`;
- model `Gemma3-1B-IT_multi-prefill-seq_q4_ekv4096.litertlm`;
- model SHA-256 `1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be`;
- app-private import and independent SHA verification pass;
- CPU initialization pass, observed cold load `5350 ms`;
- Hindi fixed generation `729 ms`;
- Hinglish fixed generation `1816 ms`, but output quality is weak;
- English fixed generation `620 ms`;
- close/unload reclaims isolated `:localbrain` process and launcher survives;
- reload-after-close and English generation `747 ms` pass;
- zero permissions, no tool calls, memory writes, messages or device actions.

Conclusion: local CPU inference is physically proven. This model is not yet promoted as Mayra's production-quality Hindi/Hinglish brain.

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

1. Settle CI for the self-learning policy foundation.
2. Extend J4 benchmark UI with longer Hindi/Hinglish/English prompts and output-length metrics.
3. Add generation cancellation and repeated load/generate/close cycles.
4. Capture runtime RAM before load, after load, during generation and after close.
5. Run Airplane mode, background, screen-lock and thermal rounds.
6. Build auditable local learned-memory storage and review UI behind deterministic policy.
7. Preserve J1/J2/J3/full-app regressions on every batch.
8. Keep PR #12 Draft/open/unmerged until explicit owner approval.
