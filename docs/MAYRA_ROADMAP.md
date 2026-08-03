# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Pinpoint audit: `docs/MAYRA_PINPOINT_AUDIT.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Test contract: `docs/MAYRA_TEST_MATRIX.md`
Rollback playbook: `docs/MAYRA_BASELINE_AND_ROLLBACK.md`
J1 device sheet: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`
J2 preflight: `docs/feasibility/MAYRA_J2_VOICE_PREFLIGHT.md`
Canonical product issue: #13
Mandatory major-feature feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every batch |
| Repository clarity | IN_PROGRESS | PR #12 is the only active implementation PR; old PR tracks superseded; branches classified in issue #15 | Verify delete-candidate reachability before deletion |
| Feasibility control | DONE / CONTINUOUS | Issue #14 blocks major features; J2 voice preflight is now recorded | Local-LLM and wake-word preflights later |
| Secure baselines | DONE / CONTINUOUS | Pre-Jarvis #1795, Jarvis #1851, J1 #44 and activation-repair #56 protected | Promote next exact-head green/device milestone |
| J1 Assistant role proof | DEVICE_VERIFY | Motorola accepts/selects Mayra; configured Power trigger invokes orb while unlocked; Back/lock dismiss | Touch-repair CI, repeated lifecycle, lock-screen and reboot evidence |
| Animated Mayra presence | DEVICE_VERIFY | Orb physically rendered on Motorola; direct touch dismiss was missing and is repaired in source | Fresh CI then retest tap/repeat lifecycle, preferably through J2 |
| J2 invocation-time voice | IN_PROGRESS | Isolated `.j2` variant, one-permission manifest, state model, on-device recognizer wrapper and CI workflow committed | J2 CI → Motorola microphone/on-device STT proof |
| Wake phrase / always-awake voice | PLANNED | `SpeechRecognizer` continuous loop explicitly rejected; dedicated detector required | Engine/license/battery/lock-screen preflight after J2 |
| Full owner installation | BLOCKED / IN_PROGRESS | Full debug Personal Alpha was blocked by Play Protect; stable signing path exists | Private certificate + trusted distribution |
| Local conversational brain | PLANNED | No local LLM integrated | Model/license/RAM/storage/latency/thermal benchmark |
| Background/lock-screen voice | DEVICE_VERIFY | Assistant framework and keyguard declaration exist; unlocked invocation proven | Locked-screen J1/J2 evidence |
| Incoming-call control | PLANNED | No default Phone/InCallService yet | Dedicated feasibility review after voice foundation |
| AI caller message-taking | PLANNED WITH CONSTRAINTS | Direct arbitrary SIM audio capture/injection is not assumed | Voicemail/VoIP/call-forwarding design |
| Production release | IN_PROGRESS | Minified release/signing scaffold exist | Stable certificate, AAB/APK provenance, trusted channel |

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

## Motorola evidence now established

1. J1 clean installation and launch: PASS.
2. Motorola Default Apps route opens: PASS.
3. `Mayra J1 Assistant Test` appears as a valid Digital assistant candidate: PASS.
4. Mayra can be selected as the default Digital assistant: PASS.
5. J1 itself reports `Mayra is selected`: PASS.
6. Motorola Power-button action can be configured to Digital assistant: PASS.
7. Unlocked assistant invocation launches Mayra `VoiceInteractionSession`: PASS.
8. Animated Mayra orb renders over the current screen: PASS.
9. Back gesture dismisses the orb: PASS.
10. Locking the phone dismisses the current unlocked orb: PASS.
11. Direct orb/outside tap on the tested #68 build: FAIL because no click listener existed; source repair now adds bounded tap exits.
12. Locked-screen invocation while already locked, repeated 10–20 cycles and reboot role recovery remain unverified.

Canonical evidence: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

## J2 scope now under implementation

J2 is deliberately smaller than the final Mayra app:

- package `ai.mayra.app.j2`;
- only `RECORD_AUDIO` permission;
- one small setup/status activity;
- same Android Assistant/session foundation;
- invocation-time on-device speech recognizer only when Android reports it available;
- no continuous microphone loop;
- no internet/provider, contacts, reminders, notifications, WorkManager/Room/background intelligence or full chat runtime;
- transcript proof only: J2 shows what it heard and does not pretend a local LLM answered.

Dedicated J2 CI must pass compile, unit tests, lint, APK assembly, exactly-one-permission audit, launcher/component audit and artifact upload.

## Immediate next actions

1. Settle fresh exact-head J1, J2, Android CI and Project Governance.
2. Repair any compile/lint/manifest/audit finding; do not weaken checks to force green.
3. If all gates pass, create a new protected code baseline before additional high-risk work.
4. Prefer the new J2 package for the next owner test so J1 zero-permission evidence remains untouched.
5. On Motorola J2: grant microphone, select Mayra J2 as Digital assistant, verify on-device recognition availability, invoke with Power trigger and speak short Hindi/Hinglish/English phrases.
6. Verify tap/Back/lock dismissal and 20 repeat cycles in the same J2 test.
7. Run locked-screen invocation and reboot/role recovery evidence.
8. Only after J2 proof: complete dedicated wake-word feasibility; do not use continuous SpeechRecognizer as a hotword loop.
9. In parallel, prepare local-model benchmark criteria; do not bundle a model before RAM/thermal/license evidence.
10. Keep Phone/InCallService and AI-call-answering implementation blocked until their own preflights.
11. Keep PR #12 Draft/open/unmerged until explicit owner approval.
