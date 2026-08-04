# Mayra AI — Execution Roadmap

Last updated: 2026-08-04
Entry point: `START_HERE.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
Neural TTS preflight: `docs/feasibility/MAYRA_NEURAL_TTS_PREFLIGHT.md`
Neural TTS benchmark: `docs/testing/MAYRA_NEURAL_TTS_BENCHMARK.md`
Local LLM preflight: `docs/feasibility/MAYRA_LOCAL_LLM_PREFLIGHT.md`
Local LLM benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`
Canonical product issue: #13
Mandatory feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; Power trigger invokes orb | Preserve regression baseline |
| J2 on-device recognition | CORE DEVICE ACCEPTED | Hindi/Hinglish/English transcript, direct dismissal, 20 cycles, locked invocation and reboot/no-speech/rapid tests pass | Preserve in consolidated regressions |
| Lock-screen privacy | DEVICE VERIFIED FOUNDATION | Keyguard-aware transcript/speech suppression physically exercised | Preserve in future integrations |
| Android system TTS | FALLBACK PASS | Offline speech works but owner finds it robotic | Keep as safe fallback |
| Free neural TTS | DEVICE BENCHMARK PASS / LICENSE BLOCKED | J3 #29 loads/speaks fully offline, RTF 0.72, stability round passed | Find production-license-clear voice using proven boundary |
| Voice actions | SAFE FOUNDATION | Voice bridge understands app/reminder intent without false execution claims | Connect to typed confirmation-safe runtime |
| Wake phrase | BENCHMARK | Continuous SpeechRecognizer loop rejected | Dedicated KWS battery/false-trigger benchmark |
| Local LLM | ACTIVE J4 | J4 #2 zero-permission model-import harness green; hardened SHA/storage/device diagnostics + SDK provenance probe under fresh CI | Pin LiteRT-LM SDK, import Gemma3-1B on Motorola, then runtime init |
| Calls | ACCEPTED / GATED | Default Phone/InCallService preflight complete | No role takeover before full UI/runtime |
| Trusted install | IN_PROGRESS | Stable owner signing/trusted distribution required | Private certificate + upgrade proof |

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136, J1 #239, Android CI #2131, Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`

J3/J4 remain engineering evidence packages and do not replace the protected production application baseline.

## J3 neural TTS device evidence

Passing source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`

- J3 #29 / Android CI #2202 / J2 #207 / J1 #311 / Governance #383: success;
- app-private model materialization: PASS;
- sherpa native constructor/model load/synthesis/playback: PASS;
- observed sample: 3091 ms generation for 4297 ms audio, RTF 0.72;
- six phrases reported fine; phrase #1 preferred;
- Airplane mode, Stop cleanup and 20 sequential replies reported pass.

The Priyamvada benchmark voice remains production-license blocked. Android system TTS stays fallback while a license-clear neural pack is researched independently.

## J4 local brain progress

### Green foundation

Source `062894809bb8b0989f79ab99db644c9d0cbdfa2d`:

- J4 Local LLM Test #2 — SUCCESS;
- Android CI #2224 — SUCCESS;
- Project Governance #405 — SUCCESS;
- J1 #333 / J2 #229 / J3 #51 — SUCCESS.

This proves a dedicated zero-permission J4 package and owner-selected `.litertlm` import foundation without bundling a huge model.

### Hardened L1 now in source

- reusable `MayraLocalModelIntegrity` boundary + unit tests;
- `.litertlm` filename check;
- storage headroom/overflow checks;
- atomic `.partial` import;
- selected/copied byte verification;
- SHA-256 import + independent re-verification;
- saved name/bytes/hash metadata;
- remove/re-import lifecycle;
- visible device ABI/RAM/heap/private-storage diagnostics.

### SDK compatibility probe now in J4 CI

Before linking LiteRT-LM, CI resolves the current Android Maven release as evidence and records the AAR SHA-256, POM, class-file major/approximate Java level and contents. This prevents an unmeasured Java/Kotlin/toolchain upgrade from destabilizing the mature assistant/voice tracks.

## Local LLM runtime plan

First device candidate: **Gemma3-1B chat-ready `.litertlm` (~557 MB upstream reference)**.

Ordered gate:

1. fresh J4/J1/J2/J3/Android/Governance green on hardened L1;
2. install J4 and verify Motorola RAM/storage diagnostics;
3. owner imports exact Gemma3-1B model;
4. require SHA-256 import + re-verify + reopen persistence + remove/re-import pass;
5. inspect CI SDK provenance and pin exact LiteRT-LM Android version;
6. link runtime to J4 only;
7. CPU engine initialization off UI thread with exact load/error diagnostics;
8. bounded Hindi/Hinglish/English conversation benchmark;
9. record cold/warm load, first-token latency, tokens/sec, RAM and thermal behavior;
10. only then consider GPU and a stronger multilingual comparison.

## Non-negotiable local-brain boundaries

- model text never directly executes calls/messages/device actions;
- model text never directly writes personal memory;
- document retrieval/provenance remains structured;
- confirmation tokens remain typed/action-bound/expiring;
- local mode never silently sends owner context to network;
- missing/corrupt/killed model falls back to deterministic Mayra;
- large model stays removable owner-managed data, not base APK weight.

## Immediate next actions

1. Settle hardened J4 L1 + SDK provenance CI.
2. Produce exact J4 artifact provenance and Motorola model-import checklist.
3. Import/verify Gemma3-1B on Motorola before linking inference.
4. Pin LiteRT-LM SDK only after CI proves current Maven/toolchain facts.
5. Build crash-contained CPU runtime initialization and fixed prompt benchmark.
6. Preserve J1/J2/J3/full-app regressions on every J4 batch.
7. Keep PR #12 Draft/open/unmerged until explicit owner approval.
