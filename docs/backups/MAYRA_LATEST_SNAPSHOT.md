# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: second Motorola Assistant activation repair after J1 #56 device failure
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
- `baseline/mayra-0.2.1-j1-activation-repair-green-56` at `ce96f8e83fe33b878d426c407715d4a3e1b0495a`

## J1 #56 verified package

- Package: `ai.mayra.app.j1`
- Label: `Mayra J1 Assistant Test`
- J1 #56, Android CI #1947 and Governance #128: success
- Zero requested Android permissions
- Exactly one launcher
- APK SHA-256: `2def2acd55a0ea751c3cd70c9d78674c275f2c2d8e2e4e03ae527464cf48a318`

## Motorola device evidence

### Install/update and launch

PASS:

- #56 installed/updated and opened normally.
- No runtime permission prompt appeared.
- Visible activation diagnostic text rendered.

### Activate Mayra

FAIL:

- Tapping `Activate Mayra` still did not leave the J1 screen.
- No usable Android Assistant/default-app selection screen appeared.
- Status remained `Mayra is not selected`.
- Owner screenshot received at approximately 19:46 IST on 2026-08-03.

Evidence is recorded in `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

## Pinpoint root cause after #56

Two concrete problems were found:

1. Shared voice-interaction metadata referenced `ai.mayra.app.MainActivity` as `settingsActivity`, but J1 removes `MainActivity`. J1 therefore had an invalid settings activity target.
2. Motorola Edge 70 Fusion Android 16 documents the assistant-selection route as `Settings → Apps → Default apps → Digital assistant`; the earlier generic Voice Input/role-request fallback did not reliably reach that screen.

## Second repair

- Added J1-specific `mayra_voice_interaction_service.xml` with `settingsActivity="ai.mayra.app.j1.J1AssistantTestActivity"`.
- `Activate Mayra` now launches `Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS` directly.
- On-screen instructions show the exact Motorola path: `Settings → Apps → Default apps → Digital assistant → Mayra`.
- If Default apps cannot be launched, fallback is general Settings with the same manual path.
- No OEM-private component, root method, Accessibility hack or security bypass is used.

Code commits in this repair chain include:

- `dfe95331b65ba5d912653d8d43a1ddd55d124efe` — J1-specific valid settingsActivity.
- `3a94d202b4cffe4e625d55a377e656d46010776b` — direct Motorola Default apps route.
- Documentation synchronized afterward.

## Next exact gate

1. Run J1 Assistant Test, Android CI and Project Governance on the final synchronized second-repair head.
2. Share no new APK unless all three are green.
3. Install/update the new artifact over #56 where signing permits.
4. Tap Activate Mayra; expect Android Default apps screen.
5. Tap Digital assistant and record whether Mayra appears.
6. If app navigation still fails, manually test `Settings → Apps → Default apps → Digital assistant` to separate navigation failure from candidate-eligibility failure.
7. If Mayra appears, select it, refresh status, then test unlocked/locked invocation and orb lifecycle.
8. Keep local LLM, wake phrase and Phone role blocked until J1 evidence is complete.

## Distribution truth

The complete Mayra owner app still requires one stable private signing certificate and trusted distribution. Temporary debug Personal Alpha builds must not be used to bypass Play Protect.

## Merge/secret truth

- PR #12 remains Draft/open/unmerged.
- No merge or ready transition is authorized.
- No API key, keystore, password or private owner data belongs in GitHub records.
