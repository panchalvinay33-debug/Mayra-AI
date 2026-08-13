# Mayra AI — Blueprint Addendum: Jarvis / AI-Native Launcher

Date: 2026-08-05
Status: Canonical extension of `docs/MAYRA_BLUEPRINT.md`
Master plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Decision: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`

## Architecture extension

The existing Mayra Blueprint remains valid for core assistant, voice, local brain, memory, documents, notifications, actions, call-role research and trusted distribution. This addendum extends the product shell into an AI-native launcher/Home architecture.

## Layered system

### Layer A — Mayra Home Shell

Responsibilities:

- Android HOME/default launcher role;
- stable Home rendering;
- installed app discovery/search/launch;
- favorites/categories;
- basic clock/device-status/context surfaces;
- Mayra orb/voice entry;
- My Day/context cards;
- owner-visible launcher settings and switch-back route.

Must not depend on:

- local LLM being loaded;
- cloud network/provider availability;
- neural TTS availability;
- Notification Access/Contacts permissions being granted;
- Accessibility service availability.

### Layer B — Presence and conversation

Reuse existing J1/J2/J3 foundations:

- Android Digital Assistant;
- invocation-time on-device recognition;
- Mayra orb/listen/think/speak state;
- offline TTS fallback;
- later dedicated wake phrase.

Launcher and Digital Assistant must coexist without navigation loops.

### Layer C — Context fabric

Typed context providers expose bounded records such as:

- reminder/task context;
- approved memory context;
- notification summary context;
- people/contact context;
- document/library context;
- media/file context where supported;
- current foreground/app/screen context only where supported and explicitly enabled.

Every context item carries provenance/source and freshness metadata. The LLM receives a bounded projection, not raw unrestricted device state.

### Layer D — Brain/planner

- deterministic local command engine remains fallback;
- local LLM provides conversational reasoning where benchmarked;
- optional cloud provider remains booster only;
- planner produces typed proposals, never direct privileged execution.

### Layer E — Trust/action engine

All actions pass through policy:

- GREEN — low-risk, reversible, proven capability;
- AMBER — communication/data modification or uncertain impact; bounded confirmation/review;
- RED — destructive, financial, credential/security, irreversible or protected; explicit confirmation or unsupported.

The action engine, not the launcher or LLM, owns execution truth.

## Launcher process/reliability rules

- Critical Home UI should remain lightweight.
- Heavy model initialization/generation must not block Home rendering.
- If a heavy local-brain process is killed, launcher must continue.
- If Mayra provider/network fails, launcher must continue.
- If context permissions are revoked, only affected cards degrade.
- If Mayra Home crashes/restarts, app drawer/basic navigation must recover.
- Owner must always be able to restore another launcher from Android settings; Mayra should also surface a safe shortcut/instruction where feasible.

## J5 minimum viable launcher

The first J5 milestone intentionally excludes proactive intelligence and advanced automation. It must prove only:

1. Mayra can register/qualify as HOME app.
2. Owner can select Mayra as default launcher.
3. Home button/gesture returns to Mayra.
4. Installed apps appear and launch correctly.
5. Search finds installed apps.
6. Favorites/basic layout persist.
7. Reboot/default-launcher persistence behaves correctly.
8. Mayra orb/assistant entry works from Home.
9. Local model disabled/killed does not break Home.
10. Previous launcher can be restored.

Only after these pass should J6 context cards be connected.

## Later Home surfaces

After J5:

- My Day;
- reminders;
- notification summaries;
- favorite/recent people;
- documents/files/media;
- contextual app suggestions;
- pending items;
- routines;
- multimodal/contextual assistant entry.

Each surface remains separately permissioned and testable.

## Security/privacy boundary

Launcher status grants UI prominence, not unrestricted access. Data access still uses Android permissions/roles/APIs. No protected Android security boundary is assumed bypassed.

Sensitive notification/auth/payment/security content is not broadly surfaced or autonomously acted upon. Owner-private context stays local unless explicitly routed to an enabled provider under existing policy.

## Promotion rule

J5 cannot be promoted until its exact source passes relevant CI/audits and Motorola acceptance, then receives:

- synchronized Roadmap/Blueprint/Idea/Decision/Changelog/Latest Snapshot;
- immutable J5 milestone snapshot;
- protected `baseline/mayra-...-j5-launcher-...` recovery branch.
