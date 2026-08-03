# Mayra AI — Execution Roadmap

Last updated: 2026-08-03
Entry point: `START_HERE.md`
Latest recovery state: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
J2 device sheet: `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`
Canonical product issue: #13
Mandatory major-feature feasibility gate: #14
Repository hygiene registry: #15

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, governance CI and protected baselines exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; Power trigger invokes orb; Back/lock dismiss | Preserve as regression baseline |
| J2 invocation-time voice | DEVICE RETEST READY | CI #106 is exact-head quadruple-green with support probing + single-recognizer reuse; previous CI #90 device failure is documented | Motorola transcript retest with #106 |
| Animated Mayra presence | DEVICE VERIFY | Orb physically renders; tap-dismiss repair exists in shared session | Verify direct tap dismissal after transcript proof |
| Wake phrase | BENCHMARK | Continuous SpeechRecognizer loop rejected; dedicated local KWS required | Start only after J2 transcript/lifecycle acceptance |
| Local brain | BENCHMARK | LiteRT-LM/Qwen-class direction preflighted, no model selected | Motorola benchmark after J2 voice proof |
| Lock-screen voice | DEVICE VERIFY | Assistant framework foundation exists | Test already-locked invocation after unlocked transcript |
| Calls | ACCEPTED / GATED | Default Phone/InCallService architecture preflighted | No role takeover before full call UI/runtime |
| Trusted install | IN_PROGRESS | Stable owner signing/trusted distribution still required for seamless upgrades | Configure private certificate/channel |

## Protected baselines

- `baseline/mayra-0.2.1-green-1795`
- `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- `baseline/mayra-0.2.1-j1-activation-repair-green-56`
- `baseline/mayra-0.2.1-j2-voice-green-18`
- `baseline/mayra-0.2.1-j2-locale-repair-green-90`
- `baseline/mayra-0.2.1-j2-speech-support-green-106`

Latest protected J2 application baseline:

- source `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 #106 success
- J1 #210 success
- Android CI #2101 success
- Governance #282 success
- artifact `mayra-j2-voice-apk-106`, ID `8866441207`
- APK size `19,209,329` bytes
- APK SHA-256 `d0917d17b50429a843f3a5e688580df66f3eea678be4806b44ef9f1535adeb6e`
- ZIP SHA-256 `b2109366a0140a66f85fef3cd6a85a95263815643ae86076ba0a9f20194140db`
- package boundary remains exactly `RECORD_AUDIO`.

## Motorola evidence established

1. J1/J2 appear as valid Digital assistant candidates.
2. Mayra can be selected as default Digital assistant.
3. Motorola Power-button Assistant trigger launches Mayra.
4. Orb renders over Home.
5. Back/lock dismiss common session.
6. J2 microphone permission/readiness is PASS.
7. Android reports an on-device recognition service is available.
8. CI #18 recognition FAIL: `Speech language unavailable`.
9. CI #90 recognition FAIL: `Speech recognizer unavailable`.
10. No false transcript or crash was observed in either failure.

## J2 #106 repair now green

- one `SpeechRecognizer` instance per bounded attempt instead of destroy/recreate on every locale fallback;
- Android 13+ uses `checkRecognitionSupport()` before listening;
- Mayra prefers actually installed on-device languages over guessed locales;
- if a model is not installed but available for download, surface `On-device speech language pack needed`;
- OEM support-probe failure falls back to bounded delayed locale trials;
- locale retry delay is 450 ms while reusing the recognizer;
- no cloud STT fallback, no endless listening loop, no permission expansion;
- new unit tests cover support-state language ordering and normalization.

## Immediate next actions

1. Clean-install J2 #106 if CI-signature conflict occurs; remove only engineering J2 package, not full Mayra.
2. Select J2 as Digital assistant if Android reset the role.
3. Motorola retest `Mayra namaste` first and capture exact transcript/error.
4. If transcript succeeds: test `kal subah saat baje`, `open WhatsApp`, and one short English phrase.
5. Then verify tap/root/label dismissal, microphone indicator stop, 20 cycles, already-locked invocation and reboot recovery.
6. If Mayra reports `On-device speech language pack needed`, record that exact state and add explicit language-pack guidance rather than hidden cloud recognition.
7. Keep wake-word/local-LLM/Phone-role production integration gated.
8. Keep PR #12 Draft/open/unmerged until explicit owner approval.
