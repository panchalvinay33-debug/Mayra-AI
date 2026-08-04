# Mayra AI — Local Conversational Brain Feasibility Preflight

Status: **J4 STORAGE/INTEGRITY FOUNDATION GREEN; RUNTIME SDK COMPATIBILITY PROBE ACTIVE**
Date: 2026-08-04
Gate: Issue #14
Target: Motorola Edge 70 Fusion / Android 16
Rollback application baseline: `baseline/mayra-0.2.1-j2-privacy-tts-green-136`

## Owner outcome

Mayra should remain conversationally useful without OpenAI or another cloud API. The local brain should handle ordinary Hindi/Hinglish/English conversation, clarification, short summaries and bounded reasoning while existing deterministic code continues to own actions, reminders, memory writes and document trust boundaries.

## Runtime direction

Primary Android runtime direction: **Google LiteRT-LM Kotlin API**.

Current upstream facts reviewed on 2026-08-04:

- LiteRT-LM documents Kotlin as a stable Android/JVM API;
- Android CPU/GPU/NPU backends are supported by the framework;
- the Kotlin API exposes `Engine`, `EngineConfig`, `Conversation` and coroutine/Flow-based streaming;
- model files use the `.litertlm` format;
- upstream recommends model initialization off the UI thread because initialization can take seconds;
- official supported-model documentation lists Gemma3-1B at about **557 MB**, 4-bit per-channel, 4096 context as the smallest chat-ready reference candidate.

References:
- https://github.com/google-ai-edge/LiteRT-LM
- https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md

## First benchmark candidate

### Candidate A — Gemma3-1B LiteRT-LM

Role: **first runtime/proof candidate**.

Why first:

- explicitly listed by the LiteRT-LM project as a supported chat-ready `.litertlm` model;
- approximately 557 MB, much smaller than the Gemma-3n reference models;
- 4-bit model class makes it plausible for a first Motorola RAM/storage proof;
- first goal is runtime compatibility and usable latency, not declaring the final Mayra brain.

Do not claim strong Hindi/Hinglish quality before device evaluation.

### Candidate B — later multilingual comparison

Only after Candidate A is technically stable, compare another exact LiteRT-LM-supported multilingual model if it materially improves Hindi/Hinglish quality. The candidate must be re-confirmed against the current upstream supported-model table and exact `.litertlm` artifact before coding. Old shortlist names are not treated as commitments.

## J4 architecture stages

### L0 — model lifecycle boundary — DONE / CI GREEN

Dedicated package `ai.mayra.app.j4`:

- Android document picker only;
- zero Android permissions;
- `.litertlm` filename gate;
- app-private model storage;
- no large model bundled in APK;
- remove/replace without reinstalling Mayra.

### L1 — integrity + device diagnostics — IMPLEMENTED / FRESH CI PENDING

- atomic `.partial` → final model import;
- selected-size vs copied-size verification;
- SHA-256 computed during import;
- saved model metadata;
- independent re-verification button;
- 256 MB private-storage safety headroom;
- device ABI / RAM / heap-class / private-free-space diagnostics;
- reusable `MayraLocalModelIntegrity` production boundary with unit tests.

### L2 — LiteRT-LM SDK compatibility — ACTIVE CI PROBE

Before linking the runtime, J4 CI downloads the current Android Maven AAR only as evidence and records:

- resolved Maven release;
- AAR SHA-256;
- class-file major/approximate Java level;
- AAR contents/provenance.

This is deliberately separate from production dependency wiring so a Java/Kotlin/toolchain mismatch cannot destabilize J1/J2/J3 or the full app.

### L3 — crash-contained runtime initialization — NEXT

After L2 evidence is green:

1. pin one exact LiteRT-LM Android SDK version;
2. link it to J4 only;
3. initialize `Engine` from the verified app-private model path on a worker/coroutine;
4. use CPU for the first compatibility proof; GPU becomes a later comparison;
5. contain heavy-runtime failure from the launcher where practical, following the successful J3 isolation pattern;
6. show exact load time/error state instead of closing the app;
7. support explicit close/reload.

### L4 — conversation benchmark

Fixed prompts:

1. Hindi casual conversation;
2. Hinglish casual conversation;
3. English casual conversation;
4. reminder clarification wording only — no action execution;
5. summarize a short supplied paragraph;
6. explain a simple phone concept in Hindi;
7. 5–10-turn context;
8. airplane-mode repeat.

Record first-token latency, total time, approximate tokens/sec, RAM, thermal behavior and owner quality score.

## Architecture boundary

The local LLM is **never** authority for privileged actions.

Required final pipeline:

1. speech/text enters Mayra;
2. deterministic router classifies conversation/document/reminder/action;
3. local LLM handles bounded conversational generation/summarization only where appropriate;
4. action execution remains typed/capability-gated and confirmation-safe;
5. memory writes remain proposal/approval controlled;
6. document retrieval/citations remain structured;
7. model failure falls back to deterministic Mayra rather than breaking the assistant.

## Storage / RAM engineering targets

- base APK must not absorb a 500 MB–multi-GB model;
- model is owner-managed/downloadable and checksum-pinned;
- preserve at least 256 MB import headroom in J4; production policy may require more after runtime measurements;
- leave enough RAM for Android/System UI and ordinary apps;
- model initialization never blocks UI thread;
- engine can be released under memory pressure/session end;
- no endless model reload/crash loop.

## Performance targets

Record on Motorola:

- cold and warm engine initialization;
- warm first-token latency;
- decode speed/tokens per second;
- RAM before load / after load / during generation / after release;
- process recovery after app switching;
- battery and thermal observations;
- Hindi/Hinglish/English quality;
- airplane-mode operation.

Promotion requires owner-perceived conversational speed and acceptable phone stability. No synthetic benchmark alone is sufficient.

## Model distribution and integrity

- exact model source/revision/license/size/SHA-256 recorded before benchmark promotion;
- imported/downloaded bytes verified before runtime use;
- corrupt/missing model never reaches engine initialization;
- model removable/replacable without reinstall;
- no Hugging Face/API credential embedded in GitHub or APK;
- remote downloader remains a later explicit owner-controlled feature, not part of J4 L0/L1.

## Privacy

Local prompts/results stay on-device unless the owner separately enables a cloud provider path. Provider/local mode must remain distinguishable. Private memory/document context must never silently fall through to network.

## Failure/fallback UX

If model initialization fails, model is missing/corrupt, storage/RAM is insufficient, device is hot, generation is cancelled or the runtime process dies:

- cancel/close cleanly;
- release resources;
- keep launcher/session alive where possible;
- continue with deterministic Mayra fallback;
- expose concise diagnostics only in engineering/setup surfaces.

## Promotion rule

A local model becomes Mayra's first conversational brain only when:

- runtime and model license/distribution are acceptable;
- exact SDK/model versions are pinned;
- Motorola RAM/storage/latency/thermal tests pass;
- Hindi/Hinglish quality is useful;
- airplane mode works;
- cancellation/release/recovery work;
- deterministic fallback and action/memory/document trust boundaries remain intact;
- Blueprint/Roadmap/Decision Log/Latest Snapshot are synchronized.
