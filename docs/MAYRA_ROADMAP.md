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
| Repository clarity | IN_PROGRESS | PR #12 is the only active implementation PR; old tracks superseded; branches classified in issue #15 | Reachability verification before branch deletion |
| Feasibility control | DONE / CONTINUOUS | Issue #14 blocks major features until Android/Motorola/permission/performance/distribution/fallback review | Apply before local LLM and wake phrase |
| Secure baselines | DONE | Pre-Jarvis #1795, Jarvis #1851, J1 #44 and activation-repair #56 protected | Promote only exact-head green/device-evidenced milestones |
| Zero-permission J1 test | SECOND REPAIR IN PROGRESS | #44 and #56 installed/launched; both failed to reach usable Assistant selection on Motorola | Triple-green second repair, then Default apps/Digital assistant retest |
| Full owner installation | BLOCKED / IN_PROGRESS | Full debug Personal Alpha was blocked by Play Protect; stable signing workflow exists | Private certificate and trusted distribution |
| Local conversational brain | PLANNED | No local LLM integrated | Issue #14 model/licensing/RAM/thermal benchmark |
| Animated Mayra presence | BLOCKED BY ACTIVATION | Orb/session package is CI verified but cannot be invoked until Mayra is selected as Digital assistant | Fix candidate visibility/selection, then invoke orb |
| Android Assistant role | SECOND REPAIR IN PROGRESS | VoiceInteraction foundation valid; J1 metadata had invalid settingsActivity and activation used wrong/generic settings route | J1-specific metadata + Motorola Default apps path + device retest |
| Offline wake phrase | PLANNED | Recognition shell only | Battery/background/lock-screen preflight after J1 |
| Incoming-call control | PLANNED | No Phone/InCallService yet | Dialer/InCallService feasibility after J1/J2 |
| Production release | IN_PROGRESS | Minified release and signing scaffold exist | Stable certificate, AAB/APK provenance, Internal Testing |

## Protected baselines

### Pre-Jarvis
- `baseline/mayra-0.2.1-green-1795`
- `065e22524c835f3ddd3b2f56215a3616f071d4b3`

### Jarvis J1 foundation
- `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- `0d9435adb92b425bfb47a710d4f4516a6aaac398`

### Zero-permission J1 package
- `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 #44, Android CI #1935, Governance #116: success

### First activation repair
- `baseline/mayra-0.2.1-j1-activation-repair-green-56`
- `ce96f8e83fe33b878d426c407715d4a3e1b0495a`
- J1 #56, Android CI #1947, Governance #128: success
- Device activation still failed; baseline remains useful as exact known-green package evidence.

## Motorola evidence

1. J1 #44 installed and launched without bypassing Play Protect.
2. #44 `Activate Mayra` produced no visible response.
3. #56 installed/updated and launched; diagnostic text worked, but tapping Activate Mayra still did not leave the app or produce a usable system selection screen.
4. Pinpoint review found the shared voice-interaction metadata referenced `ai.mayra.app.MainActivity`, which J1 removes. J1 now overrides the XML so `settingsActivity` is `ai.mayra.app.j1.J1AssistantTestActivity`.
5. Motorola Edge 70 Fusion Android 16 documentation identifies the supported manual route as `Settings → Apps → Default apps → Digital assistant`.
6. J1 Activate Mayra now opens `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS` directly and displays the exact Motorola path on-screen.
7. Acceptance evidence is recorded in `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

## Immediate next actions

1. Run J1 CI, Android CI and Project Governance on the synchronized second-repair head.
2. Share no APK until all three are green.
3. Install the new artifact over #56 where signing permits.
4. Tap Activate Mayra; expect Android Default apps screen.
5. Tap Digital assistant and record whether Mayra appears.
6. If app navigation still fails, manually use `Settings → Apps → Default apps → Digital assistant` to separate navigation failure from candidate-eligibility failure.
7. If Mayra appears, select it, refresh J1 status, then test unlocked/locked invocation and orb lifecycle.
8. Keep local LLM, wake phrase and Phone-role coding blocked until J1 evidence is resolved.
9. Keep PR #12 Draft/open/unmerged until explicit owner approval.
