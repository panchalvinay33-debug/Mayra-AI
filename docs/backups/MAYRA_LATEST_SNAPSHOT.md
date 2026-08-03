# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: Motorola validation of the verified zero-permission J1 Assistant package
Canonical product issue: #13
Mandatory major-feature feasibility gate: #14
Repository hygiene registry: #15

## Canonical repository truth

- PR #12 is the only active implementation PR.
- PR #9 and PR #11 are closed and explicitly marked superseded.
- Issue #10 is closed and explicitly marked superseded.
- Issue #13 is the product North Star.
- Issue #14 prevents major capabilities entering implementation before Android/Motorola/permission/performance/distribution/fallback review.
- Issue #15 classifies every branch as active, protected, retained backup or delete candidate.
- Protected baselines and required backups must not be deleted or force-moved.

## Protected baselines

### Pre-Jarvis
- Branch: `baseline/mayra-0.2.1-green-1795`
- Commit: `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- Android CI #1795: success

### Jarvis J1 code baseline
- Branch: `baseline/mayra-0.2.1-jarvis-j1-green-1851`
- Commit: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- Android CI #1851: success
- Project Governance #32: success

### Zero-permission J1 artifact baseline
- Branch: `baseline/mayra-0.2.1-j1-zero-permission-green-44`
- Commit: `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- J1 Assistant Test #44: success
- Android CI #1935: success
- Project Governance #116: success

## Verified J1 artifact

- Build type: `j1AssistantTest`
- Package: `ai.mayra.app.j1`
- Label: `Mayra J1 Assistant Test`
- Requested Android permissions: zero, verified by APK audit
- Launcher count: exactly one
- Included: Assistant activation/status activity, VoiceInteractionService, session service/orb and RecognitionService shell
- Excluded: full chat, provider, contacts, reminders, notifications, boot recovery, notification listener, documents, memory, WorkManager, AndroidX Startup, Room and ProfileInstaller receiver
- Artifact name: `mayra-j1-zero-permission-apk-44`
- Artifact ID: `8854905288`
- Artifact ZIP SHA-256: `12f4e148fbac99e916b78321b9ae75d87a5b4b5cebe2060bfa6e6b5f7545be3b`
- APK size: `19,192,842` bytes
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`

## Motorola evidence already received

- Personal Alpha update failed because old/new APKs used different temporary CI debug certificates.
- Full Personal Alpha clean-install retry was blocked by Google Play Protect because that sideloaded debug app could request sensitive access.
- Play Protect must not be disabled or bypassed for that artifact.

Evidence:
- `docs/testing/MAYRA_J1_INSTALL_RESULT_2026-08-03.md`
- `docs/testing/MAYRA_PLAY_PROTECT_BLOCK_2026-08-03.md`

## J1 engineering history

- Run #16 caught the API-29 role-request guard.
- Run #22 caught inherited AndroidX permissions and background infrastructure.
- Run #32 caught the lint/removal-model mismatch.
- Run #38 proved the ProfileInstaller receiver still survived final manifest merging.
- Run #44 passed compile, lint, assembly, zero-permission/component audit and artifact upload.

## Current device gate

Use only J1 #44 for the next Motorola test:

1. clean install;
2. launch the single J1 icon;
3. verify Mayra appears in Android Assistant selection;
4. select and remove Mayra;
5. invoke while unlocked;
6. invoke while locked, where Motorola permits;
7. observe orb creation/dismiss/repeated invocation;
8. record PASS/FAIL/BLOCKED with screenshots and exact steps.

No local LLM, wake phrase or Phone/InCallService coding begins until this evidence is recorded and issue #14 preflight is complete.

## Full owner-app distribution truth

The complete Mayra app still requires one stable private owner/release certificate, protected build secrets, signed APK/AAB provenance, trusted owner-controlled distribution (preferably Play Internal Testing), and install-over-install/local-data-retention proof.

Temporary CI debug-signed Personal Alpha APKs are not long-term owner releases and must not be used to bypass Play Protect.

## Merge and secret truth

- PR #12 remains Draft/open/unmerged.
- No merge/ready transition is authorized.
- No API keys, keystores, passwords, tokens or owner-private data belong in GitHub records or chat.
- Device claims require actual Motorola evidence.
