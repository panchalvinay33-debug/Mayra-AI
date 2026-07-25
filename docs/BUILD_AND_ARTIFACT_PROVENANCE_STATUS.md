# Build and Artifact Provenance — Personal Alpha V0.1

## Purpose

A Mayra Personal Alpha APK must be traceable to one exact clean source commit and must carry evidence for every required build gate. A successful Gradle command alone is not sufficient release evidence.

## Controlled local Windows build

`scripts/build-personal-alpha.ps1` now:

- requires Git and a valid 40-character source commit SHA;
- refuses a dirty working tree;
- runs `verify-personal-alpha-source.ps1 -Strict` before Java, Gradle or Android compilation;
- locks JDK 17, Gradle 8.9, SDK 35 and one Gradle worker;
- runs `compileDebugKotlin`;
- runs the complete debug unit-test suite unless explicitly skipped;
- runs Android lint unless explicitly skipped;
- assembles the debug APK;
- copies the APK under a source-SHA-based file name;
- records the APK SHA-256 and exact byte size;
- records the source-preflight report SHA-256;
- writes `artifact-manifest.json` using schema `mayra.personal-alpha.artifact.v1`;
- verifies the newly generated APK and manifest before reporting success.

Builds made with `-SkipTests` or `-SkipLint` remain diagnostic artifacts. Their manifest records the skipped gate as false, and the normal installer rejects them.

## Artifact manifest

The manifest records:

- repository, branch/ref and exact source SHA;
- clean-source status;
- JDK, Gradle, compile SDK, target SDK and worker count;
- source-preflight, compile, test, lint and assemble gate states;
- APK file name, SHA-256 and exact size;
- source-preflight report SHA-256.

The manifest contains no API key, backup password, user data, conversation or device contact content.

## Artifact verification

`scripts/verify-personal-alpha-artifact.ps1` rejects:

- unsupported manifest schemas;
- missing or malformed source SHAs;
- APK file-name mismatch;
- malformed or mismatched SHA-256;
- file-size mismatch;
- missing or false required gates.

`-AllowSkippedGates` is an explicit diagnostic-only override and is not used by the normal installer.

## Installer controls

`scripts/install-personal-alpha.ps1`:

- verifies the artifact before installation when a manifest is present;
- blocks an APK without a manifest unless `-AllowUnverifiedArtifact` is explicitly supplied;
- no longer attempts to grant `CALL_PHONE` or `SEND_SMS`;
- captures device manufacturer, model, Android version, SDK and build fingerprint;
- verifies that `ai.mayra.app` exists after installation;
- captures installed package path, version name and version code;
- writes `install-manifest.json` with source, artifact and device evidence.

The explicit unverified-artifact switch exists only for diagnosis of old/local APKs and must not be treated as Personal Alpha acceptance evidence.

## CI controls

Android CI now follows one canonical chain:

1. exact checkout with persisted Git credentials disabled;
2. JDK 17 and Gradle 8.9 setup;
3. clean-source/environment verification;
4. strict source preflight;
5. artifact-provenance regression tests;
6. Kotlin compile;
7. complete unit tests;
8. Android lint;
9. APK assembly;
10. artifact manifest generation;
11. independent manifest/APK verification;
12. APK, manifest, preflight and reports upload.

The CI artifact manifest records the GitHub source SHA, ref, event, run ID and run attempt.

## Provenance regression tests

`scripts/test-artifact-provenance.ps1` independently checks:

- valid artifact acceptance;
- tampered APK rejection;
- wrong file-name rejection;
- skipped-gate rejection by default;
- explicit diagnostic skipped-gate override;
- wrong-hash rejection.

The test uses the current PowerShell host and is compatible with Windows PowerShell and GitHub-hosted `pwsh` runners.

## Current validation status

These controls are committed but have not successfully executed in the current GitHub environment. Workflow run 880 failed before exposing any job steps or logs. That infrastructure result is not source compile, test, lint or provenance-test evidence.

Required evidence remains:

- strict preflight execution on an actual checkout;
- provenance regression-test execution;
- Kotlin compile;
- complete unit tests;
- lint;
- APK assembly;
- generated artifact verification;
- installation and package evidence on the owner's Motorola phone.
