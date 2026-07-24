# Mayra AI Master Blueprint

**Status:** Locked product and engineering reference  
**Scope:** Existing Mayra concept + implemented GitHub architecture + merged Phone & App Control System  
**Rule:** Future architecture, UI, features, tests, GitHub batches and release decisions must preserve this blueprint unless an explicit later product decision extends it. Existing capabilities must not be silently removed, reduced or replaced.

---

## 1. Final Mayra AI Vision

Mayra AI is not a chatbot. It is a **voice-first, memory-enabled, context-aware, action-taking Personal Intelligence System and Digital Companion for Android**.

Mayra should make the phone feel alive by combining:

- natural Hindi, Hinglish, English and multilingual conversation;
- a premium animated presence that visibly listens, understands, thinks, speaks and requests attention;
- short-term conversation context and long-term user memory;
- personal knowledge about family, relationships, contacts, routines, projects, preferences and goals;
- contextual phone awareness covering battery, network, storage, memory, notifications, permissions and supported capabilities;
- safe action execution across Android and supported apps;
- reminders, notes, calendar, files, search, vision, travel, health, finance, learning and work assistance;
- proactive briefings, follow-ups and workflow recovery;
- online intelligence with limited offline/local continuity;
- visible confirmations, audit history, privacy controls and an immediate stop/kill switch.

Mayra must feel powerful without pretending that Android or third-party apps allow unrestricted control. It must never fabricate success, secretly monitor the user, impersonate the user, bypass app security, silently send sensitive content, handle OTP/password/payment/legal acceptance autonomously, or claim unsupported app automation.

---

## 2. Existing Concept Preserved

The complete earlier concept remains active and is expanded rather than replaced:

1. AI Brain and natural conversation.
2. Hindi, English, Hinglish and multilingual support.
3. Voice-first and text chat interaction.
4. Long-term memory, short-term context and a personal knowledge graph.
5. Context engine and emotion-aware response style.
6. Premium animated Mayra presence with idle, listening, understanding, thinking, speaking, offline and attention states.
7. Smart reminders, calendar, notes, voice notes and follow-ups.
8. File/PDF intelligence, OCR, image understanding and vision memory.
9. Smart web search, weather, maps, calculator and conversions.
10. Morning brief, night summary and proactive suggestions.
11. Family, relationship and trusted-contact memory.
12. Health, finance, travel, learning, work, media and project assistants.
13. Goal tracking, long-term planning, workflows and agent execution.
14. Plugin/skills framework and multi-provider AI architecture.
15. Limited local/offline intelligence.
16. Encrypted local memory, permission dashboard and privacy center.
17. Productivity analytics, self-diagnostics and runtime supervision.
18. Adaptive scheduling, workflow history, recovery and auditability.
19. Cross-device sync and Mayra Business, Home, Kids, Drive and ecosystem roadmap.

---

## 3. Newly Merged Features

This master blueprint adds a modular **Mayra Phone & App Control System** while preserving all earlier architecture.

New capability families:

- Mayra Action Engine.
- Contact and identity resolution.
- WhatsApp, SMS and supported messaging workflows.
- Notification reading, summarisation and supported direct reply.
- Incoming-call intelligence and a conditional default-dialer roadmap.
- Safe app/device control through capability tiers.
- Risk, confirmation, privacy, emergency and compatibility guards.
- Verification, retry, fallback, undo and action audit history.
- Explicit distinction between official Android APIs, app-supported integrations, user-assisted control and unsupported/restricted actions.

---

## 4. Complete Feature Inventory

### Intelligence and conversation

Mayra understands free-form voice/text, maintains bounded conversation context, detects likely intent, selects a local or online provider, asks clarifying questions only when required, and responds in the user’s language/style. Emotional awareness changes tone and pacing; it does not diagnose mental health or claim human emotion.

### Memory and personal knowledge

Mayra stores user-approved preferences, identities, relationships, routines, projects, goals and recurring instructions. Memory entries require source, confidence, sensitivity, retention and revocation metadata. Sensitive categories are encrypted locally and excluded from cloud prompts unless the user enables that use.

### Presence and phone awareness

The living orb reflects real application state: ready, listening, understanding, thinking, speaking, offline-ready or needs-attention. Device health is translated into understandable suggestions without fake consciousness claims.

### Personal productivity

Reminders, notes, voice notes, calendar events, follow-ups, task lists, daily briefs, night summaries, goal checkpoints and multi-step workflow recovery.

