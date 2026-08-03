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

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE | Canonical records, governance CI, immutable snapshots and protected baselines | Continuous synchronization |
| Secure baselines | DONE | Pre-Jarvis #1795 and Jarvis J1 #1851 protected branches | Promote only exact-head dual-green milestones |
| Zero-permission J1 test | IN_PROGRESS | `ai.mayra.app.j1` is isolated to Assistant activation/services/orb; run #16 fixed API lint and run #22 exposed inherited AndroidX infrastructure, now explicitly removed and audited | Fresh J1 CI green, artifact provenance, Motorola install/role test |
| Full owner installation | BLOCKED / IN_PROGRESS | Full debug Personal Alpha was blocked by Play Protect; stable owner signing workflow exists | Configure private certificate and use trusted owner/Play Internal distribution |
| Simple onboarding | IN_PROGRESS | Full app has two-step permissions + Assistant activation setup; API-29 request directly guarded | Fresh full CI and trusted-install device test |
| One-app packaging | DEVICE_VERIFY | Final product remains one Mayra launcher; J1 is a temporary engineering proof package | Final owner build one-icon acceptance |
| Core conversation | DEVICE_VERIFY | Local Hinglish foundation + optional provider | Trusted full-app long chat/voice test |
| Local conversational brain | PLANNED | No local LLM integrated | Begin after J1 Assistant role proof |
| Optional cloud provider | DEVICE_VERIFY | HTTPS/Keystore/live fallback | Real key/network test through trusted build |
| Personal memory | DEVICE_VERIFY | Approval/provenance/lifecycle implemented | Motorola lifecycle test |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX implemented | Physical representative files |
| Reminders | DEVICE_VERIFY | Persistent scheduling/actions/recovery | Doze/reboot timing test |
| App/contact actions | DEVICE_VERIFY | Open apps, contacts, dialer/composer, expiry | Motorola end-to-end test |
| Animated Mayra presence | DEVICE_VERIFY | CI #1851 foundation | Zero-permission J1 device invocation |
| Android Assistant role | DEVICE_VERIFY | Foundation CI verified | J1 role visibility/select/remove/invoke |
| Lock-screen/background voice | DEVICE_VERIFY | Declaration/package foundation | Locked invocation evidence |
| Offline wake phrase | PLANNED | Recognition shell only | Battery/thermal engine benchmark after J1 |
| Incoming-call control | PLANNED | No Phone/InCallService yet | Start after J1/J2 stable baseline |
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

## Motorola findings retained

1. Update attempt failed because old and new Personal Alpha APKs used different temporary debug certificates.
2. Google Play Protect blocked the full sideloaded Personal Alpha because it could request sensitive access.
3. Play Protect must not be bypassed for this artifact.

## J1 CI findings retained

- Run #16: lint caught an API-29 role-request guard; direct runtime guard added without suppression.
- Run #22: compile/lint/assembly passed, but the manifest audit found AndroidX-inherited `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, an app-private dynamic-receiver permission, WorkManager/Startup/Room/ProfileInstaller components.
- The J1 manifest now explicitly removes those permissions/components.
- The workflow now permanently rejects the inherited infrastructure as well as broad Mayra features.

## Correct installation strategy

### Phase A — prove Android Assistant support

Build and test `Mayra J1 Assistant Test` (`ai.mayra.app.j1`): no requested permissions, one launcher, no broad Mayra features, only Assistant activation/status and orb/session foundation.

### Phase B — full personal Mayra

Use one private stable certificate, protected secrets, signed APK/AAB provenance, trusted owner/Play Internal distribution and install-over-install data-retention testing.

## Immediate next actions

1. Let fresh Android CI, Project Governance and J1 Assistant Test workflows run on the exact latest head.
2. Repair any compile/lint/manifest/audit failure before new feature coding.
3. Download only a fully green zero-permission J1 artifact.
4. Test installation, Assistant role visibility, selection/removal, unlocked/locked invocation and orb lifecycle.
5. Record PASS/FAIL/BLOCKED evidence.
6. Configure stable owner signing privately.
7. Produce trusted full-app distribution and run install-over-install data-retention testing.
8. Begin local LLM/wake-word work only after J1 role evidence.
9. Keep PR #12 Draft/open/unmerged until explicit owner approval.
