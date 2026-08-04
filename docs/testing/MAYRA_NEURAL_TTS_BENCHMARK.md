# Mayra AI — Neural TTS Motorola Benchmark

Status: FIRST DEVICE STARTUP FAIL RECORDED — EXPLICIT-LOAD REPAIR IN CI
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

## Device result — first J3 package

CI-green J3 #4 installed on Motorola but the app closed immediately on launch before the owner could touch any control.

Interpretation:

- packaging/build/lint/zero-permission audit PASS did not prove native model initialization on the Motorola;
- the first J3 Activity automatically started sherpa-onnx + neural-model initialization immediately after Compose content creation;
- exact crash cause is not yet proven because no device logcat was captured;
- likely fault domain is startup-time native/model initialization rather than UI interaction;
- J3 #4 is therefore a DEVICE FAIL candidate and must not be promoted.

## Repair under test

The replacement J3 source changes startup semantics:

- Activity opens without loading sherpa-onnx/model;
- visible UI must remain usable first;
- owner explicitly taps `Load Neural Voice` to begin native/model initialization;
- model-loading Java/Kotlin exceptions are rendered in the UI;
- playback construction is also guarded;
- model release is guarded during destroy;
- this separates `app startup` proof from `native model load` proof.

If the native runtime itself aborts the process on explicit load, that becomes a much narrower reproducible failure and the next repair will isolate neural inference into a separate process/runtime candidate.

## Measurements to capture

For each candidate record:

| Measurement | Result |
|---|---|
| Pack/model version | PENDING |
| SHA-256 | PENDING |
| Installed bytes | PENDING |
| Cold first-audio latency | PENDING |
| Warm first-audio latency | PENDING |
| 10 s synthesis wall time | PENDING |
| Peak process RAM delta | PENDING |
| 20-reply stability | PENDING |
| Airplane-mode synthesis | PENDING |
| Dismiss stops audio | PENDING |
| Locked private speech suppression | PENDING |
| Hindi naturalness 1–5 | PENDING |
| Hinglish intelligibility 1–5 | PENDING |
| Names/numbers 1–5 | PENDING |
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

## Replacement test procedure

1. Install replacement J3.
2. Open app and wait 10 seconds without touching anything. App must remain open.
3. Capture screenshot of the visible `Load Neural Voice` control/status.
4. Tap `Load Neural Voice` once.
5. If app remains open and reports Ready, play phrases 1–6 and score voice quality.
6. If model load reports an error, capture the exact visible error.
7. If app process closes only after tapping Load, record `native explicit-load crash`; do not repeatedly retry.
8. Only after successful load continue airplane mode, warm latency, 20 replies, stop/playback, thermal and quality comparisons.

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

- J3 #4 startup on Motorola: FAIL — app closed immediately before interaction;
- replacement explicit-load build: CI pending.
