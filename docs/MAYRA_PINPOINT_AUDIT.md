# Mayra AI — Full Project Pinpoint Audit

Audit date: 2026-08-04
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged

## Audit method

Each subsystem is checked against five truths:

1. product requirement / owner intent;
2. actual source/runtime wiring;
3. automated tests and failure paths;
4. APK/manifest/permission/package boundary;
5. Motorola physical validation evidence.

## Executive result

- Core Mayra architecture is mature but full final-app device acceptance is still incomplete.
- J1 Android Assistant foundation is physically proven.
- J2 on-device recognition/privacy/dismissal is physically proven and stable through repeated cycles.
- Android system TTS is functional fallback but owner rates it robotic.
- J3 sherpa-onnx Hindi neural-TTS runtime is technically device-proven, including fully offline synthesis and RTF < 1.0, but the tested Priyamvada pack remains production-license blocked.
- J4 local conversational brain work is now active. The zero-permission `.litertlm` model lifecycle package is CI-green and hardened integrity/toolchain evidence is under fresh CI.
- PR #12 remains Draft/open/unmerged. No ready/merge transition is authorized.

## Pinpoint module register

| Area | Source state | Automated/package evidence | Device evidence | Status / exact gap |
|---|---|---|---|---|
| One launcher / internal screens | Implemented | full Android package audits | partial device smoke | DEVICE_VERIFY final app |
| Local deterministic chat | Implemented | regression tests green | partial old evidence | DEVICE_VERIFY |
| Local LLM brain | J4 storage/integrity harness implemented; runtime not linked yet | J4 #2 green; hardened L1 + SDK probe fresh CI pending | no model import/inference evidence yet | IN_PROGRESS |
| Optional cloud provider | Implemented | transport/settings/fallback tests | owner-key path incomplete | DEVICE_VERIFY |
| Provider credential security | Keystore AES-GCM | tests/lint/backup-off/HTTPS-only | recovery pending | DEVICE_VERIFY |
| Personal memory | Implemented | lifecycle/provenance tests | full device lifecycle pending | DEVICE_VERIFY |
| TXT/PDF/DOCX library | Implemented | extraction/search/health + Document Test | latest physical files pending | DEVICE_VERIFY |
| OCR / legacy DOC | Explicitly unsupported | unsupported-path tests/docs | none | DEFERRED |
| App opening | Implemented | routing/action tests | partial | DEVICE_VERIFY |
| Contact resolution | Implemented | resolver/action tests | partial | DEVICE_VERIFY |
| Calls/messages | review-first dialer/composer | intent/confirmation + forbidden-permission audits | pending consolidated final-app round | DEVICE_VERIFY |
| Reminder parser/store | Implemented | language/time/state tests | pending consolidated final-app round | DEVICE_VERIFY |
| Reminder follow-up/recovery | Implemented/repaired | DUE→MISSED + remaining-delay tests | Doze/reboot full-app pending | DEVICE_VERIFY |
| Confirmation expiry | Implemented | replay/mismatch/expiry tests | final UI flow pending | DEVICE_VERIFY |
| Activity History | Implemented | persistence tests | pending | DEVICE_VERIFY |
| Voice recognition | J2 bounded on-device SpeechRecognizer after explicit invocation | J2/J1/Android/Governance regressions green | Hindi/Hinglish/English + 20 cycles + locked invocation pass | DEVICE VERIFIED FOUNDATION |
| Lock-screen privacy | keyguard-aware transcript/speech suppression | CI + policy tests | physically exercised | DEVICE VERIFIED FOUNDATION |
| Android system TTS | offline fallback implemented | compile/lifecycle policy | audible device pass; quality robotic | FALLBACK PASS |
| Neural TTS | J3 sherpa-onnx isolated process + app-private model materialization | J3 #29 exact-head green | model load/synthesis/RTF 0.72 + stability round pass | DEVICE BENCHMARK PASS / LICENSE BLOCKED |
| Animated assistant session | Implemented | J1/J2 regression CI | Power invocation/dismissal/repeated cycles pass | DEVICE VERIFIED FOUNDATION |
| Android Assistant role | Implemented | package/component audits | visible/selectable/invokable on Motorola | DEVICE VERIFIED FOUNDATION |
| Wake phrase / always listening | dedicated KWS only; SpeechRecognizer loop rejected | feasibility docs | none | BENCHMARK PLANNED |
| Local model integrity | reusable `MayraLocalModelIntegrity` + J4 import UI | unit tests + J4 zero-permission audit; fresh head pending | none yet | IN_PROGRESS |
| LiteRT-LM SDK/toolchain | Maven AAR provenance probe added, runtime not linked | fresh J4 CI pending | none | IN_PROGRESS |
| Notification intelligence | listener foundation | package audit | special-access behavior pending | DEVICE_VERIFY / PLANNED expansion |
| Default Phone role | preflight only | docs | none | PLANNED / GATED |
| Incoming answer/reject/speaker | not integrated | none | none | PLANNED after Phone role |
| Call screening | not integrated | none | none | PLANNED |
| Caller message-taking | constrained architecture | docs/preflight | none | PLANNED_WITH_CONSTRAINTS |
| Release minification | implemented | final R8/manifest audits | signed upgrade pending | IN_PROGRESS |
| Release signing | environment scaffold | build config | stable owner cert/update proof pending | PLANNED finalization |
| Project docs/governance | implemented | Governance continuously enforced | N/A | DONE / CONTINUOUS |
| Baseline/rollback | protected branches + playbook | Git references | rollback install not fully exercised | DONE code / DEVICE_VERIFY install |

