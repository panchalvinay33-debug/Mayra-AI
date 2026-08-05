# Mayra AI — Owner Signing Migration

Date: 2026-08-05
Status: **SIDE-BY-SIDE OWNER SIGNING MIGRATION IN PROGRESS**

## What happened

The Motorola owner device has `ai.mayra.app.alpha` installed from the earlier J5 Personal Alpha artifact built by ordinary Android CI. A newer Personal Alpha APK could not install over it and Android reported a package conflict.

Root cause: ordinary Android CI falls back to the runner's temporary debug signing certificate when stable owner signing is unavailable. Separate hosted CI runners therefore do not provide a durable update certificate. Android correctly refuses to replace an installed package with the same application ID when the signing certificate changes.

This is a signing continuity issue, not a launcher/runtime failure.

## Evidence

- Earlier physically proven J5 APK: Android CI #2384 / artifact `8919388343` / package `ai.mayra.app.alpha`.
- Unified J5 code checkpoint: `cc89a392a53fcb910166c92badaab3543b5520ff`, Android CI #2416 green.
- Owner-signed workflow exists at `.github/workflows/owner-alpha.yml`.
- Stable Owner Alpha run #6 (`30984237319`) reached the required signing-material step and failed because `MAYRA_OWNER_KEYSTORE_BASE64` was not configured. The complete owner signing secret set is therefore considered unconfigured.

## Permanent rule

A device-facing Mayra update line must use one persistent owner signing key. Ordinary CI Personal Alpha artifacts are engineering/test artifacts and must not be assumed update-compatible across runs when `STABLE_OWNER_SIGNING=false`.

The owner signing key and passwords must never be committed to GitHub source/history. They belong in the owner's secure backup and GitHub Actions encrypted Secrets only.

Required GitHub Secrets:

1. `MAYRA_OWNER_KEYSTORE_BASE64`
2. `MAYRA_OWNER_STORE_PASSWORD`
3. `MAYRA_OWNER_KEY_ALIAS`
4. `MAYRA_OWNER_KEY_PASSWORD`

## Safer permanent owner package

A dedicated `ownerAlpha` build type now uses application ID **`ai.mayra.app.owner`** and visible label **`Mayra AI Owner`**. The Stable Owner Alpha workflow builds this package with the persistent owner key.

This intentionally avoids trying to replace the transient-signed `.alpha` package. The working `.alpha` Mayra can remain installed while the new permanent owner line is installed and tested side-by-side.

The owner variant is preflighted without private signing material by `.github/workflows/owner-alpha-preflight.yml`; the actual installable owner APK is produced only by the dedicated stable-signing lane after all four encrypted Secrets are configured.

## Migration sequence

1. Keep the currently working `ai.mayra.app.alpha` installed as rollback/reference.
2. Keep the previous Motorola launcher installed and available.
3. Securely back up the permanent Mayra owner signing key outside GitHub source/history.
4. Configure the four GitHub Actions owner-signing Secrets.
5. Build `ai.mayra.app.owner` with Stable Owner Alpha and record APK SHA-256 plus signer SHA-256.
6. Install `Mayra AI Owner` side-by-side with the existing `.alpha` package; do **not** uninstall the working alpha first.
7. Select/test `Mayra AI Owner` as Home and, where desired, as Digital Assistant. Re-run launcher, lock/reboot, switch-back, Airplane and unified orb/assistant checks.
8. Keep old `.alpha` until owner-package behavior and any needed owner data transfer/recreation are accepted.
9. Produce a second stable-owner APK with the same `ai.mayra.app.owner` package and prove direct install-over-install without uninstall. This is the key signing-continuity proof.
10. Only after the new owner line is accepted may the old transient `.alpha` be removed at the owner's discretion.

After step 9, owner-device updates must come from the stable `ai.mayra.app.owner` signing lane unless an explicit migration is documented.

## Data note

Android isolates app-private data by package. `ai.mayra.app.owner` cannot silently read the private data of `ai.mayra.app.alpha`. Keeping both packages installed preserves access to the old data while export/import or explicit recreation is handled. Do not claim data migration unless physically proven.

## Non-goals

- Do not bypass Android package-signature checks.
- Do not store private keys/passwords in repository files, issues, PR comments, Actions logs or ordinary CI artifacts.
- Do not call a transient debug-signed CI APK an owner update.
- Do not uninstall the physically working `.alpha` before the side-by-side owner package is verified.
