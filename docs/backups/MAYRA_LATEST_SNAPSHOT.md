# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest batch head before validation: `f4213b14a484ab3180e650b60c9f26c26380b3c6`
Authoritative CI for this latest batch: pending
Previous fully green validation: Android CI #1407 on `80395aa758398d37f18541e00ca1e0735cff5ae6`

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, provider eligibility, audited runtime, idempotency and persistent history are implemented.
- Replay-safe confirmation and concrete answer/document/action adapter boundaries are implemented.
- Android composition root is now installed in the full application container.
- Activity History is reachable from the main chat and from the isolated validation APK.
- The existing suspend assistant, document wrapper and voice behavior remain preserved.
- Personal memory, fresh cited search, calendar/email/reminders and controlled Hindi/Hinglish voice remain major product tracks.

## Completed in this batch

- Verified Android CI #1407 completed successfully for the previous visible-runtime head.
- Installed `MayraAndroidRuntimeComposition` during `MayraApplication.onCreate()`.
- Reused the deterministic local command engine as the typed normal-answer provider without network access.
- Exposed the installed typed runtime and persistent activity log through the application service container.
- Preserved the stable suspend chat assistant, document-aware wrapper, voice assistant and background initialization behavior.
- Added a History chip to the main chat header.
- History navigation opens the persistent Activity History screen directly.
- Kept the first Android action limited to the permission-free system document picker.
- Updated roadmap and this rolling recovery snapshot.

## Safety contract

- Persistent history is audit data, not execution permission.
- Destructive actions still require exact-action confirmation and idempotency, but the first Android executor deliberately does not implement destructive operations.
- Document retrieval remains Current-index only.
- The isolated APK continues to remove all permissions, services, receivers, providers and the main assistant application.
- The typed runtime was added alongside the stable assistant path rather than replacing voice/chat behavior prematurely.

## Physical-device truth

Owner verified APK installation/launch and PDF selection/metadata persistence in an earlier build. PDF text search, DOCX search, freshness UI, Smart refresh, transactional maintenance, Activity History UI, export/clear, main-chat History navigation, system-picker action and confirmation flows remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md` and `docs/MAYRA_ROADMAP.md`.
2. Confirm PR #12 remains Draft/unmerged.
3. Use only the newest fully green CI head as authoritative.
4. Do not overclaim physical validation.
5. Keep the first Android action executor narrow until each additional action receives explicit safety review and tests.
6. Update this snapshot after every coding batch.

## Next step

Run full Android CI on the latest governed head. After green validation, add main-chat confirmation dialog state and a non-blocking typed runtime bridge without replacing stable voice behavior.
