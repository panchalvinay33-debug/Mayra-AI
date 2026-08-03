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
| IDEA-001 | One personal Mayra app with one launcher | DEVICE_VERIFY | Main Chat, Library, Memory, Provider and History are internal screens under one launcher; audited variants exist only for engineering | Motorola one-icon acceptance |
| IDEA-002 | Natural Hindi/Hinglish/English companion | DEVICE_VERIFY | Local conversation foundation, voice input/TTS and optional online provider implemented | Physical speech quality and long-conversation testing |
| IDEA-003 | Mayra must work without OpenAI/API | ACCEPTED | Deterministic offline engine exists; full local LLM not integrated | Select, benchmark and integrate an on-device model |
| IDEA-004 | Optional cloud intelligence, not cloud dependency | DEVICE_VERIFY | OpenAI Responses-compatible provider, Keystore credentials, live enable/disable and offline fallback implemented | Owner API-key and failure-mode device test |
| IDEA-005 | Owner-approved personal memory | DEVICE_VERIFY | Propose/approve/use/edit/delete/expiry and trusted provenance implemented | Complete Motorola recovery and lifecycle checks |
| IDEA-006 | Private document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX import, indexing, search, summary, grounded answers and health tooling implemented | Physical PDF/DOCX acceptance |
| IDEA-007 | Mayra-owned reminders | DEVICE_VERIFY | Persistent parser/scheduler/notification/actions/follow-up/reboot recovery implemented | Timing, Doze, Snooze, Complete and reboot device tests |
| IDEA-008 | Open apps and prepare phone actions | DEVICE_VERIFY | App open, contact resolution, dialer/composer handoffs and expiring confirmations implemented | Motorola action acceptance |
| IDEA-009 | Animated Mayra presence | IN_PROGRESS | Animated VoiceInteractionSession foundation added | CI validation and real listening/thinking/speaking state wiring |
| IDEA-010 | Mayra as Android system Assistant | IN_PROGRESS | VoiceInteractionService/session/recognition metadata foundation added | Role selection and invocation on Motorola |
| IDEA-011 | Always-available wake phrase | ACCEPTED | Recognition-service shell only; no production wake-word engine yet | Choose offline hotword engine and battery policy |
| IDEA-012 | Background/lock-screen voice operation | IN_PROGRESS | Assistant-role and lock-screen session foundation added | System-role invocation and background behavior device proof |
| IDEA-013 | Advanced incoming-call control | ACCEPTED | Existing flow only opens dialer for outgoing calls | Build optional default Phone/InCallService and Call Screening modules |
| IDEA-014 | Caller announce, answer, reject, mute, speaker | ACCEPTED | Not implemented | Default Phone role UI/runtime + owner acceptance |
| IDEA-015 | AI takes a message from caller | ACCEPTED_WITH_CONSTRAINTS | Standard cellular audio injection/capture is not assumed | Design voicemail/VoIP answering path or device-specific supported route |
| IDEA-016 | Proactive notifications and daily context | ACCEPTED | Notification listener/background foundations exist | Owner controls, relevance policy and physical validation |
| IDEA-017 | Personal Owner Mode with fewer repeated confirmations | ACCEPTED | Routine low-risk actions can be streamlined; destructive guards remain | Define owner trust levels and per-action policy |
| IDEA-018 | Production-signed final Mayra | IN_PROGRESS | Minified release audit and environment-only signing scaffold implemented | Private signing setup, provenance and distribution |

## Deferred ideas

| ID | Idea | Status | Reason / promotion condition |
|---|---|---|---|
| IDEA-101 | Scanned document OCR | DEFERRED | Promote after current PDF/DOCX pipeline and local-brain device performance are stable |
| IDEA-102 | Legacy binary `.doc` parsing | DEFERRED | Users can convert to DOCX; not blocking current product |
| IDEA-103 | Exact-alarm permission for second-level reminder timing | DEFERRED | Current WorkManager design is safer; promote only if physical tests prove owner need |
| IDEA-104 | Root-only unrestricted phone control | DEFERRED | Fragile, device-update-sensitive and high-risk; official Android roles first |
| IDEA-105 | Generic autonomous accessibility tapping across apps | DEFERRED | Reliability and privacy concerns; prefer official intents/APIs and explicit integrations |

## Superseded or removed ideas

| ID | Previous idea | Status | Replaced by / reason |
|---|---|---|---|
| IDEA-X01 | Multiple user-facing Library/Document/Full Test apps | SUPERSEDED | One user-facing Mayra app; engineering variants are not the product |
| IDEA-X02 | Direct `CALL_PHONE` execution | SUPERSEDED | Review-first dialer now; advanced control will use default Phone role where appropriate |
| IDEA-X03 | Direct silent `SEND_SMS` execution | SUPERSEDED | Message composer with owner final Send action |
| IDEA-X04 | Store memory attribution inside visible assistant text markers | REMOVED | Typed trusted response metadata prevents spoofing |
| IDEA-X05 | Persist raw action-confirmation token across process death | REMOVED | Token store is memory-bound; stale tokens must expire and action must be requested again |
| IDEA-X06 | Treat low-permission Full Test as the final app | REMOVED | Personal Alpha/final release are the real capability tracks; Full Test is only a safer engineering build |

## Update rule

Whenever the owner introduces or changes an idea:

1. assign or reuse an ID;
2. record the owner outcome and platform constraint;
3. update status here;
4. update roadmap priority;
5. update blueprint if architecture/data/permission/background behavior changes;
6. update latest snapshot;
7. record supersession or removal rather than deleting history.
