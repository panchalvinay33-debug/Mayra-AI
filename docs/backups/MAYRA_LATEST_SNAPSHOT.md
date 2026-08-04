# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-04
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 core voice is device-proven; Android offline TTS speaks but owner rates it robotic; isolated J3 neural-TTS benchmark reached device testing and exposed a startup-time native/model failure, now repaired to explicit-load semantics for narrower Motorola diagnosis

## Canonical truth

- Final product remains one Mayra app; J1/J2/J3 are temporary engineering packages only.
- PR #12 is not authorized for merge/ready.
- Protected baselines are immutable.
- Device capability claims require Motorola evidence.
- The primary Mayra voice should be free/offline after pack installation; no paid TTS API is required for the target design.

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136 / J1 #239 / Android CI #2131 / Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK size `19,209,329` bytes
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`
- ZIP SHA-256 `405406128d1f44a7bd9c90b71cc173d4bb352ba5d9e8c2863c1100c9a4d13b36`

The neural-TTS track is not promoted into production Mayra until exact-head CI plus Motorola quality/stability evidence pass. J3 remains isolated from J2.

## Motorola voice evidence

Physically proven:

- Android Digital assistant selection and Power-button invocation;
- offline Hindi/Hinglish/English transcription;
- direct dismissal paths and 20-cycle stability;
- already-locked invocation;
- owner-reported reboot/no-speech/rapid-interaction checks OK;
- CI #136 produces audible Mayra speech through Android TTS;
- owner reports that speech is understandable but robotic and wants a more natural voice;
- unlocked screenshots show weather/reminder requests captured cleanly without the previous visible text overlap.

## J3 neural voice device result

The first J3 package was fully CI-green for compile/lint/packaging/zero-permission audit, but on the Motorola it closed immediately on launch before the owner could touch any control.

This proves CI packaging success is not sufficient evidence for sherpa-onnx/model runtime compatibility on the target phone. No crash stack is available yet, so the exact native cause is not claimed.

Repair now committed:

- J3 Activity opens before neural runtime/model initialization;
- model load is explicitly triggered by a visible `Load Neural Voice` button;
- Java/Kotlin load and playback errors are surfaced in UI;
- startup proof and native-model-load proof are separated;
- if the process now closes only after the explicit load, the failure domain is narrowed to sherpa/model native initialization and a separate-process runtime test becomes the next step.

## Free neural voice foundation

- `MayraSpeechOutput` isolates the Assistant from any one TTS engine.
- `MayraOfflineTtsSpeaker` implements that contract and remains the safe zero-cost fallback.
- `MayraVoicePackPolicy` records candidate metadata and blocks automatic production use when model terms are not cleared.
- unit tests protect benchmark-only/production gates.
- `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md` records architecture/license/performance gates.
- `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md` records the actual J3 startup failure and replacement test sequence.

## Candidate truth

### Sherpa-ONNX

Preferred first mobile inference runtime: Android/offline capable, Apache-2.0, Kotlin/Java support, supports VITS/Piper-class TTS. Target-device runtime compatibility is still under test because J3 #4 closed during automatic startup initialization.

### Hindi Priyamvada Medium

First phone-sized quality benchmark candidate: about 63.5 MB, hi-IN, 22.05 kHz. **Benchmark only.** The specific model card cites a CC BY-NC-SA 4.0 dataset, so production redistribution/use is not approved.

### Indic Parler-TTS

Apache-2.0 and strong Hindi/style capabilities, but the current 0.9B/F32 checkpoint is roughly 3.75 GB, so it is a quality reference rather than the first Motorola integration target.

### IndicF5

MIT, about 0.4B parameters and high-quality Indic speech, but reference-audio workflow and mobile runtime cost make it a later research candidate.

## Acceptance targets for a natural Mayra voice

- free/offline after pack installation;
- Hindi naturalness >= 4/5 owner score;
- Hinglish intelligibility >= 4/5;
- preferred warm first-audio <= 1.5 s, maximum 3 s;
- sustained synthesis at least real-time;
- preferred first pack <= 250 MB;
- 20 sequential replies stable;
- dismissal stops audio;
- no private speech while locked;
- acceptable RAM/battery/thermal behavior;
- exact model version/hash/license pinned before production.

## Next gate

1. Fresh J3/J2/J1/Android/Governance CI for explicit-load repair.
2. Install only the new J3 build after green.
3. Open and wait 10 seconds without touching: app must remain visible.
4. Tap `Load Neural Voice` once and record Ready/error/process-close behavior.
5. Only after successful load begin listening/latency/stability scoring.
6. Continue full Mayra typed-brain/confirmation-safe action work independently so neural voice research does not stall functionality.

## Distribution truth

Hosted CI debug signatures may conflict. Stable owner signing/trusted distribution remains required. Never bypass Play Protect or Android security.
