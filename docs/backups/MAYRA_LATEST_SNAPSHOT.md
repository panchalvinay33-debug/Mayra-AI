# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest source head before governance commits: `3bcf4e7e4bdb5cbb9c54895d707860229a6408be`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458
Previous APK artifact: `mayra-document-test-apk-1458`
Previous APK artifact ZIP SHA-256: `75751ce1848b73618adfcb10627420cb88756c0dd2125d164451d07d37d4b869`
Previous reports artifact SHA-256: `931d86bcc5bfdf9f8037ba7e90c8c21d1ae15783f9e5f4b67ab1e327c69a7a69`

## Validation truth

The current Memory Center management and answer-disclosure batch is newer than the last fully green functional head. Compile, complete unit tests, lint, R8 APK and permission/component audit are pending for the newest governed head. This work is `IN_PROGRESS`, not `DONE`.

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, audited runtime, confirmation, idempotency and persistent activity history remain implemented.
- Personal memory includes deterministic commands, approved/pending persistence, process-death restoration, visual approval, conflict review and approved-only answer context.
- Memory Center now adds key/value search, category filters, direct value edit and full pending-proposal review.
- Answers influenced by approved memory now append a visible disclosure naming the memory keys used.
- Direct expiry editing and protected at-rest migration remain pending.

## Completed in this coding batch

- Added Memory Center search across approved key/value text.
- Added category filter chips for all memory categories.
- Added direct owner edit dialog using existing privacy-policy revalidation and revision updates.
- Added pending proposal list with Save/Replace/Not now actions.
- Displayed current and proposed values for conflicting pending records.
- Expanded Clear all to cover approved memories and pending proposals.
- Added compact answer disclosure: `Used approved personal memory: <keys>`.
- Added regression assertions that disclosure appears only for approved relevant memory.
- Updated roadmap, blueprint and rolling snapshot.
- Added no Android permission, service, receiver or background component.

## Memory safety contract

- Conversation text is never silently stored.
- Only explicit approval persists a safe proposal.
- Prohibited and sensitive candidates are rejected before approval or edit.
- Pending proposals are bounded, local, expiring and replay-safe.
- Same-key contradictory values require visible comparison.
- Answer context uses only approved, active, relevant records and cannot write memory.
- Memory use is disclosed to the owner by memory key.
- Delete, reject and clear take effect immediately.

## Known limitations and risks

- Direct expiry editing/custom expiry selection is not implemented.
- Private app storage has not yet migrated to encrypted/Keystore-backed storage.
- The disclosure is currently appended as text rather than represented by a dedicated structured UI chip.
- UI behavior and process-restart flows remain unverified on the Motorola phone.
- The newest governed source has not yet received a full-green CI result.

## Physical-device truth

Owner previously verified APK installation/launch and PDF selection/metadata persistence in an earlier build. Memory search/filter/edit, pending review, Save/Replace, restart restoration, answer disclosure, PDF/DOCX search, freshness, Activity History, system picker and chat confirmation remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`, `docs/MAYRA_ROADMAP.md` and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft/unmerged.
3. Check the newest governed-head CI before claiming validation.
4. Use CI #1458 as the newest fully green evidence until a newer complete pipeline succeeds.
5. Do not overclaim physical validation.
6. Keep memory approval explicit and privacy exclusions conservative.
7. Update this snapshot after every coding batch.

## Next step

Run and stabilize the full governed CI chain. Then implement direct expiry editing and protected at-rest storage with backward-compatible migration and rollback tests, followed by Motorola owner-device acceptance for memory save, replace, restart, edit, filter and grounded-answer disclosure.
