# Mayra AI — START HERE

> **This is the first document to read whenever work on Mayra starts or resumes.**
>
> It is the navigation and recovery entry point for the entire project. Detailed truth lives in the linked canonical records below.

Last synchronized: **2026-08-03**
Current development branch: **`agent/document-library-foundation`**
Current pull request: **#12 — Draft, open, unmerged**
Current app version: **0.2.1 / versionCode 4**
Last fully verified pre-Jarvis milestone: **Android CI #1795 — green**
Protected green baseline: **`baseline/mayra-0.2.1-green-1795`** at `065e22524c835f3ddd3b2f56215a3616f071d4b3`
Current active phase: **Mayra Jarvis Mode / Android Assistant-role foundation**

## 1. Product north star

Mayra is a personal Android AI companion for the owner’s Motorola device—not merely a chat screen.

The intended experience is:

- natural Hindi, Hinglish and English conversation;
- an on-device local brain that keeps Mayra useful without an API key;
- optional cloud providers as a stronger fallback, never as Mayra’s identity;
- owner-approved memory and private document intelligence;
- reminders, apps, contacts and supported phone actions;
- an animated voice presence when the display is active;
- voice-only/background operation when the display is off;
- Android Assistant-role integration for reliable system-supported invocation;
- optional default Phone/Call Screening roles for advanced incoming-call control;
- one user-facing app and one launcher icon.

Movie-level unrestricted Jarvis behavior is not promised. The engineering goal is the maximum reliable personal-assistant experience permitted by the owner’s Android device and chosen system roles.

## 2. Read these records in this order

1. **`START_HERE.md`** — current entry point and resume instructions.
2. **`docs/MAYRA_PINPOINT_AUDIT.md`** — whole-project source/test/package/device gap register.
3. **`docs/MAYRA_BLUEPRINT.md`** — long-lived architecture, product boundaries and module design.
4. **`docs/MAYRA_ROADMAP.md`** — current work status, completed milestones and next ordered gates.
5. **`docs/backups/MAYRA_LATEST_SNAPSHOT.md`** — exact rolling recovery state: head, CI, artifacts, risks and next action.
6. **`docs/MAYRA_TEST_MATRIX.md`** — required automated, packaging and Motorola evidence for every subsystem.
7. **`docs/MAYRA_BASELINE_AND_ROLLBACK.md`** — protected baselines, promotion rules and recovery procedure.
8. **`docs/MAYRA_IDEA_LEDGER.md`** — accepted, changed, deferred and removed ideas.
9. **`docs/MAYRA_DECISIONS.md`** — architecture and product decision log with supersession history.
10. **`docs/MAYRA_CHANGELOG.md`** — user-visible and engineering milestone history.
11. **`docs/MAYRA_FULL_APP_ACCEPTANCE.md`** — Motorola physical-device acceptance checklist.
12. **`docs/BLUEPRINT_UPDATE_POLICY.md`** — mandatory governance and documentation-update rules.

Do not rely only on chat history, a PR description or an old APK. Those are supporting records, not the source of truth.

## 3. Current implemented capability summary

### Core and conversation

- Kotlin/Jetpack Compose Android app with one launcher.
- Typed routing for conversational answers, document retrieval and controlled actions.
- Hindi/Hinglish/English greetings, common offline commands and contextual fallback.
- Voice input and text-to-speech foundation.
- Optional OpenAI Responses-compatible HTTPS provider.
- Android Keystore-protected provider credential storage.
- Live provider enable/disable/key replacement without restarting Mayra.
- Offline fallback when the remote provider is disabled or unavailable.

### Personal intelligence

- Explicit personal-memory proposal and approval flow.
- Local memory retrieval, edit, replace, expiry and delete controls.
- Trusted structured memory-use metadata; documents/providers cannot spoof memory attribution through visible text.
- TXT, PDF and DOCX import/index/search/summary/grounded-answer foundation.
- Document freshness and health tooling.

### Device actions

- Open installed applications.
- Resolve contacts for calls/messages.
- Review-first Android dialer and message-composer handoffs.
- No silent direct-call or direct-SMS privilege in the current production boundary.
- One-time, action-bound, expiring confirmation tokens.
- Activity History and Device Readiness surfaces.

### Reminders

- Hindi/Hinglish/English reminder parsing.
- Persistent Mayra-owned reminder store.
- WorkManager scheduling.
- Notification actions: Complete and Snooze 10 minutes.
- Follow-up notifications and missed state.
- Reboot/app-update recovery with remaining follow-up delay.
- Revision checks to prevent stale workers or notification actions from changing newer reminder state.

### Packaging and release engineering

- Personal Alpha owner-device package: `ai.mayra.app.alpha`.
- Safe Full Test package: `ai.mayra.app.fulltest`.
- Isolated zero-permission Document Test package.
- Minified/R8 final release candidate audit for `ai.mayra.app`.
- Environment-only production signing scaffold; no signing secrets in source control.
- Automated manifest, permission, component and one-launcher audits.

### Jarvis Mode currently in progress