### Knowledge and media

Web search, weather, maps, calculations, conversions, OCR, PDF/file understanding, image analysis, camera/file picker integration, media suggestions and supported media controls.

### Domain assistants

Health, finance, travel, learning, work/projects, family/home and business assistance use dedicated safety policies. High-stakes advice must identify uncertainty and defer to qualified professionals where appropriate.

### Ecosystem

Future cross-device sync, wearables, car mode, smart home, business integrations, plugins and developer SDK.

---

## 5. Phone and App Control System

### User experience

A user can say:

- “YouTube kholo.”
- “Google Maps mein Indore ka route lagao.”
- “Mummy ko call karo.”
- “Alarm 6 baje ka lagao.”
- “Latest photo share karo.”
- “Notification padhkar batao.”
- “Downloaded PDF dhoondo.”

Mayra responds with a visible plan, resolves missing details, performs permission/risk checks, obtains confirmation when required, executes through the strongest available safe capability, verifies the result and records an audit entry.

### Capability tiers

#### Tier 1 — Official Android actions

Use Android intents, system APIs, Contacts Provider, Calendar Provider, Alarm Clock intents, camera/file/photo picker, share sheet, Telecom/Dial intents, media sessions and notification APIs.

**Play-compliant default:** prefer user-visible official flows and least-privilege access.

#### Tier 2 — App-provided integrations

Use official SDKs, public APIs, app links, deep links, shortcuts, media sessions and notification actions. Capability availability is discovered at runtime and never assumed.

#### Tier 3 — User-assisted screen control

Only when the user explicitly enables a lawful assistive mode. The app must disclose what it can see/do, keep the user informed, avoid secure/password fields, require confirmation for destructive actions and never hide clicks.

**Important Play constraint:** general AI-driven autonomous Accessibility execution is not a safe Play Store assumption. Any Accessibility implementation must be policy-reviewed, deterministic, transparent and restricted to an allowed core use case. A Play-distributed build should function without it.

#### Tier 4 — Unsupported or restricted

Mayra explains the limitation, opens the nearest correct screen, prepares the content, or asks the user to complete the final protected step.

### Execution lifecycle

`UNDERSTAND → RESOLVE → CHECK_CAPABILITY → CHECK_PERMISSION → CLASSIFY_RISK → CONFIRM → PLAN → EXECUTE → VERIFY → RECORD → RETRY/FALLBACK/UNDO`

---

## 6. Messaging Architecture

### Example

User: “Mayra, Rahul ko WhatsApp par message bhejo ki main 20 minute late aaunga.”

1. `MayraContactResolver` searches contacts and relationship memory.
2. Multiple Rahul matches cause disambiguation.
3. `MayraMessagingController` builds a draft.
4. `MayraRiskClassifier` marks ordinary send as high risk; a draft is medium risk.
5. `MayraConfirmationEngine` says: “Rahul Sharma ko ‘Main 20 minute late aaunga’ bheju?”
6. Execution chooses the strongest supported route:
   - official app deep link/intent;
   - share/send intent;
   - notification `RemoteInput` reply when available;
   - user-visible assisted compose flow;
   - draft-only fallback.
7. `MayraActionVerifier` verifies only signals actually available. Opening a compose screen is not reported as “sent”.
8. Audit stores destination identity, channel, action type, timestamp and result; sensitive message text retention is configurable.

### Confirmation modes

- Always confirm.
- Quick confirm for trusted contacts.
- Draft only.
- Open app and let user press send.

### Protection rules

Mandatory confirmation for financial, OTP, legal, abusive, intimate, medical or highly sensitive messages. Group sending requires recipient-count confirmation. Duplicate-send fingerprints block accidental repeats. Wrong-contact protection uses full name/avatar/last-four digits where appropriate.

### Android and Play limits

- SMS compose through `ACTION_SENDTO`/`smsto:` is preferred when Mayra is not the default SMS/Assistant handler.
- Direct SMS and call-log permissions are highly restricted on Google Play and require an approved default-handler/core-function use case.
- WhatsApp/Telegram sending cannot be guaranteed unless the app exposes a supported integration. Opening a prepared compose flow is the reliable fallback.

### Version

- **V1:** SMS/app drafts, share intents, notification quick replies, summaries, duplicate protection.
- **V1.5:** assisted WhatsApp flows and richer app-specific capability adapters.
- **V2:** trusted-contact workflows and multi-step messaging automation where officially supported.

