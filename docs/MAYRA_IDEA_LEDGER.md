# Mayra AI — Idea Ledger

Last updated: 2026-08-03

This ledger records product ideas from acceptance through delivery, deferral, replacement or removal. Ideas must never disappear silently from the roadmap.

## Status meanings

- `ACCEPTED`: approved direction; not necessarily scheduled.
- `IN_PROGRESS`: active implementation.
- `DELIVERED`: implemented and relevant automated checks passed.
- `DEVICE_VERIFY`: implemented/CI-verified; physical Motorola validation pending.
- `DEFERRED`: intentionally postponed.
- `SUPERSEDED`: replaced by a better design; replacement recorded.
- `REMOVED`: intentionally dropped; reason recorded.

## Active ideas

| ID | Idea | Status | Current implementation truth | Next gate |
|---|---|---|---|---|
| IDEA-001 | One personal Mayra app with one launcher | DEVICE_VERIFY | Main Chat, Library, Memory, Provider and History are internal screens under one launcher | Motorola one-icon acceptance |
| IDEA-002 | Natural Hindi/Hinglish/English companion | DEVICE_VERIFY | Local conversation foundation, voice input/TTS and optional online provider implemented | Physical speech quality and long-conversation testing |
| IDEA-003 | Mayra must work without OpenAI/API | ACCEPTED | Deterministic offline engine exists; full local LLM not integrated | Select, benchmark and integrate an on-device model |
| IDEA-004 | Optional cloud intelligence, not cloud dependency | DEVICE_VERIFY | Responses-compatible provider, Keystore credentials, live enable/disable and fallback implemented | Owner API-key and failure-mode test |
| IDEA-005 | Owner-approved personal memory | DEVICE_VERIFY | Propose/approve/use/edit/delete/expiry and trusted provenance implemented | Motorola recovery and lifecycle checks |
| IDEA-006 | Private document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX import, indexing, search, summary and grounded answers implemented | Physical PDF/DOCX acceptance |
| IDEA-007 | Mayra-owned reminders | DEVICE_VERIFY | Persistent parser/scheduler/notification/actions/follow-up/reboot recovery implemented | Timing, Doze, Snooze, Complete and reboot tests |
| IDEA-008 | Open apps and prepare phone actions | DEVICE_VERIFY | App open, contact resolution, dialer/composer handoffs and expiring confirmations implemented | Motorola action acceptance |
| IDEA-009 | Animated Mayra presence | DEVICE_VERIFY | Animated VoiceInteractionSession foundation is CI #1851 verified | Real listening/thinking/speaking state wiring and device invocation |
| IDEA-010 | Mayra as Android system Assistant | DEVICE_VERIFY | VoiceInteractionService/session/recognition metadata foundation is CI verified | Role selection and invocation on Motorola |
| IDEA-011 | Always-available wake phrase | ACCEPTED | Recognition-service shell only; no production wake-word engine | Choose engine and battery policy after J1 |
| IDEA-012 | Background/lock-screen voice operation | DEVICE_VERIFY | Assistant-role and lock-screen session foundation added | System-role invocation and background proof |
| IDEA-013 | Advanced incoming-call control | ACCEPTED | Existing flow only opens dialer for outgoing calls | Build optional Phone/InCallService and Call Screening modules |
| IDEA-014 | Caller announce, answer, reject, mute, speaker | ACCEPTED | Not implemented | Default Phone role UI/runtime + owner acceptance |
| IDEA-015 | AI takes a message from caller | ACCEPTED_WITH_CONSTRAINTS | Standard cellular audio injection/capture is not assumed | Voicemail/VoIP or supported device route |
| IDEA-016 | Proactive notifications and daily context | ACCEPTED | Notification listener/background foundations exist | Owner controls, relevance policy and validation |
| IDEA-017 | Personal Owner Mode with fewer repeated confirmations | ACCEPTED | Routine low-risk actions can be streamlined; destructive guards remain | Define trust levels and per-action policy |
| IDEA-018 | Production-signed final Mayra | IN_PROGRESS | Minified release audit and environment-only signing scaffold implemented | Private signing, provenance and distribution |
| IDEA-019 | Stable owner APK updates without uninstall | IN_PROGRESS | Personal Alpha supports secret-backed stable signing; dedicated workflow added | Configure secrets and pass A→B update/data-retention test |
| IDEA-020 | Very simple first-start setup | IN_PROGRESS | Two-step permission + Assistant-role onboarding committed | Compile/lint and Motorola first-launch test |

## Deferred ideas

| ID | Idea | Status | Reason / promotion condition |
|---|---|---|---|
| IDEA-101 | Scanned document OCR | DEFERRED | Promote after PDF/DOCX pipeline and local-brain performance are stable |
| IDEA-102 | Legacy binary `.doc` parsing | DEFERRED | Users can convert to DOCX; not blocking current product |
| IDEA-103 | Exact-alarm permission | DEFERRED | Promote only if physical reminder tests prove owner need |
| IDEA-104 | Root-only unrestricted phone control | DEFERRED | Fragile and high-risk; official Android roles first |
| IDEA-105 | Generic autonomous accessibility tapping | DEFERRED | Prefer official intents/APIs and explicit integrations |

## Superseded or removed ideas

| ID | Previous idea | Status | Replaced by / reason |
|---|---|---|---|
| IDEA-X01 | Multiple user-facing apps | SUPERSEDED | One user-facing Mayra app; engineering variants are not the product |
| IDEA-X02 | Direct `CALL_PHONE` execution | SUPERSEDED | Review-first dialer; advanced control through Phone role |
| IDEA-X03 | Direct silent `SEND_SMS` execution | SUPERSEDED | Message composer with owner final Send action |
| IDEA-X04 | Memory attribution inside visible text markers | REMOVED | Typed trusted response metadata prevents spoofing |
| IDEA-X05 | Persist raw confirmation token across process death | REMOVED | Stale tokens expire; action must be requested again |
| IDEA-X06 | Treat low-permission Full Test as final app | REMOVED | Full Test is an engineering build only |
| IDEA-X07 | Scatter required permissions across feature screens | SUPERSEDED | One minimal first-launch owner setup, with later recovery controls only when needed |

## Update rule

Whenever the owner introduces or changes an idea, update this ledger, roadmap, blueprint when applicable, latest snapshot and decision/changelog records. Removed ideas remain recorded with reasons.
