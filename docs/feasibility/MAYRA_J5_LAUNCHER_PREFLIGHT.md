# Mayra AI — J5 AI-Native Launcher Preflight

Date: 2026-08-05
Status: **AUTOMATED PREFLIGHT PASS / DEVICE_VERIFY READY**

## Goal

Prove that Mayra can become the owner's stable default Android Home/launcher while keeping ordinary apps reachable and Home usable when AI components fail.

## Preconditions — satisfied for device testing

- J4 Room/KSP failure repaired and protected at `baseline/mayra-0.2.1-j4-ci-recovery-green-134`;
- J5 exact source `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a` is fully green;
- Android #2384, J1 #493, J2 #389, J3 #211, J4 #162 and Governance #565 all passed;
- exact green engineering backup: `backup/j5-home-contract-ci-green-2026-08-05`.

## Android integration implemented

The candidate uses a separate `MayraLauncherActivity` with the standard `MAIN + HOME + DEFAULT` Home boundary and supported user-consent `ROLE_HOME` request where available.

Launcher status grants no access to private app data and no security bypass is assumed.

## First implementation boundary — current truth

Implemented:
- launcher HOME activity/surface;
- installed launchable-app inventory;
- deterministic case-insensitive app search by label/package;
- direct launchable-activity opening;
- bridge to normal Mayra;
- Android Home settings switch/restore path;
- basic fallback Home UI independent from local model/cloud/memory/action-engine startup.

Still intentionally deferred within J5 or later gates:
- favorites/basic persistent layout;
- deeper Mayra voice/orb entry integration;
- unrestricted Accessibility automation;
- payments/authentication/security automation;
- proactive notification intelligence;
- broad photo/video/contact cards;
- always-on wake word;
- Phone-role takeover;
- multimodal vision;
- autonomous routines.

## Reliability requirements

J5 Home must remain usable with no network, cloud provider disabled, local model absent/corrupt/killed, Notification Access denied, Contacts denied, neural TTS unavailable, and after app-process restart.

## Automated validation — PASS on exact source

Source: `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`

- Android CI #2384: compile, full debug unit tests, all governed lint variants, Personal Alpha, minified Release/R8, FullTest and DocumentTest package/audits — PASS;
- deterministic launcher app-search tests — PASS;
- Personal Alpha and Release manifest audits explicitly require `MayraLauncherActivity`, `android.intent.category.HOME` and `android.intent.category.DEFAULT` — PASS;
- normal application launchable-entry count remains exactly one (`MainActivity`) — PASS;
- no new high-risk permissions were accepted by package audits — PASS;
- J1 #493, J2 #389, J3 #211, J4 #162, Governance #565 — PASS.

## Device validation — next gate

On Motorola Edge 70 Fusion / Android 16 prove:
- Mayra appears in Home/default launcher selection;
- selection succeeds;
- Home gesture/button returns to Mayra repeatedly;
- installed apps are visible/searchable and launch correctly;
- lock/unlock and reboot preserve a usable Home;
- model/provider failures leave Home functional;
- Assistant invocation does not create navigation loops;
- previous launcher can be restored;
- RAM/battery/thermal behavior is acceptable.

## Exact device-test artifact

- source: `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`
- Android CI: #2384
- Personal Alpha artifact ID: `8919388343`
- artifact ZIP digest: `sha256:92a6aa72d54e48c9fbc835e277b5d3471ec4c9112dfa9ab6e65a16ff229a2e17`
- APK SHA-256: `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`

## Rollback

- Previous launcher remains installed/choosable.
- Android Home settings route is exposed from Mayra Home.
- Protected J4 recovery baseline is unchanged.
- J5 exact-green source is preserved in `backup/j5-home-contract-ci-green-2026-08-05`.

## Promotion

J5 is **not a protected baseline yet**. Only after exact-source Motorola acceptance:
1. synchronize canonical evidence;
2. create immutable J5 milestone snapshot;
3. create protected J5 baseline branch;
4. then begin J6 context integration.