---

## 7. Incoming-Call Assistant Architecture

### Technically honest boundary

Full incoming-call answer/reject/speaker/mute/end control requires Mayra to qualify for and be selected as the **default dialer**, implement `ACTION_DIAL`, `InCallService`, incoming-call UI and ongoing-call UI. OEM/Android behavior differs. Emergency calls remain controlled by the system/preloaded dialer.

Without default-dialer status, V1 can provide caller-aware prompts from allowed signals, initiate calls, open the dialer, create callback reminders and prepare quick responses, but must not promise full in-call control.

### Modules

- `MayraDefaultDialerModule`: role eligibility, role request, dial pad, call UI and graceful role loss.
- `MayraInCallService`: maps Telecom call state to Mayra state and exposes supported answer/reject/silence/speaker/mute/end operations.
- `MayraCallController`: user-facing call command facade.
- `MayraCallAssistant`: disclosed AI relay/screening state machine.
- `MayraVoiceRelayEngine`: TTS/STT pipeline with disclosure and consent gates.
- `MayraEmergencyGuard`: blocks AI takeover for emergency, banking, legal and medical categories.

### States

`RINGING → USER_PROMPTED → USER_DECISION → CONNECTING → DISCLOSURE → RELAY_ACTIVE → MESSAGE_CAPTURED → USER_SUMMARY → ENDED`

Failure states include role unavailable, OEM unsupported, audio route unavailable, speech service unavailable, consent not obtained and emergency restriction.

### Privacy

- Never pretend to be the user.
- Mandatory caller disclosure: “Main Mayra, Vinay ki AI assistant bol rahi hoon.”
- No hidden recording.
- Transcription only after legally appropriate disclosure/consent.
- No OTP, password or payment collection.
- Transcript retention is user-configurable and encrypted.

### Version

- **V1:** call initiation, contact resolution, callback reminders and prepared responses.
- **V1.5:** incoming announcements and default-dialer prototype.
- **V2:** complete dialer UI, supported controls, disclosed screening and message relay.

---

## 8. Call-Assistant Conversation Examples

### Busy response

Mayra: “Vinay ka call aa raha hai. Receive, reject, silent, ya busy message?”  
User: “Message bhejo ki meeting mein hoon.”  
Mayra: “Vinay ko ‘Main meeting mein hoon, 20 minute mein call karta hoon’ bheju?”  
User: “Haan.”  
Mayra executes the supported SMS/app compose flow and reports the verified result.

### Disclosed relay

Mayra to caller: “Main Mayra, Vinay ki AI assistant bol rahi hoon. Vinay abhi available nahi hain. Kya main ek message note karun?”  
Caller: “Unse kehna 7 baje meeting hai.”  
Mayra: “Maine message note kiya: 7 baje meeting hai.”  
Mayra to user: “Caller ne kaha hai ki 7 baje meeting hai.”

### Restricted case

Caller context indicates bank/OTP/legal/medical emergency. Mayra does not take over. It alerts the user and offers answer/reject/silence only where supported.

---

## 9. Notification Intelligence

### What it does

After explicit notification-listener access, Mayra identifies app, sender, category and importance; groups duplicates; creates unread briefs; suggests replies; creates reminders; and executes supported `RemoteInput` replies after confirmation.

### User flow

- “Notifications ka summary batao.”
- “Sirf work notifications padho.”
- “Is message ka reply draft karo.”
- “Is notification se kal ka reminder banao.”

### Internal execution

`MayraNotificationService → PrivacyFilter → NotificationClassifier → ThreadGrouper → SummaryEngine → ReplyCapabilityDetector → Confirmation → RemoteInputExecutor → Verification/Audit`

### Privacy

- Per-app allow/deny list.
- Lock-screen and headphone/private modes.
- OTP detected but hidden by default; never uploaded or spoken automatically.
- Minimal retention; summaries can be stored without raw content.
- No notification data used for advertising or unrelated profiling.

### Limits

Notification access does not provide universal app control. Reply works only when the notification exposes a compatible action.

---

## 10. Permission Matrix

