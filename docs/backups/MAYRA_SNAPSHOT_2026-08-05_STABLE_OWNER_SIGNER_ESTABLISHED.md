# Mayra AI — Stable Owner Signer Established

Date: 2026-08-05
Status: **FIRST PERMANENT OWNER-SIGNED APK BUILT AND VERIFIED — DEVICE INSTALL PENDING**

## Exact source / workflow

- source commit: `b72270aa83aecb24f120e619fc50094a77816f45`
- branch: `agent/document-library-foundation`
- workflow: Stable Owner Alpha #16
- run ID: `30987409944`
- engineering backup: `backup/j5-stable-owner-signer-green-2026-08-05`

## Permanent owner package

- build type: `ownerAlpha`
- application ID: `ai.mayra.app.owner`
- visible label: `Mayra AI Owner`
- version: `0.2.1-owner`
- versionCode: `4`

## Signing evidence

GitHub Actions owner-signing Secrets are configured and the workflow successfully materialized the persistent owner keystore without exposing private values.

The build, tests, lint, owner APK assembly, package verification, certificate verification and artifact upload all passed.

- signer certificate DN: `CN=Mayra Owner, OU=Personal, O=Mayra AI, C=IN`
- signer certificate SHA-256: `1617672fc426020921598dc4cc5f361d464ed6e65d9d7e8919b6931964d289dd`
- APK Signature Scheme v2: verified
- APK Signature Scheme v3: verified

## Artifact evidence

- artifact ID: `8922774120`
- artifact name: `mayra-stable-owner-apk-16`
- artifact ZIP digest: `sha256:9aa9ca2b5c3f8b7a6aab9582303003471a0da17775f3707ca2a116e2178ac19d`
- APK SHA-256: `233cb686851abeab1f923bf8be2a39dccf003d5debc3613951d2165db2d7d439`

The artifact contains the APK plus package badging, signing certificate output, signer SHA-256 and APK SHA-256 records.

## Safety / migration state

The previously working `ai.mayra.app.alpha` remains installed and is not replaced. The new `ai.mayra.app.owner` package is intentionally side-by-side so the owner has rollback and access to old package-private data during migration.

No device success is claimed for the new owner package until it is physically installed and tested on the Motorola.

## Next gate

1. install exact `ai.mayra.app.owner` APK side-by-side with `.alpha`;
2. verify Home selection, app list/search/open, lock/unlock, reboot, switch-back, Airplane mode, Mayra orb and Digital Assistant coexistence;
3. then produce a second owner-signed APK using the same signer and prove direct install-over-install without uninstall;
4. only after accepted device evidence may J5 owner line be considered for protected promotion.

Private signing key material/passwords are not stored in repository files or this snapshot.
