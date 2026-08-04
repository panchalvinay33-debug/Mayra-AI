# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-04
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 core voice is device-proven; Android offline TTS speaks but owner rates it robotic; free/offline neural-TTS benchmark foundation is now in progress without replacing the safe fallback

## Canonical truth

- Final product remains one Mayra app; J1/J2 are temporary engineering packages only.
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

The neural-TTS architecture work after this baseline is not promoted until fresh exact-head CI is green. No neural model binary has been committed or bundled.

## Motorola voice evidence

Physically proven:

- Android Digital assistant selection and Power-button invocation;
- offline Hindi/Hinglish/English transcription;
- direct dismissal paths and 20-cycle stability;
- already-locked invocation;
- owner-reported reboot/no-speech/rapid-interaction checks OK;
- CI #136 produces audible Mayra speech through Android TTS;
- owner reports that speech is understandable but robotic and wants a more natural voice;
- latest unlocked screenshots show weather/reminder requests captured cleanly without the previous visible text overlap.

## Free neural voice foundation added

- `MayraSpeechOutput` now isolates the Assistant from any one TTS engine.
- `MayraOfflineTtsSpeaker` implements that contract and remains the safe zero-cost fallback.
- `MayraVoicePackPolicy` records candidate metadata and blocks automatic production use when model terms are not cleared.
- unit tests protect the benchmark-only/production gates.
- `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md` records architecture/license/performance gates.
- `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md` defines the Motorola comparison matrix and fixed listening phrases.

## Candidate truth

### Sherpa-ONNX

Preferred first mobile inference runtime: Android/offline capable, Apache-2.0, Kotlin/Java support, supports VITS/Piper-class TTS.

### Hindi Priyamvada Medium

First phone-sized quality benchmark candidate: about 63.5 MB, hi-IN, 22.05 kHz. **Benchmark only.** The enclosing voice repository is labeled MIT, but the specific model card cites a CC BY-NC-SA 4.0 dataset, so production redistribution/use is not approved.

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

1. Fresh CI for the new abstraction/policy/tests/docs.
2. Build an isolated sherpa-onnx neural voice benchmark path that does not broaden normal J2 permissions and does not silently download a model.
3. Compare neural Hindi voice against system TTS on the Motorola using the benchmark matrix.
4. In parallel, continue connecting the device-proven voice bridge to Mayra's typed local brain/confirmation-safe actions so voice-quality work does not stall functionality.

## Distribution truth

Hosted CI debug signatures may conflict. Stable owner signing/trusted distribution remains required. Never bypass Play Protect or Android security.
