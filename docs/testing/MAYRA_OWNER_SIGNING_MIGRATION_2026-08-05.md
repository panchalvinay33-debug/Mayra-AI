# Mayra AI — Owner Signing Migration

Date: 2026-08-05
Status: **STABLE OWNER SIGNER ESTABLISHED — FIRST OWNER APK READY FOR SIDE-BY-SIDE DEVICE VERIFY**

## What happened

The Motorola owner device has `ai.mayra.app.alpha` installed from the earlier J5 Personal Alpha artifact built by ordinary Android CI. A newer Personal Alpha APK could not install over it and Android reported a package conflict.

Root cause: ordinary Android CI can fall back to a runner's temporary debug signing certificate when stable owner signing is unavailable. Separate hosted CI runners therefore do not provide a durable update certificate. Android correctly refuses to replace an installed package with the same application ID when the signing certificate changes.

This is a signing continuity issue, not a launcher/runtime failure.

## Earlier evidence

- Physically proven J5 alpha: Android CI #2384 / artifact `8919388343` / package `ai.mayra.app.alpha`.
- Unified J5 code checkpoint: `cc89a392a53fcb910166c92badaab3543b5520ff`, Android CI #2416 green.
- Stable Owner Alpha run #6 originally proved the signing lane but failed because owner-signing Secrets were not yet configured.

## Permanent rule

A device-facing Mayra update line must use one persistent owner signing key. Ordinary CI Personal Alpha artifacts are engineering/test artifacts and must not be assumed update-compatible across runs when stable owner signing is unavailable.

The owner signing key and passwords must never be committed to GitHub source/history. They belong in the owner's secure backup and GitHub Actions encrypted Secrets only.

Required GitHub Secrets are now configured:

1. `MAYRA_OWNER_KEYSTORE_BASE64`
2. `MAYRA_OWNER_STORE_PASSWORD`
3. `MAYRA_OWNER_KEY_ALIAS`
4. `MAYRA_OWNER_KEY_PASSWORD`

## Permanent side-by-side owner package

A dedicated `ownerAlpha` build type uses application ID **`ai.mayra.app.owner`** and visible label **`Mayra AI Owner`**. The Stable Owner Alpha workflow builds this package with the persistent owner key.

This intentionally avoids trying to replace the transient-signed `.alpha` package. The working `.alpha` Mayra can remain installed while the permanent owner line is installed and tested side-by-side.

The owner variant is preflighted without private signing material by `.github/workflows/owner-alpha-preflight.yml`; the actual installable owner APK is produced only by `.github/workflows/owner-alpha.yml` with encrypted owner Secrets.

## First stable owner build — PASS

Exact source: `b72270aa83aecb24f120e619fc50094a77816f45`

Stable Owner Alpha #16 / run `30987409944` completed successfully:

- owner signing Secrets materialized successfully;
- owner compile + debug unit tests + owner lint passed;
- `assembleOwnerAlpha` passed;
- package verification passed;
- stable certificate verification passed;
- artifact upload passed.

Artifact identity:

- artifact ID: `8922774120`
- artifact name: `mayra-stable-owner-apk-16`
- artifact ZIP digest: `sha256:9aa9ca2b5c3f8b7a6aab9582303003471a0da17775f3707ca2a116e2178ac19d`
- package: `ai.mayra.app.owner`
- label: `Mayra AI Owner`
- version: `0.2.1-owner` / versionCode `4`
- APK SHA-256: `233cb686851abeab1f923bf8be2a39dccf003d5debc3613951d2165db2d7d439`
- signer SHA-256: `1617672fc426020921598dc4cc5f361d464ed6e65d9d7e8919b6931964d289dd`
- signer DN: `CN=Mayra Owner, OU=Personal, O=Mayra AI, C=IN`
- APK Signature Scheme v2: verified
- APK Signature Scheme v3: verified

Engineering backup: `backup/j5-stable-owner-signer-green-2026-08-05`

Immutable milestone snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_STABLE_OWNER_SIGNER_ESTABLISHED.md`

## Migration sequence

1. Keep the currently working `ai.mayra.app.alpha` installed as rollback/reference.
2. Keep the previous Motorola launcher installed and available.
3. Keep the permanent owner signing bundle securely backed up outside GitHub source/history.
4. Install exact stable `ai.mayra.app.owner` APK from Stable Owner Alpha #16 side-by-side; do **not** uninstall `.alpha` first.
5. Select/test `Mayra AI Owner` as Home and, where desired, as Digital Assistant. Re-run launcher, lock/reboot, switch-back, Airplane and unified orb/assistant checks.
6. Keep old `.alpha` until owner-package behavior and any needed owner data transfer/recreation are accepted.
7. Produce a second stable-owner APK with the same package and signer and prove direct install-over-install without uninstall. This is the key signing-continuity proof.
8. Only after the new owner line is accepted may the old transient `.alpha` be removed at the owner's discretion.

After step 7, owner-device updates must come from the stable `ai.mayra.app.owner` signing lane unless an explicit migration is documented.

## Data note

Android isolates app-private data by package. `ai.mayra.app.owner` cannot silently read the private data of `ai.mayra.app.alpha`. Keeping both packages installed preserves access to the old data while export/import or explicit recreation is handled. Do not claim data migration unless physically proven.

## Non-goals

- Do not bypass Android package-signature checks.
- Do not store private keys/passwords in repository files, issues, PR comments, Actions logs or ordinary CI artifacts.
- Do not call a transient debug-signed CI APK an owner update.
- Do not uninstall the physically working `.alpha` before the side-by-side owner package is verified.
