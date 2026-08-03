# Mayra AI — Full Project Pinpoint Audit

Audit date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged

## Audit method

Each subsystem is checked against five truths:

1. Product requirement and owner intent.
2. Actual source/runtime wiring.
3. Automated tests and failure paths.
4. APK/manifest/permission/package boundary.
5. Motorola physical validation evidence.

Status meanings follow `START_HERE.md`.

## Executive result

- Last fully green secure baseline: `baseline/mayra-0.2.1-green-1795` at `065e22524c835f3ddd3b2f56215a3616f071d4b3`.
- Current Jarvis/governance head is not a baseline until latest Android CI and Project Governance are both green.
- Governance workflow is operational and has passed.
- Android CI #1833 found two compile incompatibilities in the new Assistant foundation; both were repaired forward without weakening the design.
- No merge/ready transition is authorized.

## Pinpoint module register

| Area | Source state | Automated evidence | Packaging evidence | Device evidence | Status / exact gap |
|---|---|---|---|---|---|
| One launcher / internal screens | Implemented | CI audits | Personal Alpha/Full Test/release audits | Early Full Test launch only | DEVICE_VERIFY — repeat on latest owner candidate |
| Local deterministic chat | Implemented | Regression tests | Included | Partial screenshots | DEVICE_VERIFY — longer Hinglish/context tests |
| Local LLM brain | Not integrated | None | None | None | PLANNED — benchmark model/runtime first |
| Optional cloud provider | Implemented | transport/settings/fallback tests | INTERNET only in capable variants | Not tested with real owner key | DEVICE_VERIFY |
| Provider credential security | Keystore AES-GCM | tests/lint | backup off/HTTPS only | Not recovery-tested | DEVICE_VERIFY |
| Personal memory | Implemented | lifecycle/provenance tests | Internal screen | Not fully tested | DEVICE_VERIFY |
| TXT/PDF/DOCX library | Implemented | extraction/search/health tests | isolated Document Test | Partial/old evidence | DEVICE_VERIFY |
| OCR / legacy DOC | Not implemented | explicit unsupported paths | None | None | DEFERRED |
| App opening | Implemented | routing collision tests | query visibility audited | Not retested latest | DEVICE_VERIFY |
| Contact resolution | Implemented | resolver/action tests | READ_CONTACTS only capable variants | Not tested latest | DEVICE_VERIFY |
| Calls/messages | Dialer/composer review-first | intent/confirmation tests | no CALL_PHONE/SEND_SMS | Not tested latest | DEVICE_VERIFY |
| Reminder parser/store | Implemented | language/time/state tests | notification/boot components | Not tested on Motorola | DEVICE_VERIFY |
| Reminder follow-up/recovery | Implemented and repaired | DUE→MISSED and remaining-delay tests | WorkManager/receiver audit | Doze/reboot pending | DEVICE_VERIFY |
| Confirmation expiry | Implemented | replay/mismatch/expiry tests | N/A | UI expiry pending physical check | DEVICE_VERIFY |
| Activity History | Implemented | persistence tests | internal screen | Not tested latest | DEVICE_VERIFY |
| Voice input/TTS | Implemented foundation | limited logic tests | RECORD_AUDIO capable variants | Physical quality pending | DEVICE_VERIFY |
| Animated assistant session | Foundation implemented | compile/lifecycle validation pending | assistant components intended capable variants only | None | IN_PROGRESS |
| Android Assistant role | Foundation implemented | manifest/API compilation pending latest CI | role services/meta-data need explicit audit | None | IN_PROGRESS |
| Lock-screen assistant | Declaration foundation | no device automation | manifest only | None | IN_PROGRESS |
| Wake phrase / always listening | Not integrated | None | None | None | PLANNED — battery/thermal gate required |
| Notification intelligence | Listener foundation exists | limited tests | special-access service in capable variants | Not accepted | DEVICE_VERIFY/PLANNED expansion |
| Default Phone role | Not implemented | None | None | None | PLANNED |
| Incoming call answer/reject/speaker | Not implemented | None | None | None | PLANNED after Phone role |
| Call screening | Not implemented | None | None | None | PLANNED |
| Caller message-taking | Architecture constrained | None | None | None | PLANNED_WITH_CONSTRAINTS |
| Release minification | Implemented | R8 CI gate | final `ai.mayra.app` audited | unsigned/no upgrade test | IN_PROGRESS |
| Release signing | Environment scaffold | config compile only | no signed production artifact | None | PLANNED finalization |
| Project docs/governance | Implemented | Project Governance green | N/A | N/A | DONE, continuous maintenance |
| Baseline/rollback | Protected branch + playbook | Git reference | N/A | Device rollback not yet exercised | DONE for code; DEVICE_VERIFY for install rollback |

## Critical sequencing findings

1. **Do not start wake-word/local-model work on a red Assistant foundation.** J1 must compile, lint, package and pass device role-selection first.
2. **Default Phone work must be isolated behind optional role selection.** It must not silently change current dialer/composer safety or the safe Full Test variant.
3. **Local LLM must not receive direct action authority.** It may propose structured intents; deterministic policy executes them.
4. **Every device claim needs artifact provenance.** Version/package/SHA/source/CI must be recorded before testing.
5. **Current PR is large.** Future work should remain coherent and baseline-promoted regularly to avoid unrecoverable drift.
6. **CI must audit assistant components explicitly.** Presence in Personal Alpha/final and absence in Full Test should become a hard gate.
7. **Physical acceptance is the largest remaining evidence gap.** Core features are mostly coded but not fully proven on the Motorola target.

## Current repair batch

CI #1833 failures:

- `MayraRecognitionService.onCheckRecognitionSupport` did not match the project SDK API surface.
- assistant orb attempted to set `repeatCount` through the base `Animator` type.

Repairs:

- keep only mandatory `RecognitionService` abstract callbacks until the real recognizer is connected;
- configure repeat behavior on each `ObjectAnimator` before adding it to the set.

Promotion remains blocked until full latest-head green.

## Next required gates

1. Full Android CI latest-head green.
2. Project Governance latest-head green.
3. Add hard CI assertions for Assistant service/session/recognition metadata and Full Test exclusion.
4. Create next protected baseline branch only after both workflows pass.
5. Generate provenance-recorded Personal Alpha artifact.
6. Execute J1 Motorola test sheet: role visibility, selection/removal, unlocked/locked invocation, animation lifecycle, reboot and battery observation.
7. Update this audit, roadmap, latest snapshot and acceptance evidence from actual results before J2 begins.
