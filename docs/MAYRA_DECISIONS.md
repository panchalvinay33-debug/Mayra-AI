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

---

## ADR-015 — First launch uses one minimal owner setup

**Status:** Implemented; CI/device validation pending

The full Mayra app will present a small two-step first-launch setup: essential runtime permissions, Android Assistant activation, then Start Mayra.

---

## ADR-016 — J1 Assistant proof uses a zero-permission isolated APK

**Status:** Implemented; CI/device validation pending

Assistant-role compatibility must be tested separately from the full sensitive-capability app after Play Protect blocked the sideloaded Personal Alpha.

**Decision:** Create `ai.mayra.app.j1`, an engineering-only package containing only Assistant activation/status and the VoiceInteraction service/session/orb foundation. It requests zero Android permissions.

**Reason:** J1 needs to answer one question—whether the Motorola exposes, selects and invokes Mayra as Android Assistant. Contacts, internet, reminders, notification access and owner data are unrelated to that proof and increase sideload trust friction.

**Consequences:**

- dedicated CI rejects every requested Android permission and forbidden feature/background component;
- the J1 APK is not the final Mayra product and is removed after role testing;
- full Mayra remains on stable private signing and trusted distribution;
- Play Protect is not bypassed.