| Capability | Android permission/API | Default request timing | Fallback | Phase |
|---|---|---|---|---|
| Voice input | `RECORD_AUDIO`, SpeechRecognizer | When user taps Talk | Text input | V1 |
| Contacts | Contact Picker preferred; `READ_CONTACTS` only when broad resolution is core and disclosed | When user invokes contact action | Manual number/contact selection | V1 |
| Initiate call | `ACTION_DIAL` preferred; `CALL_PHONE` only for justified direct call | At call action | Open dialer | V1 |
| SMS draft | `ACTION_SENDTO` | No sensitive permission | Open messaging app | V1 |
| Direct SMS | Default SMS/Assistant role + Play approval where required | Only after role active | Draft-only | V2/conditional |
| Notifications | `NotificationListenerService` special access | Dedicated disclosure screen | Manual share/copy | V1 |
| Notification reply | Notification `RemoteInput` | Per reply confirmation | Open source app | V1 |
| Calendar | Calendar intents or provider permissions | At first calendar action | Open calendar compose | V1 |
| Alarms | Alarm Clock intent; exact-alarm policy where truly needed | At reminder setup | Inexact WorkManager reminder | V1 |
| Files/PDF | Storage Access Framework, Photo Picker | User-selected file | Manual file share | V1 |
| Camera | Camera intent; `CAMERA` only for embedded capture | At scan/camera action | System camera | V1 |
| Location/maps | Maps URI; foreground location only when needed | At navigation/local query | User-entered location | V1 |
| Default dialer | `RoleManager.ROLE_DIALER`, Telecom, `InCallService` | Dedicated opt-in | System dialer | V1.5/V2 |
| Accessibility assist | Accessibility special access | Separate prominent disclosure | Official intents/manual flow | Conditional |
| Biometrics/PIN | BiometricPrompt/local PIN | Sensitive action setup | Device credential | V1.5 |

Every permission is granular, revocable, purpose-bound and requested at the moment of need—not as a mass first-launch demand.

---

## 11. Risk and Confirmation Matrix

| Risk | Examples | Confirmation | Authentication | Audit/Undo |
|---|---|---|---|---|
| Low | Open app, weather, calculator, navigation preview, media play | Usually none; configurable | No | Basic audit |
| Medium | Message draft, calendar event, call initiation, file-share preparation | Single confirmation when destination/data is involved | Optional | Visible history; undo draft/event where possible |
| High | Send message, public post, delete, share private data, answer on user’s behalf | Mandatory explicit confirmation | Voice auth/PIN/biometric for configured sensitive classes | Full audit; undo where platform supports |
| Critical/Restricted | Payment, OTP/password entry, legal acceptance, emergency takeover, hidden recording | Mayra refuses autonomous completion | User performs protected step | Record refusal/reason without secret data |

Destructive actions require double confirmation. “Mayra stop” cancels active plans and prevents queued continuation. Lock-screen actions are limited to low-risk, pre-approved operations.

---

## 12. Privacy Architecture

### Core principles

- Local-first and data-minimising.
- Explicit purpose and retention for every sensitive data class.
- Android Keystore encryption for secrets.
- Encrypted local stores for sensitive memory, transcripts and audit details.
- Cloud prompts use only the minimum approved context.
- Per-domain and per-app privacy switches.
- Export, inspect, delete and reset controls.
- No secret monitoring, hidden call recording, impersonation or advertising profile from personal data.

### Components

- `MayraPrivacyGuard`: policy checks before context leaves the device.
- `MayraSensitiveDataClassifier`: marks OTP, financial, health, intimate, legal and credential content.
- `MayraConsentLedger`: records disclosures and consent versions.
- `MayraRetentionManager`: TTL and deletion jobs.
- `MayraKillSwitch`: stops voice, workflows, background jobs and pending actions.
- `MayraAuditRedactor`: stores safe metadata while masking secrets.

---

## 13. Android Technical Architecture

### Existing foundations retained

- Brain coordinator, event bus, task planner and plan runtime.
- Agent registry/planner/runtime.
- Context and conversation engines.
- Device intelligence and Phone Pulse.
- Voice runtime and continuous voice policy.
- Vision runtime and memory.
- Personal knowledge and memory.
- Privacy center.
- Automation bridge.
- Execution control plane, coordinator, checkpoint store, supervisor and adaptive scheduler.
- Runtime control center, notifications, diagnostics and recovery.
- Multi-provider local/OpenAI hybrid assistant.

### New modules

#### `MayraActionEngine`
Receives structured intent, creates an action plan, coordinates all gates and emits lifecycle events. Interface: `plan(request)`, `execute(plan)`, `cancel(id)`, `resume(id)`.

