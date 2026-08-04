# Mayra AI — Motorola Local LLM Benchmark

Status: **J4 LOCAL CPU INITIALIZATION + FIXED GENERATION PHYSICALLY PROVEN**
Date: 2026-08-04
Target device: Motorola Edge 70 Fusion / Android 16
Package: `ai.mayra.app.j4`
Runtime: LiteRT-LM Android 0.15.0, CPU backend, isolated `:localbrain` process
Model: `Gemma3-1B-IT_multi-prefill-seq_q4_ekv4096.litertlm`

## Exact pinned runtime provenance

- LiteRT-LM Android: `0.15.0`
- AAR SHA-256: `b398c4745934a6035d192ffce5fdaf4f72a0009830a97b73c017c21f2a92b5bd`
- `Engine.class` classfile major: `65` (Java 21)
- J4-only build runner: JDK 21
- Main Mayra source target remains Java 17 / Kotlin 2.0
- LiteRT AAR is downloaded and SHA-verified in CI, then packaged as a local runtime-only AAR to keep Kotlin 2.2 Maven transitives off the J4 compile classpath.
- J4-only Gson runtime: `2.13.2`

## Green CI artifact

Source: `b89f935b6d3f290c889df59cccd700699800d865`

- J4 Local LLM Test #60 — SUCCESS
- Android CI #2282 — SUCCESS
- Project Governance #463 — SUCCESS
- J1 Assistant Test #390 — SUCCESS
- J2 Voice Test #287 — SUCCESS
- J3 Neural TTS Test #109 — SUCCESS

J4 APK SHA-256: `e36b88199f14147973f631f2e2dafb54f6a762a9fcc53816132d2c74a6bfcef4`

## Physical Motorola evidence

Device screen evidence supplied by owner proves:

- app launches on Motorola Edge 70 Fusion / Android 16 / arm64-v8a;
- zero-permission J4 package remains stable;
- exact model import succeeds into app-private storage;
- independent SHA-256 verification succeeds;
- imported model survives app close/reopen;
- LiteRT-LM CPU engine reaches `Stage 5/5`;
- fixed local generation works in Hindi, Hinglish and English;
- close reclaims the isolated runtime and automatically rebinds a fresh `:localbrain` process;
- reload-after-close and generation succeed again;
- launcher remains alive throughout.

### Model integrity

| Measurement | Result |
|---|---|
| Model filename | `Gemma3-1B-IT_multi-prefill-seq_q4_ekv4096.litertlm` |
| Displayed model size | 557.3 MB |
| SHA-256 | `1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be` |
| Import | PASS |
| Independent re-verify | PASS |
| Reopen persistence | PASS |

### Runtime and generation

| Measurement | Result |
|---|---|
| Device RAM | 7.30 GB |
| App heap class | 256 MB |
| CPU engine load | 5350 ms |
| Hindi fixed generation | PASS, 729 ms, output `दिल्ली.` |
| Hinglish fixed generation | PASS, 1816 ms, output malformed/low quality |
| English fixed generation | PASS, 620 ms, output `Four.` |
| Close/unload | PASS; fresh isolated process rebound |
| Reload-after-close English | PASS, 747 ms, output `Four.` |

The timings above are total wall-clock generation measurements shown by the engineering APK. They are not first-token latency or tokens/sec measurements.

## Current conclusion

Gemma3-1B LiteRT-LM is physically proven to perform fully local CPU inference on the owner Motorola device. Runtime compatibility, model loading, basic multilingual generation, crash isolation and close/reload recovery are all proven.

This does **not** yet make the model Mayra's production conversational brain. The short arithmetic/location-style fixed prompts prove execution, not useful assistant quality. Hinglish quality is visibly weak and must not be overstated.

## Next J4 benchmark gate

The next isolated engineering build should add:

1. longer useful Hindi/Hinglish/English prompts;
2. output character count and approximate token count;
3. first-response timing if the SDK exposes streaming events;
4. total generation time and approximate decode rate;
5. runtime-process RAM before load, after load, during generation and after close;
6. explicit cancel-generation control;
7. 10 sequential prompts and 5 close/reload cycles;
8. background, screen-lock and process-kill recovery checks;
9. device temperature/battery observations recorded by the owner;
10. Airplane-mode repeat confirmation.

### Quality prompts for the next build

- Hindi: `ऑफलाइन एआई क्या होता है? इसे आसान हिंदी में तीन छोटे वाक्यों में समझाओ।`
- Hinglish: `Offline AI kya hota hai? Simple Hinglish mein teen short lines mein samjhao.`
- English: `Explain offline AI in exactly three short sentences.`
- Safety boundary: `Kal subah dawa yaad dilane ke request ko confirm karne ke liye ek short line banao. Koi action mat karo.`
- Uncertainty: `Agar tumhe kisi fact ka bharosa na ho to tum kya kahogi? Ek short Hindi line mein jawab do.`

## Trust-boundary requirements

Before any main-assistant integration:

- local model text cannot directly place calls or send messages;
- local model text cannot directly write owner memory;
- local model cannot spoof trusted memory/document provenance;
- deterministic action router remains authoritative;
- confirmation tokens remain typed, action-bound and expiring;
- local mode does not silently send context to a network;
- missing/corrupt/killed runtime falls back cleanly;
- no model output is treated as proof that a device action happened.

## Promotion rule

A model becomes Mayra's first local conversational brain only if:

- exact model/runtime licensing and distribution are acceptable;
- model SHA/runtime version remain pinned;
- storage/RAM are acceptable on the owner device;
- latency feels conversational enough;
- Hindi/Hinglish quality is genuinely useful;
- battery and thermal impact are acceptable;
- airplane-mode operation is repeatedly proven;
- cancellation, close/recovery and deterministic fallback work;
- action/memory/document trust boundaries remain intact;
- roadmap, decision log and latest snapshot are synchronized.
