# Hindi/Hinglish Voice Intelligence Architecture

**Status:** Locked architecture; implementation deferred until Personal Alpha V0.1 acceptance  
**Owner branch for this documentation lock:** `stabilize/living-companion-v0.1` / PR #11  
**Future implementation branch:** `agent/hindi-hinglish-voice-intelligence` from the accepted integrated Personal Alpha source  
**Rule:** Do not add offline speech packages, model binaries or replace the current voice engine until the Personal Alpha gate is accepted and the Motorola benchmark plan has been run.

## 1. Goal

Mayra must reliably understand Indian Hindi, Roman Hinglish, mixed Hindi-English, compact/fast speech, personal names, places, contacts, business words and common speech-to-text mistakes. Voice output should remain fast, natural, mostly free and useful offline.

This milestone improves understanding without weakening Action Safety. High recognition confidence never bypasses confirmation for calls, messages, deletion, payments, privacy-sensitive sharing, security changes or other protected actions.

## 2. Audit summary

### Already implemented and reusable

- Android `SpeechRecognizer` baseline.
- Android `TextToSpeech` baseline.
- Hindi (`hi-IN`), Hinglish (`en-IN`) and English (`en-IN`) settings.
- partial recognition results.
- up to three recognition alternatives requested from Android.
- first-result confidence capture.
- continuous listen/speak turn loop with bounded recovery.
- transcript and response size limits.
- duplicate transcript and duplicate spoken-response suppression.
- offline/high-quality Android TTS voice preference.
- TTS rate, pitch and basic markdown/link cleanup.
- existing identity resolution and Action Safety confirmation boundaries.

### Reusable with changes

- `AndroidVoiceAssistant` remains the Android adapter but should emit all recognition alternatives rather than selecting the first result immediately.
- `RealtimeVoiceLoopPolicy` remains responsible for bounded turn-taking and duplicate suppression.
- `MayraSpeechTextPolicy` remains the TTS preparation boundary and can later gain pronunciation rules.
- existing contact identity, reminder grammar, command routing and action safety engines should provide context signals to transcript interpretation.

### Missing

- explicit Android on-device recognizer selection where supported.
- recognition-alternative scoring and agreement analysis.
- dedicated Hindi/Hinglish normalization pipeline.
- owner-approved personal vocabulary and correction mappings.
- contextual entity correction for names, places, contacts and tent-house terms.
- combined acoustic, language, entity and action-risk confidence policy.
- downloadable offline model manager.
- wake-word/keyword-spotting engine distinct from continuous listening.
- Motorola device benchmarks for accuracy, latency, memory, battery and thermal behavior.
- pronunciation normalization for amounts, dates, abbreviations and mixed technical terms.

## 3. Locked baseline architecture

```text
Microphone
→ Android SpeechRecognizer adapter
→ recognition alternatives + platform confidence
→ script/language detection
→ Hindi/Hinglish transcript normalizer
→ personal vocabulary and entity correction
→ command/context interpretation
→ effective confidence and ambiguity policy
→ existing Action Safety Core
→ execute, display interpretation, ask clarification or request confirmation
```

Android SpeechRecognizer and Android TextToSpeech remain the first free implementation. No third-party offline model is selected by this architecture lock.

## 4. Primary speech recognition

### Baseline

- use Android SpeechRecognizer.
- prefer the on-device recognizer on supported Android versions/devices.
- gracefully fall back to the available system recognizer.
- support `hi-IN` and `en-IN` profiles.
- retain partial results for UI feedback only; do not execute from partial text.
- retain multiple final alternatives with their confidence values when supplied.
- never fabricate confidence when the platform omits it; mark it unavailable and use other signals.

### Alternative selection

Each final candidate should be scored from:

- platform acoustic confidence when available.
- agreement between alternatives.
- language/script compatibility.
- personal vocabulary matches.
- contact/place/business entity matches.
- command grammar fit.
- current conversational context.
- ambiguity and risky-action penalties.

