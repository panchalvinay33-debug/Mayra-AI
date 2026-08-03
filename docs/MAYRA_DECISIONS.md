# Mayra AI — Decision Log

Last updated: 2026-08-03

This log preserves important product and architecture decisions. Newer entries may supersede older ones, but history is not deleted.

## Decision format

Each entry records: decision, reason, consequences, validation state and supersession status.

---

## ADR-001 — One user-facing application

**Status:** Accepted

Mayra must present one launcher icon and one coherent app. Library, Memory, Provider, History and Device controls remain internal screens. Separate packages may exist only for CI/device testing.

---

## ADR-002 — Local-first, cloud-optional intelligence

**Status:** Accepted; local LLM pending

Mayra must retain offline commands, memory, documents, reminders and device actions without an API key. Cloud providers are optional intelligence boosters.

---

## ADR-003 — Remote providers are text-only

**Status:** Implemented

A remote conversational provider may produce response text but cannot directly execute device actions or write personal memory.

---

## ADR-004 — Trusted memory attribution is structured metadata

**Status:** Implemented; supersedes visible marker design

Memory keys used in a response travel through `MayraAssistantResponse.usedPersonalMemoryKeys`, not hidden strings embedded in visible text.

---

## ADR-005 — Review-first call and message handoffs

**Status:** Implemented for current action layer

Outgoing call and message commands open Android’s dialer/composer after resolution and confirmation. Advanced incoming-call control will use official Phone roles.

---

## ADR-006 — Mayra-owned reminders use WorkManager

**Status:** Implemented

Mayra persists reminders locally and schedules through WorkManager, with Complete, Snooze, follow-up and reboot/update recovery.

---

## ADR-007 — Confirmation tokens are exact, expiring and process-local

**Status:** Implemented

Sensitive actions use one-time tokens bound to the exact action and short expiry. Raw tokens are not persisted across process death.

---

## ADR-008 — Provider credentials use Android Keystore protection

**Status:** Implemented

API credentials are encrypted with an Android Keystore-backed key and are never stored in source control or ordinary settings.

---

## ADR-009 — Jarvis availability uses official Android Assistant role

**Status:** In progress

Always-available Mayra should be built around `VoiceInteractionService` / Assistant role and lock-screen voice sessions, not an unrestricted permanently running microphone service.

---

## ADR-010 — Advanced call control uses official Phone/Call Screening roles

**Status:** Accepted; not implemented

Caller announce, answer, reject, silence, mute and speaker operations should be implemented through default Phone/InCallService and Call Screening roles where supported.

---

## ADR-011 — Cellular AI answering/recording is not assumed

**Status:** Accepted constraint

Mayra will not claim it can inject AI speech into, or secretly record/transcribe, arbitrary SIM-call audio through standard public Android APIs.

---

## ADR-012 — Personal Owner Mode does not mean zero safeguards

**Status:** Accepted

The app may streamline routine actions for its single owner, but broad destructive operations, credential handling and irreversible changes retain clear guards.

---

## ADR-013 — Documentation is part of every feature

**Status:** Accepted and CI-enforced

Meaningful code/architecture work must update the roadmap and rolling snapshot; affected blueprint/idea/decision/changelog records must also be synchronized.

---

## ADR-014 — Owner APKs require stable signing

**Status:** Implemented foundation; secret setup and upgrade test pending

Personal Alpha APKs intended for repeated installation on the owner device must use one stable signing certificate supplied through protected GitHub/environment secrets. Hosted-runner debug certificates are temporary and must not be presented as update-compatible.

**Reason:** Android only allows an installed package to be upgraded by an APK signed with the same certificate. CI #1851 exposed this through a real Motorola install failure.

**Consequences:**

- a dedicated `Stable Owner Alpha` workflow builds the update-compatible owner package;
- keystore bytes and passwords never enter source control, project documents or chat;
- each stable artifact records certificate information and APK SHA-256;
- clean-install and install-over-install data-retention tests are mandatory before promotion.

---

## ADR-015 — First launch uses one minimal owner setup

**Status:** Implemented; CI/device validation pending

Mayra will present a small two-step first-launch setup instead of scattering essential setup across many screens.

**Flow:**

1. request only microphone, contacts and notification runtime permissions;
2. open Android's Assistant-role selector;
3. start Mayra.

**Reason:** The owner wants maximum capability with minimum confusion and no unnecessary settings maze.

**Consequences:** Internet and boot recovery remain manifest permissions with no runtime prompt. Android roles and special access still require explicit system approval because apps cannot grant them silently.