- Android `VoiceInteractionService` foundation.
- Voice interaction session service and animated assistant-session UI foundation.
- Recognition-service shell and assistant metadata.
- Lock-screen assistant-session declaration.
- Assistant components excluded from the low-permission Full Test variant.

These Jarvis Mode items are **not yet device-verified** and must not be described as fully working until CI and Motorola acceptance are complete.

## 4. Major remaining work

Priority order is controlled by the roadmap, but the broad remaining program is:

1. Complete and validate Android Assistant-role selection and invocation.
2. Add a real offline wake-word pipeline with battery/thermal controls and explicit owner setup.
3. Integrate and benchmark an on-device local language model suitable for the Motorola target.
4. Connect the animated listening/thinking/speaking state to the real voice pipeline.
5. Build optional default Phone/Call Screening role modules for incoming-call announce, answer, reject, silence, mute and speaker control.
6. Design a lawful/reliable AI answering-message path; standard cellular call audio injection/recording cannot be assumed.
7. Improve proactive context, notification summaries and owner-defined automation rules.
8. Complete Motorola physical testing across provider, voice, memory, documents, reminders, reboot recovery and device actions.
9. Configure private release signing and Play/Internal or owner-controlled distribution.
10. Final branding, onboarding, battery guidance, accessibility and crash/performance hardening.

Deferred unless explicitly promoted:

- scanned-image OCR;
- legacy binary `.doc` parsing;
- unrestricted cross-app UI automation;
- root-only phone control;
- hidden cellular call recording or protected call-audio injection.

## 5. Mandatory resume procedure

Every new work session—human or AI—must do this before coding:

1. Read this file.
2. Read `docs/MAYRA_PINPOINT_AUDIT.md` and `docs/backups/MAYRA_LATEST_SNAPSHOT.md`.
3. Read the active section of `docs/MAYRA_ROADMAP.md`.
4. Check PR #12 head, Draft/open/unmerged state and latest-head CI/Governance.
5. Confirm the latest code does not contradict the blueprint, decision log or test matrix.
6. Identify one coherent batch, its required tests and rollback point.
7. Never expand a red head; repair or revert first.
8. Never claim physical-device success without owner-device evidence.
9. Never merge or mark the PR ready without explicit owner approval.

## 6. Mandatory completion procedure

A coding batch is not complete until all applicable records are synchronized:

- `docs/MAYRA_ROADMAP.md` — status and next gate;
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` — exact recovery state;
- `docs/MAYRA_PINPOINT_AUDIT.md` — changed subsystem evidence/gap;
- `docs/MAYRA_TEST_MATRIX.md` — when a new test category or acceptance condition appears;
- `docs/MAYRA_BLUEPRINT.md` — architecture/scope/privacy changes;
- `docs/MAYRA_IDEA_LEDGER.md` — new, changed, deferred or removed ideas;
- `docs/MAYRA_DECISIONS.md` — significant decisions and supersessions;
- `docs/MAYRA_CHANGELOG.md` — meaningful delivered behavior;
- `START_HERE.md` — when project entry truth/document map changes;
- protected baseline + immutable snapshot — after major exact-head green milestones;
- PR description — when scope/milestone truth materially changes.

The governance workflow must remain green. A stale-document failure is a real project failure, not optional paperwork.

## 7. Status vocabulary

- **DONE** — implementation and relevant automated checks passed.
- **DEVICE_VERIFY** — code/CI passed; Motorola physical validation pending.
- **IN_PROGRESS** — actively being implemented or latest-head validation pending.
- **PLANNED** — accepted but not implemented.
- **DEFERRED** — intentionally postponed and non-blocking.
- **REMOVED** — intentionally removed or superseded; reason recorded.

## 8. Safety and ownership truth

Mayra is for personal owner use, so the project may pursue deeper Android roles and owner-granted access than a generic public app. Personal use does not remove Android platform boundaries or make destructive mistakes harmless.

The design preference is:

- maximum owner control;
- official Android roles before fragile hacks;
- no hidden secrets in GitHub;
- no false claims about actions, calls, recordings or device validation;
- confirmations can be reduced for routine owner-approved actions, but irreversible or broad destructive operations retain a clear guard;
- the owner can always disable background, provider, memory or privileged-role behavior.

## 9. Backup model

- Git history is the primary code/document backup.
- `baseline/*` branches are immutable known-green recovery markers, never development branches.
- `docs/backups/MAYRA_LATEST_SNAPSHOT.md` is the rolling recovery point.
- Immutable milestone snapshots live under `docs/backups/`.
- CI artifacts are temporary evidence and must be recorded with source/run/digest before becoming an install candidate.
- No API keys, keystores, passwords, tokens or private owner data belong in snapshots or Git history.

## 10. Current immediate next action

Validate the repaired Jarvis Assistant-role foundation through compile, complete tests, lint and all APK audits. Keep the failed CI #1833 recorded as evidence. Promote a new protected Jarvis baseline only after both Android CI and Project Governance pass on the exact same source head. Then perform Motorola Assistant-role selection/invocation testing before wake-word, local-model or Phone-role expansion.
