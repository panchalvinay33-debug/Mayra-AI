# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-04
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: J2 voice is device-proven; J3 neural TTS is device-stable but the tested voice pack is production-license blocked; J4 local-brain model lifecycle/integrity is now the active offline-intelligence gate.

## Canonical truth

- Final product remains one Mayra app; J1/J2/J3/J4 are engineering packages only.
- PR #12 is not authorized for merge/ready.
- Protected baselines are immutable.
- Device capability claims require Motorola evidence.
- Android offline TTS remains the production-safe speech fallback until a license-clear neural voice is selected.
- Heavy local LLM integration remains isolated from final Mayra until Motorola benchmark evidence exists.

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136 / J1 #239 / Android CI #2131 / Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`

## Motorola voice evidence

Physically proven:

- Android Digital assistant selection and Power-button invocation;
- offline Hindi/Hinglish/English recognition;
- direct dismissal paths and 20-cycle stability;
- already-locked invocation;
- reboot/no-speech/rapid-interaction checks reported OK;
- Android offline TTS speaks but sounds robotic to owner.

## J3 neural voice milestone

Passing source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`

- J3 #29 / Android CI #2202 / J2 #207 / J1 #311 / Governance #383: success;
- app-private model materialization: PASS;
- sherpa native constructor/model load/synthesis/playback: PASS;
- sample RTF: 0.72;
- all six phrases reported fine, phrase #1 preferred;
- Airplane mode / Stop cleanup / 20-reply stability reported pass.

The Priyamvada voice remains benchmark-only because its model-card dataset terms are not production-cleared. Keep system TTS fallback and reuse J3 architecture only with a license-clear voice pack.

## J4 local brain foundation

### Green L0 milestone

Source `062894809bb8b0989f79ab99db644c9d0cbdfa2d`:

- J4 Local LLM Test #2 — success;
- Android CI #2224 — success;
- Governance #405 — success;
- J1 #333 / J2 #229 / J3 #51 — success.

L0 proves a dedicated zero-permission `ai.mayra.app.j4` package can import an owner-selected `.litertlm` file into app-private storage without bundling a huge model or requesting broad storage/network permissions.

### Hardened L1 now in source

- reusable `MayraLocalModelIntegrity` boundary;
- unit tests for filename policy, storage headroom/overflow and SHA-256 known vector;
- `.litertlm` filename gate;
- provider-size validation;
- 256 MB private-storage safety headroom;
- atomic `.partial` → final import;
- copy-byte verification;
- SHA-256 during import;
- saved model name/bytes/hash;
- independent SHA-256 re-verification;
- remove/re-import lifecycle;
- visible Motorola/Android/ABI/RAM/app-heap/private-free-space diagnostics.

Fresh CI for this hardened source is pending.

### LiteRT-LM SDK provenance probe

J4 CI now downloads the current Android Maven AAR **without linking it into the app** and records:

- resolved Maven release;
- AAR SHA-256;
- POM;
- class-file major / approximate Java level;
- AAR contents.

This is required because Mayra currently builds with Kotlin 2.0.21 / Java 17. Toolchain changes will be made only from evidence and must preserve J1/J2/J3/full-app regressions.

## First local-model candidate

**Gemma3-1B chat-ready LiteRT-LM model** — upstream reference around 557 MB, 4-bit, 4096 context.

It is chosen for first runtime compatibility proof because it is a current LiteRT-LM reference model and is smaller than the multi-GB alternatives. It is not yet declared Mayra's final Hindi/Hinglish brain.

## J4 ordered next gate

1. settle fresh J4/J1/J2/J3/Android/Governance CI on hardened L1;
2. record J4 APK + audit provenance;
3. install J4 on Motorola and capture RAM/private-storage diagnostics;
4. import exact Gemma3-1B `.litertlm` model;
5. require import SHA-256 + re-verify + reopen persistence + remove/re-import lifecycle pass;
6. inspect SDK probe, pin exact LiteRT-LM Android version;
7. link runtime to J4 only;
8. initialize CPU engine off UI thread with exact load/error diagnostics and explicit close;
9. run fixed Hindi/Hinglish/English prompt set in Airplane mode;
10. record cold/warm load, first-token latency, tokens/sec, RAM and thermal behavior;
11. only then compare GPU or a stronger multilingual model.

## Trust boundary

- local LLM never directly executes calls/messages/device actions;
- local LLM never directly writes owner memory;
- document provenance remains structured;
- confirmation tokens remain typed/action-bound/expiring;
- local mode never silently sends owner context to network;
- missing/corrupt/killed local model falls back to deterministic Mayra.

## Distribution truth

Hosted CI debug signatures may conflict. Stable owner signing/trusted distribution remains required. Never bypass Play Protect or Android security.
