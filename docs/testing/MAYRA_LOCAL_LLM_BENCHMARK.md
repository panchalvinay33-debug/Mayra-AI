# Mayra AI — Motorola Local LLM Benchmark

Status: **J4 L0 STORAGE FOUNDATION CI-GREEN; L1 INTEGRITY HARDENING + SDK PROBE UNDER FRESH CI**
Date: 2026-08-04
Target device: Motorola Edge 70 Fusion / Android 16
Preflight: `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`
Runtime direction: Google LiteRT-LM Kotlin API
First runtime candidate: Gemma3-1B chat-ready `.litertlm` (~557 MB upstream reference)

## J4 package truth

Package: `ai.mayra.app.j4`
Label: `Mayra J4 Local Brain Test`
Engineering-only, zero-permission model lifecycle/runtime benchmark.

J4 #2 source `062894809bb8b0989f79ab99db644c9d0cbdfa2d` passed:

- J4 Local LLM Test #2 — SUCCESS;
- Android CI #2224 — SUCCESS;
- Project Governance #405 — SUCCESS;
- J1 Assistant Test #333 — SUCCESS;
- J2 Voice Test #229 — SUCCESS;
- J3 Neural TTS Test #51 — SUCCESS.

That milestone proves the first owner-selected `.litertlm` import APK compiles/packages with a zero-permission boundary. It does not yet prove a physical model import or inference.

## Current L1 source additions

Fresh source after J4 #2 adds:

- `.litertlm` filename validation;
- source-size validation when Android provider reports size;
- 256 MB private-storage safety headroom;
- atomic `.partial` import then final rename;
- copy-size verification;
- SHA-256 during import;
- persistent local metadata for name/bytes/hash;
- independent `Verify Imported Model` SHA-256 pass;
- explicit model removal + metadata cleanup;
- visible device manufacturer/model, Android version, primary ABI, physical RAM, app heap class and private free storage;
- reusable `MayraLocalModelIntegrity` source boundary with tests for extension, storage overflow/headroom and SHA-256 known vectors.

## Active LiteRT-LM SDK compatibility probe

J4 CI now resolves the current Android Maven metadata **without linking the SDK into Mayra yet** and stores:

- resolved release version;
- POM;
- AAR SHA-256;
- AAR class-file major / approximate Java level;
- AAR contents.

Reason: the main Mayra project is currently Kotlin 2.0.21 / Java 17. We will not casually upgrade the whole app or pin a runtime until the exact SDK bytecode/toolchain evidence is known. J1/J2/J3/full-app stability takes priority over rushing the heavy runtime dependency.

## First physical J4 sequence — model lifecycle

When the fresh J4 artifact is green:

1. install/launch J4;
2. confirm the screen reports Motorola/Android/ABI/RAM/private-free-space diagnostics;
3. select the exact Gemma3-1B `.litertlm` candidate through Android document picker;
4. verify import reports exact bytes + SHA-256;
5. tap `Verify Imported Model` and require the same SHA-256;
6. close/reopen J4 and confirm model-present metadata survives;
7. remove model and confirm private model path clears;
8. re-import once to prove replacement lifecycle;
9. airplane mode may remain on throughout because L0/L1 has no network permission.

Record:

| Measurement | Result |
|---|---|
| J4 source | PENDING fresh head |
| J4 CI run/artifact | PENDING |
| Device RAM | PENDING screen evidence |
| Private free storage before import | PENDING |
| Model exact name/revision | PENDING |
| Model source/license | PENDING |
| Selected file bytes | PENDING |
| Imported SHA-256 | PENDING |
| Re-verify SHA-256 | PENDING |
| Reopen persistence | PENDING |
| Remove/re-import lifecycle | PENDING |

## Runtime L2/L3 sequence

Only after SDK compatibility and model-byte integrity are both proven:

1. pin exact LiteRT-LM Android Maven version and AAR hash/provenance;
2. link SDK to J4 only;
3. initialize `EngineConfig(modelPath = privateModelPath, backend = CPU)` off UI thread;
4. initialize engine and display cold load ms;
5. create one bounded conversation;
6. send fixed prompts;
7. stream response into engineering UI;
8. close conversation + engine explicitly;
9. load again for warm timing;
10. only then compare GPU, if worthwhile.

### Fixed prompts

1. Hindi: `नमस्ते मायरा, आज तुम मेरी किस तरह मदद कर सकती हो?`
2. Hinglish: `Mayra, mujhe simple Hinglish mein batao ki offline AI kya hota hai.`
3. English: `Explain in three short sentences what an on-device AI assistant can do.`
4. Reminder clarification only: `Kal subah dawa yaad dilane ke request ko confirm karne ke liye ek short line banao. Action mat karo.`
5. Summary: a fixed short supplied paragraph.
6. Phone concept in Hindi: airplane mode or app permissions explanation.
7. 5–10 turn context consistency.
8. Repeat core prompts in Airplane mode.

## Runtime measurements

For every prompt/runtime candidate record:

- cold engine initialization ms;
- warm engine initialization ms;
- first-token latency;
- total generation wall time;
- approximate tokens/sec if API/metrics permit;
- output length;
- RAM idle/load/generate/release;
- backend;
- thermal/battery observation;
- cancellation/close behavior.

## Quality rubric

Owner score 1–5:

- Hindi naturalness/usefulness;
- Hinglish naturalness/usefulness;
- English clarity;
- instruction following;
- hallucination tendency;
- repetition;
- response concision;
- multi-turn consistency;
- ability to admit uncertainty.

No credit for claiming an Android action happened when J4 did not execute it.

## Trust-boundary regression

Before production integration verify:

- [ ] local model text cannot directly place calls/send messages;
- [ ] local model text cannot directly write owner memory;
- [ ] local model cannot spoof trusted memory/document provenance;
- [ ] deterministic action router remains authoritative;
- [ ] confirmation tokens remain typed/action-bound/expiring;
- [ ] local mode does not silently send context to network;
- [ ] missing/corrupt model falls back cleanly.

## Stress/failure cases

- [ ] wrong extension rejected;
- [ ] empty model rejected;
- [ ] insufficient private storage rejected before copy;
- [ ] interrupted import leaves no accepted partial model;
- [ ] SHA mismatch detected;
- [ ] model removal works;
- [ ] engine initialization failure leaves launcher alive;
- [ ] generation cancellation;
- [ ] background/screen-lock behavior;
- [ ] Android kills heavy runtime;
- [ ] repeated load/close without crash loop;
- [ ] device hot/thermal throttling;
- [ ] context/output limit reached cleanly.

## Promotion rule

A model becomes Mayra's first local conversational brain only if:

- exact model/runtime license and distribution are acceptable;
- model SHA/runtime version are pinned;
- storage/RAM are acceptable on the owner device;
- latency feels conversational enough;
- Hindi/Hinglish quality is clearly useful;
- battery/thermal impact is acceptable;
- airplane-mode operation works;
- engine close/recovery and deterministic fallback work;
- Mayra action/memory/document trust boundaries remain intact;
- Blueprint/Roadmap/Decision Log/Latest Snapshot are synchronized.
