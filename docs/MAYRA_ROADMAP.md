# Mayra AI — Execution Roadmap

Last updated: 2026-08-05
Entry point: `START_HERE.md`
Jarvis/launcher master plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Local LLM benchmark: `docs/testing/MAYRA_LOCAL_LLM_BENCHMARK.md`

## Product direction locked on 2026-08-05

Mayra is targeting a practical **Jarvis-style personal Android operating layer**. The final experience combines the proven Digital Assistant/voice foundation with an AI-native launcher/Home shell, local-first brain, provenance-aware context, trust-gated typed actions, proactive assistance, later multimodal understanding and owner-defined routines.

The launcher is the primary Home shell, not the privileged brain. Heavy AI/model/provider failure must never make basic Home/app access unusable.

## Overall program view

| Track | Status | Current truth | Next gate |
|---|---|---|---|
| Governance/backups | DONE / CONTINUOUS | Canonical records, immutable snapshots and protected recovery refs exist | Sync every meaningful batch |
| J1 Assistant role | DEVICE VERIFIED FOUNDATION | Motorola accepts/selects Mayra; Power invocation works | Preserve regression |
| J2 recognition/privacy | DEVICE VERIFIED FOUNDATION | Hindi/Hinglish/English recognition, dismissal, lock/privacy cycles proven | Preserve regression |
| Android system TTS | FALLBACK PASS | Offline fallback works; quality is robotic | Keep safe fallback |
| J3 neural TTS | DEVICE BENCHMARK PASS / LICENSE BLOCKED | Offline neural synthesis proven; tested Priyamvada pack not production-cleared | Find license-clear voice |
| J4 local LLM runtime | DEVICE RUNTIME PROVEN | Gemma3-1B import/SHA, CPU Stage 5/5, fixed multilingual generation and close/reload physically proven | Quality/operability device round |
| J4 CI recovery | DONE | Room/KSP shared-schema race repaired; exact source `e72488a…` green across Android/J1/J2/J3/J4/Governance | Preserve protected recovery ref |
| J4 quality harness | CI GREEN / DEVICE VERIFY | Source `8624509…` adds useful prompts, RAM telemetry, rough generation metrics, 10-prompt stress and process-bounded cancel; all major CI green | Run Motorola quality/cancel/RAM/stability round |
| Self-learning | POLICY FOUNDATION | Deterministic candidate policy + owner confirmation boundaries exist | Auditable learned-memory store/review UI later |
| J5 launcher shell | ACCEPTED / NEXT AFTER J4 DEVICE ROUND | Architecture/preflight/acceptance contract exist; no production launcher code yet | HOME role + app inventory/search + switch-back proof |
| J6 context fabric | ACCEPTED | Existing reminders/memory/documents/notification foundations can feed typed context | Start after J5 Home reliability |
| J7 trust/action orchestration | ACCEPTED | Typed actions/confirmations are foundation | Formal GREEN/AMBER/RED policy |
| J8 proactive Mayra | ACCEPTED | Briefing/notification foundations exist | Privacy/battery/context-quality gates |
| J9 multimodal | ACCEPTED / LATER | Document/image architecture can expand | Local/device privacy/RAM/thermal benchmark |
| J10 routines | ACCEPTED / LATER | Typed workflow philosophy established | Owner-defined routines after trust layer |

## Protected recovery points

### J4 CI recovery baseline

`baseline/mayra-0.2.1-j4-ci-recovery-green-134`

- source `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`;
- Android CI #2356 SUCCESS;
- J1 #465 SUCCESS;
- J2 #361 SUCCESS;
- J3 #183 SUCCESS;
- J4 #134 SUCCESS;
- Governance #537 SUCCESS.

This is the rollback point for current J4 quality work and future launcher work until a later milestone is formally promoted.

### J4 quality engineering checkpoint

Source `862450933da3700d4d1559e09ebde910a4185914` is backed by `backup/j4-quality-harness-ci-green-2026-08-05`.

