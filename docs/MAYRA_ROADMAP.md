# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Pinpoint audit: `docs/MAYRA_PINPOINT_AUDIT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Test contract: `docs/MAYRA_TEST_MATRIX.md`
Rollback playbook: `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
J1 device sheet: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`
Canonical product issue: #13
Mandatory major-feature feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE | Canonical records, governance CI and protected baselines | Continuous synchronization |
| Repository clarity | IN_PROGRESS | PR #12 is the only active implementation PR; old tracks are superseded and branches classified in issue #15 | Reachability verification before branch deletion |
| Feasibility control | DONE / CONTINUOUS | Issue #14 blocks major features until Android/Motorola/permission/performance/distribution/fallback review | Apply before local LLM and wake phrase |
| Secure baselines | DONE | Pre-Jarvis #1795, Jarvis #1851 and zero-permission J1 #44 protected | Promote only exact-head green/device-evidenced milestones |
| Zero-permission J1 test | REPAIR IN PROGRESS | J1 #44 installed and launched, but Activate Mayra produced no visible response on Motorola | Triple-green repaired APK, then role-selection retest |
| Full owner installation | BLOCKED / IN_PROGRESS | Full debug Personal Alpha was blocked by Play Protect; stable signing workflow exists | Private certificate and trusted distribution |
| Local conversational brain | PLANNED | No local LLM integrated | Issue #14 model/licensing/RAM/thermal benchmark |
| Animated Mayra presence | BLOCKED BY ACTIVATION | Orb/session package is CI verified but cannot be invoked until role selection works | Repair activation route and test invocation |
| Android Assistant role | REPAIR IN PROGRESS | Role foundation packaged; Motorola activation button was silent | Visible role/settings routing and device retest |
| Offline wake phrase | PLANNED | Recognition shell only | Battery/background/lock-screen preflight after J1 |
| Incoming-call control | PLANNED | No Phone/InCallService yet | Dialer/InCallService feasibility after J1/J2 |
| Production release | IN_PROGRESS | Minified release and signing scaffold exist | Stable certificate, AAB/APK provenance, Internal Testing |

## Protected baselines

### Pre-Jarvis
- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`

### Jarvis J1 foundation
- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`

### Zero-permission J1 package
- Branch: `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- Commit: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 #44, Android CI #1935 and Governance #116: success
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`

This baseline remains the rollback point even though device activation failed; the failure is evidence about Motorola runtime behavior, not package integrity.

## Motorola evidence

1. J1 #44 installed successfully without bypassing Play Protect.
2. J1 app launched and displayed `Mayra is not selected`.
3. Tapping `Activate Mayra` produced no visible response.
4. Root cause: intent resolution/launch failures were hidden by silent fallback logic.
5. Repair candidate `2b06cf8fe92b12c2c9d36d5099d1695ea13a1cf9` adds resolve checks, official settings fallback order and visible diagnostics.
6. Acceptance evidence is recorded in `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

## Immediate next actions

1. Run J1 CI, Android CI and Project Governance on the synchronized repair head.
2. Share no repaired APK until all three are green.
3. Install the repaired J1 package over/after #44 as signing permits.
4. Tap Activate Mayra and capture the visible status/system screen.
5. Test role visibility, select/remove, unlocked invocation, lock-screen behavior and orb lifecycle.
6. Update device evidence and promote a new baseline only after green CI plus improved device result.
7. Keep local LLM, wake phrase and Phone-role coding blocked until J1 is resolved.
8. Keep PR #12 Draft/open/unmerged until explicit owner approval.
