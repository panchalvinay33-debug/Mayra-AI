# Mayra AI Personal Alpha V0.1 — Stabilization Status

**Track:** `stabilize/living-companion-v0.1`  
**Control issue:** #10  
**Draft pull request:** #11  
**Target:** first stable owner-only Android build that feels alive, remains safe and can later extend to other device bodies.

## Current engineering state

### Preserved

- Main integration work remains in PR #9.
- Permanent recovery branch: `backup/pr9-living-companion-2026-07-25`.
- Stabilization work is isolated from new product expansion.
- Master Blueprint V2 remains the product source of truth.
- The Living Intelligence extension locks the one-brain/many-bodies future architecture.

### Build controls added

- JDK 17, Android SDK 35 and Gradle 8.9 are locked for the personal alpha.
- The Windows build uses one Gradle worker and bounded memory for a 4 GB PC.
- Gradle 8.9 may be bootstrapped from the official distribution with SHA-256 verification.
- Build output records branch, commit, environment and APK SHA-256.
- Installation records target device, Android version, APK path and APK SHA-256.
- Source preflight checks required project files, SDK targets, low-memory settings, launcher/notification/network declarations and obvious tracked secrets.

## Evidence status

The latest GitHub Actions runs fail before Checkout and expose no steps or logs. This is an infrastructure/account runner condition and is not evidence that source compilation succeeded or failed.

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
- reminders survive reboot and can complete/snooze;
- Floating Mayra starts, moves, docks, restores and stops;
- at least 80% of mandatory device checks pass;
- source commit and APK SHA-256 are recorded;
- a rollback snapshot exists.

## Product discipline

Until this gate is accepted, only build, integration, safety, data protection, backup design and critical living-experience fixes belong in this track. Unrelated major features remain frozen.