#### `MayraCapabilityRegistry`
Runtime registry of official intents, APIs, app links, notification actions, roles and OEM support. Returns `AVAILABLE`, `USER_SETUP_REQUIRED`, `TEMPORARILY_UNAVAILABLE`, `UNSUPPORTED`, `POLICY_RESTRICTED`.

#### `MayraPermissionManager`
Maps capability to Android permission/special access/role, produces disclosure copy and checks revocation.

#### `MayraRiskClassifier`
Deterministic policy plus contextual sensitivity. AI may suggest risk signals but cannot lower a policy-defined minimum.

#### `MayraConfirmationEngine`
Creates spoken/visual confirmation prompts, handles expiry, duplicate confirmation and authentication requirements.

#### `MayraContactResolver`
Combines Contacts Provider, aliases, relationship memory, recent interactions and preferred channels. Returns confidence and alternatives.

#### `MayraAppController`
Selects Tier 1–4 routes and never reports app-level completion without verification.

#### `MayraIntentExecutor`
Executes Android intents/system APIs with exception isolation and package-resolution checks.

#### `MayraNotificationService`
Ingests allowed notifications, privacy-filters content, detects reply actions and publishes context events.

#### `MayraMessagingController`
Draft/send workflow, destination protection, sensitive-content rules, dedupe and channel fallback.

#### `MayraCallController`
Outgoing and incoming call command facade; delegates to default-dialer module only when role is active.

#### `MayraInCallService`
Telecom integration for incoming/ongoing call UI and supported controls.

#### `MayraDefaultDialerModule`
Role eligibility, request, role-loss handling and dialer compliance.

#### `MayraCallAssistant`
Disclosed screening/relay state machine, restricted-category guard and user summary.

#### `MayraVoiceRelayEngine`
Speech recognition/TTS orchestration with disclosure, consent and audio-route checks.

#### `MayraActionAuditLog`
Append-only bounded action metadata, redaction, search, export and retention.

#### `MayraActionVerifier`
Evidence-based verification: activity opened, result callback, notification reply accepted, calendar row created, call state changed, etc. Unknown remains unknown.

#### `MayraFallbackPlanner`
Offers alternative channel, open-screen flow, draft-only route, retry timing or manual instructions.

#### `MayraPrivacyGuard`
Blocks unapproved data access/transmission.

#### `MayraEmergencyGuard`
Prevents autonomous handling of emergency calls, protected transactions and dangerous contexts.

#### `MayraDeviceCompatibilityManager`
Maintains Android API/OEM capability profiles and runtime probes.

### Shared error model

`MayraActionError(code, userMessage, technicalCause, retryable, fallback, requiredSetup, auditSafeDetails)`

No module swallows failure or turns “opened screen” into “completed action”.

---

## 14. Module Dependency Map

```text
Voice/Text UI + Living Presence
        |
Intent/Context/Memory/AI Brain
        |
MayraActionEngine
  |-- ContactResolver
  |-- CapabilityRegistry -- DeviceCompatibilityManager
  |-- PermissionManager -- PrivacyGuard
  |-- RiskClassifier -- EmergencyGuard
  |-- ConfirmationEngine -- Biometric/PIN
  |-- MessagingController -- NotificationService
  |-- CallController -- DefaultDialerModule -- InCallService
  |-- AppController -- IntentExecutor
  |-- ActionVerifier
  |-- FallbackPlanner
  |-- ActionAuditLog
        |
Execution Control Plane / Supervisor / Scheduler / Recovery
```

The AI model proposes language and plans; deterministic policy modules control permissions, risk and execution.

---

## 15. Data Flow Examples

### WhatsApp assisted send

Voice transcript → intent parse → contact resolution → message sensitivity → risk HIGH → confirmation → WhatsApp capability check → compose/deep-link route → app opens → result `COMPOSE_OPENED` → user presses send unless a supported verified send adapter exists → audit.

### Notification reply

Notification listener → privacy filter → supported RemoteInput action → user says reply → draft → confirmation → RemoteInput send → action result → verification based on API result → audit.

### Reminder from notification

Notification summary → user command → time extraction → calendar/reminder capability → confirmation → WorkManager/Calendar creation → verifier → runtime scheduler and follow-up.

### Incoming call in V2

Telecom event → InCallService → contact/relationship resolution → privacy mode → spoken prompt → user decision → call control → disclosed assistant relay if enabled → caller message transcription under consent → summary → retention policy.

---