The chosen candidate, rejected alternatives and reason codes should be available to private diagnostics without storing raw audio.

## 5. Hindi/Hinglish understanding layer

The understanding layer must be pure Kotlin where possible so Android SpeechRecognizer, future Whisper, Vosk or Sherpa transcripts can use the same pipeline.

### Stages

1. whitespace, punctuation and number normalization.
2. Devanagari/Roman-script detection.
3. conservative Roman Hinglish normalization.
4. owner-approved personal vocabulary correction.
5. contact, place and business entity resolution.
6. command/entity correction using current context.
7. ambiguity scoring.
8. locally learned correction proposal.

### Initial vocabulary

- Mayra
- Vinay
- Pitol
- Jhabua
- Shree Shyam
- tent house
- takat
- sabbal
- ceiling
- pipe
- payment
- reminder
- frequently used contact names approved by the owner

### Vocabulary record

A future local record should include:

- canonical form.
- aliases/misrecognitions.
- category.
- owner-approved flag.
- sensitivity.
- usage count.
- last-confirmed time.
- source and confidence.
- revocation/deletion support.

Corrections must be contextual. A general word must not always be rewritten as a tent-house term merely because it appears in the vocabulary.

## 6. Effective confidence policy

SpeechRecognizer confidence alone is not authoritative.

```text
effective confidence =
platform confidence
+ alternative agreement
+ language/script fit
+ vocabulary/entity fit
+ command grammar fit
- ambiguity penalty
- risky-action penalty
```

### Policy

- **High confidence, safe action:** execute through normal capability and safety checks.
- **Medium confidence, safe action:** display Mayra's interpretation before execution where misunderstanding matters.
- **Low confidence:** ask the owner to repeat or choose between bounded alternatives.
- **Any risky action:** follow existing Action Safety confirmation regardless of confidence.
- **Ambiguous contact/entity:** never guess; request clarification.

Protected actions include calls, messages, deletion, payments, OTP/PIN/password handling, private-data sharing, legal acceptance, account/security changes and other critical actions.

## 7. Offline fallback evaluation

Offline engines are evaluation candidates, not selected dependencies.

### Whisper.cpp candidate

Evaluate only for difficult, open-ended or low-confidence Hindi/Hinglish if Motorola performance permits. Measure model size, first-token/final latency, RAM, CPU, thermal load, battery and word/entity accuracy.

### Vosk candidate

Evaluate for lightweight fixed-command grammars only if it materially improves latency or offline availability. Do not assume open-ended Hinglish quality.

### Sherpa-ONNX candidate

Evaluate for keyword spotting, streaming ASR or future offline TTS only when it offers measurable benefits over the Android baseline.

### Model strategy

- do not bundle every model in the base APK.
- models are optional downloadable components.
- show model language, version, size and device requirements before download.
- verify checksum/signature before activation.
- allow removal and rollback.
- keep a no-model Android-only mode.
- never download on metered data without explicit owner approval.

## 8. Wake word boundary

Wake word is separate from the conversation loop.

- continuous SpeechRecognizer listening is not considered a wake-word implementation.
- keyword spotting must have a visible enable/disable control.
- background microphone use requires clear disclosure and a visible foreground-service notification where Android requires it.
- benchmark idle battery drain and false accepts/rejects.
- Global Stop must disable wake-word/background listening.

## 9. Voice output

Android TextToSpeech remains primary.

Future pronunciation normalization should cover:

- dates and times.
- amounts and measurements.
- phone numbers where safe to speak.
- abbreviations.
- English technical terms inside Hindi sentences.
- Hindi words inside English/Hinglish sentences.
- approved contact, place and business pronunciations.

Prefer downloaded non-network Hindi/English Android voices where available. Evaluate Sherpa offline TTS only if Android TTS quality remains insufficient after pronunciation normalization and device testing.

## 10. Privacy rules

