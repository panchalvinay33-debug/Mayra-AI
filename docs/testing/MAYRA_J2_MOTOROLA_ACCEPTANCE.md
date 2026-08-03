# Mayra AI — Motorola J2 Voice Acceptance

Status: DEVICE RETEST READY — SUPPORT-PROBE REPAIR CI #106 GREEN
Date updated: 2026-08-03
Target device: Motorola Edge 70 Fusion / Android 16

## New authoritative retest candidate

- Label: `Mayra J2 Voice Test`
- Package: `ai.mayra.app.j2`
- Version: `0.2.1-j2`
- Application source: `a63ef1e7c3ddca06ce444502e5afd3a410d8fb18`
- J2 Voice Test: #106 — success
- J1 Assistant Test: #210 — success
- Android CI: #2101 — success
- Project Governance: #282 — success
- Artifact: `mayra-j2-voice-apk-106`
- Artifact ID: `8866441207`
- APK size: `19,209,329` bytes
- APK SHA-256: `d0917d17b50429a843f3a5e688580df66f3eea678be4806b44ef9f1535adeb6e`
- Artifact ZIP SHA-256: `b2109366a0140a66f85fef3cd6a85a95263815643ae86076ba0a9f20194140db`
- Protected baseline: `baseline/mayra-0.2.1-j2-speech-support-green-106`

J2 requests exactly `android.permission.RECORD_AUDIO` and excludes internet/provider, contacts, notifications, reminders, boot recovery, notification listener, WorkManager, Room, documents, personal memory, full chat runtime and call control.

## Physical evidence so far

PASS on Motorola:

- J2 can clean-install and open.
- J2 can be selected as Android Digital assistant.
- microphone permission is allowed.
- Android reports an on-device recognition service is available.
- Motorola Power-button Assistant invocation launches Mayra over Home.
- Mayra orb/label renders.
- Back and phone-lock dismissal are physically proven in common Assistant behavior.

Known transcript failures:

- CI #18: `Speech language unavailable`.
- CI #90: `Speech recognizer unavailable`.
- no false transcript and no crash in either failure.

## CI #106 repair now ready for device test

- one on-device `SpeechRecognizer` instance for a bounded attempt instead of destroy/recreate on every locale retry;
- Android 13+ uses `SpeechRecognizer.checkRecognitionSupport()` before listening;
- Mayra prioritizes actually installed on-device languages;
- installed-language preference follows device locale → `hi-IN` → `en-IN` → `en-US`;
- if a language is supported but not installed, visible state becomes `On-device speech language pack needed`;
- if OEM support discovery itself is unavailable, Mayra uses bounded delayed locale trials;
- retry reuses the recognizer and waits 450 ms between language attempts;
- no cloud STT fallback and no endless retry loop;
- J2 #106, J1 #210, Android CI #2101 and Governance #282 all pass.

## A. Installation/update

Temporary CI signing can differ between hosted runners.

- [ ] Try installing CI #106 over current J2.
- [ ] If Android reports package/signature conflict, uninstall only `Mayra J2 Voice Test`, then clean-install #106.
- [ ] Do not uninstall full Mayra/Personal Alpha for this engineering test.
- [ ] J2 #106 opens normally.
- [ ] microphone readiness remains correct.
- [ ] on-device speech service still reports available.

## B. Assistant selection

After clean reinstall Android may reset the role.

- [ ] `Settings → Apps → Default apps → Digital assistant app` shows J2.
- [ ] select `Mayra J2 Voice Test` if required.
- [ ] Power-button action remains configured as Digital assistant.

## C. Unlocked transcript — CURRENT GATE

From Home, invoke Mayra with the configured Power-button trigger and test in this order:

1. `Mayra namaste`
2. `kal subah saat baje`
3. `open WhatsApp`
4. `hello Mayra how are you`

Pass conditions:

- [ ] assistant surface appears;
- [ ] microphone becomes active;
- [ ] at least one reasonable transcript is produced;
- [ ] `open WhatsApp` remains transcript-only in J2;
- [ ] no false transcript on recognition failure.

If the visible state is `On-device speech language pack needed`, record that exact screen. Do not enable hidden cloud recognition.

## D. Direct dismissal/lifecycle repair

After transcript proof:

- [ ] orb tap closes session;
- [ ] outside/root tap closes session;
- [ ] `Mayra` label tap closes session;
- [ ] Back closes session;
- [ ] phone lock closes session;
- [ ] microphone privacy indicator disappears after dismissal;
- [ ] no stuck/duplicate orb.

## E. Repeated stability

After one successful transcript, run 20 invoke → speak/no-speech → dismiss cycles.

- [ ] no app crash;
- [ ] no System UI restart;
- [ ] no permanently busy recognizer;
- [ ] no duplicate orb;
- [ ] mic indicator does not remain active;
- [ ] animation returns on each invocation.

## F. Already-locked invocation

Pending unlocked transcript proof. Record normal Motorola/Android behavior; do not force with hacks.

## G. Reboot/recovery

Pending unlocked transcript proof.

## H. Failure cases

- [ ] microphone denied;
- [ ] no speech;
- [x] language unavailable — CI #18 physical observation;
- [x] recognizer unavailable — CI #90 physical observation;
- [ ] language pack needed;
- [ ] recognizer busy/error;
- [ ] rapid invoke/dismiss;
- [ ] screen lock while listening.

## Promotion rule

J2 becomes `DEVICE_VERIFIED` only after successful unlocked recognition plus dismissal, repeated lifecycle, locked-screen behavior and reboot recovery are physically recorded.

J2 success does not prove production wake phrase, local LLM, full Mayra conversation or call control.
