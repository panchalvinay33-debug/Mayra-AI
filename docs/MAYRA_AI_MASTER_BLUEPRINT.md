# Mayra AI Master Blueprint V2 — Living Companion System

**Status:** Locked product and engineering source of truth  
**Repository path:** `docs/MAYRA_AI_MASTER_BLUEPRINT.md`  
**Primary development mode:** Personal Owner Build first, Play Store hardening later  
**Rule:** Future architecture, UI, features, tests, GitHub batches and release decisions must preserve this blueprint unless the owner explicitly changes it. Existing capabilities must not be silently removed.

---

## 1. Final Product Vision

Mayra AI is not a chatbot and not merely an app screen. It is a **voice-first, memory-enabled, context-aware, animated Personal Intelligence System and Digital Companion for Android**.

Mayra must feel like a living presence across the phone:

- a premium animated home presence;
- a minimized floating assistive ball available over other apps;
- voice, text and contextual quick actions;
- local phone awareness and safe action execution;
- reminders, agenda, notifications, people, memory and proactive follow-up;
- optional screen-context assistance through explicitly enabled Accessibility;
- online intelligence with local/offline continuity;
- visible permissions, confirmations, audit history and a global stop switch.

Mayra must feel powerful without pretending Android or third-party apps allow unrestricted control. It must never secretly monitor, impersonate the user, bypass secure fields, silently send sensitive content, handle OTP/password/payment/legal acceptance autonomously, hide clicks or falsely claim success.

---

## 2. Core Product Identity

Mayra has four connected surfaces:

1. **Living Home** — the visual home and control center.
2. **Floating Mayra** — the daily cross-app interaction layer.
3. **Context Assist** — optional screen understanding and deterministic assistive actions.
4. **Safety Core** — permission, risk, confirmation, execution, verification and audit.

The full app is not the only way to interact with Mayra. The user should be able to reach Mayra from anywhere on the phone through the floating presence, voice invocation, notifications or system entry points.

---

## 3. Living Home Experience

The home screen must be minimal, premium and emotionally clear.

### Home screen contains only

- Mayra title and three-dot menu;
- animated Mayra presence;
- current state: Ready, Listening, Understanding, Thinking, Speaking, Working, Offline or Needs Attention;
- one primary **Talk to Mayra** action;
- one compact “Today” card;
- one compact “Mayra noticed” or attention card when useful;
- a small access warning only when something important is missing.

### Home screen must not become a settings dashboard

Secondary functions move into the three-dot menu:

```text
⋮
├── My day
│   ├── Personal agenda
│   ├── Reminders & follow-ups
│   └── Notification intelligence
├── My people
│   └── People & relationships
├── Phone control
│   ├── Phone pulse
│   ├── Live activity
│   ├── Action safety
│   └── Personal device check
├── Mayra access
│   ├── Permissions
│   ├── Floating Mayra
│   ├── Notification access
│   ├── Accessibility
│   └── Background access
└── Settings
    ├── Language
    ├── Voice
    ├── AI provider
    ├── Memory
    └── Privacy
```

---

## 4. Animated Mayra Presence

The Mayra presence is not decorative. It reflects real runtime state.

| State | Visual behaviour |
|---|---|
| Idle/Ready | slow breathing glow |
| Listening | outward audio waves |
| Understanding | rotating inner ring |
| Thinking | particles/orbit movement |
| Speaking | voice-reactive pulse |
| Working | orbit/progress animation |
| Success | soft expansion and settle |
| Needs attention | gentle pulse and dot |
| Offline | dim but alive |
| Error | calm warning pulse |

Animation must remain smooth on low-memory phones and degrade gracefully when battery saver or performance limits are active.

---

## 5. Floating Mayra

Floating Mayra is the primary daily interaction surface outside the full app.

### Minimized state

- small draggable animated orb;
- docks to screen edges;
- remembers position;
- can be hidden or disabled;
- shows subtle state changes without blocking content.

### Expanded state

Tapping the orb opens a compact panel with:

- current app name;
- Talk;
- Type;
- Ask about screen;
- contextual quick actions;
- open full Mayra;
- minimize;
- stop floating service.

### Smart appearance

The orb may expand or request attention when:

- a reminder is due;
- an important notification arrives;
- an action requires confirmation;
- a workflow fails;
- an incoming call needs user attention;
- the user invokes Mayra;
- phone health becomes critical.

Interruption frequency must be user-controlled.

### Android implementation

- `SYSTEM_ALERT_WINDOW` / Display over other apps;
- `WindowManager` with `TYPE_APPLICATION_OVERLAY`;
- foreground service with visible persistent notification;
- no secret background overlay;
- no hidden touch interception.

---

## 6. Context-Aware Cross-App Assistance

