# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: **J5 unified Mayra owner-signing migration + device verification; J4 quality device gate remains separate**.

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

## J5 unified Mayra integration checkpoint — automated PASS

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
- shared `MayraEntryContract` between Home, Android voice-session surface and full Mayra;
- large central Mayra orb/card on Home;
- orb/Open-Mayra routes to full Mayra with clear-top/single-top semantics;
- Home remains lightweight and independent of heavy AI/memory/privileged-action startup;
- unlocked voice-session response can continue into full Mayra;
- lock-screen privacy and bounded assistant dismissal remain intact.

## Owner signing migration — current blocker

The #2416 Personal Alpha could not install over the physically proven #2384 Personal Alpha. Android reported a package conflict because the application ID is the same (`ai.mayra.app.alpha`) but the signing certificate changed.

Root cause: ordinary hosted Android CI falls back to a transient runner debug key when stable owner signing is unavailable. The old runner's private debug key was not preserved, so an in-place signer migration is not possible through normal package update semantics.

Dedicated workflow: `.github/workflows/owner-alpha.yml`.

Stable Owner Alpha run #6 (`30984237319`) was triggered successfully but failed at `Require and materialize stable owner signing` because GitHub Actions secret `MAYRA_OWNER_KEYSTORE_BASE64` is absent. Therefore stable owner signing is not yet configured.

Canonical migration record: `docs/testing/MAYRA_OWNER_SIGNING_MIGRATION_2026-08-05.md`.

A permanent Mayra owner key has been generated outside repository source/history for owner custody. The key/passwords must remain in secure owner backup and GitHub Actions encrypted Secrets only; they must never be committed to GitHub files/issues/PR comments/logs.

Required GitHub Secrets:
- `MAYRA_OWNER_KEYSTORE_BASE64`
- `MAYRA_OWNER_STORE_PASSWORD`
- `MAYRA_OWNER_KEY_ALIAS`
- `MAYRA_OWNER_KEY_PASSWORD`

## Controlled one-time migration

1. preserve Mayra owner data that must survive uninstall;
2. keep another launcher available as rollback;
3. securely back up the permanent owner signing key;
4. configure all four GitHub Actions Secrets;
5. build first stable-owner Personal Alpha and record APK + signer SHA-256;
6. one time only, uninstall transient-signed `ai.mayra.app.alpha` and install stable-owner build;
7. restore/recreate preserved owner data as applicable and reselect Mayra as Home;
8. produce a second stable-owner build and prove direct install-over-install without uninstall;
9. only stable-owner APKs become owner-device update candidates afterward.

## Promotion rule

Do not create a protected J5 baseline until the promoted exact source has green automated gates, stable owner-signing continuity and accepted Motorola evidence. Existing older device proof remains preserved but does not automatically prove the new unified source.

## Trust boundaries

- free-form LLM output never directly executes privileged actions;
- no direct LLM trusted-memory writes;
- no silent cloud use in local mode;
- no security/Play Protect/signing bypass;
- launcher always preserves basic app access and a route to another Home app;
- private signing keys/passwords never enter repository source/history;
- no device claim without owner evidence;
- PR #12 remains Draft/open/unmerged until explicit owner approval.

## Immediate next action

Configure the four stable owner-signing GitHub Secrets from the private owner bundle, run Stable Owner Alpha to produce the first permanent-signer unified J5 APK, perform the controlled one-time migration, then immediately prove the second stable-owner build updates in place before resuming J5 physical promotion.
