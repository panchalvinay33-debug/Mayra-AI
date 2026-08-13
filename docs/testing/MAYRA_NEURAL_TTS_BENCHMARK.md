# Mayra AI — Neural TTS Motorola Benchmark

Status: J3 #29 DEVICE QUALITY + STABILITY PASS — BENCHMARK CANDIDATE ACCEPTED, PRODUCTION LICENSE GATE REMAINS
Date: 2026-08-04
Target: Motorola Edge 70 Fusion / Android 16

## Purpose

Compare free/offline neural Hindi voice candidates against the Android system-TTS fallback and select a Mayra voice only from physical evidence.

## Candidate A — system fallback

Engine: Android `TextToSpeech`
Cost: free
Network after voice data install: not required for an offline voice
Physical result: functional and reliable, but owner reports robotic voice quality.

This remains the reliability fallback.

## Candidate B — Sherpa-ONNX + Hindi Priyamvada Medium

Role: accepted benchmark candidate only.

Runtime/package facts:

- sherpa-onnx AAR: `1.13.2`;
- J3 package: `ai.mayra.app.j3`;
- passing source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`;
- J3 Neural TTS Test #29: SUCCESS;
- Android CI #2202: SUCCESS;
- J2 Voice Test #207: SUCCESS;
- J1 Assistant Test #311: SUCCESS;
- Project Governance #383: SUCCESS;
- model: Hindi Priyamvada Medium;
- model SHA-256: `8871f3e07adb6ca490f8dbcd3956a8647c53c35b5d0a1c2a8d097b3bf721a31b`;
- model archive SHA-256: `399d91cc97eb288725633261f26b715f9a971e3bf7ec4fa1d7910cd0080d37eb`.

License caution: the specific voice model card cites a CC BY-NC-SA 4.0 dataset. Therefore this exact voice remains **BENCHMARK_ONLY** and is not approved for production redistribution/use.

## Failure / repair chain

### First J3 package

CI-green J3 #4 installed on Motorola but closed immediately on launch before interaction.

### Crash-isolated diagnostic

The neural runtime was moved into `:neuraltts` and model loading became explicit. Physical diagnostics showed:

- `EXIT_SELF`, status `1`;
- last completed stage: `Stage 3/4 • config built • entering sherpa native constructor`.

This proved UI/process startup/assets/config were good and narrowed the failure boundary to native `OfflineTts` construction.

### Filesystem-materialization repair

J3 #29 copies model/tokens/espeak data from APK assets into app-private filesystem paths before creating `OfflineTts` and uses one inference thread.

Physical result: PASS.

## Device evidence — J3 #29

Observed on Motorola:

- `Neural Voice Loaded ✓`;
- speech generation and AudioTrack playback PASS;
- sample generation wall time: `3091 ms`;
- generated audio duration: `4297 ms`;
- observed RTF: `0.72` — faster than real time;
- all six benchmark phrases tested successfully;
- owner reports all tested outputs are fine and phrase #1 sounds best;
- Airplane-mode synthesis test reported done/pass;
- Stop/playback cleanup test reported done/pass;
- 20 sequential replies reported done/pass;
- no owner-reported crash, forced reload or stuck playback during that stability round.

Owner did not provide numeric 1–5 quality scores, so no numeric score is inferred.

## Measurement summary

| Measurement | Result |
|---|---|
| Runtime version | sherpa-onnx 1.13.2 |
| Model | Hindi Priyamvada Medium benchmark candidate |
| Model SHA-256 | `8871f3e07adb6ca490f8dbcd3956a8647c53c35b5d0a1c2a8d097b3bf721a31b` |
| Model archive SHA-256 | `399d91cc97eb288725633261f26b715f9a971e3bf7ec4fa1d7910cd0080d37eb` |
| Model load | PASS |
| Synthesis/playback | PASS |
| Observed synthesis wall time | 3091 ms |
| Observed generated audio duration | 4297 ms |
| Observed RTF | 0.72 — PASS |
| Airplane-mode synthesis | PASS — owner reported done |
| Stop/playback cleanup | PASS — owner reported done |
| 20-reply stability | PASS — owner reported done |
| Voice listening result | all tested outputs fine; phrase #1 preferred |
| Exact numeric naturalness scores | NOT RECORDED |
| Peak RAM delta | NOT RECORDED |
| Detailed battery/thermal numbers | NOT RECORDED |
| Production redistribution license | BLOCKED for this exact voice |

## Listening set

1. `नमस्ते, मैं मायरा हूँ। मैं आपकी बात सुन रही हूँ।`
2. `कल सुबह सात बजे मुझे दवा लेने की याद दिलाना।`
3. `आज मौसम कैसा है और बारिश होने की संभावना कितनी है?`
4. `WhatsApp खोलने से पहले मैं आपसे पुष्टि करूँगी।`
5. `Hello, main Mayra hoon. Aap bataiye main kya help kar sakti hoon?`
6. `आज 4 अगस्त है और समय नौ बजकर बत्तीस मिनट है।`
7. a 20–30 second paragraph for future sustained comparison.

## Acceptance decision

### Device benchmark decision

**ACCEPTED as a device-stable neural-TTS benchmark candidate.**

The test demonstrates that a small sherpa/VITS Hindi voice can load, synthesize and play fully offline at usable real-time performance on the target Motorola, with owner-reported stability across the requested repeated test round.

### Production decision

**NOT APPROVED for production Mayra using this exact voice pack.**

Reason: model/dataset licensing gate remains unresolved for intended production redistribution/use. Android offline TTS remains the production-safe fallback until a license-clear neural Hindi voice using the proven runtime architecture is selected.

## Next engineering action

1. Preserve the J3 #29 sherpa/filesystem-materialization architecture as proven technical evidence.
2. Do not bundle Priyamvada into production Mayra.
3. Search/benchmark a production-license-clear Hindi/Hinglish neural voice compatible with the same speech-output boundary, or train/obtain an approved voice pack.
4. In parallel, advance the local conversational brain benchmark; natural-voice licensing must not block Mayra's offline intelligence work.
5. Keep `MayraOfflineTtsSpeaker` as safe fallback and retain lock-screen privacy/lifecycle rules during any future neural integration.
