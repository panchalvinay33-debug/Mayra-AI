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
| J2 on-device recognition | CORE DEVICE ACCEPTED | Hindi/Hinglish/English transcript, direct dismissal, 20 cycles, locked invocation and owner-reported reboot/no-speech/rapid tests pass | Consolidated privacy + spoken-reply regression |
| Lock-screen privacy | REPAIR IN CI | CI #106 exposes transcript/overlap before unlock | Generic locked state, no private transcript/TTS |
| Spoken Mayra reply | IN PROGRESS | Offline-first Android TTS + deterministic local response policy implemented in source | Fresh CI, then voice-quality device round |
| Voice actions | SAFE FOUNDATION | J2 understands app/reminder intent but does not execute or falsely claim success | Integrate full typed action/confirmation runtime after spoken-reply proof |
| Wake phrase | BENCHMARK | Continuous SpeechRecognizer loop rejected; dedicated KWS required | After consolidated voice acceptance |
| Local LLM | BENCHMARK | LiteRT-LM/Qwen-class direction preflighted, no model selected | Motorola benchmark after voice bridge |
| Calls | ACCEPTED / GATED | Default Phone/InCallService preflight complete | No role takeover before full UI/runtime |
| Trusted install | IN PROGRESS | Stable owner signing/trusted distribution required | Private certificate + upgrade proof |

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-speech-support-green-106`

- source `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 #106, J1 #210, Android CI #2101, Governance #282: success
- artifact ID `8866441207`
- APK SHA-256 `d0917d17b50429a843f3a5e688580df66f3eea678be4806b44ef9f1535adeb6e`
- exactly `RECORD_AUDIO`

Do not move/promote the new privacy/TTS source until fresh exact-head J2/J1/Android/Governance CI is green.

## Device evidence

PASS:

- Digital assistant selection and Power-button invocation;
- on-device Hindi/English language-pack discovery;
- Hindi/Hinglish/English transcript;
- transcript-only `open WhatsApp` with no execution;
- all direct dismissal paths;
- 20-cycle stability without reported crash, duplicate orb, stuck mic or permanent busy recognizer;
- already-locked invocation;
- owner-reported consolidated reboot/no-speech/rapid-open-close behavior OK.

OPEN DEFECT:

- CI #106 shows transcript/private text and overlapping layout before unlock.

## Current implementation batch

- keyguard-aware locked rendering;
- no transcript-derived/private spoken response while locked;
- layout spacing repair;
- offline TTS voice selection: Hindi India → English India → English US → offline fallback;
- speech rate 0.95, neutral pitch;
- deterministic local replies for greeting, time, capability, reminder and app-open intent;
- no action execution in J2; confirmation wording only;
- TTS/recognizer lifecycle cleanup;
- unit tests for no fake action claim and private unknown transcript handling.

## Immediate next actions

1. Settle fresh J2/J1/Android/Governance workflows.
2. Repair compile/lint/test/audit failures without weakening checks.
3. Promote exact-head baseline and artifact only after all gates pass.
4. Run one consolidated Motorola round: voice quality, privacy, action-confirmation wording, all lifecycle regressions, locked state and reboot.
5. Then connect the proven voice bridge to the existing full Mayra typed local brain and confirmation-safe action runtime.
6. Keep PR #12 Draft/open/unmerged until explicit owner approval.
