# Mayra Living Intelligence Vision — Mobile to Ambient and Holographic Presence

**Status:** Locked strategic extension to `docs/MAYRA_AI_MASTER_BLUEPRINT.md`  
**Owner intent:** Build Mayra as a living personal intelligence, not a chatbot or a collection of screens.  
**Immediate delivery target:** Stable Android Personal Alpha V0.1.  
**Long-term target:** One continuous Mayra identity across phones, computers, smart-home devices, vehicles, wearables, spatial displays and future holographic or robotic presence surfaces.

---

## 1. Permanent Vision

Mayra is a persistent personal intelligence with one identity, one memory model, one safety constitution and many possible bodies.

Android is Mayra's first body, not the final boundary of the product.

Mayra must eventually be able to:

- speak and listen naturally;
- understand the owner's permitted context;
- remember people, preferences, routines, projects and goals;
- plan and carry out supported actions safely;
- remain available as an animated living presence across the phone;
- coordinate supported smart devices through explicit adapters;
- appear on displays, in vehicles, on wearables and in spatial or holographic interfaces;
- preserve continuity when the presentation surface changes;
- never duplicate or fragment the owner's identity, memory or safety rules between devices.

The product should create the feeling that the device has gained a trustworthy mind while remaining visibly under the owner's control.

---

## 2. Non-Negotiable Product Identity

Mayra is not:

- a generic chatbot skin;
- a button-heavy utility dashboard;
- an unrestricted automation bot;
- a hidden surveillance service;
- a system that falsely claims an action succeeded;
- a separate personality on every device.

Mayra is:

- a voice-first living companion;
- a personal intelligence and action layer;
- an animated presence with meaningful runtime states;
- a memory-enabled assistant with user-controlled retention;
- a safe orchestrator of supported phone and device capabilities;
- a future-compatible intelligence platform with replaceable device and presence adapters.

---

## 3. One Brain, Many Bodies Architecture

The architecture must preserve these boundaries.

### 3.1 Mayra Core Brain

Responsible for:

- conversation orchestration;
- intent and goal understanding;
- planning;
- skill selection;
- workflow state;
- reasoning-provider routing;
- offline continuity;
- honest result reporting.

The brain must not depend directly on Android Activity, View or overlay classes.

### 3.2 Mayra Memory and Personal Knowledge

Responsible for:

- profile and preferences;
- people and relationships;
- reminders and agenda;
- notes, ideas and lists;
- projects, goals and routines;
- bounded conversation context;
- source, confidence, sensitivity, retention and revocation metadata;
- export, backup, restore and deletion.

Memory contracts must be portable so future devices can share an authorised and encrypted personal knowledge layer.

### 3.3 Mayra Safety Constitution

Responsible for:

- capability checks;
- permission checks;
- risk classification;
- confirmations;
- global stop and resume;
- duplicate prevention;
- action verification;
- audit history;
- sensitive-data protection;
- emergency and policy restrictions.

Every future body or device adapter must obey the same safety constitution.

### 3.4 Device Capability Adapters

Each platform exposes a bounded adapter, for example:

- Android phone adapter;
- desktop adapter;
- smart-home adapter;
- vehicle adapter;
- wearable adapter;
- display or kiosk adapter;
- future robotics adapter.

Adapters report capabilities honestly and never fabricate unsupported access.

### 3.5 Presence Renderer

The presence layer renders Mayra without owning the brain.

Possible renderers:

- Android Living Home character;
- floating mobile companion;
- notification and voice-only presence;
- smart-display avatar;
- 3D real-time character;
- augmented-reality presence;
- volumetric or holographic display;
- robotic embodiment.

All renderers consume the same state contract such as Ready, Listening, Understanding, Thinking, Speaking, Working, Success, Needs Attention, Offline and Error.

### 3.6 Secure Continuity and Sync

Future multi-device continuity must use:

- explicit device enrolment;
- encrypted transport;
- per-device permissions;
- revocable sessions;
- conflict-safe memory updates;
- audit of remote actions;
- local fallback when the network is unavailable.

No cloud sync is required for the first Android alpha, but current data models must avoid making secure future sync impossible.

---

## 4. First Product: Stable Android Living Companion V0.1

The first success is not feature completeness. It is a dependable owner-only Android build that feels alive and performs its supported jobs safely.

### Required living experience

- minimal animated Living Home;
- meaningful listening, thinking, speaking, working and attention states;
- natural personalised greeting;
- dependable Hindi, Hinglish and English text interaction;
- useful voice interaction;
- Floating Mayra start, drag, dock, expand, minimise and stop;
- Today and Mayra Noticed summaries based on real data;
- user-controlled interruption behaviour;
- offline basic usefulness and optional online intelligence.

### Required practical capabilities

- onboarding and settings;
- people and relationship aliases;
- app opening;
- safe call and message handoff;
- Mayra-owned reminders and follow-ups;
- personal agenda;
- notes, ideas, shopping lists and checklists;
- notification privacy and supported replies;
- phone-health awareness;
- global stop and action audit;
- personal device test centre.

