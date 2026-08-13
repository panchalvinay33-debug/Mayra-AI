# Mayra AI — Canonical Test Matrix

Last updated: 2026-08-04
Entry point: `START_HERE.md`

This document defines mandatory evidence before any capability may move from `IN_PROGRESS` to `DEVICE_VERIFY`, `DONE` or a protected baseline.

## Evidence levels

| Level | Meaning | Required evidence |
|---|---|---|
| L0 — Designed | Requirement/architecture only | Blueprint + roadmap + idea/decision record |
| L1 — Compiles | Source is syntactically valid | Governed variant compilation |
| L2 — Automated | Important behavior/failure paths tested | unit/Robolectric tests + lint |
| L3 — Packaged | Real artifact contains intended package/components/permissions | APK/R8/manifest/component/launcher audits |
| L4 — Device verified | Behavior works on owner Motorola | dated physical checklist evidence/screens/logs where useful |
| L5 — Release verified | Signed production artifact and upgrade path verified | signing/provenance/install/upgrade/rollback evidence |

No capability may be described above its proven level.

## Global gates for every meaningful batch

- [ ] START_HERE and Latest Snapshot read before coding.
- [ ] PR #12 remains Draft/open/unmerged unless owner explicitly changes it.
- [ ] Exact changed modules identified.
- [ ] Focused regression tests added or explicit reason recorded.
- [ ] Debug, Personal Alpha and Full Test compile where applicable.
- [ ] Complete debug unit-test suite passes.
- [ ] Governed lint variants pass.
- [ ] Personal Alpha APK audit passes.
- [ ] Minified final `ai.mayra.app` release/R8 audit passes.
- [ ] Safe Full Test audit passes.
- [ ] Isolated engineering package audits pass for active J1/J2/J3/J4 work.
- [ ] Project Governance workflow passes.
- [ ] Roadmap + Latest Snapshot + Pinpoint Audit synchronized.
- [ ] Blueprint/ideas/decisions/changelog updated when architecture/product truth changes.
- [ ] New protected baseline only after exact-head required gates are green.

## Module matrix

### Core conversation and routing

Automated:
- greeting/wellbeing/capability/date/time behavior;
- typed routing precedence;
- action vs document vs conversation collisions;
- multi-turn reminder clarification;
- provider fallback/cancellation;
- no trusted metadata spoofing through visible text.

Device:
- Hindi/Hinglish/English conversations;
- long-session state and clear flow;
- offline/online transitions;
- rotation/process recreation where applicable.

### Local brain / J4

#### Model lifecycle + integrity automated gates

- `.litertlm` filename validation;
- invalid/empty model rejected;
- storage headroom and integer-overflow protection;
- model stored outside base APK;
- app-private target path only;
- interrupted import never promotes `.partial` bytes as final model;
- provider-reported source size equals copied size when available;
- SHA-256 known-vector tests;
- imported SHA-256 persisted and independently re-verifiable;
- corrupt/mismatched model blocked before runtime;
- remove/replace lifecycle does not require app reinstall;
- no Internet/storage/audio/contact/notification permission added to J4 L0/L1;
- deterministic fallback remains independent from heavy runtime.

#### SDK/runtime provenance automated gates

Before runtime link:
- exact LiteRT-LM Maven release resolved and recorded;
- AAR SHA-256 recorded;
- POM/dependency provenance recorded;
- class-file/toolchain level recorded;
- Java/Kotlin/toolchain change reviewed rather than assumed.

After runtime link:
- SDK pinned to exact version, not floating latest;
- J4 compile/lint/package audit green;
- runtime dependency limited to J4 until promoted;
- engine initialization off UI thread;
- explicit close/release path;
- missing/corrupt model fails before engine init;
- initialization failure does not create restart loop;
- bounded input/output/context policy;
- local model output cannot bypass action/memory/document trust boundaries.

#### J4 model-lifecycle device gates

- Motorola model/Android/ABI/RAM/private-storage diagnostics visible;
- exact Gemma3-1B candidate source/version/license/bytes recorded;
- import completes with SHA-256;
- Verify recomputes same SHA-256;
- app reopen preserves accepted model metadata/path;
- remove clears model and metadata;
- re-import works;
- Airplane mode remains compatible with model lifecycle package.

#### J4 runtime device gates

- CPU engine cold initialization time;
- warm initialization/cache behavior;
- launcher survives load/generation failure;
- Hindi/Hinglish/English fixed prompts;
- first-token latency;
- total response time;
- approximate decode tokens/sec where measurable;
- RAM before load / after load / during generation / after release;
- conversation cancellation;
- explicit conversation + engine close;
- repeated load/close stability;
- app background/screen-lock behavior;
- Android process-kill recovery;
- airplane-mode generation;
- battery/thermal observation;
- only after CPU pass, GPU comparison if useful.

#### Local-brain trust regression

