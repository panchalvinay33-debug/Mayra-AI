# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Current governed source head before this snapshot record: `b1de1d30b01ec0baff0d140490d6d1c8e426ab1e`
Active validation: Android CI #1479 — in progress at snapshot time
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458
Previous APK artifact: `mayra-document-test-apk-1458`
Previous APK artifact ZIP SHA-256: `75751ce1848b73618adfcb10627420cb88756c0dd2125d164451d07d37d4b869`
Previous reports artifact SHA-256: `931d86bcc5bfdf9f8037ba7e90c8c21d1ae15783f9e5f4b67ab1e327c69a7a69`

## Validation truth

Android CI #1479 is validating the new memory-chat batch. Until it completes successfully, compile, complete unit tests, lint, R8 APK and permission/component audit are not claimed for the new head. The memory-chat work remains `IN_PROGRESS`, not `DONE`.

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, capability gates, audited runtime, confirmation, idempotency and persistent activity history are implemented.
- Main-chat typed bridge and exact-action Confirm/Cancel UX are CI-verified but not owner-device verified.
- Consent-first personal-memory core and persistent Android storage are implemented.
- User-visible Memory Center is implemented with view, source, revision, expiry, delete, clear and export controls.
- Deterministic memory chat commands are implemented for remember, confirm, cancel, forget and list operations.
- Visual Save/Not now confirmation, pending-proposal process recovery, conflict review and approved-memory answer-context integration remain active work.

## Completed in this coding batch

- Added `MayraMemoryChatController` as a model-independent local command boundary.
- Added English, Hindi and Hinglish aliases for explicit remember/list/forget requests.
- Added exact `confirm memory <proposalId>` and `cancel memory <proposalId>` handling.
- Kept proposal creation non-persistent until explicit confirmation.
- Reused the existing privacy classifier so blocked secrets and identifiers cannot be proposed for storage.
- Added deterministic category inference.
- Integrated memory command interception into `MayraChatRuntimeBridge` before normal model delegation.
- Installed the controller from `ChatViewModel` only when personal memory is available.
- Added regression tests for approval, prohibited-secret rejection, cancellation/replay prevention, listing, forgetting and unrelated-chat delegation.
- Updated roadmap, canonical blueprint and rolling snapshot.
- Added no Android permission, service, receiver or background component.

## Memory safety contract

- Conversation text must never be silently stored as personal memory.
- Only explicit approval may move a safe proposal into persistent storage.
- Prohibited and sensitive candidates are rejected before approval.
- Retrieval uses only active approved records.
- Provenance is visible in Memory Center.
- Delete and clear take effect immediately.
- Pending proposals currently remain in memory and do not survive process death.
- The conversational AI provider cannot bypass the deterministic memory controller.

## Known limitations and risks

- Confirmation currently uses an exact text reply rather than the main chat visual confirmation dialog.
- Pending memory proposals are not persisted across process death.
- Contradictory facts do not yet receive a dedicated conflict-review screen.
- Approved memories are not yet injected into normal assistant answer context.
- Physical-device memory command validation has not occurred.

## Physical-device truth

Owner previously verified APK installation/launch and PDF selection/metadata persistence in an earlier build. Memory Center, memory chat commands, PDF/DOCX search, freshness, Activity History, system picker, chat Confirm/Cancel and rotation retention remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`, `docs/MAYRA_ROADMAP.md` and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft/unmerged.
3. Check Android CI #1479 before claiming the new batch validated.
4. Use CI #1458 as the newest fully green evidence until #1479 or a later run succeeds.
5. Do not overclaim physical validation.
6. Keep memory approval explicit and sensitive-memory exclusions conservative.
7. Update this snapshot after every coding batch.

## Next step

Complete Android CI #1479 and fix any regression. Then implement visual Save/Not now confirmation, process-death-safe pending proposals, contradiction review and transparent injection of only approved relevant memories into normal assistant context.
