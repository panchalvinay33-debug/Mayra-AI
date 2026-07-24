# Mayra AI — Windows Personal Alpha Build

This guide builds the private owner-first debug APK from branch `batch-12-runtime-control-center` and installs it on a personal Android phone.

## Required setup

- Windows 10 or newer.
- Android Studio with Android SDK Platform 35 and Platform Tools.
- JDK 17. Android Studio's embedded JDK is acceptable.
- At least 4 GB RAM. Close browsers and other heavy programs during the build.
- USB cable and Developer options / USB debugging enabled on the phone.

## Recommended Android Studio path

1. Open the Mayra-AI repository in Android Studio.
2. Checkout branch `batch-12-runtime-control-center`.
3. In **Settings > Build, Execution, Deployment > Build Tools > Gradle**, select JDK 17.
4. In **SDK Manager**, install Android SDK Platform 35 and current Platform Tools.
5. Wait for Gradle sync.
6. Run **Build > Make Project**.
7. Run the `testDebugUnitTest` Gradle task.
8. Run the `lintDebug` Gradle task.
9. Run **Build > Build APK(s)**.
10. The expected APK is `app/build/outputs/apk/debug/app-debug.apk`.

Do not install an APK if compilation, unit tests or lint fail. Save the first error and its surrounding lines.

## Automated PowerShell build

From the repository root:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\build-personal-alpha.ps1
```

For a 4 GB PC, the script already uses one Gradle worker and bounded JVM memory.

Optional development-only switches:

```powershell
.\scripts\build-personal-alpha.ps1 -SkipLint
.\scripts\build-personal-alpha.ps1 -SkipTests -SkipLint
.\scripts\build-personal-alpha.ps1 -Clean
```

Skipping tests or lint is only for locating compile errors. The APK is not accepted for personal alpha until full tests and lint pass.

Build logs are written to:

- `build/personal-alpha/compile.log`
- `build/personal-alpha/tests.log`
- `build/personal-alpha/lint.log`
- `build/personal-alpha/assemble.log`
- `build/personal-alpha/build-summary.txt`

## Install through USB

After a successful build:

```powershell
.\scripts\install-personal-alpha.ps1
```

Install over an existing Mayra build while preserving data:

```powershell
.\scripts\install-personal-alpha.ps1
```

Fresh-install test that removes existing Mayra app data:

```powershell
.\scripts\install-personal-alpha.ps1 -FreshInstall
```

Optionally grant common runtime permissions through ADB:

```powershell
.\scripts\install-personal-alpha.ps1 -GrantCommonPermissions
```

Notification Access, battery optimization, Accessibility and default-app roles still require visible manual Android consent.

## First phone run

1. Complete onboarding.
2. Open **Complete phone access setup**.
3. Enable only the access needed for the current test.
4. Open **Start personal device check**.
5. Use harmless test data: a test contact, a draft message and a short reminder.
6. Mark each test Pass, Fail or Blocked.
7. Do not test payment, OTP, emergency, destructive or important real-world actions in the first session.

## GitHub Actions APK

The Android CI workflow supports manual dispatch and, when GitHub runners are healthy, uploads:

- `Mayra-AI-personal-alpha-debug.apk`
- `app-debug.sha256`
- compile, test, lint and assemble logs

The APK artifact is retained for 14 days. A successful APK artifact must only be used when compile, unit tests and lint in that same run are green.

## What to share after a failure

For a compile failure, share `build/personal-alpha/compile.log`.
For a test failure, share `build/personal-alpha/tests.log`.
For a lint failure, share `build/personal-alpha/lint.log`.
For installation failure, share the complete ADB output.
For a phone crash, share the action that triggered it and Android Studio Logcat filtered by `ai.mayra.app`.
