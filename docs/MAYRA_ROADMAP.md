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
| J5 Home core | STRONG DEVICE PROOF | Default Home, app search/launch, reboot, switch-back and Airplane behavior physically proven on #2384 | Preserve while verifying permanent owner build |
| J5 unified presence | CI GREEN / DEVICE VERIFY | Home orb + shared entry contract + voice-session-to-full-Mayra handoff green on `cc89a392…` | Verify on permanent owner package |
| J5 owner delivery line | **STABLE SIGNER ESTABLISHED / DEVICE VERIFY** | First `ai.mayra.app.owner` APK built and certificate-verified on Stable Owner Alpha #16 | Install side-by-side, then prove second owner build updates in place |
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

## Permanent owner signing line — established

A durable owner-device update line now exists independently from transient `.alpha` signing:

- build type: `ownerAlpha`;
- package: `ai.mayra.app.owner`;
- label: `Mayra AI Owner`;
- encrypted GitHub Actions owner-signing Secrets configured;
- exact stable signer held outside repository source/history;
- working `.alpha` stays installed as rollback/reference during migration.

First stable owner build:

- exact source: `b72270aa83aecb24f120e619fc50094a77816f45`;
- Stable Owner Alpha #16 / run `30987409944` — SUCCESS;
- artifact `8922774120` / `mayra-stable-owner-apk-16`;
- ZIP digest `sha256:9aa9ca2b5c3f8b7a6aab9582303003471a0da17775f3707ca2a116e2178ac19d`;
- APK SHA-256 `233cb686851abeab1f923bf8be2a39dccf003d5debc3613951d2165db2d7d439`;
- signer SHA-256 `1617672fc426020921598dc4cc5f361d464ed6e65d9d7e8919b6931964d289dd`;
- v2/v3 signing verified;
- package/label verification passed.

Engineering backup: `backup/j5-stable-owner-signer-green-2026-08-05`.
Immutable signer milestone: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_STABLE_OWNER_SIGNER_ESTABLISHED.md`.

## Permanent owner Motorola gate

1. Keep working `.alpha` installed.
2. Install exact Stable Owner Alpha #16 APK side-by-side.
3. Confirm `Mayra AI Owner` appears/selects as Home.
4. Confirm central orb and full-Mayra handoff.
5. Re-check app search/open/Home return.
6. Re-check Power-button Android Assistant invocation/dismissal where configured.
7. Re-check lock/unlock, reboot, Airplane mode and launcher switch-back.
8. Observe crashes/jank/thermal/battery regressions.
9. Keep `.alpha` until `.owner` is accepted.
10. Build a second stable-owner APK using the same signer and prove direct install-over-install without uninstall.

## Promotion rule

Older J5 device evidence remains valid for its exact source but does not automatically prove the permanent owner package. Protected J5 promotion requires exact promoted source + automated green + stable signer continuity + accepted Motorola evidence. After pass: synchronize docs → immutable J5 milestone snapshot → protected J5 baseline → begin J6.

## Major-step discipline

Idea/Decision/Blueprint/Roadmap/Preflight → coherent implementation → CI/package audits → Motorola proof → canonical evidence sync → immutable milestone snapshot → protected baseline. A red/pending head is never stable.

## Immediate next actions

1. Install Stable Owner Alpha #16 `ai.mayra.app.owner` on Motorola side-by-side with `.alpha`.
2. Run unified launcher/assistant regression on the owner package.
3. Produce a second stable owner build and prove update-over-update with the same signer.
4. If accepted, promote J5 protected baseline and then start J6 context cards.
5. Separately finish J4 quality/RAM/cancel/stress/background/lock/Airplane/thermal round.
6. Keep PR #12 Draft/open/unmerged until explicit owner approval.
