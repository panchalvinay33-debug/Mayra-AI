# Mayra AI — START HERE

> **This is the first document to read whenever work on Mayra starts or resumes.**
>
> It is the navigation and recovery entry point for the entire project. Detailed truth lives in the linked canonical records below.

Last synchronized: **2026-08-03**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Current active phase: **J1 Android Assistant device proof achieved; J2 invocation-time voice foundation under fresh CI**
Last stable full-app baseline: **`baseline/mayra-0.2.1-green-1795`** at `065e22524c835f3ddd3b2f56215a3616f071d4b3`
Protected J1 zero-permission baseline: **`baseline/mayra-0.2.1-j1-zero-permission-green-44`** at `a8a7a1dc338a1474cb9bc0f32de55f6c3b834976`

## 1. Product north star

Mayra is the owner’s personal Android AI companion, not merely a chat screen.

Target experience:

- natural Hindi/Hinglish/English conversation;
- a future on-device local conversational brain so Mayra is not dependent on OpenAI/API access;
- optional cloud providers as boosters only;
- owner-approved personal memory and private document intelligence;
- reminders, apps, contacts and supported device actions;
- Android Digital Assistant integration for system-supported availability;
- animated listening/thinking/speaking presence while interacting;
- voice/background behavior when Android permits it;
- future optional default Phone/Call Screening role for supported incoming-call control;
- one final user-facing Mayra app and one launcher.

Engineering goal: maximum reliable behavior on the owner’s Motorola Edge 70 Fusion using supported Android roles/APIs. Unsupported protected capabilities are not claimed.

## 2. Read these records in this order

1. **`START_HERE.md`** — entry point and resume rules.
2. **`docs/MAYRA_PINPOINT_AUDIT.md`** — whole-project gap/evidence register.
3. **`docs/MAYRA_BLUEPRINT.md`** — architecture and platform boundaries.
4. **`docs/MAYRA_ROADMAP.md`** — active ordered work.
5. **`docs/backups/MAYRA_LATEST_SNAPSHOT.md`** — rolling recovery truth.
6. **`docs/MAYRA_TEST_MATRIX.md`** — evidence ladder and test requirements.
7. **`docs/MAYRA_BASELINE_AND_ROLLBACK.md`** — protected baselines and recovery procedure.
8. **`docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md`** — real Assistant-role Motorola evidence.
9. **`docs/feasibility/MAYRA_J2_VOICE_PREFLIGHT.md`** — current voice-phase feasibility gate.
10. **`docs/MAYRA_IDEA_LEDGER.md`** — accepted/deferred/superseded ideas.
11. **`docs/MAYRA_DECISIONS.md`** — architecture decisions and reasons.
12. **`docs/MAYRA_CHANGELOG.md`** — milestone history.
13. **`docs/MAYRA_FULL_APP_ACCEPTANCE.md`** — final Motorola app acceptance.
14. **`docs/BLUEPRINT_UPDATE_POLICY.md`** — documentation/governance contract.

Never reconstruct project truth from chat history or an old APK when these records exist.

## 3. Current implemented capability summary

### Core final-app foundations

- Kotlin/Jetpack Compose app with one final launcher.
- Typed routing for conversation, documents and controlled actions.
- Hindi/Hinglish/English local commands/greetings and contextual fallback.
- Existing app-level voice input and TTS foundations.
- Optional Responses-compatible HTTPS provider with Android Keystore-protected credentials and local fallback.
- Owner-controlled personal memory lifecycle.
- TXT/PDF/DOCX Library intelligence.
- Persistent reminders, Complete/Snooze/follow-up and reboot recovery.
- App opening, contact resolution and review-first dialer/message handoffs.
- Minified/R8 final release audit and protected-secret signing scaffolds.

### Android Assistant / J1 — physically proven on Motorola

J1 package: `ai.mayra.app.j1`, zero requested Android permissions.

Physical evidence now proves:

- J1 installs/launches;
- Android 16 recognizes Mayra as a Digital assistant candidate;
- owner can select Mayra as default Digital assistant;
- Motorola Power-button Digital assistant action invokes Mayra while unlocked;
- Mayra’s animated orb/session renders over the current screen;
- Back gesture dismisses the current session;
- locking the phone dismisses the current session.

