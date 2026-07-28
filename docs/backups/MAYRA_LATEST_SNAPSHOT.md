# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest authoritative code head: `e9ab540a9b81e8d846a63d81890c839e6305b473`
Authoritative CI: Android CI #1427
APK artifact: `mayra-document-test-apk-1427`
APK artifact ZIP SHA-256: `794aa8a8bf2fb75ef27a7c7a8181af1d7de97773ac21ff9885cf34e5b05f9138`
Reports artifact SHA-256: `255a06851978858b322d58760189b620e61e897f80d080cebf87f541a8edcec2`

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, provider eligibility, audited runtime, idempotency and persistent history are implemented.
- Android composition root, Activity History and main-chat typed bridge are implemented and CI-verified.
- Exact-action Confirm/Cancel UX is implemented in the main chat.
- Normal conversational answers still use the stable suspend assistant and voice path.
- Personal memory, fresh cited search, calendar/email/reminders and controlled Hindi/Hinglish voice remain major product tracks.

## Completed in this batch

- Added `MayraChatRuntimeBridge`.
- Normal `ANSWER` outcomes delegate to the existing suspend assistant.
- `RETRIEVE`, `ACT`, `CLARIFY` and `UNSUPPORTED` outcomes use the typed runtime.
- Added pending-confirmation state to `ChatUiState` and `ChatViewModel`.
- Pending state survives Android configuration changes because it is retained by the ViewModel.
- Added main-chat Confirm/Cancel dialog showing the exact requested action.
- Confirm consumes the one-time token and appends the typed execution result to chat.
- Cancel clears the pending action and appends an explicit cancellation message.
- Input, voice, clear and duplicate sends are locked while confirmation is pending.
- Added bridge tests for stable-answer delegation, document retrieval, token creation, exact execution and replay blocking.
- Android CI #1427 passed compile, complete tests, lint, R8 isolated APK, zero-permission/component audit and artifact upload.

## Safety contract

- Persistent history is audit data, not execution permission.
- Confirmation tokens remain bound to one normalized action and cannot be replayed.
- Destructive operations remain unsupported by the first Android executor even after confirmation.
- Document retrieval remains Current-index only.
- Normal chat and voice behavior are preserved rather than replaced by the typed runtime.
- Pending confirmation is configuration-safe but is not yet persisted across process death.

## Physical-device truth

Owner verified APK installation/launch and PDF selection/metadata persistence in an earlier build. PDF text search, DOCX search, freshness UI, Smart refresh, transactional maintenance, Activity History, export/clear, system-picker action, main-chat Confirm/Cancel and rotation retention remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md` and `docs/MAYRA_ROADMAP.md`.
2. Confirm PR #12 remains Draft/unmerged.
3. Use Android CI #1427 on `e9ab540a9b81e8d846a63d81890c839e6305b473` as the newest fully verified code head until a later full-green run exists.
4. Do not overclaim physical validation.
5. Keep the first Android action executor narrow until each additional action receives explicit safety review and tests.
6. Update this snapshot after every coding batch.

## Next step

Begin consent-first personal memory: explicit save approval, provenance, sensitive-memory exclusions, edit/delete/expiry, deterministic retrieval and user-facing controls.