## 16. UI Screens

1. **Mayra Living Home:** animated orb, current state, proactive greeting, phone health, top attention item, Talk and Chat.
2. **Conversation:** text/voice chat, visible thinking/listening states, attachments and action cards.
3. **Action Confirmation Sheet:** destination, content, risk, permission, exact operation, confirm/edit/cancel.
4. **Action Progress:** planning, waiting, executing, verifying, completed/failed/fallback.
5. **Action History:** searchable, redacted, retry/undo where supported.
6. **Mayra Pulse:** battery, storage, memory, thermal, network and capabilities.
7. **Runtime Control:** workflows, pending approvals, background scans and recovery.
8. **Notifications Hub:** grouped unread brief, privacy filters, reply/reminder actions.
9. **Contacts & Relationships:** aliases, trusted levels and preferred channels.
10. **Reminders/Notes/Calendar:** unified personal productivity surfaces.
11. **Files & Vision:** picker, OCR, PDF/image summaries and saved insights.
12. **Call Assistant:** conditional default-dialer UI, incoming/ongoing call and screening controls.
13. **Privacy Center:** permissions, data categories, retention, export/delete and kill switch.
14. **Compatibility Center:** supported/conditional/unsupported capabilities on the current device.

---

## 17. Settings Screens

- Profile, language and response style.
- Voice, continuous conversation and private/headphone mode.
- AI provider and encrypted API key.
- Memory categories and personalization.
- Contacts/relationship aliases and trusted levels.
- Messaging confirmation mode and per-channel policy.
- Notification per-app access and sensitive-content rules.
- Call assistant mode, default-dialer role, disclosure voice, emergency restrictions and retention.
- Action permissions and risk authentication.
- Proactive briefings and quiet hours.
- Background runtime schedule.
- Privacy, cloud context, diagnostics, export, delete and reset.
- Accessibility/assistive control disclosure only when an allowed implementation exists.

---

## 18. User Onboarding and Permissions Flow

1. Explain Mayra’s role: companion, not uncontrolled automation.
2. Name, language, voice and memory choices.
3. Start in local/private mode; cloud provider is optional.
4. Show living home and a safe demo.
5. Request microphone only when Talk is used.
6. Request contact access/picker only when a contact action is used.
7. Explain notification access on a dedicated screen before opening system settings.
8. Explain messaging behavior and choose confirmation mode.
9. Explain default-dialer V2 capability separately; never force it.
10. Offer biometric/PIN for sensitive actions.
11. Show privacy center, kill switch and action history.
12. Allow “Not now” and maintain functional fallbacks.

---

## 19. Roadmap

### V1 — Useful and Play-safe personal intelligence

- AI chat, voice and living presence.
- Local/OpenAI hybrid provider.
- Short/long memory foundations.
- Notes, reminders and calendar.
- Contact resolution and relationships.
- Official-intent app opening, maps, camera, files and media actions.
- Outgoing call initiation.
- SMS/WhatsApp/app message drafts and share flows.
- Notification access, summaries and supported direct replies.
- File/PDF search and vision.
- Safe confirmation engine, privacy guard and action history.
- Daily brief, night summary and basic proactive follow-up.

### V1.5 — Deeper device integration

- Capability registry and compatibility center.
- Better app-specific adapters and assisted WhatsApp flow.
- Incoming call announcements where permitted.
- Default-dialer prototype with full compliance UI.
- Supported answer/reject/speaker controls.
- Call quick responses, biometrics/PIN and extensive device testing.

### V2 — Mayra phone operating layer

- Full Mayra dialer.
- Disclosed AI call screening/message relay.
- Multi-step phone actions and workflow automation.
- Trusted-contact rules and richer identity graph.
- Advanced offline voice/model support.
- Contextual proactive routines and robust undo/recovery.

### V3/Future — Mayra ecosystem

- Cross-device encrypted sync.
- Wearables, car mode and smart home.
- Business/Home/Kids/Drive variants.
- Plugin marketplace and third-party developer SDK.
- Enterprise connectors and team workflows.

---

## 20. Current GitHub Code vs Blueprint Gap Analysis

### Already present as implemented foundation

