# Mayra AI — Motorola Jarvis J1 Acceptance

Status: ASSISTANT ROLE SELECTED — POWER KEY STILL MAPPED TO POWER MENU
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

#68 contains the Motorola activation repairs:

1. J1-specific voice-interaction metadata points `settingsActivity` to `ai.mayra.app.j1.J1AssistantTestActivity`.
2. `Activate Mayra` targets Android Default Apps with the Motorola path `Settings → Apps → Default apps → Digital assistant` shown in-app.

### #68 installation history

Initial update attempt: BLOCKED by package signing conflict because CI debug certificates changed between runs.

Safe recovery used: uninstall the J1 test package, then clean-install #68. J1 contains no personal Mayra memory/documents/reminders and requests zero Android runtime permissions.

### #68 Motorola device evidence — 20:44 to 20:52 IST

Default Apps navigation: PASS.

- `Settings → Apps → Default apps` opened successfully.
- `Digital assistant app` was visible in Motorola settings.

Assistant candidate/selection: PASS.

- `Mayra J1 Assistant Test` appears as the selected `Digital assistant app` in Motorola Default Apps.
- J1 itself shows `Status: Mayra is selected ✓`.
- This proves Android 16 on the target Motorola recognizes the J1 `VoiceInteractionService` as a valid assistant candidate and accepts Mayra as the default digital assistant.

Power-key invocation attempt: BLOCKED BY DEVICE TRIGGER CONFIGURATION.

- Owner pressed and held the physical Power key after Mayra was selected.
- The phone displayed the standard Motorola Power menu with `Emergency`, `Lockdown`, `Power off`, and `Restart`.
- This proves the Power key is still mapped to `Power menu`, not `Digital assistant`; it is not evidence that the Mayra `VoiceInteractionSession` failed.
- Motorola's documented Android 16 path is `Settings → Gestures → Power key → Press & hold → Digital assistant`.
- Gesture-navigation alternative: swipe diagonally inward from either lower corner to invoke the selected digital assistant.

Permanent signing repair:

- Commit `2d1d78f477f9fcd592c67b7a63a3c22358efdf1c` changes `j1AssistantTest` to use the `mayraOwner` signing configuration whenever owner signing secrets are available.
- Stable owner secrets still need to be configured in GitHub Actions before future CI J1 APKs are install-over-install stable.
- No keystore/password/private key is committed to the repository.

## Next device test sequence

### D. Configure Motorola invocation trigger — CURRENT GATE

- [ ] Open `Settings → Gestures → Power key → Press & hold`.
- [ ] Select `Digital assistant` instead of `Power menu`.
- [ ] Optionally shorten the press-and-hold duration for easier testing.
- [ ] Return to the home screen.
- [ ] Press and hold the Power key.
- [ ] Record exactly what appears.

Alternative invocation:

- [ ] With gesture navigation enabled, swipe diagonally inward from either lower screen corner.
- [ ] Record whether Mayra session/orb appears.

### E. Unlocked invocation and orb

- [ ] Mayra assistant session opens without launching the full test activity.
- [ ] Animated Mayra orb/session appears.
- [ ] Dismissal stops the animation.
- [ ] Repeat invoke/dismiss 10 times without crash, duplicate surface or System UI restart.

### F. Locked-screen invocation

- [ ] Lock the phone normally.
- [ ] Press and hold Power with `Digital assistant` still configured.
- [ ] Mayra follows Android lock-screen policy.
- [ ] No private Mayra content is exposed before unlock.
- [ ] Dismissal returns cleanly to lock screen.

### G. Role state/recovery

- [ ] Open J1 and tap `Refresh status`; it shows Mayra selected.
- [ ] Reboot and verify assistant selection remains/recoverably returns.
- [ ] Deselect Mayra and restore the previous assistant without errors.

## Promotion rule

J1 moves to device-verified only after role selection, configured invocation trigger, unlocked invocation, orb/session lifecycle, locked-screen behavior and recovery pass on the target Motorola. Local LLM, wake phrase and Phone-role implementation remain blocked until that evidence is processed.
