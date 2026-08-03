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

**Status:** Device-proven foundation

Always-available Mayra is built around `VoiceInteractionService` / Android Digital Assistant role and `VoiceInteractionSession`, not a general overlay or unrestricted permanently running microphone service.

Motorola evidence now proves Mayra can be selected as the target device’s Digital assistant and invoked through the configured Power-button assistant trigger.

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

Personal Alpha and repeated engineering APKs intended for owner-device upgrades should use one stable signing certificate supplied through protected secrets. Hosted-runner debug certificates are temporary and must not be presented as update-compatible.

---

## ADR-015 — First launch uses one minimal owner setup

**Status:** Implemented; trusted full-app device validation pending

The final Mayra app will present a small first-launch setup: essential runtime permissions, Android Assistant activation, then Start Mayra. Android may still show its own mandatory system dialogs.

---

## ADR-016 — J1 Assistant proof uses a zero-permission isolated APK

**Status:** Device-proven for selection/unlocked invocation; lifecycle completion pending

Assistant-role compatibility is tested separately from the full sensitive-capability app.

**Decision:** `ai.mayra.app.j1` contains only Assistant activation/status and VoiceInteraction service/session/orb foundations and requests zero Android permissions.

**Consequences:** dedicated CI rejects every permission, extra launcher and unrelated feature/background component; Play Protect/signature checks are never bypassed.

---

## ADR-017 — J2 voice proof is isolated from J1

**Status:** Implemented foundation; CI/device validation pending

J1 must remain the permanent zero-permission proof baseline. Real invocation-time voice therefore moves to a separate engineering package, `ai.mayra.app.j2`.

J2 may request exactly `RECORD_AUDIO` and must remove internet, contacts, notifications, reminders, WorkManager/Room/background listeners and the full Mayra runtime. A dedicated J2 CI gate enforces this one-permission boundary.

Reason: if microphone/speech work breaks, J1 remains a clean known-good proof that Android Assistant selection/invocation itself works.

---

## ADR-018 — Android SpeechRecognizer is invocation-time STT, not the wake-word engine

**Status:** Accepted and implemented in J2 foundation

`SpeechRecognizer.createOnDeviceSpeechRecognizer()` is used only for a short visible/invoked assistant session when Android reports on-device recognition available.

It must stop on result/error/cancel/hide/destroy and must not be looped continuously in the always-running `VoiceInteractionService`.

A true always-awake `Mayra` hotword needs a separate dedicated wake-word detector and Issue #14 battery/background/privacy preflight.

---

## ADR-019 — Assistant UI must always have a bounded exit

**Status:** Repair implemented; device retest pending

Every visible Mayra assistant session must be dismissible through reliable Android navigation and direct surface interaction. Back, orb tap, label tap and outside/root tap call `hide()`. Session hide/destroy stops recognition, keep-awake state and animations.

Reason: the first J1 device invocation exposed that an orb with no click listener felt stuck even though Back and phone lock could dismiss it.
