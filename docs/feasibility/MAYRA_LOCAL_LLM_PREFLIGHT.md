# Mayra AI — Local Conversational Brain Feasibility Preflight

Status: PRELIMINARY APPROVAL FOR BENCHMARKING ONLY — MODEL INTEGRATION BLOCKED UNTIL DEVICE BENCHMARK
Date: 2026-08-03
Gate: Issue #14
Target: Motorola Edge 70 Fusion / Android 16, 8/12 GB RAM variants
Rollback source before future integration: `baseline/mayra-0.2.1-j2-voice-green-18`

## Owner outcome

Mayra should remain conversationally useful without OpenAI or another cloud API. The local brain should handle ordinary Hindi/Hinglish/English conversation, intent clarification, short summaries and bounded reasoning while existing deterministic code continues to own sensitive actions, reminders, memory writes and document trust boundaries.

## Runtime direction

Primary Android runtime direction for benchmarking: **Google LiteRT-LM**.

Reasons:

- current Android/JVM Kotlin API;
- on-device execution;
- CPU/GPU/NPU backend options;
- production-oriented model orchestration;
- conversation API and broader model support;
- avoids building the new long-term architecture around deprecated/maintenance-only mobile LLM paths.

No runtime is final until it is proven on the actual Motorola.

## Initial model shortlist

Primary benchmark candidate: **Qwen3-1.7B**.

Reasons to benchmark:

- Apache-2.0 model license;
- multilingual/conversational model family;
- small enough to be plausible for an 8 GB owner device after quantization;
- large enough to test whether quality is meaningfully better than the existing deterministic fallback.

A second small model may be benchmarked for comparison before selection. The project does not lock itself to Qwen until conversion/runtime/device results are proven.

## Architecture boundary

The local LLM is **not** allowed to become the authority for privileged actions.

Required pipeline:

1. Mayra receives typed transcript/text.
2. Existing deterministic router decides whether the request is conversation, document, reminder or controlled action.
3. Local LLM handles bounded conversational generation/summarization where appropriate.
4. Sensitive actions remain typed/capability-gated and confirmation policy remains outside free-form model output.
5. Personal-memory writes remain proposal/approval controlled.
6. Document citations/grounding remain structured retrieval, not model invention.

## Hard boundaries

- Do not bundle a large model blindly before measuring package/storage/RAM impact.
- Do not keep a multi-GB model permanently resident merely for wake-word detection.
- No remote dependency is required for the local mode after model installation.
- If the local model fails to load or is killed under memory pressure, Mayra must fall back to the deterministic local engine rather than becoming unusable.
- Model output cannot directly execute calls/messages/device actions.

## Storage and RAM budget — engineering targets

These are targets to benchmark, not current claims:

- initial model download ideally around or below ~1.5 GB;
- model stored as an owner-manageable downloadable asset rather than inflating the base APK;
- peak Mayra memory should leave enough headroom for Android/System UI and ordinary foreground apps on the 8 GB device;
- target cold-load time should not block the UI thread;
- engine must unload/release cleanly when Android memory pressure or owner settings require it.

A 1.7B model at 4-bit is theoretically in the sub-1 GB raw-weight range before runtime/metadata overhead, but actual converted artifact size and runtime RAM must be measured rather than assumed.

## Performance targets

Device benchmark must record:

- model initialization/cold-load time;
- warm first-token latency;
- sustained generation speed;
- RAM before load, after load and during generation;
- storage size/checksum;
- battery change over repeated conversations;
- thermal behavior and throttling;
- process survival when switching to other common apps;
- quality in Hindi, Hinglish and English;
- airplane-mode operation.

Promotion requires a user-perceived response speed that feels conversational on the target device and does not make the phone unstable or unreasonably hot.

## Context policy

Start small and bounded:

- short recent-conversation window;
- explicit retrieved memory snippets only;
- explicit document snippets only when routing selected document mode;
- strict maximum input/output lengths;
- no unlimited transcript accumulation.

The context budget should expand only after RAM/latency tests.

## Model distribution and integrity

- Base app should not automatically ship a multi-GB model unless later evidence justifies it.
- Prefer first-run/owner-initiated model download through a trusted channel.
- Record model version, source, license and SHA-256.
- Verify checksum before activation.
- Support model replacement/removal without reinstalling Mayra.
- Preserve deterministic local fallback if the model asset is missing/corrupt.

## Privacy

Local-model prompts/results stay on-device unless the owner explicitly enables a separate cloud provider path.

Provider and local-model modes must remain distinguishable in diagnostics. Local memory/document data must not silently leave the device through a remote fallback.

## Failure/fallback UX

If model initialization fails, model file is missing/corrupt, RAM is insufficient, device is hot or generation fails:

- cancel cleanly;
- release model resources;
- continue with deterministic Mayra fallback;
- show concise diagnostic status only in Setup/diagnostics, not technical clutter in normal chat.

## Benchmark entry decision

APPROVED NOW:

- LiteRT-LM proof-of-concept research;
- Qwen3-1.7B conversion/runtime investigation;
- benchmark harness design;
- model download/checksum/storage architecture design;
- comparison with one smaller suitable model if available.

BLOCKED UNTIL MOTOROLA BENCHMARK:

- bundling a local model in the production Mayra APK;
- declaring Qwen3-1.7B as final;
- replacing the deterministic router/action boundary;
- claiming offline conversational quality/performance;
- keeping model permanently resident.

## Sources reviewed

- Google AI Edge LiteRT-LM Android/Kotlin documentation.
- Google LiteRT Android inference documentation.
- Qwen3-1.7B official model card/license metadata.
