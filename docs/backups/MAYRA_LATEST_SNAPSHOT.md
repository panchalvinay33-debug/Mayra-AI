# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: **J4 quality device verification + J5 launcher/Home DEVICE_VERIFY**.

## Canonical direction

Mayra targets a practical **Jarvis-style personal Android operating layer**: Digital Assistant voice presence, local-first conversational brain, owner-controlled memory/documents, typed context, AI-native Home, trust-gated actions, proactive assistance, later multimodal support and owner-defined routines.

The launcher is a resilient Home shell, not privileged authority. Basic Home/app access must survive model/provider/brain failure.

## Protected recovery baseline

`baseline/mayra-0.2.1-j4-ci-recovery-green-134`

Exact source: `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`

This remains the immutable protected rollback point while J4 quality and J5 device validation advance.

## J4 quality engineering checkpoint

Source: `862450933da3700d4d1559e09ebde910a4185914`
Backup: `backup/j4-quality-harness-ci-green-2026-08-05`

Automated evidence: Android #2364, J1 #473, J2 #369, J3 #191, J4 #142, Governance #545 — SUCCESS.

Adds useful multilingual prompts, safety/uncertainty prompts, 10-prompt stress, response metrics, localbrain RAM telemetry and process-bounded cancellation/rebind. Physical Motorola quality/RAM/thermal/background/lock/Airplane evidence remains pending; local model is not yet production-brain promoted.

## J5 Home contract checkpoint — automated PASS

Exact source: `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`
Engineering backup: `backup/j5-home-contract-ci-green-2026-08-05`

Automated evidence:
- Android CI #2384 — SUCCESS
- J1 #493 — SUCCESS
- J2 #389 — SUCCESS
- J3 #211 — SUCCESS
- J4 #162 — SUCCESS
- Governance #565 — SUCCESS

Implemented/proven in source and package audits:
- separate `MayraLauncherActivity`;
- `MAIN + HOME + DEFAULT` Home qualification;
- user-consent `ROLE_HOME` request;
- launchable-app enumeration;
- deterministic app search by label/package;
- direct app launch;
- `Ask Mayra` bridge;
- explicit Home-settings switch/restore path;
- Home render path does not initialize local model/cloud/memory/privileged action engine;
- Personal Alpha and minified Release audits explicitly require Home activity/categories;
- normal application icon/LAUNCHER entry remains exactly one (`MainActivity`);
- shared J1/J2/J3/J4 and isolated FullTest/DocumentTest boundaries remain green.

### Exact Motorola J5 test artifact

- Android CI: #2384 (`30978034598`)
- Personal Alpha artifact: `8919388343`
- artifact ZIP digest: `sha256:92a6aa72d54e48c9fbc835e277b5d3471ec4c9112dfa9ab6e65a16ff229a2e17`
- extracted APK SHA-256: `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`
- package: `ai.mayra.app.alpha`
- version: `0.2.1-alpha` / versionCode 4

J5 is **DEVICE_VERIFY**, not a protected promoted baseline. Physical Motorola HOME selection/reboot/switch-back/AI-failure evidence is pending.

## Immediate physical gates

### J4 quality
1. useful Hindi/Hinglish/English output;
2. RAM metrics;
3. 10-prompt stress;
4. cancel/rebind/reinitialize;
5. five close/reload cycles;
6. background/lock/process recovery;
7. Airplane mode;
8. battery/thermal observations.

### J5 Home
1. verify exact APK hash/install;
2. Mayra appears/selects as default Home;
3. Home gesture/button returns 20/20;
4. apps/search/open work;
5. lock/unlock and reboot preserve usable Home;
6. switch previous launcher and switch back;
7. model/provider/network/voice/permission failures do not break basic Home;
8. record RAM/battery/thermal/jank.

## Promotion rule

If J5 physical acceptance passes on exact source, synchronize evidence, create immutable J5 milestone snapshot and create protected exact-green J5 baseline. Do not begin J6 context integration before that promotion.

## Trust boundaries

- no free-form LLM privileged execution;
- no direct LLM trusted-memory writes;
- structured provenance for context;
- typed/action-bound confirmations;
- no silent cloud use in local mode;
- no security/Play Protect/signing bypass;
- default Home remains restorable;
- no device claim without Motorola evidence;
- PR #12 stays Draft/open/unmerged until explicit owner approval.

## Immediate next action

Run the exact J5 #2384 Personal Alpha on Motorola and complete the J5 acceptance checklist while separately finishing J4 #142 physical quality testing.