Floating Mayra adapts to the foreground app.

### WhatsApp / messaging

- summarize visible conversation context where permitted;
- prepare a reply draft;
- create a reminder from a message;
- remember a relationship/contact;
- use RemoteInput, deep link, share intent or visible compose flow where supported;
- never claim “sent” unless verifiable.

### Chrome / browser

- summarize page;
- explain selected text;
- translate;
- save note;
- search related information.

### Gallery / camera

- explain image;
- extract visible text;
- share;
- remember context;
- create note/reminder.

### Maps / files / calendar / other apps

Mayra offers only actions supported by official APIs, app integrations or transparent assistive workflows.

---

## 7. Optional Accessibility Assist Mode

Accessibility is an optional, owner-enabled capability layer for transparent assistive use.

It may provide:

- foreground app detection;
- visible UI text snapshot;
- current screen type;
- supported clickable nodes;
- deterministic user-requested actions;
- screen summary and contextual suggestions.

It must not:

- read password or secure fields;
- capture OTPs for autonomous use;
- bypass banking/security screens;
- perform hidden clicks;
- autonomously approve payments, legal consent or account changes;
- impersonate the user;
- continue after the global stop command.

Every assistive action must be visible, bounded and auditable.

---

## 8. Permission and Access Journey

Permissions are not scattered across the home screen. They are organized in a guided **Mayra Access Journey**.

### First-run flow

1. Welcome and explain owner-first mode.
2. Request basic runtime permissions with context:
   - microphone;
   - notifications;
   - contacts;
   - phone;
   - camera.
3. Guide special access individually:
   - Notification Access;
   - Display over other apps;
   - unrestricted battery/background mode;
   - Accessibility Assist Mode;
   - default assistant role when implemented;
   - default dialer role in later phase.
4. Show readiness score and missing capabilities.
5. Allow basic mode if the user skips optional access.

### Rules

- never request a permission without explaining why;
- request only capabilities relevant to the personal build;
- special permissions open their official Android settings screens;
- denied access has a graceful fallback;
- all access can be reviewed and revoked from **Mayra access**.

---

## 9. Personal Owner Build Strategy

The current development target is the owner’s personal Android phone.

### Owner-first priorities

- sideloaded debug/personal builds;
- maximum officially grantable Android access;
- real-device testing before broad product hardening;
- fast iteration from actual failures;
- optional trusted direct handoffs for ordinary actions;
- strong protection remains for sensitive, destructive, financial, legal and critical actions.

### Later Play Store profile

A future Play build will:

- reduce or separate restricted permissions;
- function without Accessibility;
- use least-privilege official flows;
- add policy declarations and broad compatibility testing;
- use release signing and distribution hardening.

The personal build must not rely on secret root or security bypasses.

---

## 10. Conversation and Intelligence

Mayra supports Hindi, Hinglish, English and multilingual conversation.

The intelligence stack has two layers:

1. **Local deterministic brain** for device commands, reminders, agenda, phone state, safety and offline continuity.
2. **Online AI provider** for broader conversation, reasoning and flexible language understanding.

Phone actions always pass through the local safety layer even when online AI is active.

The local parser must support natural word order rather than only rigid command phrases.

---

## 11. Memory and Personal Knowledge

Mayra stores user-approved:

- names and preferred language;
- family and relationship aliases;
- trusted and sensitive contacts;
- routines;
- reminders and agenda;
- preferences;
- projects and goals;
- useful conversation context.

Memory must include source, confidence, sensitivity, retention and revocation metadata. Sensitive information stays encrypted locally and is excluded from cloud prompts unless explicitly enabled.

---

## 12. Phone and App Control Architecture

Capability tiers:

### Tier 1 — Official Android actions

Intents, system APIs, Contacts Provider, Calendar Provider, Alarm/Reminder APIs, camera/file picker, share sheet, Telecom/Dial intents, media sessions and notification APIs.

### Tier 2 — App-provided integrations

Official SDKs, deep links, shortcuts, public APIs, media sessions and notification actions.

### Tier 3 — User-assisted screen control

Optional Accessibility-based, deterministic and visible assistance.

### Tier 4 — Restricted/unsupported

Mayra explains the limit, prepares content, opens the nearest correct screen or asks the user to complete the protected step.

Execution lifecycle:

```text
UNDERSTAND
→ RESOLVE
→ DETECT_CONTEXT
→ CHECK_CAPABILITY
→ CHECK_PERMISSION
→ CLASSIFY_RISK
→ CONFIRM
→ SHOW_PLAN
→ EXECUTE
→ VERIFY
→ REPORT
→ RECORD
→ RETRY / FALLBACK / UNDO
```

---

## 13. Messaging Safety

Mayra resolves identity before messaging.

