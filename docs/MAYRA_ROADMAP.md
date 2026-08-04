# Mayra AI — Execution Roadmap

Last updated: 2026-08-04
Entry point: `START_HERE.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
Canonical product issue: #13
Mandatory feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; Power trigger invokes orb | Preserve regression baseline |
| J2 on-device recognition | CORE DEVICE ACCEPTED | Hindi/Hinglish/English transcript, direct dismissal, 20 cycles, locked invocation and owner-reported reboot/no-speech/rapid tests pass | CI #136 consolidated regression |
| Lock-screen privacy | CI GREEN / DEVICE VERIFY | Keyguard-aware transcript suppression and layout repair green in CI #136 | Verify no private transcript/TTS while locked |
| Spoken Mayra reply | CI GREEN / DEVICE VERIFY | Offline-first Android TTS + deterministic local response policy green in CI #136 | Verify audible quality and lifecycle on Motorola |
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

Previous device-proven baseline remains `baseline/mayra-0.2.1-j2-speech-support-green-106` until the new privacy/TTS behavior is physically retested.

## Device evidence

PASS:

- Digital assistant selection and Power-button invocation;
- on-device Hindi/English language-pack discovery;
- Hindi/Hinglish/English transcript;
- transcript-only `open WhatsApp` with no execution;
- all direct dismissal paths;
- 20-cycle stability without reported crash, duplicate orb, stuck mic or permanent busy recognizer;
- already-locked invocation;
- owner-reported reboot/no-speech/rapid-open-close behavior OK.

Previously observed defect in #106:

- private transcript and overlapping text visible before unlock.

## CI #136 implementation

- keyguard-aware locked rendering;
- no transcript-derived/private spoken response while locked;
- layout spacing repair;
- offline TTS voice selection: Hindi India → English India → English US → offline fallback;
- speech rate 0.95, neutral pitch;
- deterministic local replies for greeting, time, capability, reminder and app-open intent;
- no action execution in J2; confirmation wording only;
- TTS/recognizer lifecycle cleanup;
- tests for no fake action claim and private unknown transcript handling.

## Immediate next actions

1. Install CI #136 and run one consolidated Motorola round: speech quality, privacy, action-confirmation wording, all lifecycle regressions, locked state and reboot.
2. If all pass, promote J2 to full `DEVICE_VERIFIED` for Assistant/on-device speech/offline TTS foundation.
3. Then connect the proven voice bridge to the existing full Mayra typed local brain and confirmation-safe action runtime.
4. After that, start the local wake-word benchmark and local-LLM device benchmark as separately gated tracks.
5. Keep PR #12 Draft/open/unmerged until explicit owner approval.
