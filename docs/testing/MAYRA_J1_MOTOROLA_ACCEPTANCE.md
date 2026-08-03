# Mayra AI — Motorola Jarvis J1 Acceptance

Status: SECOND ACTIVATION REPAIR IN PROGRESS
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
- Artifact ZIP SHA-256: `18dfe69d34cd52f76fe63e26cff011b088a1ff95606e88a7ca577af99aec4300`
- APK size: `19,192,842` bytes
- APK SHA-256: `2def2acd55a0ea751c3cd70c9d78674c275f2c2d8e2e4e03ae527464cf48a318`
- Protected baseline: `baseline/mayra-0.2.1-j1-activation-repair-green-56`

### #56 result

Installation/update and launch: PASS.
Visible diagnostic text: PASS.
Activation navigation: FAIL — tapping `Activate Mayra` still did not leave the J1 screen or open a usable system selection screen.

Owner screenshot evidence was received at approximately 19:46 IST on 2026-08-03. The app still showed `Status: Mayra is not selected` and the visible instruction text, but no system screen opened.

## Root-cause review after #56

Two concrete issues were identified:

1. The shared voice-interaction metadata declared `android:settingsActivity="ai.mayra.app.MainActivity"`, but J1 intentionally removes `MainActivity`. J1 therefore had an invalid settings activity target inside its assistant metadata.
2. The generic Voice Input/role-request route is not the documented Motorola Edge 70 Fusion Android 16 path for changing the assistant. Motorola documents `Settings → Apps → Default apps → Digital assistant`.

Android assistant-role qualification remains valid because J1 exposes a `VoiceInteractionService` gated by `android.permission.BIND_VOICE_INTERACTION`, with session and recognition services.

## Second repair candidate

- J1-specific `mayra_voice_interaction_service.xml` now points `settingsActivity` to `ai.mayra.app.j1.J1AssistantTestActivity`.
- `Activate Mayra` now opens `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS` directly.
- On-screen instructions now show the exact Motorola path: `Settings → Apps → Default apps → Digital assistant → Mayra`.
- If Android does not expose Default apps, the only fallback is general Settings with the same manual path shown.
- No hidden Motorola component, private API, root, Accessibility hack or security bypass is used.

The second repair is not device-ready until J1 CI, Android CI and Project Governance all pass on the synchronized exact head and a new APK provenance is recorded.

## Next retest sequence

### A. Update/install

- [ ] Install the new repaired artifact over #56, or uninstall only if Android reports a signing conflict.
- [ ] App opens normally.
- [ ] No runtime permission prompt appears.

### B. Open Motorola Default apps

- [ ] Tap `Activate Mayra`.
- [ ] Android opens `Settings → Apps → Default apps`.
- [ ] Tap `Digital assistant`.
- [ ] Record whether `Mayra J1 Assistant Test` appears as an available choice.

If the button still does not navigate, manually test the documented Motorola path before declaring candidate visibility failure:

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

### F. Recognition honesty

- [ ] No invented transcript.
- [ ] No endless listening loop.
- [ ] No claim that wake phrase is implemented.

### G. Process/reboot recovery

- [ ] Force-stop does not create a crash loop.
- [ ] Reboot preserves or visibly recovers role state.
- [ ] Removing Mayra restores previous assistant behavior.

### H. Permission boundary

- [x] #44 and #56 CI verified zero requested Android permissions.
- [x] Exactly one launcher verified.
- [x] No WorkManager, Startup, Room, notification listener or boot receiver.
- [ ] Motorola shows no unexpected permission request on the next artifact.

## Failure protocol

Every failure must record exact steps, expected/actual behavior, screenshot or recording, repeatability after restart/reboot, artifact source SHA and APK SHA-256.

## Promotion rule

J1 moves to device-verified only after activation, role visibility, select/remove, unlocked invocation, lock-screen behavior and orb lifecycle pass on one provenance-recorded repaired APK. Local LLM, wake phrase and Phone-role implementation remain blocked until that evidence is processed.
