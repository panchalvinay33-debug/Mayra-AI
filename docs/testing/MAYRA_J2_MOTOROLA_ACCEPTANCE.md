# Mayra AI — Motorola J2 Voice Acceptance

Status: DEVICE RETEST READY — LOCALE REPAIR CI #90 GREEN
Date updated: 2026-08-03
Target device: Motorola Edge 70 Fusion / Android 16

## New authoritative retest candidate

- Label: `Mayra J2 Voice Test`
- Package: `ai.mayra.app.j2`
- Version: `0.2.1-j2`
- Application source: `e706bdfb8f53006825404db99a51f466aa251fc4`
- J2 Voice Test: #90 — success
- J1 Assistant Test: #194 — success
- Android CI: #2085 — success
- Project Governance: #266 — success
- Artifact: `mayra-j2-voice-apk-90`
- Artifact ID: `8865632199`
- APK size: `19,192,945` bytes
- APK SHA-256: `2c1e00db4a2bfd98993eb87fe091c5373931153eb3b5ac2252914d4441ac230c`
- Artifact ZIP SHA-256: `bbe4936bc5caec8a08d244ea28f82cf09daabceb49bde47b20ad1678933521b9`
- Protected baseline: `baseline/mayra-0.2.1-j2-locale-repair-green-90`
- Immutable snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-03_J2_LOCALE_REPAIR_CI90.md`

J2 #90 requests exactly `android.permission.RECORD_AUDIO` and excludes internet/provider, contacts, notifications, reminders, boot recovery, notification listener, WorkManager, Room, documents, personal memory, full chat runtime and call control.

## Previous physical candidate and evidence

The physically tested CI #18 artifact remains historical evidence, not the next candidate.

PASS on Motorola:

- J2 installed and opened.
- Microphone permission granted; readiness showed `Microphone: allowed ✓`.
- Android reported `On-device speech: available ✓`.
- J2 was selected as Android Digital assistant.
- Motorola Power-button Assistant invocation launched J2 over Home.
- Android microphone privacy indicator appeared.
- Mayra orb/label rendered.

FAIL on CI #18:

- visible state `Speech language unavailable`;
- no transcript;
- no crash and no false transcript.

## Locale repair now green

The CI #90 candidate fixes the device-discovered language gate:

- finite locale order: current device locale → `hi-IN` → `en-IN` → `en-US`;
- BCP-47 canonicalization (`HI-in` → `hi-IN`, `gu_IN` → `gu-IN`);
- duplicate removal and blank/`und` rejection;
- explicit recognition language and language preference;
- retry only for language-not-supported/language-unavailable;
- no endless loop;
- no cloud STT fallback;
- explicit Android 12/API-31 guard around on-device recognizer creation.

CI discoveries intentionally preserved:

- J1 #179 caught the missing static API-31 guard; fixed without suppressing lint.
- J2 #82 caught non-canonical locale output; production policy was fixed rather than weakening the test.

## A. Installation/update

Because CI debug signing may differ across hosted runners, CI #90 may not update-install over CI #18. If Android reports an app-signature/package conflict, J2 is an engineering-only package with no owner Mayra data, so uninstall only `Mayra J2 Voice Test` and clean-install CI #90. Do not uninstall the full Mayra/Personal Alpha merely for this test.

- [ ] CI #90 installs without Play Protect bypass.
- [ ] J2 opens normally.
- [ ] microphone permission/readiness remains correct.
- [ ] on-device speech still reports available.

## B. Assistant selection

After reinstall, Android may reset the selected assistant.

- [ ] `Settings → Apps → Default apps → Digital assistant app` shows J2.
- [ ] select `Mayra J2 Voice Test` if needed.
- [ ] Power-button action remains configured as Digital assistant.

## C. Unlocked transcript retest — CURRENT GATE

From Home, invoke Mayra with the configured Power-button Assistant trigger.

Test in this order and record visible transcript/error:

1. `Mayra namaste`
2. `kal subah saat baje`
3. `open WhatsApp`
4. `hello Mayra how are you`

Pass conditions:

- [ ] assistant surface appears;
- [ ] microphone becomes active;
- [ ] no immediate `Speech language unavailable` if any bounded candidate is installed;
- [ ] at least one reasonable Hindi/Hinglish/English transcript is produced;
- [ ] `open WhatsApp` is transcript-only in J2 and does not execute the command;
- [ ] no false transcript on recognition failure.

If every locale still reports unavailable, record the final visible error. Do not enable cloud recognition as a hidden workaround.

## D. Direct dismissal/lifecycle repair

After transcript proof:

- [ ] orb tap closes session;
- [ ] outside/root tap closes session;
- [ ] `Mayra` label tap closes session;
- [ ] Back closes session;
- [ ] phone lock closes current session;
- [ ] microphone privacy indicator disappears after dismissal;
- [ ] no stuck/duplicate orb.

## E. Repeated stability

Run 20 cycles: invoke → speak/no-speech → dismiss → invoke again.

- [ ] no app crash;
- [ ] no System UI restart;
- [ ] no duplicate session;
- [ ] no permanently busy recognizer;
- [ ] no microphone indicator left active after dismissal;
- [ ] animation returns each invocation;
- [ ] phone remains responsive/thermally reasonable.

## F. Already-locked invocation

- [ ] lock phone first;
- [ ] invoke Assistant trigger;
- [ ] record whether Android shows Mayra, requires unlock or blocks session;
- [ ] no private Mayra content before unlock;
- [ ] dismissal returns cleanly to lock screen.

This records Android/Motorola policy; do not force it with hacks.

## G. Reboot/recovery

- [ ] reboot phone;
- [ ] verify selected Digital assistant;
- [ ] invoke J2;
- [ ] verify microphone readiness and one short speech cycle;
- [ ] dismiss cleanly.

## H. Failure cases

- [ ] microphone denied;
- [ ] no speech;
- [x] language unavailable observed on CI #18, bounded and repaired in CI #90;
- [ ] all locale candidates unavailable;
- [ ] recognizer busy/error;
- [ ] rapid invoke/dismiss;
- [ ] screen lock while listening.

## Promotion rule

J2 becomes `DEVICE_VERIFIED` only after successful unlocked recognition plus dismissal, repeated lifecycle, locked-screen behavior and reboot recovery are physically recorded.

J2 success does not prove production wake phrase, local LLM, full Mayra conversation or call control.
