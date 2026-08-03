# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: repair Motorola Assistant activation after J1 #44 device failure
Canonical product issue: #13
Mandatory feasibility gate: #14
Repository hygiene registry: #15

## Canonical repository truth

- PR #12 is the only active implementation PR.
- PR #9 and #11 are closed as superseded.
- Issue #10 is closed as superseded.
- Protected baselines and retained backups must not be force-moved or deleted.

## Protected baselines

- `baseline/mayra-0.2.1-green-1795` at `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- `baseline/mayra-0.2.1-jarvis-j1-green-1851` at `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- `baseline/mayra-0.2.1-j1-zero-permission-green-44` at `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`

## J1 #44 verified package

- Package: `ai.mayra.app.j1`
- Label: `Mayra J1 Assistant Test`
- J1 #44, Android CI #1935 and Governance #116: success
- Zero requested Android permissions
- Exactly one launcher
- APK SHA-256: `edced64084537cd06ba55ddea0b2f80cdda2aaa322aa296379ac25d89ea66116`

## Motorola device evidence

### Install and launch

PASS:

- J1 #44 installed without bypassing Play Protect.
- App opened normally.
- Status showed `Mayra is not selected`.

### Activate Mayra

FAIL:

- Tapping `Activate Mayra` produced no visible response.
- No Android role-selection or settings screen appeared.
- No diagnostic message appeared.

Evidence is recorded in `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

## Root cause and repair

The J1 activity did not make intent resolution/launch failures visible. Motorola-specific failure therefore appeared as a dead button.

Repair candidate:

- code commit: `2b06cf8fe92b12c2c9d36d5099d1695ea13a1cf9`;
- resolve-check RoleManager request before launch;
- try official settings screens in order: Voice input, Default apps, general Settings;
- show visible status for every route and final failure;
- no OEM-private component, root method or security bypass.

Device evidence update:

- `f44c0599cbca290614963270c6cca3da47077992`

Roadmap synchronization followed. The repaired package is not test-ready until J1 CI, Android CI and Project Governance pass on the final synchronized head.

## Next exact gate

1. Run all three workflows on the synchronized repair head.
2. Share no APK unless all are green.
3. Retest Activate Mayra and capture the visible system screen/message.
4. Continue with role visibility, select/remove, unlocked/locked invocation and orb lifecycle.
5. Keep local LLM, wake phrase and Phone role blocked until J1 evidence is complete.

## Distribution truth

The complete Mayra owner app still requires one stable private signing certificate and trusted distribution. Temporary debug Personal Alpha builds must not be used to bypass Play Protect.

## Merge/secret truth

- PR #12 remains Draft/open/unmerged.
- No merge or ready transition is authorized.
- No API key, keystore, password or private owner data belongs in GitHub records.