- raw audio is not persisted by default.
- raw transcripts, personal vocabulary and correction mappings remain local by default.
- personal vocabulary is owner data and should use protected local storage appropriate to sensitivity.
- contacts, addresses, private names and full transcripts are not sent to an external provider unnecessarily.
- cloud AI remains optional and receives only the minimum text/context required by an explicit feature policy.
- diagnostic recordings require explicit temporary opt-in, bounded retention and visible deletion controls.
- no voice data, vocabulary or model telemetry enters Git, CI artifacts or logs.

## 11. Testing plan

Create deterministic and physical-device coverage for:

- Hindi in Devanagari.
- Roman Hinglish.
- mixed Hindi-English.
- fast compact speech.
- short commands and longer natural requests.
- wrong transcription correction.
- Mayra, Vinay, Pitol and Jhabua.
- Shree Shyam and tent-house vocabulary.
- contact names and ambiguous contacts.
- amounts, dates, times and reminder commands.
- ambiguous commands.
- risky-action confirmation.
- duplicate transcript suppression.
- offline mode.
- no on-device recognizer available.
- low-confidence fallback.
- noisy room, fan, road and event/tent-house environments.
- Bluetooth and phone microphone differences where available.
- Motorola latency, memory, battery and thermal behavior.

### Required benchmark outputs

- exact device model and Android version.
- recognizer/provider and language profile.
- test-corpus version.
- cold and warm latency.
- command accuracy.
- personal-name/entity accuracy.
- false correction rate.
- low-confidence clarification rate.
- peak memory.
- APK and optional model size.
- battery and thermal observations.

## 12. Implementation phases

### Phase V0 — Architecture lock (now)

- audit existing PR #9 and PR #11 voice foundations.
- lock this document and roadmap references.
- add no packages or models.

### Phase V1 — Baseline instrumentation (after Personal Alpha acceptance)

- preserve all recognition alternatives and confidence metadata.
- select on-device recognizer where supported.
- add private bounded diagnostics and benchmark harness.

### Phase V2 — Deterministic Hinglish normalization

- pure Kotlin normalizer.
- script/language signals.
- static owner vocabulary.
- tests for Hindi, Roman Hinglish, mixed speech and tent-house terms.

### Phase V3 — Personal correction memory

- owner-approved aliases and correction mappings.
- local protected persistence.
- contact/place/business context integration.
- review, revoke and clear controls.

### Phase V4 — Confidence and safety integration

- effective confidence calculation.
- interpretation preview.
- bounded alternatives and clarification.
- mandatory existing Action Safety handling for protected actions.

### Phase V5 — Motorola benchmark gate

- Android baseline benchmark.
- noisy environment tests.
- latency, memory, battery and accuracy report.

### Phase V6 — Optional offline fallback experiment

- evaluate one candidate at a time.
- use optional downloadable models.
- compare against Android baseline.
- accept only measurable benefit within device limits.

### Phase V7 — Wake word and advanced offline TTS

- separate opt-in evaluation.
- visible background operation.
- Global Stop integration.
- battery/privacy acceptance gate.

## 13. Pending decisions

- exact Motorola model, RAM and Android version.
- whether the OEM recognizer supports true on-device `hi-IN` and `en-IN`.
- acceptable voice-command latency target.
- acceptable optional model-download size.
- vocabulary encryption/storage implementation.
- whether corrections are suggested or automatically learned after repeated owner confirmation.
- benchmark corpus recording method without retaining private audio.
- whether a wake word is valuable enough to justify background battery/privacy cost.

## 14. Branch and release ownership

Documentation lock belongs to PR #11 because it is part of the Personal Alpha source-of-truth and stabilization controls.

Implementation must not be mixed into PR #11 or the document-intelligence PR. After Personal Alpha acceptance, create `agent/hindi-hinglish-voice-intelligence` from the accepted integrated source and open a dedicated voice PR.

No implementation phase is complete until compile, complete unit tests, lint, verified APK provenance and relevant Motorola physical-device tests pass.