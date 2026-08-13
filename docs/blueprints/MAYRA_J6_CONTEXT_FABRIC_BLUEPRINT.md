# Mayra AI — J6 Context Fabric Blueprint

Date: 2026-08-05
Status: IMPLEMENTATION READY
Baseline: `baseline/mayra-0.2.2-j5-owner-green`

## Goal

Give Mayra a small, typed, privacy-aware understanding of the current phone situation so Home and the assistant can become useful and proactive without making Home depend on AI/cloud/model availability.

## Context layers

1. **Device context** — local time/day, network availability, battery/charging state, foreground Mayra surface state where observable.
2. **User-authorized personal context** — contacts, reminders, notification summaries and other explicit-permission sources.
3. **Session context** — current Mayra conversation/session entry source and recent owner-approved interactions.
4. **Derived context** — bounded deterministic facts such as `offline`, `charging`, `morning`, `hasUpcomingReminder`; derived facts must preserve source/provenance.

## Architecture

`ContextSource -> ContextSignal -> ContextSnapshot -> ContextRepository -> Home/Assistant cards -> optional AI prompt context`

The LLM never becomes the source of truth. It may consume an already-normalized snapshot but cannot silently write trusted context facts.

## Safety / privacy rules

- Home shell must render if every context source fails.
- No Accessibility requirement for J6 foundation.
- No hidden microphone recording.
- No reading private app databases.
- Notification content is only available after explicit Notification Access.
- Contacts are only available after explicit Contacts permission.
- Missing permission must produce `Unavailable/NotGranted`, never a crash.
- Raw sensitive content is not copied into long-term memory merely because it appears in context.
- Every future action generated from context still passes through the Trust/Action engine.

## First implementation slice

- typed `MayraContextSnapshot` and signal provenance;
- deterministic local time/day-part;
- connectivity state using already-declared network state permission;
- battery/charging state;
- Home `Now` card showing only low-risk device context;
- context collection independent of localbrain/cloud provider;
- unit tests for derived day-part and unavailable-source behavior.

## Later J6 slices

- reminder context;
- notification intelligence summary after explicit access;
- people/contact context after permission;
- recent-app/session hints that do not scrape private app data;
- proactive card ranking;
- optional prompt-context adapter for Mayra Brain.

## Acceptance

J6 foundation is accepted only when:

- Home remains usable with airplane mode, denied contacts, denied notifications and unavailable AI;
- context card does not block app drawer/search/launch;
- source failures degrade to neutral text;
- no new privileged permission is silently requested;
- Android/J1/J2/J3/J4/Owner/Governance gates remain green;
- Motorola verifies the card reflects obvious state changes without launcher regression.
