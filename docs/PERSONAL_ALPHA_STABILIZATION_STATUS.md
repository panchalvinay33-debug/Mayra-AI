# Mayra AI Personal Alpha V0.1 — Stabilization Status

**Track:** `stabilize/living-companion-v0.1`  
**Control issue:** #10  
**Draft pull request:** #11  
**Target:** first stable owner-only Android build that feels alive, remains safe and can later extend to other device bodies.

## Current engineering state

### Preserved

- Main integration work remains in PR #9.
- Permanent recovery branch: `backup/pr9-living-companion-2026-07-25`.
- Safety recovery branch: `backup/personal-alpha-safety-2026-07-25`.
- Stabilization work is isolated from new product expansion.
- Master Blueprint V2 remains the product source of truth.
- The Living Intelligence extension locks the one-brain/many-bodies future architecture.

### Build controls added

- JDK 17, Android SDK 35 and Gradle 8.9 are locked for the personal alpha.
- The Windows build uses one Gradle worker and bounded memory for a 4 GB PC.
- Gradle 8.9 may be bootstrapped from the official distribution with SHA-256 verification.
- Build output records branch, commit, environment and APK SHA-256.
- Installation records target device, Android version, APK path and APK SHA-256.
- Source preflight checks required files, secrets, privacy, startup containment, encrypted backup, Global Stop, notification safety and reminder reliability markers.

### Safety and reliability foundations coded

- Android automatic/full backup and cleartext traffic are disabled.
- Memory export uses a versioned encrypted `.mayrabackup` format with authenticated encryption and preview-first additive restore.
- Non-critical startup steps are crash-contained and owner-visible through private diagnostics.
- Global Stop persists across process death, reboot and app update.
- Notification capture, reply and proactive behavior are privacy-gated and Global Stop-aware.
- Reminder workers are bound to exact persisted revisions and due times.
- Stale reminder workers and stale notification actions cannot mutate a newer reminder state.
- Completed or cancelled reminders cannot be revived by old snooze/complete actions.
- Reboot recovery does not repeatedly alert reminders already marked missed.
- Reminder UI only claims complete, snooze, cancel or delete when the transition actually succeeds.

## Evidence status

GitHub Actions runs have failed before Checkout and exposed no source steps or logs. This is not evidence that source compilation succeeded or failed.

Therefore the following remain unverified:

- `:app:compileDebugKotlin`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`
- physical-device installation
- Personal Device Test Center acceptance

## Stabilization order

1. Run source preflight.
2. Run the reproducible Windows build.
3. Fix the first compile error only; repeat until compile is green.
4. Fix unit tests.
5. Fix Android lint.
6. Assemble and hash the APK.
7. Install on the owner's phone.
8. Complete mandatory device checks.
9. Fix crashes, privacy failures, wrong-target actions, duplicate actions and background failures.
10. Create a rollback source/APK snapshot before merging into the integration branch.

## Acceptance gate

Personal Alpha V0.1 is accepted only when:

- clean install and update install launch without crash;
- global Mayra stop blocks further phone actions;
- no OTP, secret or private notification content leaks;
- no wrong contact or duplicate send/reply occurs;
- action wording remains honest and verifiable;
- reminders survive reboot, fire once per revision and can complete/snooze without stale-action races;
- Floating Mayra starts, moves, docks, restores and stops;
- at least 80% of mandatory device checks pass;
- source commit and APK SHA-256 are recorded;
- a rollback snapshot exists.

## Product discipline

Until this gate is accepted, only build, integration, safety, data protection, backup design and critical living-experience fixes belong in this track. Unrelated major features remain frozen.
