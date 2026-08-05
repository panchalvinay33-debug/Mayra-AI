# Mayra AI — Execution Roadmap

Last updated: 2026-08-05
Entry point: `START_HERE.md`
Jarvis/launcher master plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`

## Product direction

Mayra targets a practical Jarvis-style personal Android operating layer. Home, quick voice presence and full conversation should feel like one Mayra while internal modules remain isolated enough that AI/model/provider failure cannot break basic Home/app access.

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical docs, protected recovery and engineering backups exist | Sync every meaningful batch |
| J1 Assistant | DEVICE VERIFIED FOUNDATION | Android Assistant/Power invocation previously proven | Preserve regression |
| J2 voice/privacy | DEVICE VERIFIED FOUNDATION | Multilingual recognition, dismissal, lock/privacy proven | Preserve regression |
| J3 neural TTS | DEVICE BENCHMARK PASS / LICENSE BLOCKED | Technical offline voice proof exists; tested voice not production-cleared | License-clear voice later |
| J4 local brain | RUNTIME PROVEN / QUALITY DEVICE VERIFY | Runtime, stress/cancel/RAM harness built | Finish physical quality round |
| J5 Home core | STRONG DEVICE PROOF | Default Home, app search/launch, reboot, switch-back and Airplane behavior physically proven on #2384 | Preserve while verifying unified build |
| J5 unified presence | **CI GREEN / DEVICE VERIFY** | Home orb + shared entry contract + voice-session-to-full-Mayra handoff green on `cc89a392…` | Motorola regression on #2416 |
| J6 context fabric | ACCEPTED / BLOCKED ON J5 PROMOTION | reminders/memory/documents/notifications can feed typed context | Start after J5 protected baseline |
| J7 trust/actions | ACCEPTED | typed action/confirmation foundation exists | Formal GREEN/AMBER/RED policy |
| J8 proactive | ACCEPTED | briefing foundations exist | privacy/battery/context-quality gates |
| J9 multimodal | LATER | architecture can expand | privacy/RAM/thermal gate |
| J10 routines | LATER | typed workflow philosophy established | owner-defined routines after trust layer |

## Protected rollback

`baseline/mayra-0.2.1-j4-ci-recovery-green-134` at `e72488a6f6dceb24950f9b0f574ae223d52bd8bb` remains immutable.

## J5 Home core device evidence

Exact old device-proven source `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a` / Android #2384 proved on Motorola Edge 70 Fusion / Android 16:

- Mayra accepted as default Home;
- 81/81 launchable apps rendered;
- search → Chrome → Home works;
- lock/unlock and reboot preserve usable Mayra Home;
- previous launcher ↔ Mayra switching works;
- Airplane mode keeps Home/search/app launch usable;
- `Ask Mayra` opens normal Mayra;
- normal Mayra has bounded offline-core behavior without general provider connectivity.

## J5 unified presence checkpoint

Exact source `cc89a392a53fcb910166c92badaab3543b5520ff`.
Backup: `backup/j5-unified-mayra-ci-green-2026-08-05`.

Green automated evidence:
- Android CI #2416
- J1 #525
- J2 #421
- J3 #243
- J4 #194
- Governance #597

Implemented:
- shared `MayraEntryContract` between launcher, Android voice session and full Mayra;
- large central Mayra orb/card on Home;
- orb/Open Mayra launches the same full Mayra activity using clear-top/single-top semantics;
- heard unlocked Android Assistant response can be tapped to continue in full Mayra;
- Home still does not initialize heavy AI/memory/privileged-action runtime;
- locked-device assistant privacy and bounded dismissal remain intact;
- all isolated and full packaging boundaries stay green.

Exact Motorola candidate:
- Android #2416 (`30981372713`)
- Personal Alpha artifact `8920663408`
- ZIP digest `sha256:978059bca48282eb0ee86cd0d981a8a74a3a090c12121136423cde5bf3d56f2a`
- APK SHA-256 `fb4963e2678472fe471dd2f911a746e7dc8086743255952980ed4ef3c399ba77`

## Unified J5 Motorola gate

1. Install/update exact #2416 APK.
2. Confirm Mayra remains/selects as default Home.
3. Confirm new central orb/card renders.
4. Tap orb/Open Mayra → full Mayra opens without duplicate-loop behavior.
5. Re-check app search/open/Home return.
6. Re-check Power-button Android Assistant invocation/dismissal.
7. Speak an unlocked request; tap assistant response → full Mayra opens without a navigation loop.
8. Re-check lock/unlock, reboot, Airplane mode and launcher switch-back.
9. Observe crashes/jank/thermal/battery regressions.
10. Record signing identity when practical.

## Promotion rule

Older J5 device evidence remains valid for its exact source but does not automatically prove the new unified source. Protected J5 promotion requires exact unified source + automated green + accepted Motorola evidence. After pass: synchronize docs → immutable J5 milestone snapshot → protected J5 baseline → begin J6.

## Major-step discipline

Idea/Decision/Blueprint/Roadmap/Preflight → coherent implementation → CI/package audits → Motorola proof → canonical evidence sync → immutable milestone snapshot → protected baseline. A red/pending head is never stable.

## Immediate next actions

1. Physically verify #2416 unified J5 build on Motorola.
2. If accepted, promote J5 protected baseline and then start J6 context cards.
3. Separately finish J4 quality/RAM/cancel/stress/background/lock/Airplane/thermal round.
4. Keep PR #12 Draft/open/unmerged until explicit owner approval.
