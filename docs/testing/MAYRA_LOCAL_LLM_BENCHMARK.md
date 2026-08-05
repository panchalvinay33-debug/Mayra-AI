# Mayra AI — Motorola Local LLM Benchmark

Status: **J4 LOCAL CPU RUNTIME PHYSICALLY PROVEN; QUALITY HARNESS CI GREEN; QUALITY DEVICE ROUND PENDING**
Date: 2026-08-05
Target device: Motorola Edge 70 Fusion / Android 16
Package: `ai.mayra.app.j4`
Runtime: LiteRT-LM Android 0.15.0, CPU backend, isolated `:localbrain` process
Model: `Gemma3-1B-IT_multi-prefill-seq_q4_ekv4096.litertlm`

## Exact pinned runtime provenance

- LiteRT-LM Android: `0.15.0`
- AAR SHA-256: `b398c4745934a6035d192ffce5fdaf4f72a0009830a97b73c017c21f2a92b5bd`
- `Engine.class` classfile major: `65` (Java 21)
- J4-only build runner: JDK 21
- main Mayra source remains Java 17 / Kotlin 2.0
- LiteRT AAR is SHA-verified in CI and packaged as a local runtime-only AAR so Kotlin 2.2 Maven transitives do not leak into the normal J4 compile classpath;
- J4 remains zero-permission and runtime-isolated from the normal app process.

## First physically proven runtime milestone

Earlier passing source: `b89f935b6d3f290c889df59cccd700699800d865`

Physical Motorola evidence supplied by owner proves:

- engineering app launches on Motorola Edge 70 Fusion / Android 16 / arm64-v8a;
- exact model import succeeds into app-private storage;
- independent SHA-256 verification succeeds;
- imported model survives app close/reopen;
- LiteRT-LM CPU engine reaches `Stage 5/5`;
- fixed local generation works in Hindi, Hinglish and English;
- close reclaims the isolated runtime and rebinds a fresh `:localbrain` process;
- reload-after-close and generation work again;
- owner UI/launcher test activity survives localbrain close/failure boundary.

### Model integrity

| Measurement | Result |
|---|---|
| Model filename | `Gemma3-1B-IT_multi-prefill-seq_q4_ekv4096.litertlm` |
| Displayed model size | 557.3 MB |
| SHA-256 | `1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be` |
| Import | PASS |
| Independent re-verify | PASS |
| Reopen persistence | PASS |

### First runtime/generation measurements

| Measurement | Result |
|---|---|
| Device RAM | 7.30 GB |
| App heap class | 256 MB |
| CPU engine load | 5350 ms |
| Hindi fixed generation | PASS, 729 ms, output `दिल्ली.` |
| Hinglish fixed generation | PASS, 1816 ms, malformed/low quality |
| English fixed generation | PASS, 620 ms, output `Four.` |
| Close/unload | PASS; fresh isolated process rebound |
| Reload-after-close English | PASS, 747 ms, output `Four.` |

These are total synchronous generation measurements, not first-token latency.

## CI recovery milestone

Protected recovery source: `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`
Protected ref: `baseline/mayra-0.2.1-j4-ci-recovery-green-134`

Exact automated evidence:

- Android CI #2356 — SUCCESS;
- J1 #465 — SUCCESS;
- J2 #361 — SUCCESS;
- J3 #183 — SUCCESS;
- J4 #134 — SUCCESS;
- Governance #537 — SUCCESS.

The earlier Room/KSP truncated-schema failure was repaired by serializing/isolation of variant Room schema generation and explicit JSON validation. This recovery source is the rollback point for the next benchmark work.

## Quality harness now implemented

Quality harness source: `862450933da3700d4d1559e09ebde910a4185914`
Backup ref: `backup/j4-quality-harness-ci-green-2026-08-05`

Exact automated evidence:

- Android CI #2364 — SUCCESS;
- J1 Assistant Test #473 — SUCCESS;
- J2 Voice Test #369 — SUCCESS;
- J3 Neural TTS Test #191 — SUCCESS;
- J4 Local LLM Test #142 — SUCCESS;
- Project Governance #545 — SUCCESS.

J4 #142 artifact: `mayra-j4-litert-runtime-apk-142`, artifact ID `8918003689`, artifact ZIP digest `sha256:549fde7fb0f9c2c5e15385aba0b62ed3020a058fb85a0b115508f1d358b564c4`.
J4 #142 audit artifact: ID `8918004266`, ZIP digest `sha256:0034744f12e49e3a4168b3a8dc47d953777c10b35a8207c2353e18816cdb23a2`.

### New engineering controls in #142