One device UX bug was found: tested J1 #68 had no direct orb/outside tap dismissal. Common source repair now adds root/orb/label tap-to-hide plus explicit Back-to-hide and lifecycle cleanup. Fresh CI/device retest is pending.

Still pending for complete J1 proof: repeated invoke/dismiss cycles, invocation beginning while already locked, reboot role recovery and restored previous-assistant behavior.

### J2 invocation-time voice — active implementation

J2 package: `ai.mayra.app.j2`, engineering-only.

Purpose: prove a short real spoken request after explicit Mayra invocation while preserving J1 as a clean zero-permission rollback proof.

Current J2 foundation:

- exactly `RECORD_AUDIO` intended as the only Android permission;
- minimal setup/status screen;
- same official Assistant/session foundation;
- bounded voice state model;
- invocation-time on-device `SpeechRecognizer` only when Android reports it available;
- no continuous recognition/hotword loop;
- recognition stops on hide/cancel/destroy;
- dedicated J2 CI for compile/unit/lint/APK/permission/component audit.

J2 does not yet claim a local conversational brain. First proof is simply: invoke Mayra → speak → see `Listening…` / `Heard: …` honestly.

## 4. Major remaining work

Ordered broad program:

1. Finish current exact-head J1/J2/Android/Governance validation.
2. Run Motorola J2 real speech and repaired touch/session lifecycle tests.
3. Finish locked-screen, repeated-cycle and reboot Assistant-role evidence.
4. Complete a dedicated offline wake-word feasibility/benchmark; do not turn SpeechRecognizer into a permanent listener.
5. Select and benchmark an on-device local LLM suitable for the Edge 70 Fusion RAM/storage/thermal budget.
6. Connect recognized voice → Mayra local brain → response → TTS and animated listening/thinking/speaking states.
7. Complete stable private owner signing and trusted install/update channel.
8. Run full-app Motorola acceptance across provider, memory, documents, reminders and device actions.
9. Only after dedicated preflight, build optional default Phone/InCallService/Call Screening support.
10. For true AI caller message-taking, design a supported voicemail/VoIP/call-forwarding route rather than assuming protected SIM audio access.

Deferred unless explicitly promoted: scanned OCR, legacy `.doc`, root-only control, generic autonomous Accessibility tapping and hidden cellular-call recording/audio injection.

## 5. Mandatory resume procedure

Every new work session—human or AI—must do this before coding:

1. Read this file.
2. Read `docs/MAYRA_PINPOINT_AUDIT.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`.
3. Read the active section of `docs/MAYRA_ROADMAP.md` and any linked feasibility/test document.
4. Check PR #12 head, Draft/open/unmerged state and all latest-head CI/Governance workflows.
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
- `START_HERE.md` when the project entry truth changes;
- protected baseline + immutable snapshot after major exact-head green milestones;
- PR description when scope/milestone truth materially changes.

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
- request only permissions needed for the active product capability;
- official roles/APIs first;
- no secrets/private keys in GitHub;
- no false call/audio/device claims;
- routine owner-approved actions can be streamlined, while broad destructive actions retain bounded guards;
- owner can disable provider, memory and privileged roles.

## 9. Backup model

- Git history is primary code/document backup.
- `baseline/*` branches are immutable known-green recovery markers, never development branches.
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is the rolling recovery point.
- Immutable milestone snapshots live under `docs/backups/`.
- CI artifacts are temporary evidence; source/run/digest must be recorded before promotion.
- No credentials, signing keys/passwords or private owner data belong in snapshots/Git.

## 10. Current immediate next action

Settle the current touch-dismiss + J2 voice batch through **J1 Assistant Test, J2 Voice Test, full Android CI and Project Governance on the same synchronized head**. Fix any compiler/lint/package finding without weakening audits. When all required gates are green, create a protected baseline, produce the J2 Motorola artifact with provenance, then physically test microphone permission, on-device speech availability, invocation-time recognition, tap/Back/lock dismissal, repeat cycles, locked-screen behavior and reboot role recovery before starting wake-word or local-LLM implementation.