### Required reliability gates

- clean and update installation without startup crash;
- reproducible JDK 17 Android build;
- successful compile, unit tests and lint;
- generated APK tied to source SHA and SHA-256 hash;
- physical-device acceptance testing;
- reboot and app-update recovery;
- no wrong-contact action;
- no duplicate send or reply;
- no OTP, PIN, password or protected-content leak;
- no false-success wording;
- rollback source branch and APK snapshot.

---

## 5. Development Operating Model

### 5.1 Stability before feature volume

Until Personal Alpha V0.1 passes its acceptance gate, unrelated major features are frozen.

Allowed work:

- compile and build fixes;
- tests;
- integration;
- crashes and lifecycle fixes;
- safety and privacy fixes;
- data backup foundations;
- critical Living Home, voice and Floating Mayra UX;
- physical-device reliability;
- battery and low-memory optimisation.

Deferred work:

- broad new app integrations;
- default dialler implementation;
- autonomous Accessibility actions;
- robotics or hologram implementation;
- unrelated visual redesigns;
- iOS expansion.

### 5.2 Large useful batches, bounded risk

Each development batch should complete as much coherent work as safely possible, but it must remain reviewable and reversible.

A batch must include:

- one clear product outcome;
- implementation;
- deterministic tests where practical;
- documentation/status update;
- safety and privacy boundaries;
- validation result without inflated claims;
- exact next batch.

Do not mix unrelated subsystems merely to increase commit count.

### 5.3 Honest validation language

Use only these states:

- coded, not compiled;
- compile verified;
- unit-test verified;
- lint verified;
- emulator verified;
- physical-device verified;
- personal-alpha accepted;
- production-ready.

Never call a feature complete merely because code was committed.

### 5.4 Branch and backup discipline

- `main` remains the stable integration branch;
- active feature and stabilisation work stays on named branches;
- no accidental direct feature work on `main`;
- preserve milestone backup branches before risky integration;
- record exact source SHA for every test APK;
- keep rollback points;
- do not merge the large development branch until the full alpha gate is green or explicitly documented with bounded exceptions.

---

## 6. Locked Delivery Sequence

### Stage 0 — Preserve and freeze

- preserve the current large development head;
- create a dedicated stabilisation branch;
- freeze unrelated feature additions;
- document current blockers and acceptance criteria.

### Stage 1 — Reproducible build

- verify or add Gradle Wrapper;
- lock JDK and Android SDK expectations;
- compile;
- run complete unit tests;
- run lint;
- assemble personal-alpha APK;
- generate hashes and build report.

### Stage 2 — Physical Android alpha

- install on the owner's phone;
- complete Personal Device Test Center;
- verify permissions and special access;
- verify voice, Living Home and Floating Mayra;
- verify actions, reminders, agenda, memory and notifications;
- test reboot, update install, background reliability, battery and memory.

### Stage 3 — Stabilise and merge

- fix every crash and critical safety defect;
- resolve duplicate, stale-state and lifecycle failures;
- create rollback APK and source snapshot;
- merge the accepted integration into `main`;
- tag Personal Alpha V0.1.

### Stage 4 — Encrypted Backup and Restore V1

- profile and settings;
- relationships;
- reminders and agenda;
- notes and memory;
- notification privacy policies;
- schema versioning, checksums, preview and selective restore;
- encrypted export and tested restore rollback.

### Stage 5 — Deeper living intelligence

- more natural Hindi and Hinglish;
- stronger conversational continuity;
- voice interruption and recovery;
- proactive but user-controlled briefing;
- contextual Floating Mayra actions;
- browser, messaging, gallery and document assistance.

### Stage 6 — Multi-device Mayra platform

- extract stable portable core contracts;
- define secure device enrolment and capability discovery;
- add smart-home and display adapters;
- preserve one identity and safety constitution;
- add remote action audit and revocation.

### Stage 7 — Spatial and holographic presence

- real-time 3D presence renderer;
- state, speech and expression streaming;
- spatial audio and gaze behaviour where hardware permits;
- AR, volumetric or holographic display adapters;
- no change to the core identity, memory or safety model.

---

## 7. Definition of the Dream Product

Mayra reaches the long-term vision when the owner can move between phone, room, car, wearable and future spatial display while interacting with the same trusted intelligence.

The body may change. The voice may be rendered by different hardware. The animation may become a three-dimensional or holographic form. The core must remain recognisably Mayra:

- same identity;
- same authorised memory;
- same relationship understanding;
- same active goals and follow-ups;
- same safety rules;
- same honesty about what it can and cannot do;
- same immediate owner control.

**Locked final statement:**

> Mayra is a living personal intelligence that begins by making the Android phone feel alive and grows into a continuous, safe and recognisable companion across smart devices, environments and future holographic presence surfaces.