- Compose Android application and CI.
- Local command assistant and hybrid OpenAI provider configuration.
- Voice recognition/TTS and continuous voice-loop policy.
- Animated living presence and Phone Pulse.
- Brain, context, task planning, agent registry/runtime and autonomy foundations.
- Personal knowledge/memory foundations.
- Device state intelligence.
- Android device action specifications/runners for existing supported actions.
- Automation bridge.
- Vision engine/platform/memory foundations.
- Privacy center foundations.
- Execution control plane, checkpoint recovery, runtime supervisor and adaptive scheduler.
- Runtime dashboard, approvals, workflow actions, attention notifications and background diagnostics.
- Settings/onboarding, permissions readiness and encrypted provider secret storage.
- Notification listener foundation.
- Deterministic test suites and Android lint gates.

### Partial and needing product integration

- Memory UI, consent and encrypted category-level retention.
- Reminders/notes/calendar end-to-end user screens.
- Notification summarisation/reply UI and privacy rules.
- Contact alias/relationship UI and robust disambiguation.
- File/PDF search and OCR end-to-end flow.
- App capability registry and verification semantics.
- Unified action history and undo.
- Proactive morning/night briefing orchestration.
- Domain assistants and high-stakes policy layers.

### Concept-level or future

- Default dialer, InCallService and call assistant.
- Verified WhatsApp/app send adapters beyond user-visible compose flows.
- General cross-app screen understanding/control.
- Cross-device sync, smart home, car mode, wearables and plugin marketplace.
- Advanced offline language model.

---

## 21. Missing Modules

Priority missing modules:

1. `MayraActionEngine` unified coordinator.
2. `MayraCapabilityRegistry`.
3. `MayraPermissionManager` unified special-access/role model.
4. `MayraConfirmationEngine` reusable action UI/state.
5. `MayraRiskClassifier` shared policy.
6. `MayraActionAuditLog` and redaction.
7. `MayraActionVerifier`.
8. `MayraFallbackPlanner`.
9. `MayraContactResolver` and identity store.
10. `MayraMessagingController`.
11. Notification intelligence pipeline and UI.
12. Reminder/notes/calendar end-to-end platform.
13. Compatibility manager.
14. Default dialer/call modules for V1.5/V2.

---

## 22. Coding Priority Order

1. Build the shared **Action Safety Core**: action request/result models, capability registry, permission state, risk classifier, confirmation state machine, verifier, fallback and audit log.
2. Migrate current app-open/call/message/reminder actions into this engine without deleting existing runners.
3. Build Contact & Identity Engine.
4. Build Messaging Controller with draft-first flows and notification reply.
5. Complete Notification Intelligence UI/privacy.
6. Complete reminders, notes and calendar.
7. Complete files/PDF/OCR search.
8. Add proactive briefs and goal follow-up.
9. Add compatibility center and device matrix testing.
10. Begin default-dialer prototype only after V1 safety core is stable and Play policy design is reviewed.

**Exact next batch:** `MayraActionEngine` + `MayraCapabilityRegistry` + `MayraRiskClassifier` + `MayraConfirmationEngine` + `MayraActionAuditLog` contracts and deterministic tests, followed by migration of one low-risk action and one medium/high-risk draft action.

---

## 23. Testing Strategy

- Pure unit tests for intent, contact resolution, risk, confirmation, dedupe, fallback and redaction.
- Contract tests for every capability adapter.
- Robolectric tests for intents, roles, permissions, notification actions and lifecycle.
- Instrumentation tests for Compose confirmation and settings flows.
- Fake Telecom/InCall service tests before device testing.
- Physical-device matrix across Android 8–latest supported, Samsung, Pixel, Motorola, Xiaomi/Redmi/POCO, OnePlus/Oppo/Vivo where available.
- Offline, metered, low-battery, thermal, permission-revoked and process-death tests.
- Security tests: exported components, pending intents, secret leakage, log redaction, replay/duplicate action, lock-screen restrictions.
- Policy tests that assert restricted actions can never bypass confirmation/authentication.
- Release gates: compile, targeted safety suites, full unit, lint, instrumentation smoke, manifest/permission review and privacy disclosure review.

---

## 24. Device Compatibility Strategy

- Runtime capability probing, not model assumptions.
- API-level adapters and OEM-specific quirks recorded in compatibility profiles.
- Feature status shown as Available, Setup required, Limited, Unsupported or Policy restricted.
- Official fallback for every optional integration.
- WorkManager timing presented as approximate.
- Telecom and media behavior tested per OEM.
- No critical V1 feature depends solely on Accessibility or unrestricted background execution.

---

## 25. Play Store Compliance Risks

