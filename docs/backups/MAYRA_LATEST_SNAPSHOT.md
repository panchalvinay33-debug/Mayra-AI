# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Mandatory entry point: `START_HERE.md`
Current phase: J1 Motorola Assistant proof achieved; J2 invocation-time voice foundation under fresh CI
Canonical product issue: #13
Mandatory feasibility gate: #14
Repository hygiene registry: #15
J2 preflight: `docs/feasibility/MAYRA_J2_VOICE_PREFLIGHT.md`

## Canonical repository truth

- PR #12 is the only active implementation PR.
- PR #9 and #11 are closed as superseded.
- Issue #10 is closed as superseded.
- Protected baselines and retained backups must not be force-moved or deleted.
- Final product remains one Mayra app; J1/J2 are engineering proof packages only.

## Protected baselines

- `baseline/mayra-0.2.1-green-1795` at `065e22524c835f3ddd3b2f56215a3616f071d4b3`
- `baseline/mayra-0.2.1-jarvis-j1-green-1851` at `0d9435adb92b425bfb47a710d4f4516a6aaac398`
- `baseline/mayra-0.2.1-j1-zero-permission-green-44` at `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`
- `baseline/mayra-0.2.1-j1-activation-repair-green-56` at `ce96f8e83fe33b878d426c407715d4a3e1b0495a`

No new baseline is promoted until the current touch-dismiss/J2 batch is exact-head green.

## Authoritative J1 device artifact

- Package: `ai.mayra.app.j1`
- Label: `Mayra J1 Assistant Test`
- Tested Motorola-route artifact: J1 #68
- Source: `8b0e7ee33a34b8784de6b555ff7b273ab11ac525`
- J1 #68: success
- Android CI #1959: success
- Project Governance #140: success
- Artifact ID: `8859497655`
- APK SHA-256: `0e1a36ff6b5e72c7d719430b5e04e87c3f7c3707d341a0527d6e488942d13cb9`

## Motorola device evidence

PASS:

- J1 installed and launched without bypassing Play Protect.
- Motorola Default Apps opened.
- `Mayra J1 Assistant Test` appeared as a valid Digital assistant.
- Mayra was selected as the default Digital assistant.
- J1 reported `Status: Mayra is selected`.
- Motorola Power-button action was configured for Digital assistant.
- Power-button invocation launched Mayra while unlocked.
- Mayra blue/purple orb/session rendered over the current screen.
- Back dismissed the session.
- Locking the phone dismissed the current session.

FAIL/repair:

- On J1 #68, tapping orb/outside produced no response because the initial surface had no click listeners.
- Common assistant-session repair adds orb/root/label tap-to-hide and explicit Back-to-hide.
- Hide/destroy now stop animation/keep-awake work; a later show restarts the pulse.

Still unverified:

- direct touch dismissal on repaired artifact;
- 10–20 repeated invoke/dismiss cycles;
- invocation starting from an already locked screen;
- reboot persistence/recovery of Assistant role.

Evidence: `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`.

## Current J2 implementation batch

Goal: prove short real voice input after explicit Mayra invocation, locally where Android supports it.

Added:

- build type/package `j2VoiceTest` / `ai.mayra.app.j2`;
- secret-backed owner-signing selection when protected owner secrets exist, otherwise debug fallback clearly remains non-stable;
- J2 minimal Application and setup/status Activity;
- J2-specific Assistant metadata pointing settingsActivity to J2;
- exactly-one-permission manifest intent: `RECORD_AUDIO` only;
- `MayraVoiceSessionState` bounded state model;
- `MayraOnDeviceSpeechRecognizer` wrapper using on-device Android recognition only when reported available;
- common session integration guarded by `BuildConfig.VOICE_SESSION_RECOGNITION_ENABLED` so J1/full app behavior is not silently changed to microphone listening;
- stop recognition on hide/destroy;
- unit tests for state semantics;
- dedicated `J2 Voice Test` GitHub workflow with compile/test/lint/package/permission/component audits.

J2 explicitly does NOT add:

- continuous listening/hotword loop;
- internet/cloud STT;
- local LLM;
- contacts, notifications, reminders or background listener;
- call control;
- raw audio persistence.

## Feasibility decision

Android’s selected `VoiceInteractionService` is the correct lightweight always-available assistant foundation, but Android `SpeechRecognizer` is not the future always-on wake-word engine. J2 uses SpeechRecognizer only after explicit invocation. A dedicated wake-word detector requires a separate preflight/battery benchmark.

## Current exact gate

1. Settle fresh J1 Assistant Test, J2 Voice Test, Android CI and Project Governance on the final synchronized head.
2. Repair source/manifest/test issues rather than weakening the audits.
3. Promote a new protected baseline only after all required gates are green.
4. Then produce a J2 test artifact and record package/size/SHA/source provenance.
5. Motorola J2 test: microphone grant → J2 Digital assistant selection → on-device recognition availability → Power invoke → short spoken phrase → visible `Listening…`/`Heard:` result → tap/Back/lock dismiss → repeat cycles → locked-screen/reboot tests.

## Distribution truth

- Full Mayra still requires one stable private owner/release certificate and trusted distribution.
- J1/J2 can use the same owner signing path only after private signing secrets are configured.
- Temporary CI debug artifacts are not update-stable across runners.
- Play Protect/signature checks must not be bypassed.

## Merge/secret truth

- PR #12 remains Draft/open/unmerged.
- No merge or ready transition is authorized.
- No API key, keystore, password or owner-private data belongs in GitHub records.