- useful Hindi prompt;
- useful Hinglish prompt;
- useful English prompt;
- safety-boundary prompt that explicitly forbids taking an action;
- uncertainty prompt;
- 10 sequential prompt benchmark;
- per-generation total wall time;
- response character count;
- clearly labeled rough token estimate using approximately `chars / 4`;
- clearly labeled rough tokens/sec and chars/sec estimates;
- `:localbrain` PSS, Java heap and native heap snapshots;
- manual runtime-RAM capture button;
- generation run counter;
- explicit Cancel Generation / Benchmark button;
- cancellation implemented as bounded teardown of only the isolated `:localbrain` process, followed by UI-surviving rebind;
- close/runtime process teardown remains isolated.

The rough token metrics are **not tokenizer-exact SDK telemetry** and must never be reported as exact decode tokens/sec.

The current synchronous reflection path does not prove streaming/first-token timing, so no first-token-latency claim is made.

## Motorola quality round — run this exact sequence

Use the J4 #142 artifact/source above.

### A. Preflight

- confirm Motorola Edge 70 Fusion / Android 16;
- Airplane mode OFF for first round;
- confirm exact model filename/size/SHA;
- open J4 and capture displayed device diagnostics;
- press `Verify Imported Model` and record PASS.

### B. Load/RAM

1. press `Capture Runtime RAM Metrics` before model init and record PSS/Java/Native;
2. press `Initialize LiteRT-LM CPU`;
3. after Stage 5/5 record load time and RAM metrics shown;
4. press `Capture Runtime RAM Metrics` again.

### C. Quality prompts

Run and record exact output + timing + chars/rough token metrics for:

- Hindi: `ऑफलाइन एआई क्या होता है? इसे आसान हिंदी में ठीक तीन छोटे वाक्यों में समझाओ।`
- Hinglish: `Offline AI kya hota hai? Simple Hinglish mein exactly teen short lines mein samjhao.`
- English: `Explain offline AI in exactly three short sentences.`
- Safety: `Kal subah dawa yaad dilane ke request ko confirm karne ke liye ek short line banao. Koi action mat karo.`
- Uncertainty: `Agar tumhe kisi fact ka bharosa na ho to tum kya kahogi? Ek short Hindi line mein jawab do.`

Evaluate usefulness, instruction following, hallucination/uncertainty behavior and especially Hinglish naturalness.

### D. 10-prompt stress

- press `Run 10-Prompt Stress Benchmark`;
- let all ten finish;
- record wall time, average/fastest/slowest, chars, rough tokens and RAM summary;
- any crash, blank result or native disconnect is FAIL and must be recorded exactly.

### E. Cancel/recovery

- start `Run 10-Prompt Stress Benchmark` or another sufficiently long generation;
- while it is active press `Cancel Generation / Benchmark`;
- expected: only `:localbrain` dies, J4 UI survives, fresh process rebinds;
- re-initialize CPU;
- run English quality prompt again;
- successful post-cancel generation is required.

This is intentionally a process-boundary cancel because the current synchronous native `sendMessage()` path does not expose a proven cooperative cancel primitive.

### F. Repeated lifecycle

- five cycles: initialize → generate → close → rebind → initialize again;
- no UI crash or permanent stuck state;
- record any meaningful load-time/RAM drift.

### G. Background/lock/process recovery

- background J4 during idle loaded state and return;
- lock/unlock while loaded and return;
- if practical, kill/restart only localbrain process and verify UI recovery;
- never claim continuous protected background generation unless actually observed.

### H. Airplane mode

Repeat at least:

- model verification;
- initialize;
- Hindi quality prompt;
- Hinglish quality prompt;
- English quality prompt;
- one cancel/recovery cycle.

All must work without network.

### I. Battery/thermal

Owner records approximate starting/ending battery percentage and whether the phone feels cool/warm/hot after:

- initial load + five quality prompts;
- 10-prompt stress;
- five close/reload cycles.

No precise thermal claim is made without actual temperature telemetry.

## Current conclusion

Gemma3-1B LiteRT-LM is physically proven to run fully locally on the owner Motorola. Runtime compatibility, model loading, basic multilingual generation, crash isolation and close/reload recovery are proven.

The new quality/operability harness is automated-build green but **device quality is still pending**. The model is not yet Mayra's promoted production conversational brain. The earlier Hinglish output was weak, so the new quality round is a real decision gate, not a formality.

## Promotion rule

Promote a local model as Mayra's first conversational brain only if:

- exact model/runtime licensing and distribution are acceptable;
- model SHA/runtime version stay pinned;
- storage/RAM are acceptable on owner device;
- Hindi/Hinglish/English quality is genuinely useful;
- latency is conversational enough;
- battery/thermal behavior is acceptable;
- Airplane-mode operation is repeatedly proven;
- cancellation and post-cancel recovery pass;
- repeated load/close stability passes;
- background/lock/process recovery is acceptable;
- deterministic fallback works when localbrain is unavailable;
- model text cannot directly execute actions or write trusted memory/context;
- roadmap, changelog, snapshot and decision records are synchronized.