1. **SMS/Call Log:** restricted; direct access generally requires active default SMS/Phone/Assistant role, approved core use and Play declaration. Prefer compose/dial intents in V1.
2. **Accessibility:** cannot be treated as a universal autonomous AI control channel. Use only for an allowed, transparently disclosed, policy-reviewed core use case; keep a non-Accessibility build path.
3. **Contacts:** use least privilege and Contact Picker where sufficient; broad access requires a clear core purpose and disclosure.
4. **Files/Photos:** prefer Storage Access Framework and Photo Picker over broad storage/media access.
5. **Notification access:** dedicated prominent disclosure, per-app privacy and minimal retention.
6. **Background work:** use WorkManager/foreground-service rules; no hidden continuous surveillance.
7. **Call recording/transcription:** no hidden recording; legal disclosure/consent and country/device restrictions.
8. **User data:** privacy policy, Data Safety form, in-app disclosure, deletion/export and secure handling.
9. **Device/network abuse:** no bypassing security, unauthorized APIs, hidden clicks or interference with other apps.
10. **Deceptive behavior:** Mayra never impersonates the user or claims an action succeeded without evidence.

Official references:

- Android `InCallService`: https://developer.android.com/reference/android/telecom/InCallService
- Play SMS/Call Log policy: https://support.google.com/googleplay/android-developer/answer/10208820
- Play AccessibilityService policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Play sensitive permissions: https://support.google.com/googleplay/android-developer/answer/16558241
- Prominent disclosure and consent: https://support.google.com/googleplay/android-developer/answer/11150561

---

## 26. Final Locked Master Feature List

The following remain permanently in product scope unless explicitly superseded by a later approved master decision:

- AI brain, multimodal conversation and multilingual voice/text.
- Living animated Mayra presence and Phone Pulse.
- Context, emotion-aware tone, memory and personal knowledge graph.
- Family/relationship/contact identity and trusted-contact logic.
- Reminders, notes, voice notes, calendar, briefs and follow-ups.
- Files, PDFs, OCR, images and vision memory.
- Search, weather, maps, calculations and conversions.
- Health, finance, travel, learning, work, media, family/home and business assistants.
- Goals, plans, workflows, agents, plugins and adaptive scheduling.
- Online multi-provider AI and limited offline intelligence.
- Encrypted local data, privacy center, permissions dashboard and kill switch.
- Self-diagnostics, runtime analytics, execution supervisor, recovery and audit history.
- Modular phone/app action system.
- Contact resolver and identity engine.
- Messaging drafts, supported sends, notification replies and protections.
- Notification intelligence and private summaries.
- Conditional default-dialer and disclosed call-assistant roadmap.
- Official Android actions, app integrations, safe fallbacks and compatibility reporting.
- Cross-device, smart-home, car, wearable, business and developer ecosystem roadmap.

---

## Final Merge Statement

### Preserved from the existing concept

All earlier conversation, memory, voice, vision, personal intelligence, agents, automation, privacy, runtime, phone-awareness, multi-provider and ecosystem features remain. Nothing has been removed or simplified.

### Added by this command

A complete Phone & App Control blueprint: action engine, capability tiers, messaging, incoming-call architecture, notification intelligence, contact identity, risk/confirmation/privacy guards, module contracts, UI, permission matrix, roadmap, compliance strategy and code-gap analysis.

### Technically conditional

Direct WhatsApp/third-party sends, complete cross-app control, broad Accessibility automation, direct SMS/call-log access, full incoming-call controls, AI call screening/relay, call transcription and some media/system settings depend on official app support, Android/OEM behavior, default-handler roles, legal consent and Play policy approval.

### Already present in current code

Substantial foundations for chat/voice, OpenAI/local providers, context, memory, agents, automation, device actions, notification listener, vision, privacy, execution supervision, adaptive scheduling, runtime control, settings/onboarding and living presence.

### Still concept/partial

Unified Action Safety Core, full contact identity, end-to-end messaging/notification intelligence, productivity screens, verified app adapters, default dialer/call assistant, cross-device and ecosystem features.

### Exact next coding module

Start with **Mayra Action Safety Core**: `MayraActionEngine`, `MayraCapabilityRegistry`, `MayraRiskClassifier`, `MayraConfirmationEngine`, `MayraPermissionManager`, `MayraActionVerifier`, `MayraFallbackPlanner` and `MayraActionAuditLog`, with deterministic tests and migration of existing actions into the new pipeline.
