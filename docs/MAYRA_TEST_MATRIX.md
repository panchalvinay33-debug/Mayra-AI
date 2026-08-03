# Mayra AI — Canonical Test Matrix

Last updated: 2026-08-03
Entry point: `START_HERE.md`

This document defines the mandatory evidence required before any Mayra capability can move from `IN_PROGRESS` to `DEVICE_VERIFY` or `DONE`.

## Evidence levels

| Level | Meaning | Required evidence |
|---|---|---|
| L0 — Designed | Requirement/architecture only | Blueprint + roadmap + idea/decision record |
| L1 — Compiles | Source is syntactically valid | Governed variant compilation |
| L2 — Automated | Important behavior and failure paths tested | Unit/Robolectric tests + lint |
| L3 — Packaged | Real artifact contains intended package/components/permissions | APK/R8/manifest/component/launcher audits |
| L4 — Device verified | Behavior works on owner Motorola | Dated physical checklist evidence, screenshots/logs where useful |
| L5 — Release verified | Signed production artifact and upgrade path verified | Signing/provenance/install/upgrade/rollback evidence |

No capability may be described above its proven level.

## Global gates for every meaningful batch

- [ ] START_HERE and latest snapshot read before coding.
- [ ] PR #12 remains Draft/open/unmerged unless owner explicitly changes that.
- [ ] Exact changed modules identified.
- [ ] Focused regression tests added or an explicit reason recorded.
- [ ] Debug, Personal Alpha and Full Test compile.
- [ ] Complete debug unit-test suite passes.
- [ ] Governed lint variants pass.
- [ ] Personal Alpha APK audit passes.
- [ ] Minified final `ai.mayra.app` release/R8 audit passes.
- [ ] Safe Full Test audit passes.
- [ ] Isolated Document Test audit passes.
- [ ] Project Governance workflow passes.
- [ ] Roadmap and rolling snapshot updated.
- [ ] Blueprint/ideas/decisions/changelog updated when applicable.
- [ ] New authoritative green baseline recorded before riskier next phase.

## Module matrix

### Core conversation and routing

Automated:
- greeting/wellbeing/capability/date/time behavior;
- typed routing precedence;
- action vs document vs conversation collisions;
- multi-turn reminder clarification;
- provider fallback and cancellation;
- no trusted metadata spoofing through visible text.

Device:
- Hindi/Hinglish/English conversations;
- long-session state and clear flow;
- offline/online transitions;
- rotation/process recreation where applicable.

### Local brain

Automated:
- model integrity/checksum;
- bounded prompt/context/output;
- deterministic fallback when model unavailable;
- memory/document/action boundaries remain outside free-form model control.

Device:
- cold-start latency;
- tokens/second and perceived response time;
- idle and active battery drain;
- thermal throttling;
- RAM pressure/background survival;
- Hindi/Hinglish quality;
- airplane-mode operation.

### Provider

Automated:
- HTTPS-only endpoint;
- Keystore credential lifecycle;
- bounded request/response size;
- timeout/retry/cancellation;
- Responses schema parsing;
- live enable/disable/remove;
- no secret logging or UI read-back.

Device:
- real key connection;
- invalid/expired key UX;
- network loss and recovery;
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
- 3-minute reminder;
- tomorrow/local-time reminder;
- notification permission denied/granted;
- Complete and Snooze actions;
- Doze/battery saver timing;
- reboot and app update;
- duplicate-notification check.

### Apps, contacts, calls and messages

Automated:
- installed-app resolution;
- `open` routing collision tests;
- contact resolution ambiguity;
- dialer/composer intent correctness;
- one-time expiring confirmations;
- no false delivery/connection claims.

Device:
- WhatsApp/YouTube/Chrome opening;
- known and ambiguous contacts;
- dialer handoff;
- message composer handoff;
- cancellation/expiry behavior.

### Jarvis Assistant role

Automated:
- VoiceInteractionService/session/metadata compilation;
- manifest service permission/export audit;
- one-launcher remains unchanged;
- assistant components absent from safe Full Test;
- session animation lifecycle does not leak/carry stale animator state.

Device:
- Mayra appears in Android Assistant selection;
- owner can select/remove role;
- invocation while unlocked;
- invocation while locked;
- animation appears only during active visual session;
- screen-off path remains voice/background only;
- process restart and reboot behavior;
- battery/thermal observation.

### Wake phrase

Automated:
- explicit enable/disable;
- model checksum/version;
- false-trigger test corpus;
- restart/recovery;
- microphone-state visibility;
- no wake engine in safe Full Test.

Device:
- screen on/off detection;
- near/far voice;
- Hindi/accent/noisy-room cases;
- false accepts/rejects;
- 8-hour idle battery test;
- thermal and RAM behavior.

### Phone role and incoming calls

Automated:
- role request/status;
- InCallService lifecycle state machine;
- answer/reject/silence/mute/speaker commands;
- emergency-call and lost-role fallback;
- no call recording/injection claim without proven supported route.

Device:
- default Phone role selection and removal;
- incoming known/unknown caller announce;
- answer/reject/silence;
- mute/speaker/Bluetooth endpoint;
- outgoing and emergency fallback;
- reboot and lost-role behavior;
- Motorola-specific call UI stability.

### Packaging and release

Automated:
- package/label/version;
- one launcher;
- required and forbidden permissions;
- required/forbidden components;
- non-debuggable minified release;
- R8 mapping output;
- no secret material in repository.

Device/release:
- fresh install;
- upgrade over previous signed build;
- uninstall/reinstall data expectations;
- Play Protect/installer behavior without bypass;
- signed APK/AAB provenance and digest;
- rollback instructions verified.

## Physical test recording format

Every device test entry must record:

- date/time and timezone;
- phone model and Android/build version;
- Mayra version, package and APK SHA-256;
- source commit and CI run;
- preconditions/permissions/system roles;
- exact steps;
- expected result;
- actual result;
- PASS/FAIL/BLOCKED;
- screenshot/log reference where useful;
- follow-up issue/commit if failed.

## Promotion rule

A test failure blocks promotion of that capability only, but any crash, data-loss, secret-exposure, launcher/package, signing, manifest or destructive-action failure blocks the entire candidate.
