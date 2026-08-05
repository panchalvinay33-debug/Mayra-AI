# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: **J5 unified Mayra permanent owner line — first stable owner APK built/verified, device install + update-continuity proof pending; J4 quality device gate remains separate**.

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

## Permanent owner signing line — FIRST BUILD PASS

Earlier `.alpha` update conflict was caused by transient hosted-CI debug signing. The permanent owner line avoids that failure mode by using a dedicated package and persistent owner signer.

Owner line:
- build type: `ownerAlpha`
- package: `ai.mayra.app.owner`
- label: `Mayra AI Owner`
- stable signer is held by owner custody + GitHub Actions encrypted Secrets only
- working `ai.mayra.app.alpha` stays installed as rollback/reference during migration

The four GitHub Actions owner-signing Secrets are configured. Stable Owner Alpha #16 / run `30987409944` completed successfully on exact source `b72270aa83aecb24f120e619fc50094a77816f45`.

First permanent owner APK evidence:
- artifact ID: `8922774120`
- artifact name: `mayra-stable-owner-apk-16`
- artifact ZIP digest: `sha256:9aa9ca2b5c3f8b7a6aab9582303003471a0da17775f3707ca2a116e2178ac19d`
- package: `ai.mayra.app.owner`
- version: `0.2.1-owner` / versionCode `4`
- APK SHA-256: `233cb686851abeab1f923bf8be2a39dccf003d5debc3613951d2165db2d7d439`
- signer SHA-256: `1617672fc426020921598dc4cc5f361d464ed6e65d9d7e8919b6931964d289dd`
- signer DN: `CN=Mayra Owner, OU=Personal, O=Mayra AI, C=IN`
- v2 signing: verified
- v3 signing: verified

Engineering backup: `backup/j5-stable-owner-signer-green-2026-08-05`.
Immutable milestone snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_STABLE_OWNER_SIGNER_ESTABLISHED.md`.
Canonical migration record: `docs/testing/MAYRA_OWNER_SIGNING_MIGRATION_2026-08-05.md`.

No device success is claimed yet for `.owner`; the old `.alpha` device proof remains preserved but does not automatically transfer to the new package/build.

## Immediate owner-device gate

1. keep `ai.mayra.app.alpha` installed;
2. install exact `ai.mayra.app.owner` APK side-by-side;
3. verify Mayra Owner can be selected as Home;
4. re-run app inventory/search/open/Home return, lock/unlock, reboot, switch-back, Airplane, unified orb and Digital Assistant coexistence;
5. keep old `.alpha` until owner line is accepted;
6. then build a second owner-signed APK with the same signer and prove install-over-install without uninstall;
7. only after this continuity proof may the owner line be considered for protected J5 promotion.

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

Install exact stable owner APK from Stable Owner Alpha #16 side-by-side with the working `.alpha`, complete the physical owner-package launcher/assistant checks, then produce a second stable-owner build to prove direct update-over-update continuity before resuming J5 protected promotion.
