# Mayra AI — Motorola Jarvis J1 Acceptance

Status: ASSISTANT ROLE + UNLOCKED INVOCATION PASS — DIRECT TOUCH/LOCK-SCREEN/REBOOT COMPLETION PENDING
Date updated: 2026-08-03
Target device: Motorola owner device / Android 16

## Historical artifacts

### J1 #44

- Package: `ai.mayra.app.j1`
- Source: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 #44 / Android CI #1935 / Governance #116: success
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`
- Protected baseline: `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- Device: install/launch PASS; first activation route FAIL.

### J1 #56

- Source: `ce96f8e83fe33b878d426c407715d4a3e1b0495a`
- J1 #56 / Android CI #1947 / Governance #128: success
- APK SHA-256: `2def2acd55a0ea751c3cd70c9d78674c275f2c2d8e2e4e03ae527464cf48a318`
- Protected baseline: `baseline/mayra-0.2.1-j1-activation-repair-green-56`
- Device: visible diagnostics PASS; usable activation route still FAIL.

## Authoritative Motorola-route artifact — J1 #68

- Label: `Mayra J1 Assistant Test`
- Package: `ai.mayra.app.j1`
- Version: `0.2.1-j1`
- Source: `8b0e7ee33a34b8784de6b555ff7b273ab11ac525`
- J1 Assistant Test #68: success
- Android CI #1959: success
- Project Governance #140: success
- Artifact: `mayra-j1-zero-permission-apk-68`
- Artifact ID: `8859497655`
- APK size: `19,192,850` bytes
- APK SHA-256: `0e1a36ff6b5e72c7d719430b5e04e87c3f7c3707d341a0527d6e488942d13cb9`

## Device evidence on J1 #68

### A. Installation and package boundary

Result: PASS.

- Clean install succeeded without bypassing Play Protect after an older differently signed J1 package was removed.
- J1 requested zero runtime permissions.
- App launched normally.

### B. Motorola Default Apps route

Result: PASS.

- `Settings → Apps → Default apps` opened.
- `Digital assistant app` was visible.
- J1-specific assistant metadata correctly points to `J1AssistantTestActivity`.

### C. Assistant candidate and selection

Result: PASS.

- `Mayra J1 Assistant Test` appeared as a valid Digital assistant choice.
- Owner selected Mayra.
- Motorola Default Apps displayed `Digital assistant app — Mayra J1 Assistant Test`.
- J1 displayed `Status: Mayra is selected ✓`.

This proves Android 16 on the target Motorola accepts Mayra’s `VoiceInteractionService` as the default digital assistant.

### D. Invocation trigger

Result: PASS after device configuration.

- Initial Power hold showed Motorola Power menu because the physical key was still mapped to Power menu.
- Owner configured the Motorola Power-button action for Digital assistant under Gestures.
- Power-button invocation then launched Mayra.

### E. Unlocked assistant session/orb

Result: PASS.

- Mayra `VoiceInteractionSession` opened over the current screen.
- Blue/purple animated orb and `Mayra` label rendered.
- Full J1 activity was not required for the visible assistant surface.

### F. Dismissal behavior on tested #68

Back gesture: PASS.

- Owner confirmed Back dismisses the assistant session.

Phone lock while orb is visible: PASS.

- Owner confirmed locking the phone dismisses the current assistant session.

Orb tap: FAIL on #68.
Outside/root tap: FAIL on #68.

Root cause:

- #68 source had no click listeners on the assistant surface/orb/label.

Repair chain:

- `af593761ef68959c230b376e31e654f01e0ab9f5` adds orb/root/label tap-to-hide and explicit Back-to-hide.
- later common-session work also stops recognition/animation on hide/destroy and restarts animation cleanly on a new show.

The repaired touch path has not yet been physically retested. Because the repair is shared by J1/J2, the next owner artifact may validate this lifecycle through J2 rather than forcing another J1 signing/install cycle.

## Stable-signing truth

- J1/J2 use `mayraOwner` signing when protected owner signing secrets are available.
- Hosted-runner debug fallback remains non-update-stable until those secrets are configured.
- No private key/password is committed.

## Remaining J1/J2 Motorola acceptance sequence

### G. Direct dismissal/repeat lifecycle — CURRENT SHARED GATE

- [ ] Invoke repaired Mayra session.
- [ ] Tap orb → session closes.
- [ ] Invoke again; tap outside/root → session closes.
- [x] Back closes the tested session.
- [x] Locking the phone closes the tested unlocked session.
- [ ] Repeat invoke/dismiss 10–20 times without crash, duplicate surface, dead animation or System UI restart.

### H. Invocation beginning while already locked

- [ ] Lock phone first.
- [ ] Invoke the configured Digital assistant trigger.
- [ ] Record whether Mayra appears, requests unlock or is blocked by Android/Motorola policy.
- [ ] No private content is exposed before unlock.
- [ ] Dismissal returns cleanly to lock screen.

### I. Role/process recovery

- [ ] Reboot phone.
- [ ] Verify Digital assistant selection persists or recovers visibly.
- [ ] Invoke Mayra after reboot.
- [ ] Deselect Mayra and restore the previous assistant without errors.

## Promotion rule

J1 Assistant-role proof can be considered physically complete only after direct touch/repeated lifecycle, already-locked invocation behavior and reboot/role recovery are recorded. J2 may provide the repaired lifecycle evidence because it uses the same common session implementation, but its microphone/STT evidence remains a separate J2 gate.
