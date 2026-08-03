# Mayra AI — Motorola J2 Voice Acceptance

Status: DEVICE TEST IN PROGRESS — MIC + ON-DEVICE SPEECH PASS
Date updated: 2026-08-03
Target device: Motorola Edge 70 Fusion / Android 16

## Authoritative candidate

- Label: `Mayra J2 Voice Test`
- Package: `ai.mayra.app.j2`
- Version: `0.2.1-j2`
- Source: `ef809bbdaca80f3b953483499dc03de8e091339f`
- J2 Voice Test: #18 — success
- J1 Assistant Test: #122 — success
- Android CI: #2013 — success
- Project Governance: #194 — success
- Artifact: `mayra-j2-voice-apk-18`
- Artifact ID: `8863135214`
- APK size: `19,192,945` bytes
- APK SHA-256: `ef48c264f841efd7891e335848e90f38654a6dd25f70d10b0a3afd08b968ecbc`
- Artifact ZIP SHA-256: `ea6afeb03e9137614dd3c486b5905f8e482e53e37f39b771f7fd90fb2a60c743`
- Protected baseline: `baseline/mayra-0.2.1-j2-voice-green-18`

## Package boundary proven by CI

J2 requests exactly one Android runtime permission:

- `android.permission.RECORD_AUDIO`

J2 intentionally excludes internet/provider, contacts, notifications, reminders, boot recovery, notification listener, WorkManager, Room, documents, personal memory, full chat runtime and call control.

J2 uses Android on-device speech recognition only after explicit Assistant invocation and only when Android reports on-device recognition available. It is not an always-listening wake-word build.

## Device evidence received — 22:40 IST

Owner screenshot from the Motorola J2 readiness screen shows:

- App launch: PASS.
- Microphone permission: `allowed ✓` — PASS.
- Android on-device speech recognition: `available ✓` — PASS.
- Digital assistant selection: not yet selected — NEXT GATE.

This is physical evidence that the target Motorola exposes an on-device speech recognizer to J2 and that the one runtime microphone permission is granted successfully.

## Preconditions

1. Install the exact J2 candidate recorded above.
2. Open `Mayra J2 Voice Test` once.
3. Grant microphone permission when Mayra requests it.
4. Select `Mayra J2 Voice Test` under `Settings → Apps → Default apps → Digital assistant`.
5. Keep Motorola `Settings → Gestures → Power button → Digital assistant` configured.

## A. Installation and permission

- [x] App opens normally.
- [x] Granting microphone changes readiness correctly.
- [x] On-device speech recognition reports available on the Motorola.
- [ ] APK install/Play Protect behavior fully recorded.
- [ ] Exactly one Mayra J2 launcher icon confirmed.
- [ ] Microphone is the only runtime permission requested on device.
- [ ] Denying microphone produces a clear bounded state and does not crash.

## B. Assistant selection — CURRENT GATE

- [ ] J2 appears in `Digital assistant app` candidates.
- [ ] J2 can be selected.
- [ ] J2 status screen confirms selection.
- [ ] J1 is not required for J2 operation after J2 is selected.

## C. Unlocked voice invocation

Invoke Mayra from the configured Power-button Digital assistant action while the phone is unlocked.

Expected:

- [ ] Mayra assistant surface appears over the current screen.
- [ ] State changes to preparing/listening.
- [x] On-device speech availability is reported honestly before invocation.
- [ ] Say: `Mayra namaste` — visible transcript is reasonable.
- [ ] Say: `kal subah saat baje` — visible transcript is reasonable.
- [ ] Say: `open WhatsApp` — transcript is captured only; J2 does not execute the command.
- [ ] Say one short English phrase — transcript is reasonable.
- [ ] No transcript is falsely claimed when recognition fails.

Record exact transcript and state for each phrase.

## D. Dismissal/lifecycle repair

- [ ] Invoke, then tap the orb — session closes.
- [ ] Invoke, then tap outside/root surface — session closes.
- [ ] Invoke, then tap the `Mayra` label — session closes.
- [ ] Invoke, then Back gesture — session closes.
- [ ] Invoke, then lock the phone — current session closes cleanly.
- [ ] Microphone/recognition stops after every dismissal.
- [ ] No stuck orb remains after dismissal.

## E. Repeated stability

Run 20 cycles: invoke, speak/no-speech, dismiss, invoke again.

Pass conditions:

- [ ] no Mayra crash;
- [ ] no duplicate orb/session;
- [ ] no System UI restart;
- [ ] no recognizer permanently busy state;
- [ ] no microphone indicator remaining after dismissal;
- [ ] animation returns on the next invocation;
- [ ] phone remains responsive and thermally reasonable.

## F. Locked-screen invocation

- [ ] invoke Mayra while already locked;
- [ ] record whether Android shows Mayra, requires unlock, or blocks the visual session;
- [ ] if listening is permitted, verify only a short bounded recognition session occurs;
- [ ] no private Mayra content is exposed before unlock;
- [ ] dismissal returns cleanly to lock screen.

## G. Reboot/recovery

- [ ] Reboot phone.
- [ ] Verify selected Digital assistant state.
- [ ] Invoke Mayra after reboot.
- [ ] Verify microphone readiness/permission state.
- [ ] Verify one short speech recognition cycle.
- [ ] Dismiss cleanly.

## H. Failure cases

- [ ] microphone denied;
- [ ] no speech;
- [ ] recognition unavailable;
- [ ] recognizer busy/error;
- [ ] language mismatch;
- [ ] rapid invoke/dismiss;
- [ ] screen lock during listening.

## Promotion rule

J2 moves to `DEVICE_VERIFIED` only after installation, permission handling, Assistant selection, unlocked recognition, dismissal, repeated lifecycle, locked-screen behavior and reboot recovery are recorded on the Motorola.

J2 success does **not** prove a production wake phrase, local LLM, full Mayra voice conversation or call control. Those remain separate gated capabilities.
