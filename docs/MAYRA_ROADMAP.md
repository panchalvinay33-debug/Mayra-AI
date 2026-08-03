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
| Feasibility control | DONE / CONTINUOUS | Issue #14 is the permanent major-capability gate; all currently listed capability preflights have an explicit direction | Keep adding new major capabilities before implementation |
| Secure baselines | DONE / CONTINUOUS | Pre-Jarvis, Jarvis J1, J1 role proof and J2 CI #18 baselines protected | Promote next exact-head green repair only after fresh CI |
| J1 Assistant role proof | DEVICE_VERIFY | Motorola accepts/selects Mayra; configured Power trigger invokes orb while unlocked; Back/lock dismiss | Touch repair/repeated lifecycle/locked-start/reboot evidence through J2 |
| Animated Mayra presence | DEVICE_VERIFY | Orb physically rendered on Motorola; tap-dismiss repair is in green J2 baseline | Physical tap/repeat lifecycle verification |
| J2 invocation-time voice | IN_PROGRESS / DEVICE REPAIR | J2 #18 installs; mic allowed; on-device recognizer available; Assistant invocation and mic-active orb physically proven; first recognition failed with `Speech language unavailable` | Fresh CI for bounded locale fallback, then Motorola transcript retest |
| Wake phrase / always-awake voice | BENCHMARK | Continuous SpeechRecognizer loop rejected; dedicated local KWS preflight/benchmark recorded | Only after J2 transcript/lifecycle acceptance |
| Full owner installation | BLOCKED / IN_PROGRESS | Full debug Personal Alpha was blocked by Play Protect; stable owner-signing/trusted-install path documented | Configure private certificate + trusted distribution + upgrade proof |
| Local conversational brain | BENCHMARK | LiteRT-LM runtime direction and Qwen3-1.7B candidate are preflighted, not selected | Motorola RAM/storage/latency/thermal/quality benchmark after J2 |
| Background/lock-screen voice | DEVICE_VERIFY | Assistant framework/keyguard declaration exist; unlocked invocation proven | Locked-screen J2 evidence |
| Incoming-call control | ACCEPTED / GATED | Default Phone/InCallService preflight complete; no role takeover implemented | Complete isolated call UI/runtime before role request |
| AI caller message-taking | ACCEPTED WITH CONSTRAINTS | Direct arbitrary SIM audio path rejected; forwarding/VoIP route required | Provider/carrier architecture proof |
| Notification intelligence | ACCEPTED | Notification Access/local-first preflight recorded | Owner controls/filter tests/device acceptance |
| App workflow automation | ACCEPTED | APIs/intents/deep-link-first boundary recorded | Typed adapters per real workflow |
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

This remains the last protected J2 code baseline until the language-fallback repair becomes fresh exact-head green.

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
10. J2 installs/opens on Motorola: PASS.
11. J2 microphone permission allowed: PASS.
12. Android reports J2 on-device speech recognition available: PASS.
13. J2 can be selected/invoked as Assistant: PASS.
14. J2 invocation shows Android microphone privacy indicator: PASS.
15. First J2 transcript attempt: FAIL with `Speech language unavailable`.

The last failure is bounded: no false transcript and no crash. Root cause is missing explicit locale negotiation in the CI #18 candidate.

## Current J2 repair

Implemented after physical failure:

- `MayraSpeechLocalePolicy` with finite locale chain: device locale → `hi-IN` → `en-IN` → `en-US`;
- duplicate locale removal;
- explicit `RecognizerIntent.EXTRA_LANGUAGE` and language preference;
- automatic retry only for `ERROR_LANGUAGE_NOT_SUPPORTED` / `ERROR_LANGUAGE_UNAVAILABLE`;
- no endless retry loop and no cloud fallback;
- unit tests for locale ordering/duplicate removal/blank device locale.

Fresh validation history:

- J1 #179 failed lint because the new on-device recognizer retry call did not expose its API-31 guard strongly enough to Android lint.
- No audit was weakened and no lint baseline/suppression was added.
- Repair commit `8de560527fed1ed41e6e2f50230ac97522c393f3` adds an explicit Android 12/API-31 guard at the exact recognizer creation boundary.
- Replacement APK remains blocked until fresh J2/J1/Android/Governance checks are green.

## Immediate next actions

1. Settle latest-head J2/J1/Android/Governance workflows for the locale-retry + API-guard repair.
2. Inspect and repair any compile/lint/package/governance finding; do not weaken audits.
3. Share a replacement J2 APK only after all required gates are green and artifact provenance is recorded.
4. On Motorola, invoke J2 and test `Mayra namaste`, `kal subah saat baje`, `open WhatsApp`, and one short English phrase.
5. Record the actual transcript and which bounded error occurs if every locale remains unavailable.
6. Verify repaired orb/root/label tap dismissal plus Back and lock dismissal.
7. Run 20 invoke/listen/dismiss cycles after transcript proof.
8. Test invocation beginning from an already locked screen and reboot recovery.
9. Keep wake-word/local-LLM production integration blocked until J2 voice acceptance completes.
10. Keep Phone role owner takeover blocked until full call UI/runtime is green.
11. Keep PR #12 Draft/open/unmerged until explicit owner approval.
