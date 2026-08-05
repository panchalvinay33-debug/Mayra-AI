# Mayra AI — Immutable Snapshot: Owner Signing Migration

Date: 2026-08-05
Type: **major-step planning/recovery snapshot**

## Trigger

Motorola rejected unified J5 Personal Alpha install with `App not installed as package conflicts with an existing package.`

The installed and new APKs both used `ai.mayra.app.alpha`, but ordinary hosted Android CI had signed them with different transient debug certificates. Android correctly blocked replacement.

## Preserved proven state

The earlier J5 #2384 `.alpha` remains physically proven on Motorola for default Home, app inventory/search/open/Home return, lock/unlock, reboot, switch-back, Airplane-mode core independence and `Ask Mayra` offline-core behavior.

Unified J5 source `cc89a392a53fcb910166c92badaab3543b5520ff` remains automated-green at Android #2416, J1 #525, J2 #421, J3 #243, J4 #194 and Governance #597.

## Signing discovery

Stable Owner Alpha workflow exists but owner signing Secrets were not configured. Stable Owner Alpha run #6 (`30984237319`) failed at the signing-material gate with missing `MAYRA_OWNER_KEYSTORE_BASE64`.

No private signing material is stored in GitHub source/history.

## Architecture decision

Use a permanent side-by-side owner package rather than forcing replacement of transient `.alpha`:

- build type: `ownerAlpha`
- package: `ai.mayra.app.owner`
- visible label: `Mayra AI Owner`
- signing: persistent owner key only through dedicated Stable Owner Alpha workflow
- preflight: separate Owner Alpha compile/lint workflow without private signing material

The working `.alpha` stays installed until `.owner` is physically accepted. This avoids uninstall-first launcher risk and preserves access to old package-private data during migration.

## Required encrypted GitHub Secrets

Names only; values are deliberately absent from this snapshot:

- `MAYRA_OWNER_KEYSTORE_BASE64`
- `MAYRA_OWNER_STORE_PASSWORD`
- `MAYRA_OWNER_KEY_ALIAS`
- `MAYRA_OWNER_KEY_PASSWORD`

## Promotion gate

1. Owner Alpha preflight green.
2. Shared Android/J1/J2/J3/J4/Governance regressions green.
3. Configure four encrypted owner-signing Secrets from private owner custody.
4. Produce first stable `ai.mayra.app.owner` APK and record APK + signer SHA-256.
5. Install side-by-side and prove Home/Assistant/orb/reboot/switch-back/Airplane behavior.
6. Produce second stable-owner APK and prove direct install-over-install without uninstall.
7. Only then treat `.owner` as permanent owner update lane and consider J5 protected-baseline promotion.

## Rollback

- Keep physically proven `.alpha` installed during owner-package migration.
- Keep Motorola's previous launcher installed and selectable.
- Protected J4 recovery baseline remains unchanged.
- PR #12 remains Draft/open/unmerged.
