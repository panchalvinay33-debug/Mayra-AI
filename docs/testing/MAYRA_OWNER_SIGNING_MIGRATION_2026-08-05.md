# Mayra AI — Owner Signing Migration

Date: 2026-08-05
Status: **ONE-TIME SIGNING MIGRATION REQUIRED**

## What happened

The Motorola owner device has `ai.mayra.app.alpha` installed from the earlier J5 Personal Alpha artifact built by ordinary Android CI. A newer Personal Alpha APK could not install over it and Android reported a package conflict.

Root cause: ordinary Android CI falls back to the runner's temporary debug signing certificate when stable owner signing is unavailable. Separate hosted CI runners therefore do not provide a durable update certificate. Android correctly refuses to replace an installed package with the same application ID when the signing certificate changes.

This is a signing continuity issue, not a launcher/runtime failure.

## Evidence

- Earlier physically proven J5 APK: Android CI #2384 / artifact `8919388343`.
- Unified J5 code checkpoint: `cc89a392a53fcb910166c92badaab3543b5520ff`, Android CI #2416 green.
- Owner-signed workflow exists at `.github/workflows/owner-alpha.yml`.
- Stable Owner Alpha run #6 (`30984237319`) reached the required signing-material step and failed because `MAYRA_OWNER_KEYSTORE_BASE64` was not configured. The other owner signing secrets are therefore treated as unconfigured until the complete set is installed.

## Permanent rule

A device-facing Mayra update line must use one persistent owner signing key. Ordinary CI Personal Alpha artifacts are engineering/test artifacts and must not be assumed update-compatible across runs when `STABLE_OWNER_SIGNING=false`.

The owner signing key and passwords must never be committed to GitHub source/history. They belong in the owner's secure backup and GitHub Actions encrypted Secrets only.

Required GitHub Secrets:

1. `MAYRA_OWNER_KEYSTORE_BASE64`
2. `MAYRA_OWNER_STORE_PASSWORD`
3. `MAYRA_OWNER_KEY_ALIAS`
4. `MAYRA_OWNER_KEY_PASSWORD`

## Migration

Because the currently installed #2384 package was signed by a transient CI debug key whose private key was not preserved, it cannot be upgraded in-place to a new stable signer using ordinary Android package installation.

Safe migration sequence:

1. Preserve any Mayra owner data that must survive uninstall using available Mayra export/record mechanisms or a manual record where export is not yet implemented.
2. Keep another launcher installed and selected/available as recovery.
3. Generate and securely back up one permanent Mayra owner signing key.
4. Configure the four GitHub Actions owner-signing Secrets.
5. Build `personalAlpha` with Stable Owner Alpha and record APK SHA-256 plus signer SHA-256.
6. One time only: uninstall the transient-signed `ai.mayra.app.alpha`, install the stable-owner-signed APK, restore/recreate preserved owner data as applicable, then reselect Mayra as Home.
7. Prove the next stable-owner build installs directly over the first stable-owner build without uninstall. That second-install proof closes the migration.

After step 7, all owner-device APKs must come from the stable owner signing lane unless an explicit new migration is documented.

## Non-goals

- Do not bypass Android package-signature checks.
- Do not store private keys/passwords in repository files, issues, PR comments, Actions logs or ordinary CI artifacts.
- Do not call a transient debug-signed CI APK an owner update.
