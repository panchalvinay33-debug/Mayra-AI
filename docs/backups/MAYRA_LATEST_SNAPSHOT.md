# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Current governed head: `c084ce0da816de81fc10ccb2ffe19ba258a7423c`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative green CI: Android CI #1458
APK artifact: `mayra-document-test-apk-1458`
APK artifact ZIP SHA-256: `75751ce1848b73618adfcb10627420cb88756c0dd2125d164451d07d37d4b869`
Reports artifact SHA-256: `931d86bcc5bfdf9f8037ba7e90c8c21d1ae15783f9e5f4b67ab1e327c69a7a69`

## Validation truth

The current governed head is newer than the last fully green CI head. Compile, complete unit tests, lint, R8 APK and permission/component audit are pending for the current head. Therefore the new memory-chat work is `IN_PROGRESS`, not `DONE`, until a later full-green run exists.

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, capability gates, audited runtime, confirmation, idempotency and persistent activity history are implemented.
- Main-chat typed bridge and exact-action Confirm/Cancel UX are CI-verified but not owner-device verified.
- Consent-first personal-memory core and persistent Android storage are implemented.
- User-visible Memory Center is implemented with view, source, revision, expiry, delete, clear and export controls.
- Deterministic memory chat commands are now implemented for remember, confirm, cancel, forget and list operations.
- Visual Save/Not now confirmation, pending-proposal process recovery, conflict review and approved-memory answer-context integration remain active work.

## Completed in this coding batch

- Added `MayraMemoryChatController` as a model-independent local command boundary.
- Added English, Hindi and Hinglish aliases for explicit remember/list/forget requests.
- Added exact `confirm memory <proposalId>` and `cancel memory <proposalId>` handling.
- Kept proposal creation non-persistent until explicit confirmation.
- Reused the existing privacy classifier so passwords, PINs, OTPs, card identifiers, Aadhaar and other blocked candidates cannot be proposed for storage.
- Added deterministic category inference for preference, project, routine, relationship, profile and other memories.
- Integrated memory command interception into `MayraChatRuntimeBridge` before normal model delegation.
- Installed the controller from `ChatViewModel` only when the personal-memory runtime is available.
- Added regression tests for explicit approval, prohibited-secret rejection, cancellation/replay prevention, listing, forgetting and unrelated-chat delegation.
- Updated roadmap, canonical blueprint and this rolling snapshot.
- Added no Android permission, service, receiver or background component.

## Memory safety contract

- Conversation text must never be silently stored as personal memory.
- Only explicit approval may move a safe proposal into persistent storage.
- Prohibited and sensitive candidates are rejected before approval.
- Retrieval uses only active approved records.
- Provenance is visible in Memory Center.
- Delete and clear take effect immediately.
- Pending proposals currently remain in memory and do not survive process death.
- Memory Center currently supports deletion but not direct value/expiry editing.
- The conversational AI provider cannot bypass the deterministic memory controller.

## Known limitations and risks

- Confirmation currently uses an exact text reply rather than the main chat visual confirmation dialog.
- Pending memory proposals are not persisted across process death.
- A contradictory value for the same key can replace the existing value after approval without a dedicated conflict-review screen.
- Approved memories are not yet injected into normal assistant answer context.
- Current head has not yet completed the governed CI chain.
- Physical-device memory command validation has not occurred.

## Physical-device truth

Owner previously verified APK installation/launch and PDF selection/metadata persistence in an earlier build. Memory Center, memory chat commands, PDF/DOCX search, freshness, Activity History, system picker, chat Confirm/Cancel and rotation retention remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`, `docs/MAYRA_ROADMAP.md` and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft/unmerged.
3. Use CI #1458 on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af` as the newest fully verified functional head until a later full-green run exists.
4. Treat `c084ce0da816de81fc10ccb2ffe19ba258a7423c` as the current governed source head requiring CI.
5. Do not overclaim physical validation.
6. Keep memory approval explicit and sensitive-memory exclusions conservative.
7. Update this snapshot after every coding batch.

## Next step

Run the complete governed CI chain on the current head. Fix any compile, test, lint, R8 or audit failure. Then implement visual Save/Not now confirmation, process-death-safe pending proposals, contradiction review and transparent injection of only approved relevant memories into normal assistant context.
