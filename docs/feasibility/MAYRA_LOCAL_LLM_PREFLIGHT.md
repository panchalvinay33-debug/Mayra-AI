# Mayra AI — Local Conversational Brain Feasibility Preflight

Status: APPROVED FOR ISOLATED MOTOROLA BENCHMARK — PRODUCTION INTEGRATION BLOCKED UNTIL DEVICE EVIDENCE
Date: 2026-08-04
Gate: Issue #14
Target: Motorola Edge 70 Fusion / Android 16, 8/12 GB RAM variants
Rollback application baseline: `baseline/mayra-0.2.1-j2-privacy-tts-green-136`

## Owner outcome

Mayra should remain conversationally useful without OpenAI or another cloud API. The local brain should handle ordinary Hindi/Hinglish/English conversation, clarification, short summaries and bounded reasoning while existing deterministic code continues to own actions, reminders, memory writes and document trust boundaries.

## Current runtime direction

Primary Android runtime direction: **Google LiteRT-LM**.

Current upstream review on 2026-08-04 shows LiteRT-LM as a production-oriented cross-platform edge LLM framework with Android CPU/GPU support, stable Kotlin APIs and `.litertlm` model packaging. The upstream repository currently advertises v0.13-era functionality and broad model-family support including Gemma, Qwen, Phi and others.

Do not hard-code an old SDK/model assumption. Exact Maven/runtime version, Java/Kotlin compatibility and Android ABI/backend behavior must be pinned by the benchmark commit and CI before device installation.

## First benchmark candidates

### Candidate A — Gemma3-1B LiteRT-LM

Role: **first runtime/proof candidate**.

Why first:

- upstream LiteRT-LM currently lists a chat-ready 4-bit Gemma3-1B `.litertlm` model around 557 MB;
- substantially smaller than the stronger multilingual alternatives;
- better first choice for proving load/cancel/fallback/process survival on the Motorola without immediately consuming ~1.5 GB+;
- suitable for measuring whether the LiteRT-LM Android path itself is stable.

This candidate is selected for runtime proof, not because it is assumed to be the final Hindi/Hinglish brain.

### Candidate B — Qwen2.5-1.5B LiteRT-LM

Role: **multilingual quality comparison after Candidate A is stable**.

Upstream LiteRT-LM currently lists a chat-ready qwen2.5-1.5b model around 1524 MB. Qwen is multilingual and therefore a strong comparison candidate for Mayra's Hindi/Hinglish/English target, but its larger storage/RAM cost makes it a second step.

### Qwen3-1.7B

Qwen3 remains an interesting Apache-2.0 multilingual family, but the earlier plan naming Qwen3-1.7B as the first integration target is no longer treated as mandatory. We will not invent a conversion pipeline merely to preserve an old shortlist when currently supported `.litertlm` candidates can establish device evidence faster.

## Architecture boundary

The local LLM is **never** the authority for privileged actions.

Required pipeline:

1. Mayra receives typed transcript/text.
2. Existing deterministic router decides whether the request is conversation, document, reminder or controlled action.
3. Local LLM handles bounded conversational generation/summarization only where routing permits it.
4. Sensitive actions remain typed/capability-gated and confirmation policy stays outside free-form model output.
5. Personal-memory writes remain proposal/approval controlled.
6. Document citations/grounding remain structured retrieval, not model invention.

## Engineering package boundary

The first local-brain proof must use a dedicated engineering package/build type rather than silently adding a large model to normal Mayra.

Required properties:

- no bundled multi-hundred-MB/GB model in the ordinary app artifact;
- explicit owner-selected/imported/downloaded model file;
- exact model SHA-256 and source/version recorded;
- UI remains responsive during model load;
- heavy runtime is crash-contained where practical;
- cancel/unload is explicit;
- deterministic fallback remains usable after model failure;
- no model output can directly invoke Android actions.

## Storage and RAM budget

Targets to benchmark, not claims:

- first runtime-proof model ideally < 700 MB;
- stronger comparison model may be ~1.5 GB if the device has headroom;
- model stored as removable owner-managed data rather than permanent base-APK weight;
- model initialization must not block the UI thread;
- runtime must release resources cleanly after cancellation/unload or memory pressure;
- Android/System UI and common foreground apps must remain stable.

## Performance targets

Device benchmark records:

- model checksum and bytes;
- cold initialization time;
- warm initialization/cache behavior;
- first-token latency;
- sustained decode tokens/sec;
- total response time;
- RAM before load / after load / during generation / after unload;
- battery and thermal behavior over repeated conversations;
- process survival while switching to common apps;
- Hindi/Hinglish/English quality;
- airplane-mode operation.

Promotion requires useful conversational speed and acceptable device stability; a model that technically runs but makes the phone unstable or excessively hot fails.

## Context policy

Start small and bounded:

- short recent-conversation window;
- explicit retrieved memory snippets only;
- explicit document snippets only when structured routing selected document mode;
- strict input/output token limits;
- no unlimited transcript accumulation;
- thinking/reasoning mode disabled or tightly bounded for ordinary chat unless a benchmark proves its value.

## Model distribution and integrity

- no automatic multi-GB model download in the base app;
- owner initiates model install/download;
- record model source, license, version and SHA-256;
- verify checksum before activation;
- support removal/replacement without reinstalling Mayra;
- preserve deterministic local fallback if model asset is missing/corrupt;
- gated/private model repositories must never require embedding a secret token in the APK.

## Privacy

Local-model prompts/results stay on-device unless the owner explicitly selects a separate cloud-provider path.

Provider and local modes remain distinguishable in diagnostics. Local memory/document data must not silently leave the device through a remote fallback.

## Failure/fallback UX

If model initialization fails, model is missing/corrupt, storage/RAM is insufficient, device is too hot, generation is cancelled or the model process dies:

- cancel cleanly;
- release resources;
- keep the launcher/session alive where possible;
- continue with deterministic Mayra fallback;
- expose concise diagnostics only in the engineering/setup surface.

## Benchmark sequence

### Phase L1 — runtime proof

Use the smaller supported Gemma3-1B LiteRT-LM candidate:

- load from owner-managed local storage;
- one Hindi, one Hinglish and one English prompt;
- cancellation;
- unload/reload;
- Airplane mode;
- collect timing/RAM/thermal evidence.

### Phase L2 — quality comparison

If L1 is stable, compare qwen2.5-1.5b on the same prompt set and device conditions. Prefer the stronger candidate only if quality gain justifies storage/RAM/thermal cost.

### Phase L3 — Mayra integration

Only after a candidate passes device benchmark:

- implement `MayraLocalConversationalProvider` behind the existing routing/provider boundary;
- keep deterministic action/memory/document authority unchanged;
- connect recognized voice → routed local brain → existing speech-output boundary;
- retain Android offline TTS fallback and separately license-clear neural voice work.

## Entry decision

APPROVED NOW:

- isolated LiteRT-LM Android benchmark harness;
- Gemma3-1B runtime proof;
- qwen2.5-1.5b quality comparison if runtime proof is stable;
- owner-managed model storage/checksum architecture;
- crash/cancel/fallback diagnostics.

BLOCKED UNTIL MOTOROLA BENCHMARK:

- production model bundling;
- declaring any model final;
- replacing deterministic routing/action boundaries;
- claiming offline conversational quality/performance;
- permanently resident heavy model process.

## Public sources reviewed 2026-08-04

- Google AI Edge LiteRT-LM repository/readme and supported-model table.
- Google AI Edge LiteRT-LM Android build/run documentation.
- Qwen official repository/license information.
