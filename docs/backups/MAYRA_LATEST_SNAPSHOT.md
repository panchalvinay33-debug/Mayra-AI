# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: Jarvis J1 — Motorola device verification

## Protected baselines

### Pre-Jarvis

- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795: success

### Jarvis J1 current baseline

- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851: success
- Project Governance #32: success
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_JARVIS_J1_CI1851.md`

Protected branches are recovery markers, not development branches.

## Authoritative J1 validation

Android CI #1851 passed on exact head `0d9435adb92b425bfb47a710d4f4516a6aaac398`:

- Debug, Personal Alpha and Full Test compilation;
- complete debug unit-test suite;
- lint across Debug, Personal Alpha, Full Test, Release and Document Test;
- Personal Alpha assembly and package/permission/component/one-launcher audit;
- minified non-debuggable final `ai.mayra.app` R8/manifest audit;
- safe Full Test audit;
- isolated zero-permission Document Test audit;
- report and APK artifact upload.

Project Governance #32 passed the canonical record/update contract.

## J1 artifact provenance

Personal Alpha testing candidate:

- Artifact: `mayra-personal-alpha-apk-1851`
- Artifact ID: `8852147191`
- ZIP size: 18,722,590 bytes
- ZIP SHA-256: `ab7cb7d457ed9a034bab5ba394157cf263980b1444fd7c3cf178fc91186296af`
- APK size: 19,162,094 bytes
- APK SHA-256: `1459517f1aa375576afa353ba6683ceaf81ddbcb4e79fc6dd790a501f52307b8`

Other artifacts:

- Full Test `8852148204`, ZIP SHA-256 `d437181ac9c1b7c2c204a7cce66c29a53025b35a0e048b771d5e2b2990a0de0e`;
- Document Test `8852148923`, ZIP SHA-256 `64c5c391c1f7876cc89616c0869e83a11c66969f33fc87c463068246a1d09fa7`;
- Reports `8852146266`, ZIP SHA-256 `34a9f2e561349ff4f042801f5cf3b1d9b413132d71671be68eea77301156c2b3`.

## J1 implementation truth

CI/package verified:

- Android VoiceInteractionService foundation;
- VoiceInteractionSessionService;
- native animated Mayra orb session;
- RecognitionService shell with honest unavailable behavior;
- assistant metadata and lock-screen declaration;
- assistant components excluded from low-permission Full Test;
- compile repairs after CI #1833.

Not yet proven:

- Mayra appears in Motorola Assistant settings;
- selection/removal succeeds;
- unlocked invocation;
- locked invocation;
- repeated session stability;
- force-stop/reboot role recovery;
- battery/OEM background behavior.

Therefore J1 is `DEVICE_VERIFY`, not `DONE`.

## Testing contract

Use:

- `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md` for the next physical test;
- `docs/MAYRA_TEST_MATRIX.md` for evidence levels;
- `docs/MAYRA_PINPOINT_AUDIT.md` for full-project gaps;
- `docs/MAYRA_BASELINE_AND_ROLLBACK.md` for failure recovery.

Every failure must include exact steps, expected/actual result, screenshot/log where useful, app/source/APK provenance and whether restart/reboot reproduces it.

## Current capability status

### Mature but device verification pending

- conversation/provider;
- voice input/TTS;
- memory;
- documents;
- apps/contacts/dialer/composer;
- reminders/recovery;
- history/readiness;
- one-app packaging.

### J1 device verification pending

- Assistant role visibility/selection/removal;
- unlocked/locked assistant session;
- orb lifecycle;
- reboot/stability/background behavior.

### Blocked until J1 evidence is processed

- offline wake phrase;
- on-device local LLM;
- Owner Mode expansion;
- default Phone/InCallService;
- Call Screening;
- proactive call/notification routines.

## Failure history retained

Android CI #1833 failed due to two new Assistant API/type issues. They were repaired forward. The failure is retained in audit/changelog and was superseded by complete success #1851.

## Exact next actions

1. Install only Personal Alpha #1851 on the Motorola.
2. Verify package/label/one launcher and first launch.
3. Complete Assistant role visibility/selection/removal checks.
4. Test unlocked and locked invocation.
5. Run repeated invocation, force-stop/reboot and resource observations.
6. Run J1 regression smoke.
7. Return PASS/FAIL/BLOCKED evidence.
8. Update acceptance/audit/roadmap/snapshot before any J2 coding.

## Merge and secret truth

- PR #12 remains Draft/open/unmerged.
- No merge/ready transition authorized.
- No API keys, keystores, passwords, tokens or owner-private data belong in GitHub.
- Device claims require actual Motorola evidence.
