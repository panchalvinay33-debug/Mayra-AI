# Mayra AI — Decision Log

Last updated: 2026-08-03

This log preserves important product and architecture decisions. Newer entries may supersede older ones, but history is not deleted.

## Decision format

Each entry records: decision, reason, consequences, validation state and supersession status.

---

## ADR-001 — One user-facing application

**Status:** Accepted

Mayra must present one launcher icon and one coherent app. Library, Memory, Provider, History and Device controls remain internal screens. Separate packages may exist only for CI/device testing.

**Reason:** The product is one assistant, not a collection of test utilities.

**Consequences:** Manifest audits enforce exactly one launchable activity in Personal Alpha and final release.

---

## ADR-002 — Local-first, cloud-optional intelligence

**Status:** Accepted; local LLM pending

Mayra must retain offline commands, memory, documents, reminders and device actions without an API key. Cloud providers are optional intelligence boosters.

**Reason:** Mayra’s identity and basic usefulness cannot depend on one company, network connection or paid API.

**Consequences:** Provider failures fall back locally. A true local conversational model remains a major planned milestone.

---

## ADR-003 — Remote providers are text-only

**Status:** Implemented

A remote conversational provider may produce response text but cannot directly execute device actions or write personal memory.

**Reason:** Model output must not become an untrusted command channel.

**Consequences:** Typed routing and deterministic local boundaries own actions, confirmations and memory proposals.

---

## ADR-004 — Trusted memory attribution is structured metadata

**Status:** Implemented; supersedes visible marker design

Memory keys used in a response travel through `MayraAssistantResponse.usedPersonalMemoryKeys`, not hidden strings embedded in visible text.

**Reason:** A provider or document could spoof text markers.

**Consequences:** The old parser/marker implementation was deleted.

---

## ADR-005 — Review-first call and message handoffs

**Status:** Implemented for current action layer

Outgoing call and message commands open Android’s dialer/composer after resolution and confirmation. Mayra does not request direct `CALL_PHONE` or `SEND_SMS` permission in the current product boundary.

**Reason:** Reliable owner review with lower privilege.

**Future extension:** Incoming/ongoing call control may use the official default Phone role, not reintroduce arbitrary silent privileges.

---

## ADR-006 — Mayra-owned reminders use WorkManager

**Status:** Implemented

Mayra persists reminders locally and schedules through WorkManager, with Complete, Snooze, follow-up and reboot/update recovery.

**Reason:** Reliable broad Android support without special exact-alarm access.

**Consequence:** Android battery-saving modes may introduce timing delay. Exact alarms remain deferred pending physical need.

---

## ADR-007 — Confirmation tokens are exact, expiring and process-local

**Status:** Implemented

Sensitive actions use one-time tokens bound to the exact action and short expiry. Raw tokens are not persisted across process death.

**Reason:** Persisting UI tokens while the backing in-memory token store resets would create stale/unknown approvals.

**Consequence:** Expired or process-lost confirmations require the owner to request the action again.

---

## ADR-008 — Provider credentials use Android Keystore protection

**Status:** Implemented

API credentials are encrypted with an Android Keystore-backed key and are never stored in source control or ordinary settings.

**Reason:** A personal assistant holds unusually sensitive context.

**Consequence:** The settings screen never reads a saved key back into plaintext UI.

---

## ADR-009 — Jarvis availability uses official Android Assistant role

**Status:** In progress

Always-available Mayra should be built around `VoiceInteractionService` / assistant role and lock-screen voice sessions, not an unrestricted permanently running microphone service.

**Reason:** Official system roles are more reliable under modern Android background restrictions and battery management.

**Consequences:** The owner must explicitly select Mayra as assistant. Wake-word implementation remains a separate benchmarked component.

---

## ADR-010 — Advanced call control uses official Phone/Call Screening roles

**Status:** Accepted; not implemented

Caller announce, answer, reject, silence, mute and speaker operations should be implemented through default Phone/InCallService and Call Screening roles where supported.

**Reason:** Normal apps cannot reliably control protected cellular calls.

**Consequences:** The owner must explicitly grant the role. Mayra must provide a complete and safe call UI fallback.

---

## ADR-011 — Cellular AI answering/recording is not assumed

**Status:** Accepted constraint

Mayra will not claim it can inject AI speech into, or secretly record/transcribe, arbitrary SIM-call audio through standard public Android APIs.

**Reason:** Platform, device, legal and audio-routing constraints.

**Possible path:** Owner-controlled voicemail/VoIP answering architecture or a device-specific documented capability after proof.

---

## ADR-012 — Personal Owner Mode does not mean zero safeguards

**Status:** Accepted

The app may streamline routine actions for its single owner, but broad destructive operations, credential handling and irreversible changes retain clear guards.

**Reason:** Removing every guard does not create intelligence; it creates accidental damage and unreliable behavior.

**Consequences:** Trust levels and per-action policy can reduce friction without eliminating recovery boundaries.

---

## ADR-013 — Documentation is part of every feature

**Status:** Accepted and CI-enforced

Meaningful code/architecture work must update the roadmap and rolling snapshot; affected blueprint/idea/decision/changelog records must also be synchronized.

**Reason:** Work frequently resumes across sessions and agents. The repository must explain itself without private chat context.

**Consequences:** Governance CI fails when project records drift.
