# Mayra AI — Motorola Wake-Word Benchmark

Status: TEMPLATE — DO NOT RUN AS PRODUCTION UNTIL J2 DEVICE ACCEPTANCE PASSES
Target device: Motorola Edge 70 Fusion / Android 16
Wake phrase: `Mayra`
Preflight: `docs/feasibility/MAYRA_WAKE_WORD_PREFLIGHT.md`

## Candidate record

For each benchmark candidate record:

- engine/library + exact version/commit;
- model name/version/checksum;
- model size;
- ABI/native-library size;
- source commit/build artifact;
- microphone sample rate/frame configuration;
- threshold/sensitivity settings;
- whether any network access occurs.

## Functional acceptance

- [ ] wake engine can be enabled/disabled explicitly;
- [ ] disabled means microphone wake detection stops;
- [ ] Power-button Assistant invocation remains functional;
- [ ] one confirmed `Mayra` wake starts exactly one Mayra interaction;
- [ ] repeated wake does not create duplicate sessions;
- [ ] locking/unlocking does not create a stuck listener;
- [ ] reboot returns to documented enabled/disabled state.

## Intended wake attempts

Run at least 100 owner wake attempts across these groups and record success/failure count:

| Condition | Attempts | Accepts | Rejects | Notes |
|---|---:|---:|---:|---|
| Quiet room, phone near | 20 | | | |
| Quiet room, 2–3 m | 20 | | | |
| Fan/background appliance | 15 | | | |
| TV/music/background speech | 15 | | | |
| Screen off | 10 | | | |
| Locked screen | 10 | | | |
| Faster/slower pronunciation | 10 | | | |

Record exact owner pronunciation variants that fail.

## False-accept observation

Observe Mayra during ordinary non-command audio:

- normal conversation;
- TV/video;
- music;
- similar-sounding words;
- phone in pocket/room;
- quiet idle.

Record:

- observation duration;
- number of false wakes;
- audio condition at each false wake;
- whether false wake exposed private content or executed anything (must be no).

## Latency

For representative successful wakes record approximately:

- phrase end → wake indication;
- wake indication → listening state;
- listening → transcript start.

Goal is a consistently responsive feel without sacrificing false-trigger performance.

## Battery/thermal

Before test:

- battery percentage;
- screen state;
- Wi-Fi/mobile data state;
- battery saver state;
- ambient conditions;
- Mayra wake-word enabled state.

Run an 8-hour idle observation where practical.

Record:

- start/end battery percentage;
- phone temperature/thermal warnings if any;
- unexpected wake count;
- whether Mayra process/Assistant remained recoverable;
- comparison with a similar idle period with wake-word disabled if practical.

## RAM/process stability

- [ ] no repeated process crash/restart loop;
- [ ] no System UI restart;
- [ ] no microphone indicator after wake-word is disabled;
- [ ] no duplicate listener after reboot/app update;
- [ ] local LLM is not kept loaded merely to detect wake phrase.

## Promotion rule

Wake-word implementation can move beyond benchmark only when:

- intended detection is reliable for the owner;
- false wakes are acceptable;
- battery/thermal impact is acceptable on the Motorola;
- disable/reboot behavior is reliable;
- J2/Power-button fallback remains intact;
- package/license/maintenance risk is documented;
- all results are added to Roadmap/Latest Snapshot/Decision Log.
