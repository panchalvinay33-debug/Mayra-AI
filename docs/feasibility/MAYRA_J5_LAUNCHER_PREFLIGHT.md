# Mayra AI — J5 AI-Native Launcher Preflight

Date: 2026-08-05
Status: ACCEPTED / IMPLEMENTATION BLOCKED UNTIL J4 EXACT-HEAD GREEN

## Goal

Prove that Mayra can become the owner's stable default Android Home/launcher while keeping all ordinary apps reachable and keeping Home usable when AI components fail.

## Preconditions

J5 implementation does not start until:

- current J4 Room/KSP CI failure is repaired;
- relevant J4/J1/J2/J3/Android/Governance workflows are exact-head green;
- J4 recovery point is recorded;
- no unresolved package/signing issue would invalidate device testing.

## Android integration target

The J5 engineering candidate will declare the standard Android HOME launcher intent/category boundary and use supported package-manager APIs to enumerate launchable applications.

No security bypass is required or assumed. Launcher status does not grant access to private app data.

## First implementation boundary

Include only:

- launcher HOME activity/surface;
- installed launchable-app inventory;
- app search;
- launch app action;
- favorites/basic persistent layout;
- Mayra voice/orb entry;
- settings/switch-back help;
- deterministic fallback UI when AI runtime is unavailable.

Exclude from J5 MVP:

- unrestricted Accessibility automation;
- payments/authentication/security automation;
- proactive notification intelligence;
- broad photo/video/contact cards;
- always-on wake word;
- Phone-role takeover;
- multimodal vision;
- autonomous routines.

Those are separate later gates.

## Reliability requirements

J5 must be usable in all of these conditions:

- no network;
- cloud provider disabled;
- local model absent;
- local model corrupt;
- local brain process killed;
- Notification Access denied;
- Contacts denied;
- neural TTS unavailable;
- app process restarted.

Basic app drawer/search/launch must remain available.

## Validation matrix

### CI

- compile/lint/unit tests for launcher surface;
- manifest audit proves correct HOME intent and expected launcher count;
- no accidental new high-risk permissions;
- installed-app inventory/search tests;
- fallback-state tests;
- app launch intent tests;
- persistence tests for favorites/layout where practical;
- shared Android/J1/J2/J3/J4 regressions remain green.

### Motorola

- Mayra appears in Home app/default launcher selection;
- selecting Mayra succeeds;
- Home gesture/button returns to Mayra repeatedly;
- installed apps are visible/searchable and launch;
- reboot preserves expected Home behavior;
- lock/unlock does not trap the owner;
- local model disabled/killed leaves Home functional;
- Mayra assistant invocation from Home works;
- previous launcher can be restored;
- no navigation loop between launcher and Digital Assistant;
- idle RAM/battery/thermal behavior is acceptable.

## Rollback

- Keep previous launcher installed and documented.
- Never remove Android's ability to choose another Home app.
- Keep last protected green Mayra application baseline unchanged.
- J5 engineering package/surface must not become the sole holder of owner-private data.

## Promotion

Only after exact-source CI + Motorola acceptance:

1. update canonical docs;
2. create immutable J5 milestone snapshot;
3. create protected J5 baseline branch;
4. then begin J6 context integration.
