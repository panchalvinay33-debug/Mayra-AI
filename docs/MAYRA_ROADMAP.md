# Mayra AI — Execution Roadmap

Last updated: 2026-07-29
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest backup: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Overall program view

| Track | Status | Progress signal | Next gate |
|---|---|---|---|
| Blueprint and backup discipline | DONE | Canonical blueprint, roadmap and rolling snapshot | Keep updated every verified batch |
| Single-app packaging | CI_VERIFIED | One APK and exactly one launcher (`MainActivity`) verified by CI #1667 | Install latest artifact and confirm one icon on Motorola |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX extraction, search, summaries, grounded Q&A and health tools implemented | PDF/DOCX owner-device acceptance; OCR and legacy DOC deferred |
| Core routing and eligibility | DONE | Typed outcomes, capability gates and safe confirmation flow | Keep regression suite green |
| Personal memory | DEVICE_VERIFY | Explicit approval, provenance, edit/delete/expiry and protected storage implemented | Motorola save/use/edit/delete/recovery checks |
| Trusted assistant metadata | IN_PROGRESS | String-marker parser removed; typed response contract added | CI #1679 and owner-device memory attribution check |
| Conversational provider | FOUNDATION_VERIFIED | Bounded HTTPS transport and owner settings exist | Separate audited network flavor and Keystore credentials |
| Search and fresh knowledge | PLANNED | No completion claim | Provider composition, citations and freshness contract |
| Actions and automations | DEVICE_VERIFY | Confirmation/idempotency/chat UX implemented | Permission-scoped capability build and physical validation |
| Voice intelligence | DEVICE_VERIFY | Microphone-only safe full-test path | Hindi/Hinglish physical evaluation |
| Privacy and release | IN_PROGRESS | Safe full-test strips contacts/call/SMS/background/Internet; isolated document APK remains zero-permission | Production flavor separation, signing and release review |

## Verification truth

### Authoritative single-app baseline

Android CI #1667 passed on head `496f5d043b4adf5c446f14d14e4adfd13a7c0918`.

It verified:

1. Debug and `fullTest` Kotlin compilation.
2. Complete debug unit-test suite.
3. Debug, full-test and document-test lint.
4. Safe `fullTest` APK assembly.
5. Exactly one launchable activity: `ai.mayra.app.MainActivity`.
6. Internal availability of Document Library, Memory Center, Provider Settings and Activity History.
7. Microphone permission present for voice testing.
8. Contacts, direct-call, SMS, notification, exact-alarm, boot and INTERNET permissions absent.
9. Notification listener and boot receiver absent from the safe full-test package.
10. Isolated minified document-test APK regression build and audit.

### Historical correction

CI #1631 verified only the isolated `documentTest` APK. It was never evidence that the complete Mayra application was packaged. CI #1647/#1653 established the first full-test packaging path, but the broadly privileged sideload build triggered Play Protect review. The safe full-test variant introduced after that removes high-risk declarations while retaining the complete user-facing app surfaces.

## Current coding batch

The assistant response boundary is being migrated from text-embedded memory markers to a trusted typed result:

- `MayraAssistantResponse(text, usedPersonalMemoryKeys)`
- `MayraStructuredAssistant.replyStructured(...)`
- `PersonalMemoryAwareMayraAssistant` returns metadata out-of-band
- `ChatViewModel` consumes typed metadata directly
- legacy marker parser and its tests have been removed
- normalization/deduplication regression tests have been added

This prevents provider or document text from impersonating internal memory-use metadata.

## Immediate next priority

1. Make CI #1679 green for the typed-response refactor.
2. Download the resulting one-icon safe full-test APK.
3. Uninstall older Mayra test packages once, install the latest APK, and verify only one launcher icon.
4. Run Main Chat, capability reply, Library, Memory, Provider, History and microphone acceptance checks.
5. Continue provider isolation and secure credential work only after the offline/safe build remains stable.

PR #12 remains Draft, open and unmerged.
