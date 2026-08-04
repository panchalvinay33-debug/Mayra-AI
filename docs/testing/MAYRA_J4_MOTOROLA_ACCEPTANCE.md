# Mayra AI — J4 Motorola Local Brain Acceptance

Status: **MODEL LIFECYCLE ROUND READY AFTER FRESH CI; INFERENCE ROUND BLOCKED**
Date: 2026-08-04
Target: Motorola Edge 70 Fusion / Android 16
Package: `ai.mayra.app.j4`
Label: `Mayra J4 Local Brain Test`
Preflight: `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`
Benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`

## Purpose

Prove the heavy local-model lifecycle and runtime one boundary at a time without destabilizing the proven Assistant/voice stack.

This checklist has two separate rounds:

- **Round A — model bytes / storage / integrity** — run first;
- **Round B — LiteRT-LM inference** — do not run until exact SDK is pinned and a new artifact is generated.

## Candidate model for Round A

First candidate: current upstream-supported **Gemma3-1B chat-ready `.litertlm`** reference model, approximately 557 MB in upstream documentation.

Before device use, record:

- exact model filename;
- hosting repository/source;
- exact revision if available;
- license/model-card terms;
- downloaded file bytes;
- source-provided checksum if available.

Do not rename a different model to `.litertlm` just to pass the extension gate.

## Artifact provenance — fill only after fresh CI is green

- Source commit: PENDING
- J4 workflow/run: PENDING
- Artifact name: PENDING
- Artifact ID: PENDING
- APK bytes: PENDING
- APK SHA-256: PENDING
- Zero-permission audit: PENDING
- LiteRT-LM Maven release probed by CI: PENDING
- LiteRT-LM AAR SHA-256: PENDING
- SDK class-file Java level: PENDING

Never install a stale J4 artifact after the source head changes.

# Round A — model lifecycle / integrity

## A0 — install and launch

Preconditions:

- remove an older J4 test APK if Android reports signature conflict;
- do not bypass Play Protect/security checks;
- no Android permission should be requested by J4.

Steps:

1. install exact fresh J4 artifact;
2. launch `Mayra J4 Local Brain Test`;
3. wait 10 seconds without touching anything.

PASS:

- app stays open;
- no permission dialog;
- screen shows device manufacturer/model, Android version, primary ABI, RAM, app heap class and private free storage;
- `Select Local Model` is available.

Record screenshot and the displayed RAM/private-free values.

## A1 — wrong-file rejection

1. choose any small non-`.litertlm` document if convenient;
2. return to J4.

PASS:

- J4 reports `Model import rejected`;
- the previous accepted model, if any, is not silently replaced.

Optional if no harmless test file is available.

## A2 — exact model import

1. tap `Select Local Model`;
2. select the exact Gemma3-1B `.litertlm` candidate;
3. keep J4 foreground while copying;
4. wait for completion.

PASS:

- no crash/ANR;
- UI shows `Model import verified ✓`;
- selected/copied bytes are non-zero;
- full SHA-256 is displayed;
- no Android storage/network permission is requested.

Record:

- start time;
- finish time;
- displayed model bytes;
- displayed SHA-256;
- screenshot.

If the app reports insufficient storage, record it as a safe BLOCKED result; do not delete unrelated owner files just to force the test.

## A3 — independent SHA verification

1. tap `Verify Imported Model`;
2. wait for SHA recomputation.

PASS:

- `Private model integrity PASS ✓`;
- SHA-256 exactly matches A2.

FAIL:

- any mismatch;
- app accepts different hash;
- crash/ANR.

A mismatch blocks all runtime work.

## A4 — reopen persistence

1. close J4 normally;
2. reopen J4.

PASS:

- J4 reports imported model present;
- saved model name/size/hash metadata remain visible;
- no automatic inference starts.

## A5 — Airplane-mode model lifecycle

1. enable Airplane mode;
2. reopen J4;
3. run `Verify Imported Model` again.

PASS:

- SHA verification works fully offline;
- no network prompt/error is required for lifecycle functions.

## A6 — removal

1. tap `Remove Imported Model`;
2. close/reopen J4.

PASS:

- model removal reports success;
- reopened J4 shows no imported model;
- saved checksum metadata is cleared.

## A7 — re-import

1. import the exact same model again;
2. verify SHA again.

PASS:

- same model SHA as A2/A3;
- replacement lifecycle does not require app reinstall;
- no stale partial file is accepted.

## Round A promotion rule

Round A is DEVICE PASS only if:

- launch/device diagnostics PASS;
- model import PASS;
- SHA re-verification PASS;
- reopen persistence PASS;
- Airplane-mode SHA verification PASS;
- remove/re-import PASS;
- no crash/ANR/permission expansion.

Wrong-file rejection is recommended but may be recorded NOT_RUN if no harmless file is available.

# Round B — LiteRT-LM runtime / conversation

**BLOCKED until a later exact J4 artifact explicitly contains pinned LiteRT-LM runtime integration.**

Required artifact must record exact SDK version/AAR hash and remain isolated from final Mayra.

## B0 — CPU cold engine load

1. import/re-verify exact model;
2. tap future `Load Local Brain` control once;
3. do not repeatedly tap while loading.

PASS:

- launcher remains alive;
- load occurs off UI thread;
- exact cold-load ms is shown;
- engine reaches Ready;
- no hidden network use.

If runtime fails, capture exact visible stage/error and stop the round.

## B1 — fixed prompt set

Run these exact prompts on CPU first:

1. `नमस्ते मायरा, आज तुम मेरी किस तरह मदद कर सकती हो?`
2. `Mayra, mujhe simple Hinglish mein batao ki offline AI kya hota hai.`
3. `Explain in three short sentences what an on-device AI assistant can do.`
4. `Kal subah dawa yaad dilane ke request ko confirm karne ke liye ek short line banao. Action mat karo.`
5. fixed paragraph summary from benchmark doc;
6. simple phone concept explanation in Hindi.

Record per prompt:

- first-token latency;
- total time;
- output;
- approximate tokens/sec if exposed;
- visible RAM metrics if exposed;
- owner 1–5 Hindi/Hinglish/English usefulness scores.

The model must not claim reminder/action execution in prompt #4.

## B2 — Airplane mode generation

Repeat prompts 1–3 with Airplane mode ON.

PASS: same local engine generates responses without network.

## B3 — cancellation / close / reload

- cancel one generation;
- explicitly close conversation/engine;
- load again once;
- generate one short prompt.

PASS:

- no stuck busy state;
- no duplicate runtime;
- no crash loop;
- reload works or fails cleanly with diagnostics.

## B4 — RAM / app switching

After model load and one response:

- switch briefly to one common app;
- return to J4;
- if Android kills the model process, J4 must recover cleanly rather than loop/crash.

Do not stress the device with many heavy apps simultaneously during the first proof.

## B5 — repeated conversation / thermal

Run a practical repeated conversation session and record:

- start/end battery %;
- approximate duration;
- visible heat/thermal warnings;
- slowdown over time;
- phone responsiveness after close.

## Runtime promotion rule

No local model may enter final Mayra until CPU runtime, language quality, Airplane mode, cancellation, resource release, RAM/thermal behavior and trust-boundary regressions pass. GPU/stronger-model comparison comes only after CPU stability.

## Safety / truth rules

- J4 model output is benchmark text only;
- no calls/messages/apps/reminders are executed by J4 model output;
- no memory writes;
- no document-trust metadata spoofing;
- no cloud fallback;
- no Play Protect/security bypass;
- no device-success claim without recorded physical evidence.
