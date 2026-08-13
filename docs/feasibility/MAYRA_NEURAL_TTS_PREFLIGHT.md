# Mayra AI — Free Offline Neural TTS Preflight

Status: ACCEPTED FOR BENCHMARK — NO PRODUCTION MODEL SELECTED
Date: 2026-08-04
Target: Motorola Edge 70 Fusion / Android 16

## Objective

Give Mayra a substantially more natural Hindi/Hinglish voice without paid APIs, per-character charges, subscriptions or mandatory cloud speech synthesis.

The design must preserve a zero-cost Android system-TTS fallback and must not silently expand J2's permission/network boundary.

## Non-negotiable gates

A voice pack is not production-ready merely because it can be downloaded for free. Before Mayra may ship or auto-download a model, all of these must pass:

1. model/runtime license and training-data terms are acceptable for the intended distribution;
2. model files are pinned by exact version and SHA-256;
3. Hindi pronunciation is intelligible and natural;
4. Hinglish and Indian names are acceptable;
5. first-audio latency and real-time factor pass on the target Motorola;
6. memory peak, storage, battery and thermal behavior pass;
7. synthesis works fully offline after pack installation;
8. lock-screen privacy policy is preserved;
9. Android system TTS remains a safe fallback;
10. no model is allowed to make action/success claims — speech output only verbalizes already-approved Mayra text.

## Runtime choice

### Sherpa-ONNX — preferred mobile inference foundation

Sherpa-ONNX supports fully offline TTS on Android and exposes Kotlin/Java APIs for VITS/Piper and other supported TTS model families. Its project license is Apache-2.0.

Why it is the preferred first mobile runtime:

- Android is a first-class target;
- no cloud is required;
- VITS/Piper models can fit phone-sized storage budgets;
- the runtime is independent of Mayra's STT and assistant brain;
- the speech-output interface now allows the engine to be swapped without rewriting the Assistant session.

Reference:
- https://github.com/k2-fsa/sherpa-onnx
- https://k2-fsa.github.io/sherpa/onnx/tts/

## Candidate shortlist

### 1. Hindi Priyamvada Medium — first personal-device benchmark

- Family: VITS/Piper through sherpa-onnx
- Locale: hi-IN
- Model file: approximately 63.5 MB
- Sample rate: 22.05 kHz
- Android/Kotlin example exists in sherpa-onnx documentation
- Good size for an on-device first benchmark

License caution:

The `rhasspy/piper-voices` repository is labeled MIT, but the specific Priyamvada model card states that its dataset uses CC BY-NC-SA 4.0. Therefore Mayra must treat this model as **BENCHMARK_ONLY** until redistribution/use terms are cleared for the intended release. Personal engineering evaluation is not equivalent to production approval.

References:
- https://k2-fsa.github.io/sherpa/onnx/tts/all/Hindi/vits-piper-hi_IN-priyamvada-medium.html
- https://huggingface.co/rhasspy/piper-voices/tree/main/hi/hi_IN/priyamvada/medium
- https://huggingface.co/rhasspy/piper-voices/blob/main/hi/hi_IN/priyamvada/medium/MODEL_CARD

### 2. Indic Parler-TTS — quality reference, not first phone integration

- License: Apache-2.0
- Hindi + many Indic languages and English
- style/voice description control
- approximately 0.9B parameters; current F32 checkpoint is roughly 3.75 GB

This is attractive as a quality reference but is too heavy to make the first Motorola on-device candidate without a proven mobile quantization/runtime path.

Reference:
- https://huggingface.co/ai4bharat/indic-parler-tts

### 3. IndicF5 — research candidate

- License: MIT
- about 0.4B parameters
- supports Hindi and ten other Indian languages
- high-quality/near-human positioning
- reference-audio workflow is part of normal synthesis

This is promising for future voice quality, but model/runtime cost and reference-audio handling make it a later research candidate rather than the first production Mayra voice.

Reference:
- https://huggingface.co/ai4bharat/IndicF5

## Kokoro decision

Do not make Kokoro the Hindi default merely because it is a strong small English/multilingual TTS family. A Hindi-capable, license-cleared model must be demonstrated first. Mayra will prefer measured Hindi/Hinglish quality over model popularity.

## Architecture decision

`MayraSpeechOutput` is the stable application boundary.

Implementations:

- `MayraOfflineTtsSpeaker` — current Android system TTS fallback;
- future `MayraSherpaTtsSpeaker` — neural pack implementation after benchmark;
- future alternative engine may implement the same contract.

The Assistant session must never depend directly on one model family.

## Download/install policy

J2 must keep its existing engineering boundary and must not gain Internet merely to fetch a model. The first neural benchmark should use a dedicated engineering package or an explicitly user-selected local model pack. Full Mayra may later offer a guided download after the exact model/license/hash is pinned.

No hidden downloader, no background multi-gigabyte download, and no model fetch before explicit user action.

## Benchmark acceptance targets

Initial targets for a phone-sized Hindi voice candidate:

- first audible audio: preferred <= 1.5 s for a short sentence; hard reject if routinely > 3 s;
- real-time factor: <= 1.0 sustained, preferred <= 0.6;
- peak incremental RAM: preferred <= 500 MB, reject if it destabilizes the device;
- installed model + required data: preferred <= 250 MB for the first voice;
- 20 sequential replies without crash or thermal shutdown;
- no stuck audio after Assistant dismissal;
- airplane-mode synthesis PASS;
- lock-screen private reply suppression PASS;
- Hindi naturalness owner score >= 4/5;
- Hinglish intelligibility owner score >= 4/5;
- names/numbers/time/reminder phrases understandable without repeated correction.

These are benchmark gates, not claims about current performance.

## Benchmark phrase set

1. `नमस्ते, मैं मायरा हूँ। मैं आपकी बात सुन रही हूँ।`
2. `कल सुबह सात बजे मुझे दवा लेने की याद दिलाना।`
3. `आज मौसम कैसा है और बारिश होने की संभावना कितनी है?`
4. `WhatsApp खोलने से पहले मैं आपसे पुष्टि करूँगी।`
5. `Hello, main Mayra hoon. Aap bataiye main kya help kar sakti hoon?`
6. `Vinay, aaj 4 August hai aur samay 9 bajkar 32 minute hai.`
7. one 20–30 second paragraph for sustained synthesis.

## Promotion rule

A neural voice becomes Mayra's default only after:

- exact source/model/license audit;
- target-device benchmark;
- user listening preference;
- regression CI;
- privacy/lifecycle acceptance.

Until then Android offline TTS remains the supported fallback.
