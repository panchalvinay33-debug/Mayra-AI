# Mayra AI — START HERE

> **This is the first document to read whenever work on Mayra starts or resumes.**
>
> It is the navigation and recovery entry point for the entire project. Detailed truth lives in the linked canonical records below.

Last synchronized: **2026-08-03**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Current active phase: **J1 Android Assistant proof achieved; J2 invocation-time voice is exact-head green and ready for Motorola device acceptance**
Last stable full-app baseline: **`baseline/mayra-0.2.1-green-1795`** at `065e22524c835f3ddd3b2f56215a3616f071d4b3`
Current voice baseline: **`baseline/mayra-0.2.1-j2-voice-green-18`** at `ef809bbdaca80f3b953483499dc03de8e091339f`

## 1. Product north star

Mayra is the owner’s personal Android AI companion, not merely a chat screen.

Target experience:

- natural Hindi/Hinglish/English conversation;
- an on-device local conversational brain so Mayra is useful without OpenAI/API access;
- optional cloud providers as boosters only;
- owner-approved memory and private document intelligence;
- reminders, apps, contacts and supported device actions;
- Android Digital Assistant integration for system-supported availability;
- animated listening/thinking/speaking presence;
- voice/background behavior where Android permits it;
- future optional Phone/Call Screening roles for supported incoming-call control;
- one final user-facing Mayra app and one launcher.

Engineering goal: maximum reliable behavior on the owner’s Motorola Edge 70 Fusion through supported Android roles/APIs. Unsupported protected capabilities are not claimed.

## 2. Read these records in this order

1. **`START_HERE.md`** — entry point and resume rules.
2. **`docs/MAYRA_PINPOINT_AUDIT.md`** — whole-project gap/evidence register.
3. **`docs/MAYRA_BLUEPRINT.md`** — architecture and platform boundaries.
4. **`docs/MAYRA_ROADMAP.md`** — active ordered work.
5. **`docs/backups/MAYRA_LATEST_SNAPSHOT.md`** — rolling recovery truth.
6. **`docs/MAYRA_TEST_MATRIX.md`** — evidence ladder and test requirements.
7. **`docs/MAYRA_BASELINE_AND_ROLLBACK.md`** — protected baselines and recovery procedure.
8. **`docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`** — real Assistant-role Motorola evidence.
9. **`docs/feasibility/MAYRA_J2_VOICE_PREFLIGHT.md`** — voice-phase feasibility gate.
10. **`docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md`** — exact next physical voice checklist/artifact provenance.
11. **`docs/backups/MAYRA_SNAPSHOT_2026-08-03_J2_VOICE_CI18.md`** — immutable J2 code milestone.
12. **`docs/MAYRA_IDEA_LEDGER.md`** — accepted/deferred/superseded ideas.
13. **`docs/MAYRA_DECISIONS.md`** — architecture decisions and reasons.
14. **`docs/MAYRA_CHANGELOG.md`** — milestone history.
15. **`docs/MAYRA_FULL_APP_ACCEPTANCE.md`** — final Motorola app acceptance.
16. **`docs/BLUEPRINT_UPDATE_POLICY.md`** — documentation/governance contract.

Never reconstruct project truth from chat history or an old APK when these records exist.

## 3. Current implemented capability summary

### Core final-app foundations

- Kotlin/Jetpack Compose app with one final launcher.
- Typed routing for conversation, documents and controlled actions.
- Hindi/Hinglish/English local commands/greetings and contextual fallback.
- Existing app-level voice input and TTS foundations.
- Optional Responses-compatible HTTPS provider with Android Keystore-protected credentials and local fallback.
- Owner-controlled personal-memory lifecycle.
- TXT/PDF/DOCX Library intelligence.
- Persistent reminders with Complete/Snooze/follow-up and reboot recovery.
- App opening, contact resolution and review-first dialer/message handoffs.
- Minified/R8 final release audit and protected-secret signing scaffolds.

### Android Assistant / J1 — physically proven on Motorola

J1 package: `ai.mayra.app.j1`, zero requested Android permissions.

Physical evidence proves:

- J1 installs/launches;
- Android 16 recognizes Mayra as a Digital assistant candidate;
- owner can select Mayra as default Digital assistant;
- Motorola Power-button Digital assistant action invokes Mayra while unlocked;
- Mayra animated orb/session renders over the current screen;
- Back dismisses the current session;
- locking the phone dismisses the current session.

J1 #68 exposed a real UX bug: direct orb/outside tap did not dismiss because click listeners were absent. The common assistant-session repair is now included in the green J2 baseline and still needs physical retest.

### J2 invocation-time voice — exact-head green

J2 package: `ai.mayra.app.j2`, engineering-only.

Purpose: prove a short real spoken request after explicit Mayra invocation while preserving J1 as a clean zero-permission proof.

Verified source baseline:

- `ef809bbdaca80f3b953483499dc03de8e091339f`
- J2 Voice Test #18 — success
- J1 Assistant Test #122 — success
- Android CI #2013 — success
- Project Governance #194 — success
- protected branch `baseline/mayra-0.2.1-j2-voice-green-18`

J2 contains:

