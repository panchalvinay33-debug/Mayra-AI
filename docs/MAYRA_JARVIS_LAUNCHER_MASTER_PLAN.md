# Mayra AI — Jarvis / AI-Native Launcher Master Plan

Date accepted: 2026-08-05
Status: CANONICAL PRODUCT DIRECTION
Owner intent: Mayra is a personal-use Android AI companion that should feel like a practical Jarvis-style operating layer, not merely a chat application.

## North star

Mayra becomes the owner's primary Android interaction layer while preserving Android reliability and explicit owner control.

The final experience is built as:

`Mayra Launcher / Home Shell`
→ `Mayra Presence + Voice`
→ `Mayra Brain + Context`
→ `Memory + Documents + Notifications + People`
→ `Planner / Trust Engine`
→ `Typed Action Engine`
→ `Supported Android APIs / roles / narrow reviewed automation`

The launcher is Mayra's home and visual shell. The brain, memory and action runtime remain independent services/modules so a launcher/UI failure never makes the phone unusable and a model failure never gains device authority.

## Product experience target

When Mayra is selected as the default Home app, pressing Home returns to Mayra. The launcher provides:

- all installed apps and search;
- Mayra voice/orb as the primary interaction surface;
- My Day / reminders / calendar-like context cards;
- important notification summaries and pending items;
- favorite/recent people and supported communication handoffs;
- recent media/files where Android permissions and APIs allow;
- contextual app suggestions and routines;
- direct entry to chat, memory, documents and settings;
- explicit fallback/switch-to-previous-launcher control.

Mayra should support natural commands such as:

- "Mayra, aaj kya important hai?"
- "Mayra, mummy ko call karo."
- "Mayra, kal 10 baje yaad dilana."
- "Mayra, is notification ka matlab batao."
- "Mayra, woh file/photo dhoondo jo maine kal dekhi thi" where available through supported data access.

## Jarvis capability pillars

### J5 — AI-native launcher shell

- Android HOME/DEFAULT launcher role and safe fallback.
- App drawer, app search, favorites and categories.
- Mayra orb/voice entry from Home.
- Context cards without blocking basic launcher usability.
- Launcher survives local-model, network, provider and background-runtime failure.

### J6 — Context fabric

- Current time/day/device state.
- Approved personal memory.
- reminders and pending tasks.
- notification-derived context through explicit Notification Access.
- contacts/people through permissioned APIs.
- documents and owner library.
- optional current-screen/app context only through supported Android Assistant/accessibility surfaces and explicit policy.

Context is typed and provenance-aware. Free-form model text is never itself trusted state.

### J7 — Trust and action orchestration

Every requested action is classified before execution:

- GREEN: low-risk reversible actions may execute directly when capability is proven.
- AMBER: communication/data-modifying actions require bounded owner review/confirmation according to policy.
- RED: destructive, financial, credential/security, irreversible or protected actions require explicit confirmation or remain unsupported.

The LLM proposes intent/parameters only. Deterministic policy and typed adapters own execution.

### J8 — Proactive Mayra

- Morning/owner-requested briefing.
- Important-notification prioritization.
- Reminder follow-up.
- Pending-reply/pending-task suggestions where reliable evidence exists.
- Time/place/routine suggestions only from owner-approved context.
- Quiet mode and proactive-notification limits.

No compulsive notification loop and no indefinite raw-data hoarding.

### J9 — Multimodal Mayra

- Camera/image understanding when explicitly invoked.
- document/image grounding.
- voice + screen + text continuity.
- future local vision model only after device RAM/thermal/privacy benchmark.

### J10 — Personal routines

- deterministic owner-defined routines;
- reusable typed workflows;
- explicit triggers and stop controls;
- narrow Accessibility automation only when official APIs/intents cannot fulfill a reviewed workflow and when authentication/payment/security flows are excluded.

## Always-available interaction

Priority order:

1. default Mayra Launcher Home surface;
2. Android Digital Assistant / Power-button invocation;
3. dedicated local wake-word engine after device battery/false-trigger proof;
4. app/chat entry as fallback.

No permanent unrestricted microphone loop is assumed.

## Reliability architecture

The launcher must be lightweight and independently usable. Heavy AI runs outside the critical Home rendering path.

Required failure behavior:

- local model missing/corrupt → deterministic Mayra + launcher remain usable;
- neural TTS unavailable → Android offline TTS fallback;
- network/provider unavailable → local path;
- notification/contacts permission missing → feature card degrades cleanly;
- action adapter failure → no false success claim;
- launcher crash/restart → Home recovers without data corruption;
- owner can switch back to another launcher.

## Privacy and ownership

- local-first by default;
- owner-private data is not silently uploaded;
- memory remains inspectable/editable/forgettable/resettable;
- notifications are filtered and not retained indefinitely by default;
- secrets, OTPs, banking/auth/security content are excluded from autonomous handling;
- action history should be auditable without storing unnecessary sensitive payloads;
- device behavior claims require Motorola evidence.

## Major-step baseline discipline

Every major capability follows this lifecycle:

1. Idea Ledger entry.
2. Architecture/decision record.
3. Blueprint update.
4. Roadmap slice with explicit gate.
5. Preflight / test matrix where needed.
6. Coherent implementation batch.
7. Automated CI and package/permission/component audits.
8. Motorola evidence for device claims.
9. Changelog + Latest Snapshot synchronization.
10. Immutable milestone snapshot.
11. Protected `baseline/*` branch only from an exact green commit.
12. Next risky phase starts only from a known recovery point.

A red/pending head may receive failure analysis or documentation corrections, but it is never called a stable baseline and no speculative feature stack should be built on top of it.

## Immediate execution order

1. Repair current J4 CI failure and restore exact-head green.
2. Complete J4 local-brain quality/RAM/thermal/cancel regression gate.
3. Promote a protected J4 green recovery baseline when all applicable checks pass.
4. Add J5 launcher feasibility/preflight and isolated launcher engineering package/surface.
5. Prove HOME selection, Home-button return, reboot persistence, app drawer, fallback and crash recovery on Motorola.
6. Connect existing Mayra orb/voice to launcher without moving heavy AI into Home rendering.
7. Add context cards incrementally: reminders → notifications → people → documents/media.
8. Add trust-level action orchestration and audit history.
9. Add proactive briefing/routines only after context quality and battery/privacy evidence.
10. Add wake-word and multimodal tracks through their own benchmark gates.

## Definition of success

Mayra is successful when the owner can use the phone primarily through Mayra Home + voice, while basic phone use remains reliable even when AI components fail, and every privileged action remains deterministic, auditable and owner-controlled.