- ambiguous contacts require clarification;
- ordinary drafts may use quick owner flows;
- send actions use strongest supported route;
- duplicate-send fingerprints prevent accidental repeats;
- financial, OTP, legal, intimate, medical and highly sensitive content always requires protection;
- opening a compose screen is never reported as delivered.

---

## 14. Notification Intelligence

After explicit Notification Access:

- group notifications;
- hide OTP/sensitive content;
- create private summaries;
- offer per-app privacy modes;
- create reminders;
- support RemoteInput reply where available;
- require short-lived confirmation;
- suppress duplicate replies;
- keep bounded audit history.

Notification access is not universal app control.

---

## 15. Reminders and Personal Agenda

Mayra owns a private local reminder and agenda system.

Capabilities:

- natural language creation;
- due notifications;
- complete, snooze, cancel;
- missed follow-up;
- reboot/app-update rescheduling;
- today/upcoming summaries;
- reminder movement by voice;
- private events;
- optional Android Calendar export.

Future:

- recurring instance generation;
- exact alarm owner adapter;
- event alerts;
- conflict and free-time intelligence;
- location reminders.

---

## 16. Incoming Call Roadmap

V1:

- initiate calls;
- resolve contacts;
- callback reminders;
- prepared responses.

V1.5:

- incoming caller announcements;
- default dialer prototype;
- answer/reject/silence controls where officially supported.

V2:

- complete dialer UI;
- InCallService;
- disclosed AI call screening and message relay.

Mayra never secretly records or pretends to be the user.

---

## 17. Safety Core

The global Action Safety Engine is mandatory.

It provides:

- capability checks;
- permission checks;
- risk classification;
- confirmations;
- global stop/resume;
- execution verification;
- honest result language;
- audit history;
- duplicate prevention;
- fallback and recovery.

Mandatory confirmation/protection remains for:

- payments;
- OTP/password/PIN;
- banking;
- legal acceptance;
- destructive deletion;
- public posting;
- sensitive private-data sharing;
- account/security changes;
- critical actions.

---

## 18. UI and UX Principles

- Mayra should feel alive, not crowded.
- The main surface prioritizes presence and conversation.
- Advanced controls belong in organized menus.
- Every background capability has a visible indicator.
- Every risky action has a clear confirmation.
- Mayra reports what actually happened.
- The user can stop all actions instantly.
- Hindi/Hinglish wording must be natural, not rigid command syntax.
- Low-memory and battery-aware operation is required.

---

## 19. Development Phases From This Blueprint

### Phase A — Home and navigation redesign

- minimal Living Home;
- three-dot organized menu;
- improved state animation;
- compact Today/Attention cards.

### Phase B — Access Journey

- guided basic permissions;
- special access setup;
- readiness score;
- access management screen.

### Phase C — Floating Mayra V1

- overlay permission;
- foreground service;
- draggable/dockable orb;
- expand/minimize;
- talk/type/open app/stop controls;
- persistent position and preferences.

### Phase D — Context-aware floating actions

- foreground app awareness;
- app-specific quick actions;
- current notification/message context;
- reminder/share/note shortcuts.

### Phase E — Accessibility Assist Mode

- explicit enablement;
- visible screen snapshot;
- secure-field filtering;
- deterministic supported actions;
- audit and stop integration.

### Phase F — Smart proactive presence

- reminder and notification attention;
- action confirmations;
- runtime recovery;
- incoming-call prompt;
- user-controlled interruption policy.

### Phase G — App-specific workflows

- WhatsApp assisted messaging;
- browser summary;
- gallery/vision;
- files/PDF;
- maps;
- calendar;
- phone calls.

### Phase H — Broader intelligence

- notes and voice notes;
- file/PDF intelligence;
- daily briefing;
- long-term memory management;
- goals and workflows;
- default assistant and dialer prototypes.

---

## 20. Current Product Decision

The next implementation priority is no longer adding more buttons to the current home screen.

The locked priority is:

1. redesign the Living Home;
2. move secondary tools into the three-dot menu;
3. build the guided Access Journey;
4. implement Floating Mayra V1;
5. connect it to the existing voice, reminder, notification, identity and Action Safety systems;
6. then add optional Accessibility context assistance.

---

## 21. Definition of Success

Mayra succeeds when the phone feels like it has a trustworthy mind and companion layer:

- the user does not need to search through many screens;
- Mayra is reachable from anywhere;
- it understands natural language;
- it knows the current permitted context;
- it can act safely;
- it remains visibly under user control;
- it never lies about what it completed;
- it protects sensitive information;
- it continues to work in a useful limited way offline.

**Final product statement:**

> Mayra is a living, animated, context-aware personal companion that stays available across the Android phone, helps the owner understand and act on the current situation, and remains safe, transparent and controllable at every step.
