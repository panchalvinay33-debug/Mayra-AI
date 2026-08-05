# Mayra AI — Immutable Snapshot — J5 Home Contract CI Green

Date: 2026-08-05
Milestone type: **engineering checkpoint / device-verify entry point**

## Exact source

`6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`

Backup ref: `backup/j5-home-contract-ci-green-2026-08-05`

This snapshot is immutable evidence of the exact J5 source that passed all automated gates. It is **not** a protected promoted J5 baseline because Motorola physical acceptance remains pending.

## Automated evidence

- Android CI #2384 — SUCCESS
- J1 Assistant Test #493 — SUCCESS
- J2 Voice Test #389 — SUCCESS
- J3 Neural TTS Test #211 — SUCCESS
- J4 Local LLM Test #162 — SUCCESS
- Project Governance #565 — SUCCESS

Android #2384 passed governed compile, complete debug unit tests, all governed lint variants, Personal Alpha package/audit, minified Release/R8 package/audit, FullTest package/audit, DocumentTest package/audit and artifact upload.

The Personal Alpha and Release audits explicitly require:
- `MayraLauncherActivity` in merged manifest;
- `android.intent.category.HOME`;
- `android.intent.category.DEFAULT`;
- exactly one normal application LAUNCHER entry, `MainActivity`;
- existing permission/component safety boundaries.

## J5 implementation captured

- separate resilient Mayra Home activity;
- standard Android Home qualification;
- user-consent `ROLE_HOME` request where supported;
- launchable-app enumeration;
- case-insensitive search by label/package with deterministic tests;
- app launch action;
- bridge to normal Mayra;
- explicit switch/restore Home settings path;
- no heavy local model/cloud/memory/action-engine initialization required for Home rendering.

## Device-test artifact

- Android CI run: #2384 (`30978034598`)
- Personal Alpha artifact ID: `8919388343`
- artifact ZIP digest: `sha256:92a6aa72d54e48c9fbc835e277b5d3471ec4c9112dfa9ab6e65a16ff229a2e17`
- package: `ai.mayra.app.alpha`
- version: `0.2.1-alpha` / versionCode 4
- extracted APK SHA-256: `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`

## Promotion boundary

Do not rename this checkpoint as a protected J5 baseline until the exact artifact/source passes Motorola acceptance: default Home selection, repeated Home return, lock/unlock, reboot, apps/search/launch, switch-back, AI-failure independence and acceptable resource behavior.

If physical testing fails, record the failure and fix from this known checkpoint or fall back to the protected J4 recovery baseline. Do not weaken the checklist.
