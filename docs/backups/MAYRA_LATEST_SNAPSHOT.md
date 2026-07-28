# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest authoritative functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative CI: Android CI #1458
APK artifact: `mayra-document-test-apk-1458`
APK artifact ZIP SHA-256: `75751ce1848b73618adfcb10627420cb88756c0dd2125d164451d07d37d4b869`
Reports artifact SHA-256: `931d86bcc5bfdf9f8037ba7e90c8c21d1ae15783f9e5f4b67ab1e327c69a7a69`

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, capability gates, audited runtime, confirmation, idempotency and persistent activity history are implemented.
- Main-chat typed bridge and exact-action Confirm/Cancel UX are CI-verified but not owner-device verified.
- Consent-first personal-memory core and persistent Android storage are implemented.
- User-visible Memory Center is implemented with view, source, revision, expiry, delete, clear and export controls.
- Chat memory proposal UX and approved-memory answer-context integration remain active work.
- Fresh cited search, broader actions and controlled Hindi/Hinglish voice remain major product tracks.

## Completed in this batch

- Installed `AndroidMayraPersonalMemoryStore` and `MayraPersonalMemoryManager` in the application service container.
- Pruned expired memories at application startup.
- Added `MayraMemoryCenterActivity`.
- Displayed approved memory key, value, category, provenance, revision, updated time and optional expiry.
- Added explicit per-memory delete confirmation.
- Added explicit clear-all confirmation.
- Added local text export through Android's share sheet.
- Added a temporary launcher entry so the owner can validate Memory Center directly on a physical phone.
- Preserved the rule that no memory is created without explicit approval.
- Kept memory local and added no new Android permission.
- CI #1457 caught the incorrect Compose `setContent` import; the import was corrected.
- Android CI #1458 passed compile, complete tests, lint, R8, isolated zero-permission/component audit and artifact uploads.

## Memory safety contract

- Conversation text must never be silently stored as personal memory.
- Only explicit approval may move a safe proposal into persistent storage.
- Prohibited and sensitive candidates are rejected before approval.
- Retrieval uses only active approved records.
- Provenance is visible in Memory Center.
- Delete and clear take effect immediately.
- Pending proposals remain in-memory and do not survive process death.
- Memory Center currently supports deletion but not direct value/expiry editing.

## Physical-device truth

Owner previously verified APK installation/launch and PDF selection/metadata persistence in an earlier build. Memory Center launch, list rendering, export, delete/clear confirmations, PDF/DOCX search, freshness, Activity History, system picker, chat Confirm/Cancel and rotation retention remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md` and `docs/MAYRA_ROADMAP.md`.
2. Confirm PR #12 remains Draft/unmerged.
3. Use CI #1458 on `abfa2711f01bb526d1c6fdb93364aa8ea148c6af` as the newest fully verified functional head until a later full-green run exists.
4. Do not overclaim physical validation.
5. Keep memory approval explicit and sensitive-memory exclusions conservative.
6. Remove or consolidate the temporary Memory Center launcher entry only after main-chat navigation is implemented and device-tested.
7. Update this snapshot after every coding batch.

## Next step

Add chat detection for explicit memory requests, `Save this memory?` with Save/Not now controls, edit/expiry controls, and transparent injection of only approved relevant memories into normal assistant context. Then run full CI on the latest governed head.
