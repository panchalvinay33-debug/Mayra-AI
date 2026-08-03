# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Pinpoint audit: `docs/MAYRA_PINPOINT_AUDIT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Test contract: `docs/MAYRA_TEST_MATRIX.md`
Rollback playbook: `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
J1 device sheet: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`
Latest device result: `docs/testing/MAYRA_J1_INSTALL_RESULT_2026-08-03.md`

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Project governance/backups | DONE | Canonical records, governance CI, immutable snapshots and protected baselines | Continuous synchronization |
| Full-project audit/testing | DONE | Pinpoint audit + evidence-level test matrix | Update after each real test |
| Secure baselines | DONE | Pre-Jarvis #1795 and Jarvis J1 #1851 protected branches | Promote only exact-head dual-green milestones |
| Owner installation/update path | IN_PROGRESS | #1851 clean install is possible after uninstall; upgrade failed because CI debug certificate changed | Configure stable owner signing secrets and pass install-over-install test |
| Simple owner onboarding | IN_PROGRESS | New two-step setup requests required runtime permissions then Assistant role | Compile/lint and Motorola first-launch acceptance |
| One-app packaging | DEVICE_VERIFY | One launcher; variants and internal screens audited | Latest Motorola acceptance |
| Core conversation | DEVICE_VERIFY | Local Hinglish foundation + optional provider | Long chat/voice testing |
| Local conversational brain | PLANNED | No local LLM integrated | Begin only after J1 device gate |
| Optional cloud provider | DEVICE_VERIFY | HTTPS/Keystore/live fallback | Real key/network test |
| Personal memory | DEVICE_VERIFY | Approval/provenance/lifecycle implemented | Motorola lifecycle test |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX implemented | Physical representative files |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery | Doze/reboot timing test |
| App/contact actions | DEVICE_VERIFY | Open apps, contacts, dialer/composer, expiry | Motorola end-to-end test |
| Animated Mayra presence | DEVICE_VERIFY | CI #1851 green native assistant orb/session | Unlocked/locked device invocation |
| Android Assistant role | DEVICE_VERIFY | J1 source/package green; onboarding now exposes one activation step | Role visibility/select/remove on Motorola |
| Lock-screen/background voice | DEVICE_VERIFY | Declaration/package green | Locked invocation evidence |
| Offline wake phrase | PLANNED | Recognition shell only | Battery/thermal engine benchmark after J1 |
| Incoming-call control | PLANNED | No Phone/InCallService yet | Start after J1/J2 stable baseline |
| Call screening | PLANNED | Requirement only | Later optional role module |
| AI caller message-taking | PLANNED_WITH_CONSTRAINTS | Protected cellular audio not assumed | Supported route design |
| Production release | IN_PROGRESS | Minified release green; signing scaffold + dedicated stable owner workflow | Private signing/upgrade/provenance |

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

## Motorola install finding

The owner attempted to install Personal Alpha #1851. Android rejected it with “package conflicts with an existing package.” The exact finding is recorded in `docs/testing/MAYRA_J1_INSTALL_RESULT_2026-08-03.md`.

Root cause: the same `ai.mayra.app.alpha` package existed with a different GitHub runner debug signing certificate.

Immediate test recovery: uninstall the previous Personal Alpha, then install the new APK. This removes old test-package data.

Permanent path now being implemented:

- `personalAlpha` can use a stable owner signing configuration supplied only through environment/GitHub secrets;
- `.github/workflows/owner-alpha.yml` builds and verifies a stable update-compatible owner APK;
- signing certificate and APK digest are included in artifact evidence;
- temporary CI debug APKs are not considered upgrade-compatible owner releases.

## Simplified first-launch experience

The accepted owner UX is intentionally small:

1. one first-launch Mayra setup screen;
2. one button requests microphone, contacts and notification permissions together;
3. one button opens Android's Assistant-role selection;
4. one button starts Mayra;
5. no unrelated permission prompts or large settings maze.

Internet and boot recovery permissions do not require runtime prompts. Special Android roles cannot be silently granted and therefore remain one explicit system step.

## Jarvis J1 device gate

Status: `DEVICE_VERIFY / INSTALL RETEST REQUIRED`

Required Motorola sequence after a clean or stable-signed install:

1. Verify install/package/label/one launcher.
2. Complete the two-step owner setup.
3. Find/select/remove Mayra in Android Assistant settings.
4. Invoke while unlocked and locked.
5. Verify orb lifecycle and repeated invocations.
6. Force-stop/reopen and reboot with role selected.
7. Run core regression smoke.
8. Record evidence in the dedicated testing documents.

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

## Deferred/constrained

- scanned OCR;
- legacy binary `.doc`;
- exact alarms until physical need;
- unrestricted root/accessibility automation;
- hidden cellular recording or arbitrary protected call-audio injection;
- caller message-taking requires a supported voicemail/VoIP/device route.

## Immediate next actions

1. Let the simplified setup/stable-signing code pass Android CI and Project Governance.
2. For the current #1851 test only, uninstall the previous Personal Alpha and perform a clean install.
3. Configure stable owner signing secrets once; never place the keystore/passwords in Git history or chat.
4. Run the dedicated Stable Owner Alpha workflow.
5. Pass clean-install and install-over-install data-retention tests.
6. Complete the J1 Motorola role/invocation sheet.
7. Update all evidence documents before J2 coding.
8. Keep PR #12 Draft/open/unmerged until explicit owner approval.
