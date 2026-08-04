# Mayra AI — Execution Roadmap

Last updated: 2026-08-04
Entry point: `START_HERE.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
Neural TTS preflight: `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md`
Neural TTS benchmark: `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md`
Canonical product issue: #13
Mandatory feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; Power trigger invokes orb | Preserve regression baseline |
| J2 on-device recognition | CORE DEVICE ACCEPTED | Hindi/Hinglish/English transcript, direct dismissal, 20 cycles, locked invocation and owner-reported reboot/no-speech/rapid tests pass | Preserve in consolidated regressions |
| Lock-screen privacy | DEVICE VERIFY | Keyguard-aware transcript suppression/layout repair is CI-green; current device screenshots show cleaner unlocked layout | Verify no private transcript/TTS while already locked |
| Spoken Mayra reply | DEVICE VERIFY / QUALITY FAIL | Android offline TTS speaks successfully on Motorola, but owner reports the voice sounds robotic | Neural voice benchmark while keeping system TTS fallback |
| Free neural TTS | BENCHMARK FOUNDATION | Pluggable speech-output contract, license gate, feasibility preflight and Motorola benchmark matrix added | Integrate first benchmark-only sherpa/VITS Hindi pack in an isolated test path |
| Voice actions | SAFE FOUNDATION | J2 understands app/reminder intent but does not execute or falsely claim success | Connect proven voice bridge to typed action confirmation runtime |
| Wake phrase | BENCHMARK | Continuous SpeechRecognizer loop rejected; dedicated KWS required | After consolidated voice acceptance |
| Local LLM | BENCHMARK | LiteRT-LM/Qwen-class direction preflighted, no model selected | Motorola benchmark after voice bridge |
| Calls | ACCEPTED / GATED | Default Phone/InCallService preflight complete | No role takeover before full UI/runtime |
| Trusted install | IN_PROGRESS | Stable owner signing/trusted distribution required | Private certificate + upgrade proof |

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136, J1 #239, Android CI #2131, Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK size `19,209,329` bytes
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`
- ZIP SHA-256 `405406128d1f44a7bd9c90b71cc173d4bb352ba5d9e8c2863c1100c9a4d13b36`

New neural-TTS architecture commits are not a promoted application baseline until fresh exact-head CI is green. No neural model binary is currently bundled.

## Device evidence

PASS:

- Digital assistant selection and Power-button invocation;
- on-device Hindi/English language-pack discovery;
- Hindi/Hinglish/English transcript;
- transcript-only `open WhatsApp` with no execution;
- all direct dismissal paths;
- 20-cycle stability without reported crash, duplicate orb, stuck mic or permanent busy recognizer;
- already-locked invocation;
- owner-reported reboot/no-speech/rapid-open-close behavior OK;
- CI #136 Android TTS produces audible Mayra speech on Motorola;
- latest unlocked screenshots show recognized weather/reminder phrases cleanly without the previous visible overlap.

QUALITY FINDING:

- Android system/offline TTS is functional but owner reports it sounds robotic; this is now the fallback rather than the desired final Mayra voice.

## Neural TTS decisions

- Mayra speech output now has a small pluggable `MayraSpeechOutput` boundary.
- `MayraOfflineTtsSpeaker` remains the zero-cost fallback implementation.
- Sherpa-ONNX is the preferred first Android neural inference runtime because it is offline, Android-capable and Apache-2.0.
- Hindi `Priyamvada Medium` (~63.5 MB) is the first device-size benchmark candidate, but it is **BENCHMARK_ONLY** because its model card cites a CC BY-NC-SA dataset despite the enclosing piper-voices repository being MIT.
- Indic Parler-TTS (Apache-2.0, ~0.9B / ~3.75 GB current F32 checkpoint) is a quality reference but too heavy for the first phone path.
- IndicF5 (MIT, ~0.4B, high-quality Indic voice) is a later research candidate because it normally uses reference audio and has no proven lightweight Mayra Android path yet.
- No paid TTS API, subscription or per-character service is accepted for the primary Mayra voice.
- No model download is added to J2; the current exactly-`RECORD_AUDIO` engineering boundary remains intact.

## Neural voice target gates

- offline after pack installation;
- Hindi naturalness >= 4/5 owner score;
- Hinglish intelligibility >= 4/5;
- preferred warm first-audio latency <= 1.5 s, hard ceiling 3 s;
- sustained synthesis at least real-time;
- preferred first voice pack <= 250 MB;
- 20-reply stability;
- dismissal immediately stops speech;
- no private speech while locked;
- acceptable RAM, battery and thermal behavior;
- exact model version/hash/license pinned before production use.

## Immediate next actions

1. Settle fresh J2/J1/Android/Governance CI for the speech-output abstraction and license-gate foundation.
2. Keep CI #136 device acceptance as the functional fallback proof; do not regress recognition/privacy/lifecycle while experimenting with voice quality.
3. Build an isolated neural-TTS benchmark path around sherpa-onnx without adding Internet or a model binary to normal J2.
4. Benchmark the Hindi phone-sized candidate against Android TTS using the fixed phrase set, latency/RAM/storage/thermal/privacy matrix.
5. If the model class wins quality/performance but its license gate stays non-commercial, select/train/find a production-clear Hindi voice with the same runtime rather than shipping the restricted model.
6. Connect the proven voice bridge to the existing full Mayra typed local brain and confirmation-safe action runtime in parallel; natural-voice work must not stall command progress.
7. Keep PR #12 Draft/open/unmerged until explicit owner approval.
