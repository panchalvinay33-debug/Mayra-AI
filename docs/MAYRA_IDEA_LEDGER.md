# Mayra AI — Idea Ledger

Last updated: 2026-08-03

This ledger records product ideas from acceptance through delivery, deferral, replacement or removal. Ideas must never disappear silently from the roadmap.

## Status meanings

- `ACCEPTED`: approved direction; not necessarily scheduled.
- `IN_PROGRESS`: active implementation.
- `DELIVERED`: implemented and relevant automated checks passed.
- `DEVICE_VERIFY`: implemented/CI-verified; physical Motorola validation pending or partial.
- `BENCHMARK`: feasibility direction recorded; candidate testing required before implementation/promotion.
- `DEFERRED`: intentionally postponed.
- `SUPERSEDED`: replaced by a better design; replacement recorded.
- `REMOVED`: intentionally dropped; reason recorded.

## Active ideas

| ID | Idea | Status | Current implementation truth | Next gate |
|---|---|---|---|---|
| IDEA-001 | One personal Mayra app with one launcher | DEVICE_VERIFY | Final app keeps Chat, Library, Memory, Provider and History internal; J1/J2 are disposable engineering packages | Motorola final-app one-icon acceptance |
| IDEA-002 | Natural Hindi/Hinglish/English companion | DEVICE_VERIFY | Deterministic local conversation, app voice/TTS and optional provider foundations exist | J2 speech proof, then local-brain benchmark |
| IDEA-003 | Mayra must work without OpenAI/API | BENCHMARK | Deterministic offline engine exists; local LLM preflight recorded; LiteRT-LM runtime direction + Qwen3-1.7B initial candidate | Motorola model/runtime benchmark |
| IDEA-004 | Optional cloud intelligence, not cloud dependency | DEVICE_VERIFY | Responses-compatible provider, Keystore credentials, live enable/disable and local fallback implemented | Owner API-key/failure-mode test |
| IDEA-005 | Owner-approved personal memory | DEVICE_VERIFY | Propose/approve/use/edit/delete/expiry and trusted provenance implemented | Motorola recovery/lifecycle checks |
| IDEA-006 | Private document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX import, indexing, search, summary and grounded answers implemented | Physical PDF/DOCX acceptance |
| IDEA-007 | Mayra-owned reminders | DEVICE_VERIFY | Persistent parser/scheduler/notification/actions/follow-up/reboot recovery implemented | Timing, Doze, Snooze, Complete and reboot tests |
| IDEA-008 | Open apps and prepare phone actions | DEVICE_VERIFY | App open, contact resolution, dialer/composer handoffs and expiring confirmations implemented | Motorola action acceptance |
| IDEA-009 | Animated Mayra presence | DEVICE_VERIFY | Orb physically invoked on Motorola; Back/lock dismiss work; direct tap repair is J2 CI-green | J2 tap/repeat/lock-screen retest |
| IDEA-010 | Mayra as Android system Assistant | DEVICE_VERIFY | Motorola accepts/selects Mayra and unlocked Power-button invocation launches session | J2 locked-screen/reboot/recovery completion |
| IDEA-011 | Always-available `Mayra` wake phrase | BENCHMARK | Dedicated KWS architecture preflight recorded; continuous SpeechRecognizer loop rejected; sherpa-onnx first candidate only | J2 physical pass, then KWS false-trigger/battery benchmark |
| IDEA-012 | Background/lock-screen voice operation | DEVICE_VERIFY | Assistant role/keyguard foundation exists; unlocked invocation proven | Already-locked J2 invocation + reboot test |
| IDEA-013 | Advanced incoming-call control | ACCEPTED | Phone-role preflight recorded; no Phone takeover implemented | Build complete isolated Dialer/InCallService UI/runtime before role request |
| IDEA-014 | Caller announce, answer, reject, mute, speaker | ACCEPTED | Public Telecom path documented; deterministic call-command state machine required | Phone UI/runtime tests, then Motorola role acceptance |
| IDEA-015 | AI takes a message from caller | ACCEPTED | Direct arbitrary SIM audio path rejected; call-forwarding/VoIP/telephony endpoint architecture documented | Select provider/carrier route + privacy/retention design |
| IDEA-016 | Proactive notification intelligence | ACCEPTED | NotificationListener foundation exists; local-first/sensitive-filter preflight recorded | Owner controls, filter tests and Motorola Notification Access acceptance |
| IDEA-017 | Personal Owner Mode with fewer repeated confirmations | ACCEPTED | Routine low-risk actions can be streamlined; destructive guards remain | Define trust levels/per-action policy |
| IDEA-018 | Production-signed final Mayra | IN_PROGRESS | Minified release audit and environment-only signing scaffold implemented | Private signing/provenance/trusted distribution |
| IDEA-019 | Stable owner APK updates without uninstall | IN_PROGRESS | Personal Alpha/J1/J2 can use secret-backed owner signing when configured; trusted-install preflight recorded | Configure secrets + A→B update/data-retention proof |
| IDEA-020 | Very simple first-start setup | IN_PROGRESS | Two-step permission + Assistant-role onboarding committed | Trusted full-app Motorola first-launch test |
| IDEA-021 | Zero-permission J1 Assistant test APK | DEVICE_VERIFY | `ai.mayra.app.j1` selection and unlocked invocation physically proven | Touch lifecycle/locked-start/reboot completion through J2 |
| IDEA-022 | Isolated J2 real voice proof | DEVICE_VERIFY | `ai.mayra.app.j2` exact-source J2 #18/J1 #122/Android #2013/Governance #194 green; exactly RECORD_AUDIO | Motorola J2 speech/lifecycle/reboot acceptance |
| IDEA-023 | Invocation-time local speech before local LLM | DEVICE_VERIFY | On-device Android SpeechRecognizer wrapper and bounded state pipeline are CI-green | Verify target-device on-device availability + Hindi/Hinglish/English transcripts |
| IDEA-024 | Dedicated local wake-word engine | BENCHMARK | Wake preflight + Motorola benchmark contract recorded; no production dependency integrated | Compare KWS candidate behavior on device |
| IDEA-025 | LiteRT-LM local brain runtime | BENCHMARK | Runtime/model benchmark contracts recorded; Qwen3-1.7B is candidate only | Reproducible model conversion/load + Motorola RAM/thermal/quality test |
| IDEA-026 | Complete Mayra default-Phone runtime | ACCEPTED | Safety gate requires full Dialer/InCallService UI before asking for Phone role | Isolated call UI/state/adapters + CI |
| IDEA-027 | Mayra-managed caller-message service | ACCEPTED | Supported forwarding/VoIP architecture required; local SIM-audio shortcut rejected | Telephony provider/carrier proof with test number |
| IDEA-028 | Local-first notification summaries | ACCEPTED | Notification Access architecture/sensitive exclusions documented | Policy tests + owner device acceptance |
| IDEA-029 | Reliable cross-app workflows | ACCEPTED | API/intents/deep-link-first policy recorded; generic LLM Accessibility tapping rejected | Add typed adapters per real workflow |
| IDEA-030 | Trusted Play/Internal owner distribution | IN_PROGRESS | Stable-signing + Internal Testing architecture documented | Play Console/internal track + signed A→B install test |

