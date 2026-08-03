# Mayra AI — Motorola Jarvis J1 Acceptance

Status: REPAIR IN PROGRESS
Date updated: 2026-08-03
Target device: Motorola owner device / Android 16

## Authoritative tested artifact

- Label: `Mayra J1 Assistant Test`
- Package: `ai.mayra.app.j1`
- Version: `0.2.1-j1`
- Source: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 CI: #44 — success
- Android CI: #1935 — success
- Project Governance: #116 — success
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`
- Protected baseline: `baseline/mayra-0.2.1-j1-zero-permission-green-44`

## Evidence received

### A. Installation and launch

Result: PASS

- APK installed successfully without bypassing Play Protect.
- App launched normally.
- J1 activation screen rendered correctly.
- Status displayed: `Mayra is not selected`.

### B. Assistant activation button

Result: FAIL

Steps:

1. Open `Mayra J1 Assistant Test`.
2. Tap `Activate Mayra`.

Expected:

- Android Assistant role-selection dialog or an official Assistant/default-app settings screen opens.

Actual:

- No visible response.
- No role-selection screen appeared.
- No explanatory error appeared.

Evidence:

- Owner screenshot received at approximately 18:10 IST on 2026-08-03.

Root-cause finding:

- The J1 activity attempted `RoleManager.createRequestRoleIntent(ROLE_ASSISTANT)` and then silently fell back to Settings intents.
- Intent resolution/launch failures were not shown in the UI, so Motorola-specific failure appeared as a dead button.

Repair candidate:

- Commit `2b06cf8fe92b12c2c9d36d5099d1695ea13a1cf9`.
- Resolve-check the role request before launch.
- Try official settings screens in order: Voice input, Default apps, general Settings.
- Show a visible diagnostic status for every route and final failure.
- No hidden OEM component or security bypass is used.

The repair is not device-ready until J1 CI, Android CI and Project Governance pass on the synchronized exact head and a new APK provenance is recorded.

## Remaining test sequence after repaired APK

### C. Assistant role visibility

- [ ] Tapping Activate Mayra visibly opens a system screen or displays an exact diagnostic.
- [ ] Mayra appears as an available assistant choice.
- [ ] Mayra can be selected only through explicit owner action.
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
- [ ] No claim that the wake phrase is implemented.

### G. Process/reboot recovery

- [ ] Force-stop does not create a crash loop.
- [ ] Reboot preserves or visibly recovers role state.
- [ ] Removing Mayra restores previous assistant behavior.

### H. Permission boundary

- [x] CI verified zero requested Android permissions for J1 #44.
- [x] Exactly one launcher verified.
- [x] No WorkManager, Startup, Room, notification listener or boot receiver.
- [ ] Motorola shows no unexpected permission request.

## Failure protocol

Every failure must record exact steps, expected/actual behavior, screenshot or recording, repeatability after restart/reboot, artifact source SHA and APK SHA-256.

## Promotion rule

J1 moves to device-verified only after activation, role visibility, select/remove, unlocked invocation, lock-screen behavior and orb lifecycle pass on one provenance-recorded repaired APK. Local LLM, wake phrase and Phone-role implementation remain blocked until that evidence is processed.
