# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest batch head before validation: `9fc0067f3ec7c415cbc7d45950182ecad23f2912`
Authoritative CI for this batch: pending

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, provider eligibility, audited runtime, idempotency and persistent history are implemented.
- Replay-safe confirmation and concrete answer/document/action adapter boundaries are implemented.
- Android composition root, Activity History UI and first narrow safe action are now code-complete and awaiting CI/device validation.
- Personal memory, fresh cited search, calendar/email/reminders and controlled Hindi/Hinglish voice remain major product tracks.

## Completed in this batch

- Added `MayraAndroidRuntimeComposition` as the Android composition boundary.
- Added a deliberately narrow device-action executor that opens Android's system document picker only.
- The safe executor requests no permission and rejects delete/send/call/SMS or any unregistered action.
- Added user-visible `MayraActivityHistoryActivity`.
- History UI shows newest-first status, outcome, capability, timestamp, detail and truncated action key.
- Added local clear and Android share-sheet export controls.
- Registered the screen in the full app and exposed it as a separate launcher entry in the isolated validation APK.
- Extended the isolated manifest audit to require the history component while preserving zero permissions and no background components.
- Added Android composition tests for normal answer, Current-only document retrieval, system picker launch and destructive-action rejection.
- Updated roadmap and this rolling recovery snapshot.

## Safety contract

- Persistent history is audit data, not execution permission.
- Destructive actions still require exact-action confirmation and idempotency, but the first Android executor deliberately does not implement destructive operations.
- Document retrieval remains Current-index only.
- The isolated APK continues to remove all permissions, services, receivers, providers and the main assistant application.

## Physical-device truth

Owner verified APK installation/launch and PDF selection/metadata persistence in an earlier build. PDF text search, DOCX search, freshness UI, Smart refresh, transactional maintenance, Activity History UI, export/clear, system-picker action and confirmation flows remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md` and `docs/MAYRA_ROADMAP.md`.
2. Confirm PR #12 remains Draft/unmerged.
3. Use only the newest fully green CI head as authoritative.
4. Do not overclaim physical validation.
5. Keep the first Android action executor narrow until each additional action receives explicit safety review and tests.
6. Update this snapshot after every coding batch.

## Next step

Run full Android CI. After green validation, add a non-blocking bridge from the existing suspend assistant/chat path into the typed runtime and implement main-chat confirmation dialog state without replacing stable voice behavior.
