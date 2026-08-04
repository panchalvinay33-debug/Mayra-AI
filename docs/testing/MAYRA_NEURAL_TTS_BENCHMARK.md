# Mayra AI — Neural TTS Motorola Benchmark

Status: HARNESS/PLAN READY — DEVICE MODEL TEST NOT STARTED
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

## Test procedure

1. Reboot device and wait for normal idle state.
2. Put device in airplane mode after any required local pack is already present.
3. Synthesize phrase 1 cold; record time until audible speech begins.
4. Repeat phrase 1 warm three times.
5. Run phrases 2–6 and score pronunciation/naturalness.
6. Run the long paragraph and note stutter/underrun.
7. Run 20 consecutive Mayra replies.
8. Dismiss Mayra during speech 5 times; audio must stop promptly.
9. Invoke while already locked; private reply must not be spoken.
10. Lock while speech is active; private audio must stop.
11. Observe battery/thermal behavior for a sustained 10-minute session.
12. Compare directly with Android system TTS using the same phrases.

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

- not yet physically tested.
