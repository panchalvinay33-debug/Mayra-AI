# Mayra AI — Execution Roadmap

Last updated: 2026-08-05
Entry point: `START_HERE.md`
Jarvis/launcher master plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Local LLM benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`

## Product direction

Mayra is targeting a practical **Jarvis-style personal Android operating layer** combining Digital Assistant voice, local-first AI, owner-controlled memory/documents, AI-native Home, provenance-aware context, trust-gated actions, proactive assistance, later multimodal understanding and owner-defined routines.

The launcher is the resilient Home shell, not privileged authority. Heavy AI/model/provider failure must never block basic Home/app access.

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, immutable snapshots, protected recovery and engineering backup refs exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; Power invocation works | Preserve regression |
| J2 recognition/privacy | DEVICE VERIFIED FOUNDATION | Hindi/Hinglish/English recognition, dismissal, lock/privacy cycles proven | Preserve regression |
| Android system TTS | FALLBACK PASS | Offline fallback works; quality is robotic | Keep safe fallback |
| J3 neural TTS | DEVICE BENCHMARK PASS / LICENSE BLOCKED | Offline neural synthesis proven; tested voice pack not production-cleared | Find license-clear voice |
| J4 local LLM runtime | DEVICE RUNTIME PROVEN | Gemma3-1B import/SHA, CPU Stage 5/5, fixed multilingual generation and close/reload physically proven | Quality/operability device round |
| J4 CI recovery | DONE / PROTECTED | Room/KSP race repaired; protected baseline exists | Preserve immutable baseline |
| J4 quality harness | CI GREEN / DEVICE VERIFY | Longer prompts, RAM telemetry, stress, cancel/rebind are green | Motorola quality/RAM/thermal/lock/Airplane round |
| J5 launcher shell | **CI GREEN / DEVICE VERIFY** | HOME shell, app list/search/launch, switch-back, contract tests and merged-APK HOME audits pass on exact source `6d5e773…` | Motorola default-HOME/reboot/switch-back/AI-failure proof |
| J6 context fabric | ACCEPTED / BLOCKED ON J5 DEVICE PASS | Existing reminders/memory/documents/notifications can feed typed context | Start after protected J5 promotion |
| J7 trust/action orchestration | ACCEPTED | Typed action/confirmation foundation exists | Formal GREEN/AMBER/RED policy after context boundary |
| J8 proactive Mayra | ACCEPTED | Briefing/notification foundations exist | Privacy/battery/context-quality gates |
| J9 multimodal | ACCEPTED / LATER | Document/image architecture can expand | Privacy/RAM/thermal benchmark |
| J10 routines | ACCEPTED / LATER | Typed workflow philosophy established | Owner-defined routines after trust layer |

## Protected recovery baseline

`baseline/mayra-0.2.1-j4-ci-recovery-green-134`

Exact source `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`. Preserve unchanged while J4 quality and J5 physical verification proceed.

## J4 quality checkpoint

Source `862450933da3700d4d1559e09ebde910a4185914`, backup `backup/j4-quality-harness-ci-green-2026-08-05`.

Android #2364, J1 #473, J2 #369, J3 #191, J4 #142 and Governance #545 all passed. Physical quality/RAM/thermal/background/lock/Airplane evidence remains pending, so the tested local model is not yet promoted as production conversational brain.

## J5 automated milestone — DEVICE_VERIFY entry point

Exact implementation source: `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`

Engineering backup: `backup/j5-home-contract-ci-green-2026-08-05`
Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_J5_HOME_CONTRACT_CI_GREEN.md`

Green evidence:
- Android CI #2384
- J1 #493
- J2 #389
- J3 #211
- J4 #162
- Governance #565

Current J5 slice includes:
- separate `MayraLauncherActivity`;
- `MAIN + HOME + DEFAULT` Home qualification;
- user-consent `ROLE_HOME` request;
- launchable-app inventory;
- case-insensitive label/package search with unit tests;
- direct app launching;
- `Ask Mayra` bridge;
- explicit Home-settings switch/restore route;
- Home rendering independent of model/provider/memory/privileged-action startup;
- Personal Alpha and minified Release contract audits explicitly proving HOME component/categories while preserving one normal app LAUNCHER entry.

Exact Motorola artifact:
- Android #2384 Personal Alpha artifact `8919388343`;
- artifact ZIP digest `sha256:92a6aa72d54e48c9fbc835e277b5d3471ec4c9112dfa9ab6e65a16ff229a2e17`;
- extracted APK SHA-256 `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`.

## J5 Motorola promotion gate

1. Verify exact APK hash and signing identity.
2. Mayra appears in Android Home/default launcher selection.
3. Selecting Mayra succeeds.
4. Home gesture/button returns to Mayra 20/20.
5. App list/search/open work reliably.
6. Lock/unlock and reboot preserve a usable Home.
7. Previous launcher can be restored and Mayra re-selected.
8. Local model absent/killed, provider/network unavailable, TTS unavailable, contacts/notification access denied: basic Home still works.
9. Assistant invocation does not create Home/Assistant loops.
10. Record RAM/battery/thermal/jank/ANR/crash observations.

Only after this exact-source physical pass: synchronize evidence → immutable J5 device milestone snapshot → protected J5 baseline → then J6.

## Major-step baseline discipline

Every major phase follows Idea/Decision/Blueprint/Roadmap/Preflight before implementation, then coherent code batch → CI/package audits → Motorola proof for device claims → canonical evidence sync → immutable milestone snapshot → protected baseline only when promotion requirements are satisfied.

A red/pending head or CI-only device feature is never mislabelled as a protected device-verified baseline.

## Immediate next actions

1. Physically run J4 #142 quality gate and J5 #2384 HOME gate on Motorola.
2. For J5 use only exact artifact `8919388343` and verify APK SHA-256 first.
3. If J5 passes, promote protected J5 exact-green baseline and update all canonical records.
4. Then deepen voice/orb integration into Home and begin J6 typed context cards: reminders → notifications → people → documents/media.
5. Keep PR #12 Draft/open/unmerged until explicit owner approval.