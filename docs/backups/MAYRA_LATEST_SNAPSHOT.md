# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-04
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 voice foundation is device-proven; J3 neural TTS is device-stable as a benchmark candidate but its exact voice pack remains production-license blocked; local LLM benchmark is now the next active offline-intelligence phase.

## Canonical truth

- Final product remains one Mayra app; J1/J2/J3 and future local-LLM harnesses are engineering packages only.
- PR #12 is not authorized for merge/ready.
- Protected baselines are immutable.
- Device capability claims require Motorola evidence.
- Android offline TTS remains the production-safe speech fallback until a license-clear neural voice is selected.
- Heavy local LLM integration remains blocked until an isolated Motorola benchmark passes.

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136 / J1 #239 / Android CI #2131 / Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`

## Motorola voice evidence

Physically proven foundation:

- Android Digital assistant selection and Power-button invocation;
- offline Hindi/Hinglish/English recognition;
- direct dismissal paths and 20-cycle stability;
- already-locked invocation;
- owner-reported reboot/no-speech/rapid-interaction checks OK;
- Android offline TTS speaks but owner finds it robotic.

## J3 neural voice milestone

Passing source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`

Exact CI:

- J3 Neural TTS Test #29 — success;
- Android CI #2202 — success;
- J2 Voice Test #207 — success;
- J1 Assistant Test #311 — success;
- Project Governance #383 — success.

Physical result on Motorola:

- app-private model materialization: PASS;
- sherpa native `OfflineTts` construction: PASS;
- model load: PASS;
- synthesis/playback: PASS;
- observed sample: 3091 ms generation for 4297 ms audio, RTF 0.72;
- all six phrases reported fine;
- phrase #1 preferred by owner;
- Airplane-mode synthesis reported done/pass;
- Stop cleanup reported done/pass;
- 20 sequential replies reported done/pass;
- no numeric quality scores or detailed RAM/battery telemetry were supplied, so none is inferred.

## Neural voice decision

Sherpa-ONNX + filesystem-materialized VITS/Piper assets is technically proven on the Motorola. However, the tested Hindi Priyamvada Medium pack remains **BENCHMARK_ONLY** because its model card cites a CC BY-NC-SA dataset. It must not be silently promoted into production Mayra.

Production direction:

- preserve `MayraSpeechOutput` engine abstraction;
- keep `MayraOfflineTtsSpeaker` fallback;
- reuse J3 runtime architecture with a production-license-clear Hindi/Hinglish voice pack;
- neural voice selection proceeds independently of local-brain development.

## Local conversational brain — next active phase

Preflight refreshed on 2026-08-04 against current LiteRT-LM upstream direction.

Runtime direction: Google LiteRT-LM.

First benchmark candidate: **Gemma3-1B LiteRT-LM** as the smaller runtime-proof model (upstream currently lists ~557 MB, 4-bit, chat-ready).

Second comparison candidate: **qwen2.5-1.5b LiteRT-LM** if the first runtime proof is stable (upstream currently lists ~1524 MB, chat-ready).

Earlier Qwen3-1.7B interest is retained as research context but is no longer forced as the first integration target.

Required benchmark boundaries:

- dedicated engineering package;
- model outside ordinary APK;
- owner-managed local model file/checksum;
- UI-thread-safe load/cancel/unload;
- crash/failure containment where practical;
- Airplane mode;
- Hindi/Hinglish/English prompt set;
- cold/warm load, first-token, decode speed, RAM and thermal evidence;
- deterministic Mayra fallback;
- no direct authority for calls/messages/actions/memory writes/doc trust.

## Next gate

1. Keep J3 #29 as benchmark evidence; do not production-ship Priyamvada.
2. Build the isolated local-LLM engineering harness around a currently supported LiteRT-LM model/runtime combination.
3. Prove Gemma3-1B load/generation/cancel/unload on Motorola before stronger model comparison.
4. If stable, compare qwen2.5-1.5b for Hindi/Hinglish quality versus storage/RAM cost.
5. Only after a device pass, connect a local conversational provider behind Mayra's deterministic routing/trust boundaries.
6. Preserve voice recognition, privacy, actions and system-TTS fallback regressions throughout.

## Distribution truth

Hosted CI debug signatures may conflict. Stable owner signing/trusted distribution remains required. Never bypass Play Protect or Android security.
