# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Pinpoint audit: `docs/MAYRA_PINPOINT_AUDIT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Test contract: `docs/MAYRA_TEST_MATRIX.md`
Rollback playbook: `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
J1 device sheet: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
J2 preflight: `docs/feasibility/MAYRA_J2_VOICE_PREFLIGHT.md`
Canonical product issue: #13
Mandatory major-feature feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every batch |
| Repository clarity | IN_PROGRESS | PR #12 is the only active implementation PR; stale PR tracks are closed/superseded; branches classified in issue #15 | Reachability review before deleting redundant refs |
| Feasibility control | DONE / CONTINUOUS | Issue #14 blocks major capabilities; J2 voice preflight approved invocation-time speech only | Wake-word and local-LLM preflights before implementation |
| Secure baselines | DONE / CONTINUOUS | Pre-Jarvis, Jarvis J1, J1 role proof and J2 voice baselines protected | Promote only exact-head green/device-evidenced milestones |
| J1 Assistant role proof | DEVICE_VERIFY | Motorola accepts/selects Mayra; configured Power trigger invokes orb while unlocked; Back/lock dismiss | Touch repair/repeated lifecycle/locked-start/reboot evidence through J2 |
| Animated Mayra presence | DEVICE_VERIFY | Orb physically rendered on Motorola; tap-dismiss repair is in green J2 baseline | Physical tap/repeat lifecycle verification |
| J2 invocation-time voice | DEVICE_VERIFY | J2 #18, J1 #122, Android CI #2013 and Governance #194 all green; exactly RECORD_AUDIO only | Motorola microphone/on-device STT/lifecycle/reboot acceptance |
| Wake phrase / always-awake voice | PLANNED | Continuous SpeechRecognizer loop rejected; dedicated wake detector required | Engine/license/battery/false-trigger/lock-screen preflight |
| Full owner installation | BLOCKED / IN_PROGRESS | Full debug Personal Alpha was blocked by Play Protect; stable owner-signing path exists | Configure private certificate + trusted distribution + upgrade proof |
| Local conversational brain | PLANNED | No local LLM integrated | Model/license/RAM/storage/latency/thermal benchmark and preflight |
| Background/lock-screen voice | DEVICE_VERIFY | Assistant framework/keyguard declaration exist; unlocked invocation proven | Locked-screen J2 evidence |
| Incoming-call control | PLANNED | No default Phone/InCallService yet | Dedicated feasibility review after voice foundation |
| AI caller message-taking | PLANNED WITH CONSTRAINTS | Arbitrary SIM audio capture/injection is not assumed | Voicemail/VoIP/call-forwarding architecture |
| Production release | IN_PROGRESS | Minified release/signing scaffold exists | Stable certificate, AAB/APK provenance, trusted channel |

## Protected baselines

### Pre-Jarvis full-app
- `baseline/mayra-0.2.1-green-1795`
- `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795: success

### Jarvis J1 foundation
- `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851: success

### Zero-permission J1 package
- `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 #44, Android CI #1935, Governance #116: success

### J1 activation repair
- `baseline/mayra-0.2.1-j1-activation-repair-green-56`
- `ce96f8e83fe33b878d426c407715d4a3e1b0495a`
- J1 #56, Android CI #1947, Governance #128: success

### J2 invocation-time voice
- `baseline/mayra-0.2.1-j2-voice-green-18`
- `ef809bbdaca80f3b953483499dc03de8e091339f`
- J2 Voice Test #18: success
- J1 Assistant Test #122: success
- Android CI #2013: success
- Project Governance #194: success
- J2 APK SHA-256: `ef48c264f841efd7891e335848e90f38654a6dd25f70d10b0a3afd08b968ecbc`
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_J2_VOICE_CI18.md`

## Motorola evidence established

1. J1 clean install/launch: PASS.
2. Motorola Default Apps route: PASS.
3. Mayra appears as Digital assistant candidate: PASS.
4. Mayra can be selected as default Digital assistant: PASS.
5. Motorola Power-button Digital assistant trigger configured: PASS.
6. Unlocked invocation launches Mayra VoiceInteractionSession: PASS.
7. Animated orb renders over current screen: PASS.
8. Back gesture dismisses: PASS.
9. Locking phone dismisses current session: PASS.
10. J1 #68 direct orb/outside tap: FAIL; root cause was missing click listeners.
11. Common tap/Back/hide lifecycle repair is now compile/package-green in J2 baseline.
12. Direct tap retest, 20-cycle stability, locked-start invocation and reboot persistence remain device-unverified.

Canonical J1 evidence: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

## J2 verified engineering scope

J2 is an engineering proof package, not a second product:

- package `ai.mayra.app.j2`;
- exactly `RECORD_AUDIO` permission;
- one setup/status launcher;
- same official Android Assistant/session foundation;
- bounded invocation-time on-device Android speech recognition when Android reports support;
- explicit listening/result/error states;
- recognition stop on hide/destroy;
- tap/root/label/Back dismissal repair;
- no continuous microphone loop;
- no internet/provider, contacts, notifications, reminders, WorkManager/Room, full chat, memory, documents or calls.

Artifact provenance and physical steps are fixed in `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`.

## Immediate next actions

1. Install exact J2 CI #18 candidate on the Motorola without bypassing Play Protect.
2. Grant microphone only when requested.
3. Select `Mayra J2 Voice Test` as Digital assistant and retain Power-button Digital assistant trigger.
4. Verify Android on-device speech availability and short Hindi/Hinglish/English transcripts.
5. Verify repaired orb/root/label tap dismissal plus Back and lock dismissal.
6. Run 20 invoke/listen/dismiss cycles; check microphone indicator, recognizer-busy state, crash/System UI stability and thermal behavior.
7. Test invocation beginning from an already locked screen.
8. Reboot and verify Assistant role + one speech cycle.
9. Record all PASS/FAIL/BLOCKED evidence in the J2 acceptance sheet before calling J2 device-verified.
10. In parallel, complete wake-word and local-model feasibility documents/benchmarks; do not integrate either before its gate passes.
11. Keep Phone/InCallService and AI-call-answering implementation blocked until dedicated preflights.
12. Keep PR #12 Draft/open/unmerged until explicit owner approval.
