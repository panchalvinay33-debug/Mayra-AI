# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Pinpoint audit: `docs/MAYRA_PINPOINT_AUDIT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Test contract: `docs/MAYRA_TEST_MATRIX.md`
Rollback playbook: `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
J1 device sheet: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Project governance/backups | DONE | Canonical records, governance CI, immutable snapshots and protected baselines | Continuous synchronization |
| Full-project audit/testing | DONE | Pinpoint audit + evidence-level test matrix | Update after each real test |
| Secure baselines | DONE | Pre-Jarvis #1795 and Jarvis J1 #1851 protected branches | Promote only exact-head dual-green milestones |
| One-app packaging | DEVICE_VERIFY | One launcher; variants and internal screens audited | Latest Motorola acceptance |
| Core conversation | DEVICE_VERIFY | Local Hinglish foundation + optional provider | Long chat/voice testing |
| Local conversational brain | PLANNED | No local LLM integrated | Begin only after J1 device gate |
| Optional cloud provider | DEVICE_VERIFY | HTTPS/Keystore/live fallback | Real key/network test |
| Personal memory | DEVICE_VERIFY | Approval/provenance/lifecycle implemented | Motorola lifecycle test |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX implemented | Physical representative files |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery | Doze/reboot timing test |
| App/contact actions | DEVICE_VERIFY | Open apps, contacts, dialer/composer, expiry | Motorola end-to-end test |
| Animated Mayra presence | DEVICE_VERIFY | CI #1851 green native assistant orb/session | Unlocked/locked device invocation |
| Android Assistant role | DEVICE_VERIFY | J1 source/package green | Role visibility/select/remove on Motorola |
| Lock-screen/background voice | DEVICE_VERIFY | Declaration/package green | Locked invocation evidence |
| Offline wake phrase | PLANNED | Recognition shell only | Battery/thermal engine benchmark after J1 |
| Incoming-call control | PLANNED | No Phone/InCallService yet | Start after J1/J2 stable baseline |
| Call screening | PLANNED | Requirement only | Later optional role module |
| AI caller message-taking | PLANNED_WITH_CONSTRAINTS | Protected cellular audio not assumed | Supported route design |
| Production release | IN_PROGRESS | Minified release green; signing scaffold | Private signing/upgrade/provenance |

## Protected baselines

### Pre-Jarvis

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795: success

### Jarvis J1 CI baseline

- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851: success
- Project Governance #32: success
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_JARVIS_J1_CI1851.md`

#1851 passed compile, complete tests, lint, Personal Alpha, minified final release, safe Full Test, isolated Document Test and all governed audits/artifact uploads.

## Jarvis J1 device gate

Status: `DEVICE_VERIFY`

Candidate:

- Personal Alpha artifact: `mayra-personal-alpha-apk-1851`
- Artifact ID: `8852147191`
- Source SHA: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- APK SHA-256: `1459517f1aa375576afa353ba6683ceaf81ddbcb4e79fc6dd790a501f52307b8`

Required Motorola sequence:

1. Verify install/package/label/one launcher.
2. Find Mayra in Android default Assistant settings.
3. Select and remove Mayra explicitly.
4. Invoke while unlocked.
5. Invoke while locked.
6. Verify orb lifecycle and 10–20 repeated invocations.
7. Force-stop/reopen and reboot with role selected.
8. Check no hidden overlay, continuous microphone or role selection.
9. Run core regression smoke.
10. Record all evidence in `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

Any failure creates a focused repair batch, full CI/governance rerun, new artifact provenance and retest. J2 stays blocked until J1 findings are documented.

## Phase J2 — Local wake phrase and local brain

Status: `PLANNED / BLOCKED BY J1 DEVICE GATE`

- benchmark wake-word engines under screen-off conditions;
- measure idle battery, false triggers, heat and restart recovery;
- benchmark small quantized local language models on Motorola;
- add model integrity/storage/version controls;
- keep deterministic action/memory execution outside free-form model authority;
- keep cloud provider optional.

## Phase J3 — Advanced phone role

Status: `PLANNED`

- optional default Phone role;
- complete incoming/ongoing call UI fallback;
- caller announce;
- answer/reject/silence/mute/speaker/audio endpoint where Android permits;
- optional Call Screening rules;
- emergency/lost-role fallback tests.

## Phase J4 — Proactive owner assistant

Status: `PLANNED`

- notification/call summaries;
- owner-defined trusted routines;
- missed-task follow-ups;
- relevance/frequency controls;
- Owner Mode trust levels.

## Phase J5 — Final release

Status: `PLANNED`

- complete Motorola matrix;
- OEM battery/background hardening;
- private release signing;
- signed APK/AAB provenance;
- upgrade and rollback test;
- owner-controlled distribution;
- final branding/onboarding/accessibility/performance polish.

## Deferred/constrained

- scanned OCR;
- legacy binary `.doc`;
- exact alarms until physical need;
- unrestricted root/accessibility automation;
- hidden cellular recording or arbitrary protected call-audio injection;
- caller message-taking requires a supported voicemail/VoIP/device route.

## Immediate next actions

1. Install only Personal Alpha #1851 for J1 testing.
2. Complete the dedicated Motorola J1 sheet.
3. Report screenshots/results for each PASS/FAIL/BLOCKED item.
4. Update acceptance, pinpoint audit, roadmap and snapshot from actual evidence.
5. Repair any blocker from the secure J1 baseline.
6. Do not begin wake-word/local-model or Phone-role coding until J1 device evidence is processed.
7. Keep PR #12 Draft/open/unmerged until explicit owner approval.