- model text cannot directly place a call/send message/open app;
- model text cannot directly write owner memory;
- model cannot spoof trusted memory/document provenance;
- deterministic action router remains authoritative;
- confirmations remain typed/action-bound/expiring;
- local mode never silently leaks owner context to network;
- missing/corrupt/killed model falls back cleanly.

### Provider

Automated:
- HTTPS-only endpoint;
- Keystore credential lifecycle;
- bounded request/response size;
- timeout/retry/cancellation;
- Responses schema parsing;
- live enable/disable/remove;
- no secret logging/UI read-back.

Device:
- real key connection;
- invalid/expired key UX;
- network loss/recovery;
- Hindi/Hinglish quality;
- provider disabled while conversation continues locally.

### Memory

Automated:
- proposal/approval;
- edit/replace/delete/expiry;
- protected-storage failure handling;
- provenance keys;
- provider/document spoof resistance.

Device:
- save/use/edit/delete;
- process death/restart;
- storage migration/recovery;
- Memory Center accuracy.

### Documents

Automated:
- TXT/PDF/DOCX extraction;
- current-index policy;
- search ranking/snippets;
- summary/grounding boundaries;
- corrupt/unsupported files;
- zero-permission Document Test isolation.

Device:
- Android picker/import;
- representative PDF/DOCX files;
- large-file responsiveness;
- stale/re-index UX;
- delete/reimport behavior.

### Reminders

Automated:
- English/Hindi/Hinglish parsing;
- relative/day/time cases;
- missing-time clarification;
- revision-safe workers/actions;
- Complete/Snooze/Cancel;
- DUE→MISSED follow-up;
- reboot/update recovery;
- remaining follow-up delay.

Device:
- short reminder;
- tomorrow/local-time reminder;
- notification permission denied/granted;
- Complete/Snooze;
- Doze/battery saver;
- reboot/update;
- duplicate-notification check.

### Apps, contacts, calls and messages

Automated:
- installed-app resolution;
- `open` routing collisions;
- contact ambiguity;
- dialer/composer intent correctness;
- one-time expiring confirmations;
- no false delivery/connection claims.

Device:
- common app opening;
- known/ambiguous contacts;
- dialer/message-composer handoff;
- cancellation/expiry.

### Android Assistant role / J1-J2

Automated:
- VoiceInteractionService/session/metadata compilation;
- manifest service permission/export audit;
- one launcher remains unchanged;
- assistant components absent from safe Full Test;
- session animation/mic/TTS lifecycle cleanup.

Device:
- Mayra appears in Android Assistant selection;
- owner can select/remove role;
- unlocked + locked invocation;
- animation/session lifecycle;
- direct dismissal;
- repeated invocation stability;
- reboot behavior;
- lock-screen privacy.

### Speech output / J3

Automated:
- speech-output abstraction preserved;
- J3 zero-permission package audit;
- neural runtime isolated from launcher;
- exact runtime/model hashes recorded;
- Android system TTS fallback preserved.

Device:
- neural model load;
- synthesis/playback;
- RTF/latency;
- Hindi/Hinglish phrase quality;
- Airplane mode;
- Stop cleanup;
- repeated replies;
- thermal/RAM where available;
- production voice license separately cleared before promotion.

### Wake phrase

Automated:
- explicit enable/disable;
- model checksum/version;
- false-trigger corpus;
- restart/recovery;
- microphone-state visibility;
- no wake engine in safe Full Test.

Device:
- screen on/off detection;
- near/far voice;
- Hindi/accent/noisy-room cases;
- false accepts/rejects;
- long idle battery test;
- thermal/RAM behavior.

### Phone role and incoming calls

Automated:
- role request/status;
- InCallService lifecycle state machine;
- answer/reject/silence/mute/speaker commands;
- emergency-call/lost-role fallback;
- no call recording/injection claim without proven supported route.

Device:
- default Phone role selection/removal;
- incoming known/unknown caller announce;
- answer/reject/silence;
- mute/speaker/Bluetooth endpoint;
- outgoing/emergency fallback;
- reboot/lost-role behavior;
- Motorola call UI stability.

### Packaging and release

Automated:
- package/label/version;
- one launcher;
- required/forbidden permissions;
- required/forbidden components;
- non-debuggable minified release;
- R8 mapping;
- no secret material in repository.

Device/release:
- fresh install;
- upgrade over previous signed build;
- uninstall/reinstall data expectations;
- Play Protect/installer behavior without bypass;
- signed APK/AAB provenance/digest;
- rollback verified.

## Physical test recording format

Every device test entry records:

- date/time/timezone;
- phone model + Android/build version;
- Mayra version/package/APK SHA-256;
- source commit + CI run;
- model/runtime exact version/hash where applicable;
- preconditions/permissions/system roles;
- exact steps;
- expected result;
- actual result;
- PASS/FAIL/BLOCKED;
- screenshot/log reference where useful;
- follow-up issue/commit if failed.

## Promotion rule

A feature failure blocks promotion of that capability. Any crash loop, data loss, secret exposure, launcher/package/signing/manifest failure, corrupt trust boundary or unsafe destructive-action path blocks the entire candidate.
