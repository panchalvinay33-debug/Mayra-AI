# Mayra AI — Idea Ledger

Last updated: 2026-08-03

This ledger records product ideas from acceptance through delivery, deferral, replacement or removal. Ideas must never disappear silently from the roadmap.

## Status meanings

- `ACCEPTED`: approved direction; not necessarily scheduled.
- `IN_PROGRESS`: active implementation.
- `DELIVERED`: implemented and relevant automated checks passed.
- `DEVICE_VERIFY`: implemented/CI-verified; physical Motorola validation pending or partial.
- `DEFERRED`: intentionally postponed.
- `SUPERSEDED`: replaced by a better design; replacement recorded.
- `REMOVED`: intentionally dropped; reason recorded.

## Active ideas

| ID | Idea | Status | Current implementation truth | Next gate |
|---|---|---|---|---|
| IDEA-001 | One personal Mayra app with one launcher | DEVICE_VERIFY | Main Chat, Library, Memory, Provider and History are internal screens under one launcher | Motorola final-app one-icon acceptance |
| IDEA-002 | Natural Hindi/Hinglish/English companion | DEVICE_VERIFY | Local conversation foundation, existing voice input/TTS and optional provider implemented | Physical speech quality and long-conversation testing |
| IDEA-003 | Mayra must work without OpenAI/API | ACCEPTED | Deterministic offline engine exists; full local LLM not integrated | Local-model feasibility/benchmark |
| IDEA-004 | Optional cloud intelligence, not cloud dependency | DEVICE_VERIFY | Responses-compatible provider, Keystore credentials, live enable/disable and fallback implemented | Owner API-key/failure-mode test |
| IDEA-005 | Owner-approved personal memory | DEVICE_VERIFY | Propose/approve/use/edit/delete/expiry and trusted provenance implemented | Motorola recovery/lifecycle checks |
| IDEA-006 | Private document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX import, indexing, search, summary and grounded answers implemented | Physical PDF/DOCX acceptance |
| IDEA-007 | Mayra-owned reminders | DEVICE_VERIFY | Persistent parser/scheduler/notification/actions/follow-up/reboot recovery implemented | Timing, Doze, Snooze, Complete and reboot tests |
| IDEA-008 | Open apps and prepare phone actions | DEVICE_VERIFY | App open, contact resolution, dialer/composer handoffs and expiring confirmations implemented | Motorola action acceptance |
| IDEA-009 | Animated Mayra presence | DEVICE_VERIFY | Orb was physically invoked on Motorola; Back/lock dismiss work; direct touch-dismiss repair committed | Fresh CI + tap/repeat/lock-screen retest |
| IDEA-010 | Mayra as Android system Assistant | DEVICE_VERIFY | Motorola accepts/selects Mayra and unlocked Power-button invocation launches the session | Reboot/lock-screen/recovery completion |
| IDEA-011 | Always-available wake phrase | ACCEPTED | Recognition shell only; SpeechRecognizer loop explicitly rejected as hotword architecture | Dedicated wake-word engine preflight/benchmark |
| IDEA-012 | Background/lock-screen voice operation | DEVICE_VERIFY | Assistant role and keyguard-capable session foundation exist; unlocked invocation proven | Locked-screen physical test |
| IDEA-013 | Advanced incoming-call control | ACCEPTED | Existing flow only opens dialer for outgoing calls | Default Phone/InCallService feasibility review |
| IDEA-014 | Caller announce, answer, reject, mute, speaker | ACCEPTED | Not implemented | Phone-role UI/runtime + owner acceptance |
| IDEA-015 | AI takes a message from caller | ACCEPTED_WITH_CONSTRAINTS | Standard cellular audio injection/capture is not assumed | Voicemail/VoIP or supported device route |
| IDEA-016 | Proactive notifications and daily context | ACCEPTED | Notification listener/background foundations exist | Owner controls/relevance/privacy validation |
| IDEA-017 | Personal Owner Mode with fewer repeated confirmations | ACCEPTED | Routine low-risk actions can be streamlined; destructive guards remain | Define trust levels/per-action policy |
| IDEA-018 | Production-signed final Mayra | IN_PROGRESS | Minified release audit and environment-only signing scaffold implemented | Private signing/provenance/distribution |
| IDEA-019 | Stable owner APK updates without uninstall | IN_PROGRESS | Personal Alpha/J1/J2 can use secret-backed owner signing when configured | Configure secrets and pass A→B update/data-retention test |
| IDEA-020 | Very simple first-start setup | IN_PROGRESS | Two-step permission + Assistant-role onboarding committed | Trusted full-app Motorola first-launch test |
| IDEA-021 | Zero-permission J1 Assistant test APK | DEVICE_VERIFY | `ai.mayra.app.j1` selection and unlocked invocation are physically proven on Motorola | Touch lifecycle, lock-screen and reboot completion |
| IDEA-022 | Isolated J2 real voice proof | IN_PROGRESS | `ai.mayra.app.j2`, one-permission manifest, on-device STT state pipeline and dedicated CI committed | Compile/lint/audit, then Motorola microphone/STT test |
| IDEA-023 | Invocation-time local speech before local LLM | IN_PROGRESS | J2 prefers Android on-device SpeechRecognizer only after explicit invocation | Verify availability and Hindi/Hinglish/English recognition on target device |

## Deferred ideas

| ID | Idea | Status | Reason / promotion condition |
|---|---|---|---|
| IDEA-101 | Scanned document OCR | DEFERRED | Promote after PDF/DOCX pipeline and local-brain performance are stable |
| IDEA-102 | Legacy binary `.doc` parsing | DEFERRED | Users can convert to DOCX; not blocking current product |
| IDEA-103 | Exact-alarm permission | DEFERRED | Promote only if physical reminder tests prove owner need |
| IDEA-104 | Root-only unrestricted phone control | DEFERRED | Fragile/high-risk; official Android roles first |
| IDEA-105 | Generic autonomous accessibility tapping | DEFERRED | Prefer official intents/APIs and explicit integrations |
| IDEA-106 | Continuous SpeechRecognizer hotword loop | DEFERRED | API is not intended for continuous recognition; dedicated wake-word detector required |

## Superseded or removed ideas

| ID | Previous idea | Status | Replaced by / reason |
|---|---|---|---|
| IDEA-X01 | Multiple user-facing apps | SUPERSEDED | One final Mayra app; J1/J2 are temporary engineering proof packages |
| IDEA-X02 | Direct `CALL_PHONE` execution | SUPERSEDED | Review-first dialer; advanced control through Phone role |
| IDEA-X03 | Direct silent `SEND_SMS` execution | SUPERSEDED | Message composer with owner final Send action |
| IDEA-X04 | Memory attribution inside visible text markers | REMOVED | Typed trusted response metadata prevents spoofing |
| IDEA-X05 | Persist raw confirmation token across process death | REMOVED | Stale tokens expire; action must be requested again |
| IDEA-X06 | Treat low-permission Full Test as final app | REMOVED | Full Test is engineering-only |
| IDEA-X07 | Scatter required permissions across feature screens | SUPERSEDED | Minimal first-launch owner setup, with recovery controls only when needed |

## Update rule

Whenever the owner introduces or changes an idea, update this ledger, roadmap, blueprint when applicable, latest snapshot and decision/changelog records. Removed ideas remain recorded with reasons.
