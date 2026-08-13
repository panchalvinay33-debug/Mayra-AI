# Mayra J1 Motorola Install Result — 2026-08-03

Device: Motorola owner device
Candidate: Personal Alpha 0.2.1, Android CI #1851
Package: `ai.mayra.app.alpha`
Source: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
APK SHA-256: `1459517f1aa375576afa353ba6683ceaf81ddbcb4e79fc6dd790a501f52307b8`

## Result

Status: **FAIL — package signing conflict**

Android displayed:

> App not installed as package conflicts with an existing package.

## Root cause

A previous `ai.mayra.app.alpha` APK was already installed. Personal Alpha was signed with the GitHub hosted runner's temporary debug certificate. A later workflow run generated a different debug certificate, so Android correctly rejected the APK as an incompatible update.

This is not an application-code crash or package-name collision with another product. It is an upgrade-signature mismatch between two Mayra test artifacts.

## Immediate owner recovery

1. Android Settings → Apps → Mayra AI Personal Alpha.
2. Uninstall the previous test build.
3. Install the new candidate.

Uninstalling removes that test package's local app data. This one-time recovery is acceptable only while the stable owner signing path is being configured.

## Permanent corrective action

- Add a stable secret-backed owner signing configuration for Personal Alpha.
- Add a dedicated GitHub `Stable Owner Alpha` workflow.
- Record the APK signing certificate and SHA-256 with every stable owner artifact.
- Do not describe temporary debug-signed CI artifacts as upgrade-compatible.
- Add a simple first-launch setup that requests only microphone, contacts and notification permissions, then opens the Android Assistant-role selector.

## Retest gate

After a stable owner signing key is configured, perform:

1. clean install of stable owner build A;
2. create representative Mayra local data;
3. install stable owner build B over A without uninstalling;
4. verify install succeeds and local data remains;
5. record both APK certificate digests and application version codes.
