# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Pinpoint audit: `docs/MAYRA_PINPOINT_AUDIT.md`
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Test contract: `docs/MAYRA_TEST_MATRIX.md`
Rollback playbook: `docs/MAYRA_BASELINE_AND_ROLLBACK.md`

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Project governance and backups | DONE | Canonical records + governance CI + protected baseline/rollback playbook | Keep synchronized every batch |
| Full-project audit discipline | DONE | Pinpoint source/test/package/device matrix added | Update evidence/gaps after every milestone |
| Secure baseline | DONE | `baseline/mayra-0.2.1-green-1795` points to known-green commit `065e225...` | Promote next baseline only after exact-head dual-green |
| One-app packaging | DEVICE_VERIFY | One launcher; Chat/Library/Memory/Provider/History internal; variants separated | Motorola one-icon acceptance |
| Core conversation | DEVICE_VERIFY | Hindi/Hinglish/English local foundation + optional online provider | Long conversation and physical voice validation |
| Local conversational brain | PLANNED | Deterministic offline engine exists; no integrated local LLM | Benchmark suitable on-device model after J1 |
| Optional cloud provider | DEVICE_VERIFY | HTTPS provider, Keystore credentials, live composition/fallback | Real owner-key/network failure test |
| Personal memory | DEVICE_VERIFY | Approval, provenance, edit/replace/delete/expiry/recovery | Motorola lifecycle/protected-storage checks |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX extraction/index/search/summary/grounding | Physical PDF/DOCX acceptance |
| Mayra reminders | DEVICE_VERIFY | Persistent WorkManager reminders, Complete/Snooze/follow-up/reboot recovery | Doze/timing/reboot device acceptance |
| App and contact actions | DEVICE_VERIFY | App opening, contacts, dialer/composer handoff, expiring confirmations | Motorola end-to-end checks |
| Animated Mayra presence | IN_PROGRESS | Native animated VoiceInteractionSession; compile incompatibility repaired | Latest full CI then device session wiring |
| Android Assistant role | IN_PROGRESS | Service/session/metadata/recognition shell; API mismatch repaired | CI package audit + Motorola role selection |
| Offline wake phrase | PLANNED | Recognition shell only | Engine battery/thermal benchmark after J1 |
| Lock-screen/background voice | IN_PROGRESS | Assistant lock-screen declaration foundation | Assistant-role device proof |
| Incoming-call control | PLANNED | No default Phone/InCallService module | Build only after J1/J2 baseline |
| Call screening | PLANNED | Requirement recorded only | Add CallScreeningService owner rules later |
| AI caller message-taking | PLANNED_WITH_CONSTRAINTS | Cellular audio injection/recording not assumed | Supported voicemail/VoIP route design |
| Production release | IN_PROGRESS | Minified release audit + secret-only signing scaffold | Private signing/provenance/distribution |

## Verified baseline

### Mayra 0.2.1 pre-Jarvis protected baseline

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI: `#1795` success
- Version: `0.2.1` / versionCode `4`

Verified gates:

- Debug, Personal Alpha and Full Test compilation;
- complete unit-test suite;
- lint across governed variants;
- Personal Alpha APK package/permission/component/launcher audit;
- minified final `ai.mayra.app` R8/manifest audit;
- safe Full Test audit;
- isolated zero-permission Document Test audit;
- artifact/report upload.

The protected baseline is never a development branch and must not be force-moved.

## Current latest-head truth

Jarvis Assistant-role and governance work was added after #1795.

Android CI **#1833 failed at compilation** on two exact new-code incompatibilities:

1. optional `RecognitionService.onCheckRecognitionSupport` override was not available in the project API surface;
2. orb animation set `repeatCount` through the base Animator type.

Both were repaired forward:

- RecognitionService now keeps mandatory abstract callbacks only until the real recognizer is connected;
- each ObjectAnimator owns its repeat configuration before entering the AnimatorSet.

The failed run remains part of project evidence. The repaired head is **not stable until complete latest-head Android CI and Project Governance are both green**.

## Active Jarvis Mode plan

### Phase J1 — Assistant presence foundation

Status: `IN_PROGRESS`

Required order:

1. compile Debug, Personal Alpha and Full Test;
2. pass complete tests and lint;
3. pass Personal Alpha and final release manifest/component audits;
4. prove Assistant components are absent from Full Test;
5. expose owner-visible Assistant role setup/status;
6. generate provenance-recorded Personal Alpha;
7. test role visibility, selection/removal, unlocked invocation, locked invocation, reboot and animation lifecycle on Motorola;
8. record results in acceptance checklist/audit/snapshot;
9. create new protected baseline only after exact-head dual-green and milestone snapshot.

### Phase J2 — Local wake phrase and local brain

Status: `PLANNED`

- choose and benchmark wake-word engine;
- measure screen-off idle battery, false triggers, thermal and restart behavior;
- benchmark small quantized local models on Motorola;
- enforce model checksum/storage and bounded context/output;
- keep deterministic action/memory policy outside free-form model authority;
- keep cloud provider optional.

### Phase J3 — Advanced phone role

Status: `PLANNED`

- optional default Phone role request/status;
- required incoming/ongoing call UI fallback;
- caller announce;
- answer/reject/silence/mute/speaker/audio endpoint where Android permits;
- optional Call Screening owner rules;
- emergency/lost-role fallback tests.

### Phase J4 — Proactive owner assistant

Status: `PLANNED`

- notification/call summaries;
- owner-defined trusted routines;
- missed-task/reminder follow-ups;
- frequency/relevance controls;
- Owner Mode trust policy for routine low-risk actions.

### Phase J5 — Final release

Status: `PLANNED`

- complete Motorola matrix;
- fix OEM background/battery issues;
- private release signing;
- signed APK/AAB provenance;
- upgrade/rollback testing;
- owner-controlled distribution;
- final branding/onboarding/accessibility/performance polish.

## Testing and promotion rule

`docs/MAYRA_TEST_MATRIX.md` is mandatory. A capability progresses through design, compile, automated, package, device and release evidence levels. No status may exceed its evidence.

A new baseline may be promoted only when:

- Android CI green;
- Project Governance green;
- roadmap/snapshot/audit current;
- major transition has immutable snapshot;
- physical claims have Motorola evidence.

## Deferred or constrained work

- scanned OCR: `DEFERRED`;
- legacy binary `.doc`: `DEFERRED`;
- exact alarm access: `DEFERRED` pending device need;
- unrestricted root/accessibility automation: `DEFERRED`;
- hidden cellular recording or arbitrary AI audio injection: not assumed;
- caller message-taking requires a supported voicemail/VoIP/device route.

## Immediate ordered next actions

1. Run complete latest-head Android CI and Project Governance after Assistant compile repair and documentation sync.
2. Repair exact remaining test/lint/R8/manifest failures without weakening checks.
3. Add hard Assistant component presence/absence audits if not yet enforced.
4. Record authoritative exact-head run IDs and artifact provenance.
5. Promote a new Jarvis J1 baseline only after dual-green.
6. Perform Motorola J1 acceptance before wake-word/local-model work.
7. Keep PR #12 Draft/open/unmerged until explicit owner approval.
