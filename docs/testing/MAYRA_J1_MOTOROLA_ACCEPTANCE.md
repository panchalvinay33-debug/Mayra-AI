# Mayra AI — Motorola Jarvis J1 Acceptance

Status: READY FOR OWNER TEST
Date created: 2026-08-03
Target artifact: `Mayra AI Personal Alpha` 0.2.1-alpha
Package: `ai.mayra.app.alpha`
Source baseline: `0d9435adb92b425bfb47a710d4f4516a6aaac398`
Android CI: #1851 — success
Project Governance: #32 — success
APK SHA-256: `1459517f1aa375576afa353ba6683ceaf81ddbcb4e79fc6dd790a501f52307b8`
Target device: Motorola owner device / Android 16

## Evidence header — fill before testing

- Test date/time/timezone:
- Exact Motorola model:
- Android version:
- Android build number/security patch:
- Installed Mayra label/version:
- APK SHA-256 verified: YES / NO
- Existing Mayra variants uninstalled or intentionally retained:
- Battery saver state:
- Screen lock configured:
- Tester notes:

## A. Install and baseline sanity

- [ ] Install succeeds without bypassing Play Protect.
- [ ] Android package is `ai.mayra.app.alpha`.
- [ ] Label is `Mayra AI Personal Alpha`.
- [ ] Exactly one launcher icon appears for this package.
- [ ] Main screen launches without crash/blank state.
- [ ] History, Library, Memory, Provider and Device screens open.
- [ ] Background/foreground and force-stop/reopen do not crash.

Record result: PASS / FAIL / BLOCKED
Evidence/notes:

## B. Assistant role visibility

Path may vary by Motorola build; locate Default digital assistant app / Assistant app in Android settings.

- [ ] Mayra appears as an available assistant choice.
- [ ] Mayra can be selected only through explicit owner action.
- [ ] Android shows no unexpected second launcher/app identity.
- [ ] Mayra can be deselected and the previous assistant restored.
- [ ] Selecting/deselecting does not erase Mayra data.

Record result: PASS / FAIL / BLOCKED
Exact settings path:
Evidence/notes:

## C. Unlocked invocation

With phone unlocked and Mayra selected as Assistant:

- [ ] System assistant gesture/button invokes Mayra.
- [ ] Animated Mayra orb/session appears over the current screen.
- [ ] Only the assistant session appears; full MainActivity is not unnecessarily launched.
- [ ] Orb pulses smoothly without freezing.
- [ ] Hiding/dismissing the assistant stops the visible animation.
- [ ] Repeated invoke/dismiss cycles do not stack multiple surfaces.
- [ ] No crash after 10 repeated invocations.
- [ ] Returning to previous app preserves expected state.

Record result: PASS / FAIL / BLOCKED
Invocation method used:
Evidence/notes:

## D. Locked-screen invocation

- [ ] Lock phone normally.
- [ ] Invoke the selected assistant using the supported Motorola gesture/button.
- [ ] Mayra session behavior matches Android lock-screen policy.
- [ ] Private app content/memory/document text is not exposed without unlock.
- [ ] Dismissing returns to lock screen.
- [ ] Unlocking after invocation does not duplicate the assistant surface.
- [ ] Five repeated locked invocations do not crash System UI or Mayra.

Record result: PASS / FAIL / BLOCKED
Evidence/notes:

## E. Recognition shell honesty

The real local recognizer/wake phrase is not integrated in this baseline.

- [ ] Assistant does not invent a transcript when recognition is unavailable.
- [ ] Recognition failure is bounded; no endless spinner/service loop.
- [ ] Main Mayra voice button remains independently usable where supported.
- [ ] No claim that “always listening” works yet.

Record result: PASS / FAIL / BLOCKED
Evidence/notes:

## F. Process, reboot and role recovery

- [ ] Select Mayra as Assistant, then force-stop Mayra.
- [ ] Invoking Assistant does not create a crash loop.
- [ ] Reopen Mayra and verify main features remain usable.
- [ ] Reboot phone with Mayra selected as Assistant.
- [ ] After reboot, Android role selection remains correct or fails visibly/recoverably.
- [ ] Assistant invocation after reboot works or produces a documented bounded failure.
- [ ] Removing Mayra as Assistant restores previous system behavior.

Record result: PASS / FAIL / BLOCKED
Evidence/notes:

## G. Permission and component boundary

- [ ] No `CALL_PHONE` permission requested.
- [ ] No `SEND_SMS` permission requested.
- [ ] No system-overlay permission requested for the assistant orb.
- [ ] Microphone permission appears only when voice functionality requires it.
- [ ] Contacts/notification permissions remain feature-scoped.
- [ ] Assistant role is not silently selected.
- [ ] Low-permission Full Test is not used for this J1 role test.

Record result: PASS / FAIL / BLOCKED
Evidence/notes:

## H. Stability and resource observation

Run at least 20 invoke/dismiss cycles and keep Mayra selected for at least 30 minutes.

- [ ] No ANR/crash/System UI restart.
- [ ] No persistent orb after session dismissal.
- [ ] No unexpected screen wake loop.
- [ ] No continuous microphone indicator from this recognition-shell baseline.
- [ ] Phone temperature remains normal during idle.
- [ ] Battery drain observation recorded (not yet a full wake-word benchmark).

Start battery/time:
End battery/time:
Temperature/behavior notes:
Record result: PASS / FAIL / BLOCKED

## I. Regression smoke after Assistant role

- [ ] Main text chat works.
- [ ] `kesi ho` receives natural reply.
- [ ] `drinking water after 3 min` creates a reminder or requests required permission correctly.
- [ ] `Open WhatsApp` reaches the app-opening path.
- [ ] Library opens.
- [ ] Memory Center opens.
- [ ] Provider screen opens.
- [ ] Deselecting Assistant does not break normal Mayra app use.

Record result: PASS / FAIL / BLOCKED
Evidence/notes:

## Failure protocol

For every failure record:

- section/item;
- exact steps;
- expected result;
- actual result;
- screenshot/screen recording;
- whether it repeats after app restart/reboot;
- whether removing Assistant role recovers the phone;
- date/time;
- app version/source SHA/APK SHA;
- any Android crash dialog or system log text.

Stop the J1 test immediately if there is a System UI crash loop, lock-screen access problem, repeated unwanted screen wake, persistent microphone use or inability to restore the previous Assistant.

## Promotion rule

J1 can move from `DEVICE_VERIFY` to `DONE` only when:

1. all blocking sections pass on the Motorola;
2. failures are repaired and re-tested on a new provenance-recorded artifact;
3. audit, roadmap, latest snapshot and this sheet are synchronized;
4. a new exact-head dual-green baseline is created after any repair;
5. only then may wake-word/local-model work begin.