## J3 neural-TTS milestone

Passing source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`

Exact CI:

- J3 Neural TTS Test #29 — SUCCESS;
- Android CI #2202 — SUCCESS;
- J2 Voice Test #207 — SUCCESS;
- J1 Assistant Test #311 — SUCCESS;
- Project Governance #383 — SUCCESS.

Physical device evidence:

- first J3 automatic-start package failed on device;
- crash isolation narrowed the failure to native `OfflineTts` construction;
- filesystem materialization repair fixed it;
- final tested J3 loaded neural model and played speech;
- observed 3091 ms generation for 4297 ms audio, RTF 0.72;
- six phrases reported good, phrase #1 preferred;
- Airplane mode / Stop cleanup / 20 replies reported pass.

The exact Priyamvada model stays benchmark-only because its dataset terms are not production-cleared.

## J4 local brain milestone and active gap

### Green L0 source

`062894809bb8b0989f79ab99db644c9d0cbdfa2d`

- J4 Local LLM Test #2 — SUCCESS;
- Android CI #2224 — SUCCESS;
- Governance #405 — SUCCESS;
- J1 #333 / J2 #229 / J3 #51 — SUCCESS.

L0 proves:

- dedicated engineering package `ai.mayra.app.j4`;
- zero Android permissions;
- Android document picker model selection;
- app-private `.litertlm` model storage;
- no multi-hundred-MB model bundled into normal APK.

### Hardened L1 now in source

- filename suffix policy;
- storage headroom/overflow checks;
- atomic partial-file import;
- source/copied byte equality;
- SHA-256 during import;
- persistent model identity metadata;
- independent re-verification;
- remove/re-import lifecycle;
- device RAM/ABI/heap/private-free-space diagnostics;
- reusable local-model integrity source object with unit tests.

### Active L2 gap

Fresh J4 CI must settle the SDK provenance probe. It records the current Google Maven LiteRT-LM Android release, POM, AAR SHA-256, AAR contents and class-file major/approximate Java level before runtime linking. The main project is currently Kotlin 2.0.21 / Java 17; no broad toolchain upgrade is justified until this evidence is known.

### First physical model gate

Use a current LiteRT-LM-supported Gemma3-1B chat-ready `.litertlm` candidate (~557 MB upstream reference) for first runtime proof. Before inference:

1. capture J4 RAM/storage diagnostics;
2. import exact model;
3. record model source/license/version/bytes/SHA-256;
4. re-verify SHA;
5. reopen app and prove metadata/model persistence;
6. remove/re-import;
7. only then pin SDK and initialize engine.

## Key sequencing decisions

1. Do not regress J1/J2/J3 while adding J4.
2. Do not put a huge model in the ordinary APK.
3. Do not upgrade Java/Kotlin or link LiteRT-LM from assumptions; pin from CI evidence.
4. Use CPU first for runtime compatibility proof; compare GPU only after stable initialization/close.
5. Free-form local model output never gets direct action, memory-write or document-trust authority.
6. Deterministic Mayra fallback must remain usable if the model is missing/corrupt/killed.
7. Every install candidate requires package/source/CI/artifact/digest provenance.
8. Phone/wake-word tracks stay gated and do not interrupt the active local-brain proof.

## Current testing gate

Fresh hardened J4 exact-head CI first. If green, the next physical evidence is model lifecycle only — not inference yet:

- install J4;
- capture device diagnostics;
- import exact Gemma3-1B `.litertlm`;
- record SHA-256;
- re-verify same SHA;
- reopen persistence;
- remove/re-import.

After that, link the exact proven LiteRT-LM SDK into J4 only and begin crash-contained CPU initialization + fixed prompt benchmark.
