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
| J2 invocation-time voice | DEVICE REPAIR ACTIVE | CI #90 is quadruple-green and physically invokes with mic/readiness, but transcript still fails with `Speech recognizer unavailable` after the earlier `Speech language unavailable` failure | Green the support-probe/single-recognizer repair, then Motorola transcript retest |
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

Latest protected J2 application baseline:

- source `e706bdfb8f53006825404db99a51f466aa251fc4`
- J2 #90 success
- J1 #194 success
- Android CI #2085 success
- Governance #266 success
- APK SHA-256 `2c1e00db4a2bfd98993eb87fe091c5373931153eb3b5ac2252914d4441ac230c`

Do not move that baseline to the new device-repair source until fresh exact-head CI is fully green.

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

## Current J2 source repair

After CI #90 physical failure:

- use one `SpeechRecognizer` instance for a bounded attempt instead of destroy/recreate on every locale fallback;
- Android 13+ probes `checkRecognitionSupport()` before listening;
- prefer actually installed on-device languages over guessed locales;
- distinguish `language pack needed` / `no installed language` from generic recognizer failure;
- if OEM support probing is unavailable, use bounded locale fallback;
- delay retry 450 ms and reuse the same recognizer;
- still no cloud STT fallback, no endless listening loop, no permission expansion;
- unit tests cover installed-language ordering and normalization.

## Immediate next actions

1. Settle fresh J2/J1/Android/Governance CI for the support-probe/single-recognizer repair.
2. Repair any compile/lint/unit/package finding without suppressing checks.
3. Share a new J2 APK only after all required gates are green and artifact hashes are recorded.
4. Motorola retest `Mayra namaste` first.
5. If transcript succeeds: test Hindi/Hinglish/English phrases, tap dismissal, 20 cycles, locked-screen start and reboot recovery.
6. If Android reports `language pack needed`, handle model-download UX explicitly rather than silently using network/cloud recognition.
7. Keep wake-word/local-LLM/Phone-role production integration gated.
8. Keep PR #12 Draft/open/unmerged until explicit owner approval.
