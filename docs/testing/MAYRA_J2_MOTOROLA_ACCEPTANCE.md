# Mayra AI — Motorola J2 Voice Acceptance

Status: CI-VERIFIED — DEVICE TEST NEXT
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

## Preconditions

1. Remove or deselect J1 as Digital assistant if Android requires changing the current assistant.
2. Install the exact J2 candidate recorded above.
3. Open `Mayra J2 Voice Test` once.
4. Grant microphone permission when Mayra requests it.
5. Select `Mayra J2 Voice Test` under `Settings → Apps → Default apps → Digital assistant`.
6. Keep Motorola `Settings → Gestures → Power button → Digital assistant` configured.

## A. Installation and permission

- [ ] APK installs without Play Protect bypass.
- [ ] Exactly one Mayra J2 launcher icon appears.
- [ ] App opens normally.
- [ ] Microphone is the only runtime permission requested.
- [ ] Denying microphone produces a clear bounded state and does not crash.
- [ ] Granting microphone changes readiness correctly.

## B. Assistant selection

- [ ] J2 appears in `Digital assistant app` candidates.
- [ ] J2 can be selected.
- [ ] J2 status screen confirms selection.
- [ ] J1 is not required for J2 operation after J2 is selected.

## C. Unlocked voice invocation

Invoke Mayra from the configured Power-button Digital assistant action while the phone is unlocked.

Expected:

- [ ] Mayra assistant surface appears over the current screen.
- [ ] State changes to preparing/listening.
- [ ] On-device speech availability is reported honestly.
- [ ] Say: `Mayra namaste` — visible transcript is reasonable.
- [ ] Say: `kal subah saat baje` — visible transcript is reasonable.
- [ ] Say: `open WhatsApp` — transcript is captured only; J2 does not execute the command.
- [ ] Say one short English phrase — transcript is reasonable.
- [ ] No transcript is falsely claimed when recognition fails.

Record exact transcript and state for each phrase.

## D. Dismissal/lifecycle repair

The common assistant-session repair must be physically verified here.

- [ ] Invoke, then tap the orb — session closes.
- [ ] Invoke, then tap outside/root surface — session closes.
- [ ] Invoke, then tap the `Mayra` label — session closes.
- [ ] Invoke, then Back gesture — session closes.
- [ ] Invoke, then lock the phone — current session closes cleanly.
- [ ] Microphone/recognition stops after every dismissal.
- [ ] No stuck orb remains after dismissal.

## E. Repeated stability

Run 20 cycles:

1. invoke Mayra;
2. speak a short phrase or allow a bounded no-speech result;
3. dismiss;
4. invoke again.

Pass conditions:

- [ ] no Mayra crash;
- [ ] no duplicate orb/session;
- [ ] no System UI restart;
- [ ] no recognizer permanently busy state;
- [ ] no microphone indicator remaining after dismissal;
- [ ] animation returns on the next invocation;
- [ ] phone remains responsive and thermally reasonable.

## F. Locked-screen invocation

With the phone already locked:

- [ ] invoke Mayra using the configured Assistant trigger;
- [ ] record whether Android shows Mayra, requires unlock, or blocks the visual session;
- [ ] if listening is permitted, verify only a short bounded recognition session occurs;
- [ ] no private Mayra memory/document/chat content is exposed before unlock;
- [ ] dismissal returns cleanly to lock screen.

This test records Android/Motorola policy; it must not be forced with hacks.

## G. Reboot/recovery

- [ ] Reboot phone.
- [ ] Verify selected Digital assistant state.
- [ ] Invoke Mayra after reboot.
- [ ] Verify microphone readiness/permission state.
- [ ] Verify one short speech recognition cycle.
- [ ] Dismiss cleanly.

## H. Failure cases

Record bounded behavior for:

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
