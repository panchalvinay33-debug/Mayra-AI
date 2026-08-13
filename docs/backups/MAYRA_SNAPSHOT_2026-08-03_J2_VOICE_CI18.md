# Mayra AI — Immutable Snapshot: J2 Voice CI #18

Date: 2026-08-03
Milestone: invocation-time voice foundation after J1 Motorola Assistant proof

## Source and rollback

- Source commit: `ef809bbdaca80f3b953483499dc03de8e091339f`
- Active branch at validation: `agent/document-library-foundation`
- PR: #12 — Draft/open/unmerged
- Protected baseline: `baseline/mayra-0.2.1-j2-voice-green-18`
- Version: `0.2.1` / versionCode `4`

This source is a protected code recovery point. Later documentation commits are not required to reproduce the validated application artifact.

## Automated validation

All required gates passed on the exact source commit:

- J2 Voice Test #18 — success
  - compile;
  - voice-state unit tests;
  - Android lint;
  - APK assembly;
  - exactly-one-permission audit;
  - launcher/component isolation audit;
  - artifact upload.
- J1 Assistant Test #122 — success
  - proves the J2/common-session changes did not break the zero-permission J1 boundary.
- Android CI #2013 — success
  - debug/personal-alpha/full-test compilation;
  - complete unit-test suite;
  - lint;
  - Personal Alpha package audit;
  - minified final release/R8 audit;
  - Full Test package audit;
  - isolated Document Test audit;
  - artifact/report upload.
- Project Governance #194 — success.

## J2 artifact provenance

- Package: `ai.mayra.app.j2`
- Label: `Mayra J2 Voice Test`
- Artifact name: `mayra-j2-voice-apk-18`
- Artifact ID: `8863135214`
- APK filename in artifact: `app-j2VoiceTest.apk`
- APK size: `19,192,945` bytes
- APK SHA-256: `ef48c264f841efd7891e335848e90f38654a6dd25f70d10b0a3afd08b968ecbc`
- Artifact ZIP SHA-256: `ea6afeb03e9137614dd3c486b5905f8e482e53e37f39b771f7fd90fb2a60c743`

## Proven package boundary

J2 requests exactly:

- `android.permission.RECORD_AUDIO`

It excludes provider/internet, contacts, notifications, reminders, boot recovery, notification listener, WorkManager, Room, documents, personal memory, full chat runtime and call control.

## Device truth inherited from J1

Already proven on the target Motorola before this snapshot:

- Mayra can appear as a valid Android Digital assistant candidate;
- owner can select Mayra;
- Motorola Power-button Digital assistant action can invoke Mayra while unlocked;
- the Mayra VoiceInteractionSession/orb renders over the current screen;
- Back and screen lock dismiss the tested session.

Direct tap dismissal was missing on J1 #68 and is repaired in the common session code included in this J2 baseline; physical retest is still required.

## J2 architecture truth

Implemented:

- bounded voice-session state model;
- invocation-time Android on-device recognizer capability detection;
- on-device SpeechRecognizer use only when Android reports support;
- visible preparing/listening/result/error states;
- recognition stop on hide/destroy;
- tap/root/label/Back dismissal plumbing;
- no continuous SpeechRecognizer hotword loop.

Not implemented/proven by this snapshot:

- production wake phrase;
- local conversational LLM;
- full Mayra voice response/TTS loop inside Assistant session;
- phone-role/call control;
- AI cellular caller message-taking;
- stable owner signing secrets or trusted full-app distribution.

## Next gate

Run the physical checklist in `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md` on the Motorola Edge 70 Fusion. Do not promote J2 to device-verified until microphone permission, on-device recognition, dismissal, 20-cycle stability, lock-screen behavior and reboot recovery are recorded.
