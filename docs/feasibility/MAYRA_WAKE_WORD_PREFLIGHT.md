# Mayra AI — Production Wake-Word Feasibility Preflight

Status: PRELIMINARY APPROVAL FOR BENCHMARKING ONLY — PRODUCTION INTEGRATION BLOCKED UNTIL J2 DEVICE PROOF
Date: 2026-08-03
Gate: Issue #14
Target: Motorola Edge 70 Fusion / Android 16
Rollback source before any future implementation: `baseline/mayra-0.2.1-j2-voice-green-18`

## Owner outcome

The intended experience is that, when Mayra is the selected Android Digital assistant and wake-word mode is explicitly enabled, saying `Mayra` can start a bounded voice interaction without requiring the Power-button trigger.

The owner must be able to disable wake-word mode instantly. Screen-off/lock-screen behavior must remain compatible with Android policy and must not expose private content before unlock.

## Official Android architecture

- Keep the selected Android `VoiceInteractionService` lightweight; Android keeps the selected service running so it can support hotword-style assistant behavior.
- Heavy UI, STT, local LLM and TTS work belongs in the actual voice interaction/session path, not permanently inside the always-running service.
- Do **not** use Android `SpeechRecognizer` as an endless wake-word loop. Android documents that SpeechRecognizer is not intended for continuous recognition and may consume significant battery/bandwidth.
- Use a dedicated local keyword-spotting (KWS) engine for the wake phrase, then launch the existing Mayra VoiceInteractionSession and J2-style bounded STT pipeline.

## Candidate engine for benchmark

Primary open-source benchmark candidate: **sherpa-onnx keyword spotting**.

Reasons to benchmark it first:

- Android keyword-spotting application/support exists;
- on-device processing;
- small KWS model families are available;
- custom keyword workflow exists;
- architecture can remain independent from cloud APIs.

This is a benchmark candidate, not a final selection. The engine is promoted only if it meets Motorola battery, false-trigger, latency, package-size and maintenance requirements.

## Hard boundaries

- A permanently running general speech recognizer is rejected.
- Wake phrase must never silently turn itself on after the owner disabled it.
- No root, Accessibility automation, OEM-private microphone bypass or hidden recording behavior.
- Android may restrict or alter lock-screen invocation behavior; Mayra records and respects the actual policy.
- Detection of `Mayra` does not itself authorize a sensitive action. It only starts a Mayra interaction.

## Permission/setup burden

Expected runtime requirement:

- `RECORD_AUDIO`.

Owner setup target:

1. enable Mayra wake phrase once;
2. grant microphone if not already granted;
3. optional battery guidance only if real Motorola tests prove it necessary.

No contacts, notification, SMS, call or Accessibility permission is required merely for wake detection.

## Privacy path

- Wake-word audio stays on-device.
- Raw audio is not persisted.
- The KWS engine should process the smallest practical rolling audio window.
- Only after a confirmed wake event does Mayra start the bounded J2-style recognition session.

## Performance budget — engineering targets

These are promotion targets, not claims:

- wake detection latency: perceived near-instant response after phrase end;
- no visible thermal growth during idle monitoring;
- no stuck microphone state after disable/restart;
- memory footprint small enough that the always-running service remains lightweight;
- 8-hour idle battery impact measured on the owner device and judged acceptable by the owner;
- false accepts low enough that normal conversation/TV does not repeatedly wake Mayra;
- false rejects low enough that normal owner pronunciation works at near and room distance.

## Phrase/language plan

Primary phrase: `Mayra`.

Benchmark must include:

- normal Hindi/Hinglish pronunciation;
- faster/slower pronunciation;
- nearby and several-metre distance;
- quiet room, fan/TV/background conversation;
- male/female voices if practical;
- similar-sounding words to measure false accepts.

Do not add multiple wake phrases until one phrase is stable.

## Failure/fallback UX

If wake-word engine is unavailable, corrupted, disabled or killed:

- Power-button Digital assistant invocation remains the reliable fallback;
- Mayra must show wake-word status honestly in Setup;
- no endless restart loop;
- no silent switch to cloud listening.

## Evidence required before production integration

Automated/package:

- model checksum/version;
- explicit enabled/disabled state;
- restart/reboot state tests;
- no wake engine in J1/J2 safe proof packages unless intentionally tested;
- no internet dependency for detection;
- bounded microphone/lifecycle state machine.

Motorola benchmark:

- 100+ intended wake attempts across conditions;
- false-reject count;
- at least several hours of normal-room false-accept observation;
- screen on/off/locked tests;
- 8-hour idle battery observation;
- CPU/temperature/RAM observation;
- reboot recovery;
- enable/disable reliability;
- Power-button fallback still works.

## Entry decision

APPROVED NOW:

- documentation;
- engine/license research;
- model acquisition/conversion experiment outside the production path;
- benchmark harness design.

BLOCKED UNTIL J2 DEVICE ACCEPTANCE PASSES:

- adding wake-word runtime/dependencies to the owner Mayra app;
- background microphone activation;
- production enable/disable UI;
- calling wake phrase delivered/device-verified.

## Sources reviewed

- Android VoiceInteractionService API documentation.
- Android SpeechRecognizer API documentation.
- sherpa-onnx keyword-spotting Android documentation.