- exactly `RECORD_AUDIO` permission;
- minimal setup/status screen;
- same official Assistant/session foundation;
- bounded voice state model;
- invocation-time on-device Android SpeechRecognizer only when Android reports support;
- visible preparing/listening/result/error states;
- recognition stop on hide/destroy;
- repaired orb/root/label/Back dismissal;
- dedicated package/permission/component CI audit.

J2 intentionally excludes internet/provider, contacts, notifications, reminders, WorkManager/Room, memory, documents, full chat and calls.

It does **not** claim a wake phrase or local conversational brain. First device proof is simply: invoke Mayra → speak → see an honest `Listening…` / `Heard: …` result.

## 4. Major remaining work

Ordered broad program:

1. Run the exact J2 Motorola acceptance checklist.
2. Verify microphone permission, on-device speech availability, Hindi/Hinglish/English recognition and repaired direct tap dismissal.
3. Run 20 invoke/listen/dismiss cycles, locked-screen start and reboot recovery.
4. Complete dedicated wake-word feasibility and battery/false-trigger benchmark; do not turn SpeechRecognizer into a permanent listener.
5. Select and benchmark an on-device local LLM suitable for Edge 70 Fusion RAM/storage/thermal limits.
6. Connect recognized voice → Mayra local brain → response → TTS and listening/thinking/speaking animation.
7. Complete stable private owner signing and trusted install/update channel.
8. Run full-app Motorola acceptance across provider, memory, documents, reminders and device actions.
9. Only after dedicated preflight, build optional default Phone/InCallService/Call Screening support.
10. For true AI caller message-taking, use a supported voicemail/VoIP/call-forwarding design rather than assuming protected SIM audio access.

Deferred unless explicitly promoted: scanned OCR, legacy `.doc`, root-only control, generic autonomous Accessibility tapping and hidden cellular-call recording/audio injection.

## 5. Mandatory resume procedure

Every new work session—human or AI—must do this before coding:

1. Read this file.
2. Read `docs/MAYRA_PINPOINT_AUDIT.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`.
3. Read the active section of `docs/MAYRA_ROADMAP.md` and linked feasibility/test documents.
4. Check PR #12 head, Draft/open/unmerged state and latest-head CI/Governance.
5. Confirm code, blueprint, decisions and physical-device evidence agree.
6. Identify one coherent batch, required tests and rollback baseline.
7. Never expand a red head; repair or revert first.
8. Never claim Motorola/device success without owner evidence.
9. Never merge or mark PR #12 ready without explicit owner approval.

## 6. Mandatory completion procedure

A coding batch is not complete until applicable records are synchronized:

- `docs/MAYRA_ROADMAP.md`;
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md`;
- `docs/MAYRA_PINPOINT_AUDIT.md`;
- `docs/MAYRA_TEST_MATRIX.md` when evidence requirements change;
- `docs/MAYRA_BLUEPRINT.md` for architecture/scope/privacy changes;
- `docs/MAYRA_IDEA_LEDGER.md` for idea state changes;
- `docs/MAYRA_DECISIONS.md` for significant technical/product decisions;
- `docs/MAYRA_CHANGELOG.md` for meaningful behavior/build changes;
- `START_HERE.md` when project-entry truth changes;
- protected baseline + immutable snapshot after major exact-head green milestones;
- PR description when milestone truth materially changes.

Governance CI must remain green. Stale project records are treated as a real failure.

## 7. Status vocabulary

- **DONE** — implementation and applicable automated checks passed.
- **DEVICE_VERIFY** — source/CI exists; Motorola proof is incomplete or partial.
- **IN_PROGRESS** — active implementation/latest-head validation pending.
- **PLANNED** — accepted but not implemented.
- **DEFERRED** — intentionally postponed.
- **REMOVED/SUPERSEDED** — intentionally retired; reason retained.

## 8. Safety and ownership truth

Mayra is personal-owner software, so the project can pursue deeper owner-granted Android roles than a generic public app. Personal use does not remove Android platform boundaries.

Design preference:

- maximum owner control with minimal repeated setup;
- request only permissions needed for the active capability;
- official roles/APIs first;
- no secrets/private keys in GitHub;
- no false call/audio/device claims;
- routine owner-approved actions can be streamlined while broad destructive actions retain bounded guards;
- owner can disable provider, memory and privileged roles.

## 9. Backup model

- Git history is primary code/document backup.
- `baseline/*` branches are immutable known-green recovery markers, never development branches.
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is the rolling recovery point.
- Immutable milestone snapshots live under `docs/backups/`.
- CI artifacts are temporary evidence; source/run/digest must be recorded before promotion.
- No credentials, signing keys/passwords or private owner data belong in snapshots/Git.

## 10. Current immediate next action

Use the exact J2 CI #18 candidate recorded in `docs/testing/MAYRA_J2_MOTOROLA_ACCEPTANCE.md` on the Motorola. Test installation, microphone consent, Digital Assistant selection, on-device recognition, short Hindi/Hinglish/English phrases, repaired tap/Back/lock dismissal, 20-cycle stability, locked-screen invocation and reboot recovery. Record every result before promoting J2 to device-verified or connecting the full local brain/wake-word stack.
