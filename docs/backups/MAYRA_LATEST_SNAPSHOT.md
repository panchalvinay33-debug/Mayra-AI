# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: **J5 launcher device verification + unified Mayra integration verification; J4 quality device gate remains separate**.

## Canonical direction

Mayra targets a practical Jarvis-style personal Android operating layer: AI-native Home, Digital Assistant presence, local-first conversational brain, owner-controlled memory/documents, typed context/actions, proactive assistance, later multimodal support and owner-defined routines.

The launcher is resilient infrastructure, not privileged authority. Home/app access must remain usable when network, provider or AI components are unavailable.

## Protected recovery baseline

`baseline/mayra-0.2.1-j4-ci-recovery-green-134` at `e72488a6f6dceb24950f9b0f574ae223d52bd8bb` remains immutable.

## J4 quality checkpoint

Source `862450933da3700d4d1559e09ebde910a4185914`, backup `backup/j4-quality-harness-ci-green-2026-08-05`.

Android #2364, J1 #473, J2 #369, J3 #191, J4 #142 and Governance #545 passed. Physical quality/RAM/cancel/stress/background/lock/Airplane/thermal evidence is still required before production local-brain promotion.

## J5 Home contract checkpoint — device-proven core

Original exact source: `6d5e773df2ef822b50061ffee2851d8f5d8b3e9a`
Backup: `backup/j5-home-contract-ci-green-2026-08-05`
Original Motorola artifact: Android #2384 / artifact `8919388343` / APK SHA-256 `1ec6be33cb0c484552668145c48690094df3e44a0cb0cef613e28c1f88283096`.

Owner-supplied Motorola Edge 70 Fusion / Android 16 evidence proves on that exact build:
- Android accepted `Mayra AI Personal Alpha` as default Home;
- Mayra Home renders and reports Default Home;
- 81/81 launchable apps were enumerated;
- Chrome search, app launch and Home return work;
- lock/unlock remains usable;
- reboot preserves Mayra Home;
- previous launcher can be selected and Mayra can be selected again without trapping the owner;
- Airplane mode preserves Home/search/app launch/Home return;
- `Ask Mayra` opens normal Mayra;
- normal Mayra remains usable in offline-core/private-on-device mode without a connected general AI provider.

J5 core device proof is strong but promotion is not complete until remaining acceptance items are either proven or explicitly deferred with rationale.

## J5 unified Mayra integration checkpoint — automated PASS, device verify next

Exact source: `cc89a392a53fcb910166c92badaab3543b5520ff`
Backup: `backup/j5-unified-mayra-ci-green-2026-08-05`

Automated evidence:
- Android CI #2416 — SUCCESS
- J1 #525 — SUCCESS
- J2 #421 — SUCCESS
- J3 #243 — SUCCESS
- J4 #194 — SUCCESS
- Governance #597 — SUCCESS

What changed:
- added `MayraEntryContract` as the shared navigation boundary between Home, Android voice-session surface and full Mayra;
- Mayra Home now presents a large central Mayra orb/card instead of feeling like only an app list;
- tapping the Home orb / Open Mayra routes to the same full Mayra activity using clear-top/single-top semantics;
- Home remains lightweight and does not initialize heavy AI, memory or privileged-action runtime;
- after the Android Digital Assistant hears a request while unlocked, its response surface can be tapped to continue in full Mayra;
- the voice overlay still stays bounded and dismissible; locked-device privacy behavior remains unchanged;
- J1/J2 isolation, J3 neural voice, J4 local brain, full Android packaging and manifest/permission gates all stayed green.

Exact new Motorola candidate:
- Android CI #2416 (`30981372713`)
- Personal Alpha artifact `8920663408`
- artifact ZIP digest `sha256:978059bca48282eb0ee86cd0d981a8a74a3a090c12121136423cde5bf3d56f2a`
- extracted APK SHA-256 `fb4963e2678472fe471dd2f911a746e7dc8086743255952980ed4ef3c399ba77`
- package `ai.mayra.app.alpha`
- version `0.2.1-alpha` / versionCode 4

## Immediate J5 verification on new unified build

1. install/update exact #2416 APK;
2. confirm Mayra remains/selects as default Home;
3. verify central Mayra orb/card renders and tapping it opens full Mayra;
4. app search/open/Home return still works;
5. Power-button Digital Assistant still invokes/dismisses normally;
6. after a heard unlocked request, tap the assistant response and verify it opens full Mayra without a loop;
7. verify lock/unlock, reboot, Airplane mode and switch-back remain sane;
8. observe crashes/jank/thermal/battery and record any regression.

## Promotion rule

Do not create a protected J5 baseline until the promoted exact source has both green automated gates and accepted Motorola evidence. Existing older device proof is preserved but does not automatically prove the new unified source.

## Trust boundaries

- free-form LLM output never directly executes privileged actions;
- no direct LLM trusted-memory writes;
- no silent cloud use in local mode;
- no security/Play Protect/signing bypass;
- launcher always preserves basic app access and a route to another Home app;
- no device claim without owner evidence;
- PR #12 remains Draft/open/unmerged until explicit owner approval.

## Immediate next action

Physically verify the unified #2416 Personal Alpha on Motorola, then synchronize J5 acceptance and decide whether the J5 milestone is ready for protected-baseline promotion. Separately complete J4 #142 quality testing.