## Deferred ideas

| ID | Idea | Status | Reason / promotion condition |
|---|---|---|---|
| IDEA-101 | Scanned document OCR | DEFERRED | Promote after PDF/DOCX pipeline and local-brain performance are stable |
| IDEA-102 | Legacy binary `.doc` parsing | DEFERRED | Users can convert to DOCX; not blocking current product |
| IDEA-103 | Exact-alarm permission | DEFERRED | Promote only if physical reminder tests prove owner need |
| IDEA-104 | Root-only unrestricted phone control | DEFERRED | Fragile/high-risk; official Android roles first |
| IDEA-105 | Generic autonomous Accessibility tapping | DEFERRED | Official APIs/intents first; only narrow deterministic workflows can be reconsidered |
| IDEA-106 | Continuous SpeechRecognizer hotword loop | DEFERRED | API is not intended for continuous recognition; dedicated wake detector required |

## Superseded or removed ideas

| ID | Previous idea | Status | Replaced by / reason |
|---|---|---|---|
| IDEA-X01 | Multiple user-facing apps | SUPERSEDED | One final Mayra app; J1/J2 are temporary engineering proof packages |
| IDEA-X02 | Direct `CALL_PHONE` execution | SUPERSEDED | Review-first dialer; advanced control through complete Phone role runtime |
| IDEA-X03 | Direct silent `SEND_SMS` execution | SUPERSEDED | Message composer with owner final Send action |
| IDEA-X04 | Memory attribution inside visible text markers | REMOVED | Typed trusted response metadata prevents spoofing |
| IDEA-X05 | Persist raw confirmation token across process death | REMOVED | Stale tokens expire; action must be requested again |
| IDEA-X06 | Treat low-permission Full Test as final app | REMOVED | Full Test is engineering-only |
| IDEA-X07 | Scatter required permissions across feature screens | SUPERSEDED | Minimal first-launch owner setup, with recovery controls only when needed |
| IDEA-X08 | Use Accessibility as Mayra's universal action engine | REMOVED | Typed APIs/intents/roles first; Accessibility only narrow deterministic per-workflow if ever justified |
| IDEA-X09 | Direct cellular AI answering through ordinary InCallService audio | REMOVED | Supported forwarding/VoIP/telephony endpoint required for real AI caller conversation/message-taking |

## Update rule

Whenever the owner introduces or changes an idea, update this ledger, roadmap, blueprint when applicable, latest snapshot and decision/changelog records. Removed ideas remain recorded with reasons.
