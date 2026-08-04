# Mayra AI — Execution Roadmap

Last updated: 2026-08-04
Entry point: `START_HERE.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
Neural TTS preflight: `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md`
Neural TTS benchmark: `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md`
Local LLM preflight: `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`
Local LLM benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`
Canonical product issue: #13
Mandatory feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; Power trigger invokes orb | Preserve regression baseline |
| J2 on-device recognition | CORE DEVICE ACCEPTED | Hindi/Hinglish/English transcript, direct dismissal, 20 cycles, locked invocation and owner-reported reboot/no-speech/rapid tests pass | Preserve in consolidated regressions |
| Lock-screen privacy | DEVICE VERIFIED FOUNDATION | Keyguard-aware transcript/speech suppression path was added and physically exercised in the consolidated voice round | Preserve in future integrations |
| Android system TTS | FALLBACK PASS | Offline speech works but owner finds it robotic | Keep as safe fallback |
| Free neural TTS | DEVICE BENCHMARK PASS / LICENSE BLOCKED | J3 #29 loads and speaks fully offline; RTF 0.72; Airplane/Stop/20-reply round reported pass | Select a production-license-clear Hindi/Hinglish voice using the proven runtime boundary |
| Voice actions | SAFE FOUNDATION | Voice bridge understands app/reminder intent without false execution claims | Connect proven voice bridge to typed confirmation-safe action runtime |
| Wake phrase | BENCHMARK | Continuous SpeechRecognizer loop rejected; dedicated KWS required | Dedicated Motorola battery/false-trigger benchmark |
| Local LLM | NEXT ACTIVE BENCHMARK | On-device conversational brain preflight exists; no model selected | Build isolated Motorola benchmark harness |
| Calls | ACCEPTED / GATED | Default Phone/InCallService preflight complete | No role takeover before full UI/runtime |
| Trusted install | IN_PROGRESS | Stable owner signing/trusted distribution required | Private certificate + upgrade proof |

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136, J1 #239, Android CI #2131, Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`

The J3 benchmark is engineering evidence only and is not a production baseline because the tested Priyamvada voice pack remains license-blocked for production use.

## J3 neural TTS device evidence

Passing source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`

CI:

- J3 Neural TTS Test #29 — success;
- Android CI #2202 — success;
- J2 Voice Test #207 — success;
- J1 Assistant Test #311 — success;
- Project Governance #383 — success.

Physical Motorola evidence:

- app-private model materialization: PASS;
- sherpa native constructor: PASS;
- neural model load: PASS;
- synthesis/playback: PASS;
- observed sample: 3091 ms generation for 4297 ms audio, RTF 0.72;
- all six listening phrases reported fine;
- phrase #1 preferred by owner;
- Airplane mode test reported done/pass;
- Stop cleanup reported done/pass;
- 20 sequential replies reported done/pass;
- no numeric 1–5 quality score or detailed RAM/battery telemetry was supplied, so none is inferred.

## Neural TTS decision

Sherpa-ONNX plus filesystem-materialized VITS/Piper assets is now technically proven on the target Motorola. The exact Priyamvada pack remains benchmark-only because its model card cites a CC BY-NC-SA dataset. Therefore:

- keep Android offline TTS as the production-safe fallback;
- preserve `MayraSpeechOutput` as the stable engine boundary;
- reuse the proven J3 architecture only with a production-license-clear voice pack;
- do not let neural voice licensing block local-brain progress.

## Local LLM next phase

The next major offline-intelligence gate is an isolated local-LLM benchmark. Current research direction remains LiteRT-LM, but the benchmark should use a model/runtime combination that is actually supported by the current mobile stack rather than assuming the older Qwen3-1.7B plan is still the easiest first integration.

First benchmark goals:

- separate process or otherwise crash-contained runtime where practical;
- owner-selected/downloadable model rather than inflating the normal APK;
- checksum/version/license provenance;
- no authority over calls/messages/memory writes/actions;
- deterministic Mayra fallback if model load/generation fails;
- Hindi/Hinglish/English conversational prompts;
- airplane mode;
- cold/warm load, first-token latency, tokens/sec, RAM and thermal observations;
- cancellation/background/lock behavior.

## Immediate next actions

1. Record J3 #29 as device-stable benchmark evidence while retaining the production license block.
2. Refresh the local-LLM preflight against the current LiteRT-LM release/model support before writing Android runtime code.
3. Build a dedicated engineering-only local-LLM benchmark package; do not wire a heavy model directly into final Mayra yet.
4. Prefer a smaller supported first model for runtime proof, then compare a stronger multilingual candidate if the phone has headroom.
5. Keep model text behind the existing deterministic router/action/memory/document trust boundaries.
6. Continue using Android offline TTS as fallback; neural production voice selection can proceed independently.
7. Keep PR #12 Draft/open/unmerged until explicit owner approval.
