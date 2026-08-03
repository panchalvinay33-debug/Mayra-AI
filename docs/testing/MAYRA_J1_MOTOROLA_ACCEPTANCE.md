# Mayra AI — Motorola Jarvis J1 Acceptance

Status: J1 #68 CLEAN-INSTALL REQUIRED, STABLE SIGNING FIX IN PROGRESS
Date updated: 2026-08-03
Target device: Motorola owner device / Android 16

## Previously tested artifact #44

- Label: `Mayra J1 Assistant Test`
- Package: `ai.mayra.app.j1`
- Version: `0.2.1-j1`
- Source: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 CI: #44 — success
- Android CI: #1935 — success
- Project Governance: #116 — success
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`
- Protected baseline: `baseline/mayra-0.2.1-j1-zero-permission-green-44`

### #44 result

Installation/launch: PASS.
Activation button: FAIL — no visible response.

## Tested repaired artifact #56

- Label: `Mayra J1 Assistant Test`
- Package: `ai.mayra.app.j1`
- Version: `0.2.1-j1`
- Source: `ce96f8e83fe33b878d426c407715d4a3e1b0495a`
- J1 CI: #56 — success
- Android CI: #1947 — success
- Project Governance: #128 — success
- Artifact: `mayra-j1-zero-permission-apk-56`
- Artifact ID: `8856404389`
- APK size: `19,192,842` bytes
- APK SHA-256: `2def2acd55a0ea751c3cd70c9d78674c275f2c2d8e2e4e03ae527464cf48a318`
- Protected baseline: `baseline/mayra-0.2.1-j1-activation-repair-green-56`

### #56 result

Installation/update and launch: PASS.
Visible diagnostic text: PASS.
Activation navigation: FAIL — tapping `Activate Mayra` still did not leave the J1 screen or open a usable system selection screen.

## J1 #68 Motorola-route artifact

- Package: `ai.mayra.app.j1`
- Source: `8b0e7ee33a34b8784de6b555ff7b273ab11ac525`
- J1 Assistant Test #68: success
- Android CI #1959: success
- Project Governance #140: success
- Artifact: `mayra-j1-zero-permission-apk-68`
- Artifact ID: `8859497655`
- APK size: `19,192,850` bytes
- APK SHA-256: `0e1a36ff6b5e72c7d719430b5e04e87c3f7c3707d341a0527d6e488942d13cb9`

#68 contains the two intended Motorola activation repairs:

1. J1-specific voice-interaction metadata points `settingsActivity` to `ai.mayra.app.j1.J1AssistantTestActivity`, not the removed full-app `MainActivity`.
2. `Activate Mayra` targets Android Default Apps with the documented Motorola manual path `Settings → Apps → Default apps → Digital assistant` shown in-app.

### #68 installation result

Result: BLOCKED — package signing conflict.

Observed on the Motorola device:

`App not installed as package conflicts with an existing package.`

Root cause:

- CI J1 builds were hard-coded to the runner's temporary debug signing certificate.
- The package name remained `ai.mayra.app.j1`, so Android correctly rejected a newer APK signed by a different certificate.
- This is an installation/signing failure, not evidence that #68 activation code failed.

Current safe test action:

- J1 contains no personal memory/documents/contacts/reminder data and requests zero Android runtime permissions.
- Uninstall the currently installed `Mayra J1 Assistant Test` once, then clean-install the verified #68 APK.
- Do not bypass Play Protect or signature checks.

Permanent signing repair:

- Commit `2d1d78f477f9fcd592c67b7a63a3c22358efdf1c` changes `j1AssistantTest` to use the same `mayraOwner` signing configuration as Personal Alpha whenever owner signing secrets are available.
- It falls back to debug signing only when owner signing is unavailable, and exposes `STABLE_OWNER_SIGNING` accordingly.
- Stable owner secrets still need to be configured in GitHub Actions before CI-generated J1 APKs can be claimed as install-over-install stable.
- No keystore/password/private key is committed to the repository.

## Next retest sequence

### A. Clean install #68

- [ ] Uninstall the currently installed J1 test package.
- [ ] Install the verified #68 APK.
- [ ] App opens normally.
- [ ] No runtime permission prompt appears.

### B. Open Motorola Default apps

- [ ] Tap `Activate Mayra`.
- [ ] Android opens `Settings → Apps → Default apps`.
- [ ] Tap `Digital assistant`.
- [ ] Record whether `Mayra J1 Assistant Test` appears as an available choice.

If the button still does not navigate, manually test:

`Settings → Apps → Default apps → Digital assistant`.

### C. Assistant role visibility

- [ ] Mayra appears as an available assistant choice.
- [ ] Mayra can be selected only through explicit owner action.
- [ ] Returning to J1 and tapping `Refresh status` shows selected.
- [ ] Mayra can be deselected and the previous assistant restored.

### D. Unlocked invocation

- [ ] System assistant gesture/button invokes Mayra.
- [ ] Animated orb/session appears.
- [ ] Dismissal stops the animation.
- [ ] Ten repeated invoke/dismiss cycles do not crash or stack surfaces.

### E. Locked-screen invocation

- [ ] Supported Motorola gesture/button invokes Mayra according to lock-screen policy.
- [ ] No private Mayra content is exposed before unlock.
- [ ] Dismissal returns cleanly to lock screen.

## Promotion rule

J1 moves to device-verified only after clean installation, activation, role visibility, select/remove, unlocked invocation, lock-screen behavior and orb lifecycle pass on one provenance-recorded artifact. Local LLM, wake phrase and Phone-role implementation remain blocked until that evidence is processed.
