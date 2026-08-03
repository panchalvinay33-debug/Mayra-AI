# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Pinpoint audit: `docs/MAYRA_PINPOINT_AUDIT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Test contract: `docs/MAYRA_TEST_MATRIX.md`
Rollback playbook: `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
J1 device sheet: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`
Install evidence: `docs/testing/MAYRA_J1_INSTALL_RESULT_2026-08-03.md`
Play Protect evidence: `docs/testing/MAYRA_PLAY_PROTECT_BLOCK_2026-08-03.md`
Canonical product issue: #13
Mandatory major-feature feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE | Canonical records, governance CI, immutable snapshots and protected baselines | Continuous synchronization |
| Repository clarity | IN_PROGRESS | PR #9/#11 and issue #10 closed as superseded; PR #12 is the only active implementation PR; branches classified in issue #15 | Verify delete-candidate reachability before any branch deletion |
| Feasibility control | DONE / CONTINUOUS | Issue #14 blocks major features until Android/Motorola/permission/performance/distribution/fallback review is recorded | Apply first to local LLM and wake phrase after J1 proof |
| Secure baselines | DONE | Pre-Jarvis #1795, Jarvis J1 #1851 and zero-permission J1 #44 protected branches | Promote only exact-head green/device-evidenced milestones |
| Zero-permission J1 test | DEVICE_VERIFY | J1 #44 compile/lint/assembly/zero-permission audit green; Android CI #1935 and Governance #116 also green | Motorola install, Assistant role visibility/select/remove, unlocked/locked invocation and orb lifecycle |
| Full owner installation | BLOCKED / IN_PROGRESS | Full debug Personal Alpha was blocked by Play Protect; stable owner signing workflow exists | Configure private certificate and trusted owner/Play Internal distribution |
| Simple onboarding | IN_PROGRESS | Full app has two-step permissions + Assistant activation setup | Trusted-install device test |
| One-app packaging | DEVICE_VERIFY | Final product remains one Mayra launcher; J1 is a temporary engineering proof package | Final owner build one-icon acceptance |
| Core conversation | DEVICE_VERIFY | Local Hinglish foundation + optional provider | Trusted full-app long chat/voice test |
| Local conversational brain | PLANNED | No local LLM integrated | Complete issue #14 model/licensing/RAM/thermal benchmark before coding |
| Optional cloud provider | DEVICE_VERIFY | HTTPS/Keystore/live fallback | Real key/network test through trusted build |
| Personal memory | DEVICE_VERIFY | Approval/provenance/lifecycle implemented | Motorola lifecycle test |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX implemented | Physical representative files |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery | Doze/reboot timing test |
| App/contact actions | DEVICE_VERIFY | Open apps, contacts, dialer/composer, expiry | Motorola end-to-end test |
| Animated Mayra presence | DEVICE_VERIFY | J1 #44 package verified | Motorola orb/session invocation |
| Android Assistant role | DEVICE_VERIFY | J1 #44 package verified | Motorola role visibility/select/remove/invoke |
| Lock-screen/background voice | DEVICE_VERIFY | Declaration/package foundation | Locked invocation evidence |
| Offline wake phrase | PLANNED | Recognition shell only | Complete issue #14 battery/background/lock-screen feasibility before coding |
| Incoming-call control | PLANNED | No Phone/InCallService yet | Complete default-Dialer/InCallService feasibility review after J1/J2 |
| Production release | IN_PROGRESS | Minified release + stable signing scaffold | Private certificate, AAB/APK provenance, Internal Testing |

## Protected baselines

### Pre-Jarvis
- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795: success

### Jarvis J1 code baseline
- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851: success
- Project Governance #32: success

### Zero-permission J1 package baseline
- Branch: `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- Commit: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 Assistant Test #44: success
- Android CI #1935: success
- Project Governance #116: success
- Artifact: `mayra-j1-zero-permission-apk-44`
- Artifact ID: `8854905288`
- Artifact ZIP SHA-256: `12f4e148fbac99e916b78321b9ae75d87a5b4b5cebe2060bfa6e6b5f7545be3b`
- APK size: `19,192,842` bytes
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`

## Repository cleanup truth

- PR #12 is the only active implementation PR and remains Draft/open/unmerged.
- PR #9 and #11 are closed and titled `[Superseded]`.
- Issue #10 is closed and titled `[Superseded]`.
- Issue #13 is the canonical product North Star.
- Issue #14 is the mandatory feasibility preflight gate.
- Issue #15 classifies active, protected, retained-backup and delete-candidate branches.
- No branch deletion is claimed because the connected tool does not expose branch-ref deletion; deletion candidates require reachability verification first.

## Motorola findings retained

1. Personal Alpha update failed because old/new APKs used different temporary debug certificates.
2. Full sideloaded Personal Alpha was blocked by Play Protect because it could request sensitive access.
3. Play Protect must not be bypassed for that artifact.
4. The next device test uses only the verified zero-permission J1 package.

## J1 CI findings retained

- #16: API-29 role-request guard fixed.
- #22: inherited AndroidX permissions/components detected.
- #32: lint model rejected an unscoped ProfileInstaller removal declaration.
- #38: final APK audit proved ProfileInstaller receiver still survived.
- #44: compile, lint, APK assembly, zero-permission/component audit and artifact upload all passed.

## Immediate next actions

1. Install only the verified J1 #44 APK on the Motorola device.
2. Record install result, Assistant-role visibility, select/remove, unlocked/locked invocation and orb lifecycle.
3. Update Motorola evidence as PASS/FAIL/BLOCKED.
4. Repair only evidence-backed device failures from the protected #44 baseline.
5. Configure stable owner signing privately.
6. Produce trusted full-app distribution and run install-over-install data-retention testing.
7. Complete issue #14 feasibility reviews before local LLM, wake-word, call-control or accessibility work.
8. Verify issue #15 delete candidates before removing any branch.
9. Keep PR #12 Draft/open/unmerged until explicit owner approval.