- Android CI #2364 SUCCESS;
- J1 #473 SUCCESS;
- J2 #369 SUCCESS;
- J3 #191 SUCCESS;
- J4 #142 SUCCESS;
- Governance #545 SUCCESS.

This is **not yet a production-quality local-brain baseline** because the new quality/cancel/RAM behaviors still need Motorola physical evidence.

## J4 quality/operability implementation now in source

The J4 engineering APK now provides:

- useful Hindi, Hinglish and English prompts instead of only trivial arithmetic/location prompts;
- safety-boundary and uncertainty prompts;
- response character count;
- explicitly labeled rough token estimate (`chars / 4`) and rough tokens/sec rather than pretending tokenizer-exact SDK telemetry exists;
- total generation time;
- local-brain PSS, Java heap and native-heap snapshots;
- explicit runtime-metrics capture;
- 10 sequential prompt stress benchmark;
- generation run count;
- explicit Cancel Generation / Benchmark control;
- cancellation implemented as bounded isolated `:localbrain` process termination so the owner UI survives even if native synchronous generation cannot cooperate;
- fresh localbrain process rebind after cancellation/close.

## J4 Motorola quality acceptance still required

1. install exact J4 #142 engineering APK;
2. re-use/verify the pinned Gemma3-1B model;
3. capture RAM before load and after Stage 5/5;
4. run Hindi/Hinglish/English quality prompts and record actual outputs/timings;
5. run safety and uncertainty prompts;
6. run the 10-prompt benchmark and record summary;
7. start a sufficiently long generation/benchmark and press Cancel; prove UI survives and fresh runtime rebinds;
8. re-initialize and prove generation works after cancel;
9. perform five close/reload cycles;
10. exercise background, screen-lock and localbrain process-kill recovery;
11. repeat in Airplane mode;
12. record owner-observed battery/thermal behavior.

No first-token-latency claim is made because the current reflection probe uses synchronous `sendMessage()` and does not expose proven streaming callback timing.

## J5 — AI-native launcher next phase

J5 first slice remains deliberately narrow:

- standard Android HOME qualification (`ACTION_MAIN` + `CATEGORY_HOME` + `CATEGORY_DEFAULT`);
- owner-controlled default Home selection;
- launchable-app inventory via supported launcher/package APIs;
- app drawer/search;
- safe app launching;
- basic favorites/layout persistence;
- Mayra orb/voice entry;
- switch-back/settings route;
- deterministic Home UI when local model/provider is absent, corrupt or killed.

The launcher does not gain private app data merely by being default Home. Notifications, contacts, Accessibility, screen context and other capabilities remain separate permission/trust gates.

## Major-step baseline discipline

Every major phase follows:

1. Idea Ledger/decision/Blueprint/Roadmap/preflight before risky implementation;
2. coherent implementation batch;
3. applicable compile/unit/lint/package/permission/component checks;
4. Motorola evidence for device claims;
5. Changelog + Latest Snapshot + test evidence synchronization;
6. immutable milestone snapshot;
7. protected `baseline/*` only from exact green + accepted milestone evidence;
8. next risky phase starts from a known rollback point.

A red or pending head is never a stable baseline.

## Immediate next actions

1. Run J4 #142 quality/operability round on Motorola and record outputs/RAM/cancel/stress/stability evidence.
2. If the quality model is acceptable enough for a first local-brain role, record the promotion decision; if Hinglish remains weak, keep local runtime proven but model candidate unpromoted.
3. Preserve `baseline/mayra-0.2.1-j4-ci-recovery-green-134` regardless.
4. Then begin J5 launcher implementation from supported Android HOME/LauncherApps APIs.
5. Prove default-HOME selection, Home-button return, app drawer/search/launch, reboot persistence, switch-back and AI-process-failure survival on Motorola.
6. Only after J5 reliability move into J6 context, J7 trust/action orchestration, J8 proactive behavior, J9 multimodal and J10 routines.
7. Keep PR #12 Draft/open/unmerged until explicit owner approval.
