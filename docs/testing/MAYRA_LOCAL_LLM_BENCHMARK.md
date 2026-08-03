# Mayra AI — Motorola Local LLM Benchmark

Status: TEMPLATE — NO MODEL IS FINAL YET
Target device: Motorola Edge 70 Fusion / Android 16
Preflight: `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`
Initial runtime direction: LiteRT-LM
Initial model candidate: Qwen3-1.7B

## Candidate record

For every tested model/runtime combination record:

- model name/version/revision;
- license;
- source URL/repository reference;
- converted artifact format;
- quantization;
- model SHA-256;
- model file size;
- runtime/library exact version;
- backend: CPU/GPU/NPU;
- source commit/build package;
- owner-device RAM variant.

## Load/storage

Record:

- download/install size;
- available storage before/after;
- checksum verification time;
- cold initialization time;
- warm initialization time if cache applies;
- failure behavior with missing/corrupt model.

Pass conditions:

- [ ] UI thread is not blocked by model load;
- [ ] model can be removed/replaced without reinstalling Mayra;
- [ ] deterministic local fallback works if model is unavailable;
- [ ] no model secrets/private owner data are bundled into the artifact.

## RAM stability

Measure approximately:

- Mayra idle before model load;
- immediately after model load;
- during generation;
- after conversation closes/unload;
- after switching among Chrome/WhatsApp/Camera or similar common apps.

Pass conditions:

- [ ] no OS/System UI instability;
- [ ] Mayra can recover if Android kills the heavy model process;
- [ ] no endless reload/crash loop;
- [ ] model can be released under memory pressure.

## Latency and generation

Use the same prompts for every candidate.

Record:

- first-token latency;
- total response time;
- approximate tokens/sec where available;
- output length;
- backend/thermal state.

Test prompts:

1. Hindi casual conversation.
2. Hinglish casual conversation.
3. English casual conversation.
4. Short reminder clarification wording only — no action execution.
5. Summarize a short supplied paragraph.
6. Explain a simple phone concept in Hindi.
7. Multi-turn context over 5–10 turns.
8. Airplane-mode repeat of the same prompts.

## Quality rubric

Score each 1–5:

- Hindi naturalness;
- Hinglish naturalness;
- English clarity;
- instruction following;
- hallucination tendency;
- repetition;
- response concision;
- multi-turn consistency;
- ability to admit uncertainty.

The model does not receive credit for claiming device actions it cannot execute.

## Thermal/battery

Run at least one repeated conversation session of practical length.

Record:

- start/end battery percentage;
- approximate session duration;
- thermal warnings/visible heat;
- performance slowdown over time;
- whether backend changes behavior after heating;
- phone responsiveness after the test.

## Safety/trust-boundary regression

The local LLM must not bypass typed Mayra control architecture.

Verify:

- [ ] model text cannot directly place a call/send a message;
- [ ] model text cannot directly write personal memory;
- [ ] model cannot spoof trusted memory-use metadata;
- [ ] document routing remains structured;
- [ ] confirmation tokens remain action-bound/expiring;
- [ ] provider/local modes do not silently leak local context to network.

## Stress/failure cases

- [ ] model file deleted/corrupted;
- [ ] insufficient storage;
- [ ] initialization cancelled;
- [ ] rapid conversation cancellation;
- [ ] app backgrounded during generation;
- [ ] screen lock during generation;
- [ ] Android kills model process;
- [ ] device hot/thermal throttling;
- [ ] long prompt hits context limit.

All cases must fall back or fail cleanly without corrupting memory/reminders/documents.

## Promotion rule

A model becomes Mayra's first local conversational brain only if:

- license/distribution are acceptable;
- model conversion/runtime is reproducible;
- storage/RAM are acceptable on the 8 GB target;
- latency feels usable;
- Hindi/Hinglish quality is clearly useful;
- battery/thermal impact is acceptable;
- airplane-mode operation works;
- deterministic fallback and trust boundaries remain intact;
- benchmark results are recorded in Blueprint/Roadmap/Decision Log/Latest Snapshot.
