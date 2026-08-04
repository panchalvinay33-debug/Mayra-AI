# Mayra AI — Neural TTS Motorola Benchmark

Status: J3 #29 DEVICE LOAD + FIRST SYNTHESIS PASS — QUALITY/STABILITY BENCHMARK CONTINUES
Date: 2026-08-04
Target: Motorola Edge 70 Fusion / Android 16

## Purpose

Compare free/offline neural Hindi voice candidates against the current Android system-TTS fallback and select a Mayra voice only from physical evidence.

## Candidate A — system fallback

Engine: Android `TextToSpeech`
Cost: free
Network after voice data install: not required for an offline voice
Current physical result: functional but owner reports robotic voice quality.

This remains the reliability baseline.

## Candidate B — Sherpa-ONNX + Hindi Priyamvada Medium

Role: first neural benchmark candidate only.

Expected model size: ~63.5 MB.
Do not promote to production because the voice model's model card cites a CC BY-NC-SA 4.0 dataset. The test exists to measure whether this model class solves the quality/latency problem on the Motorola.

Runtime/package facts for the passing candidate:

- sherpa-onnx AAR: `1.13.2`;
- J3 package: `ai.mayra.app.j3`;
- source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`;
- J3 Neural TTS Test #29: SUCCESS;
- Android CI #2202: SUCCESS;
- J2 Voice Test #207: SUCCESS;
- J1 Assistant Test #311: SUCCESS;
- Project Governance #383: SUCCESS.

## Failure / repair chain

### First J3 package

CI-green J3 #4 installed on Motorola but the app closed immediately on launch before the owner could touch any control.

### Explicit-load + crash-isolation diagnostic

The replacement J3 moved sherpa-onnx into a secondary `:neuraltts` process and required explicit `Load Neural Voice` action. The launcher survived native-process failure.

Physical diagnostic then showed:

- process exit: `EXIT_SELF`, status `1`;
- last completed stage: `Stage 3/4 • config built • entering sherpa native constructor`;
- therefore UI, secondary-process startup, packaged model/tokens/espeak asset readability and Kotlin config construction were proven good;
- failure boundary was narrowed to the native `OfflineTts` constructor.

### Filesystem-materialization repair

J3 #29 materializes the neural model, tokens and espeak data from APK assets into app-private filesystem paths before constructing sherpa `OfflineTts`, and uses a conservative single inference thread.

Physical Motorola result: PASS.

The neural model now loads successfully and speech synthesis/playback starts while the launcher remains alive.

## Device evidence — J3 #29

Owner screenshot on Motorola Edge 70 Fusion / Android 16 shows:

- `Neural Voice Loaded ✓`;
- `Playing neural Mayra voice…`;
- generation wall time: `3091 ms`;
- generated audio duration: `4297 ms`;
- real-time factor: `0.72`.

Interpretation:

- neural runtime initialization: PASS;
- app-private model materialization path: PASS;
- native `OfflineTts` construction: PASS;
- first observed synthesis: PASS;
- AudioTrack playback start: PASS;
- observed RTF `0.72` is faster than real time and passes the initial sustained-generation requirement of `<= 1.0` for this sample;
- this is not yet a complete quality/stability or production pass.

## Measurements to capture

| Measurement | Result |
|---|---|
| Runtime version | sherpa-onnx 1.13.2 |
| Model | Hindi Priyamvada Medium benchmark candidate |
| Model SHA-256 | `8871f3e07adb6ca490f8dbcd3956a8647c53c35b5d0a1c2a8d097b3bf721a31b` |
| Model archive SHA-256 | `399d91cc97eb288725633261f26b715f9a971e3bf7ec4fa1d7910cd0080d37eb` |
| Cold model load | PASS, exact ms still to record |
| Observed synthesis wall time | 3091 ms |
| Observed generated audio duration | 4297 ms |
| Observed RTF | 0.72 — PASS vs <=1.0 gate |
| Warm first-audio latency | PENDING |
| Peak process RAM delta | PENDING |
| 20-reply stability | PENDING |
| Airplane-mode synthesis | PENDING |
| Stop/playback cleanup | PENDING |
| Locked private speech suppression | PENDING / final integration gate |
| Hindi naturalness 1–5 | PENDING owner score |
| Hinglish intelligibility 1–5 | PENDING owner score |
| Names/numbers 1–5 | PENDING owner score |
| Heat/battery observation | PENDING |

## Listening set

Use the same phrases for every engine:

1. `नमस्ते, मैं मायरा हूँ। मैं आपकी बात सुन रही हूँ।`
2. `कल सुबह सात बजे मुझे दवा लेने की याद दिलाना।`
3. `आज मौसम कैसा है और बारिश होने की संभावना कितनी है?`
4. `WhatsApp खोलने से पहले मैं आपसे पुष्टि करूँगी।`
5. `Hello, main Mayra hoon. Aap bataiye main kya help kar sakti hoon?`
6. `आज 4 अगस्त है और समय नौ बजकर बत्तीस मिनट है।`
7. a 20–30 second paragraph.

## Next physical benchmark sequence

1. Play phrases 1–6 at `1.00×` and judge naturalness/pronunciation.
2. Repeat the same useful phrases at `0.92×`; choose the better owner listening preference.
3. Record Hindi naturalness, Hinglish intelligibility and names/numbers scores from 1–5.
4. Turn Airplane mode ON and synthesize at least phrases 1 and 5; neural playback must still work.
5. Press Stop during playback; audio must stop cleanly without hanging the neural process.
6. Run 20 sequential replies, mixing Hindi/Hinglish, without crash, stuck audio or forced reload.
7. Observe device heat and obvious battery impact during the 20-reply run.
8. Record warm synthesis timings/RTF from multiple phrases.
9. Do not integrate this model into production Mayra until the model-license/redistribution gate is separately approved.

## Acceptance rule

A candidate is QUALITY PASS only if:

- Hindi naturalness >= 4/5;
- Hinglish intelligibility >= 4/5;
- no major number/time pronunciation regression;
- warm first-audio latency <= 1.5 s preferred and <= 3 s maximum;
- sustained generation is at least real-time;
- 20-reply stability passes;
- privacy/dismissal tests pass;
- storage/RAM/thermal impact is acceptable.

A QUALITY PASS is still not a PRODUCTION PASS until the exact model license/redistribution gate is approved.

## Current result

Android system TTS:

- functionality: PASS;
- owner listening result: robotic / quality improvement requested.

Neural candidate:

- J3 #4 startup: FAIL;
- crash-isolated diagnostic: PASS, root boundary narrowed to native constructor;
- J3 #29 model load: PASS on Motorola;
- J3 #29 first observed synthesis/playback: PASS;
- observed RTF: `0.72` PASS;
- quality score, airplane mode, 20-reply stability, warm latency and thermal checks: PENDING.
