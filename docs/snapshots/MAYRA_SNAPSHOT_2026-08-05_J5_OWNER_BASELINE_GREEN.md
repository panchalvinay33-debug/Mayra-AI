# Mayra AI — J5 Owner Baseline Green Snapshot

Date: 2026-08-05
Status: PROTECTED GREEN BASELINE

## Exact source

- commit: `30b652524999c08b42744c4811fb406717ef01da`
- protected branch: `baseline/mayra-0.2.2-j5-owner-green`
- package: `ai.mayra.app.owner`
- version: `0.2.2-owner`
- versionCode: `5`
- stable owner signer SHA-256: `1617672fc426020921598dc4cc5f361d464ed6e65d9d7e8919b6931964d289dd`
- Stable Owner Alpha run: #23 / `31002216206`
- owner artifact: `8928839374`
- APK SHA-256: `e953affdff36af57d712946afbffe9f6ed79ff58d6327590f2daee094d65c18a`

## Automated green evidence

All required automated gates on the exact source are SUCCESS:

- Android CI #2478
- Owner Alpha Preflight #20
- Stable Owner Alpha #23
- J1 Assistant #587
- J2 Voice #483
- J3 Neural TTS #305
- J4 Local LLM #256
- Project Governance #659

## Physical owner evidence

On the Motorola Edge 70 Fusion / Android 16, owner confirmation/screenshots prove the stable owner line can:

- install side-by-side with the transient Personal Alpha;
- be selected as the Android Home app;
- render Mayra Home with the central Mayra orb and installed-app inventory;
- open full Mayra from the orb;
- return from full Mayra to Mayra Home via Home;
- accept a second owner-signed APK (`0.2.2-owner`, versionCode 5) directly over the first owner-signed install without uninstall;
- preserve Mayra Home after the owner-signed update.

Earlier J5 device proof on the same architecture additionally established app search/open/Home-return, lock/unlock, reboot persistence, launcher switch-back, Airplane-mode launcher independence and Ask-Mayra bridge behavior. Those earlier proofs remain recorded separately and are not rewritten as if they were rerun on this exact artifact.

## Meaning of this baseline

This baseline freezes the first permanent-owner-signed, updateable, AI-native Home foundation. It is the recovery point before J6 Context Fabric work begins.

J6 may add context collection, normalization and contextual cards, but it must not make Home/app access depend on cloud availability, the local LLM, notification access, contacts, TTS or privileged actions.

## Non-negotiable rollback rule

Do not move this branch. If J6 or later work regresses launcher safety, signing continuity, Home availability or assistant routing, restore from this exact commit and diagnose forward.
