# Mayra AI — Motorola Jarvis J1 Acceptance

Status: READY FOR RETEST
Date updated: 2026-08-03
Target device: Motorola owner device / Android 16

## Previously tested artifact

- Label: `Mayra J1 Assistant Test`
- Package: `ai.mayra.app.j1`
- Version: `0.2.1-j1`
- Source: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 CI: #44 — success
- Android CI: #1935 — success
- Project Governance: #116 — success
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`
- Protected baseline: `baseline/mayra-0.2.1-j1-zero-permission-green-44`

## Evidence received from #44

### Installation and launch

Result: PASS

- APK installed without bypassing Play Protect.
- App launched normally.
- J1 activation screen rendered correctly.
- Status displayed: `Mayra is not selected`.

### Assistant activation button

Result: FAIL

- Tapping `Activate Mayra` produced no visible response.
- No role-selection screen appeared.
- No explanatory error appeared.

Root cause:

- Motorola-specific role/settings launch failures were silently swallowed, making the button appear dead.

## Authoritative repaired retest artifact

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

Repair behavior:

- Resolve-check Assistant role request before launching.
- Try official settings screens in order: Assistant role, Voice input, Default apps, general Settings.
- Show visible status for the route opened.
- Show a visible final diagnostic if no official route can open.
- No hidden OEM component, root, accessibility hack or security bypass.

## Retest sequence

### A. Update/install

- [ ] Install #56 over #44 successfully, or uninstall #44 only if Android reports a signing conflict.
- [ ] App opens normally.
- [ ] No runtime permission prompt appears.

### B. Activate Mayra

- [ ] Tap `Activate Mayra`.
- [ ] A system Assistant/default-app/settings screen opens, or an exact visible diagnostic appears in the app.
- [ ] Record the exact message shown inside Mayra.
- [ ] Capture the next system screen.

### C. Assistant role visibility

- [ ] Mayra appears as an available assistant choice.
- [ ] Mayra can be selected only through explicit owner action.
- [ ] `Refresh status` changes to selected after returning.
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

- [x] CI verified zero requested Android permissions for #56.
- [x] Exactly one launcher verified.
- [x] No WorkManager, Startup, Room, notification listener or boot receiver.
- [ ] Motorola shows no unexpected permission request.

## Failure protocol

Every failure must record exact steps, expected/actual behavior, screenshot or recording, repeatability after restart/reboot, artifact source SHA and APK SHA-256.

## Promotion rule

J1 moves to device-verified only after activation, role visibility, select/remove, unlocked invocation, lock-screen behavior and orb lifecycle pass on the repaired provenance-recorded APK. Local LLM, wake phrase and Phone-role implementation remain blocked until that evidence is processed.
