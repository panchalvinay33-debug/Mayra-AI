# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest source head before governance commits: `d098930a60b39f4e47333df4deaae37edf24bd94`
Active validation: Android CI #1505 — in progress when recorded
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458
Previous APK artifact: `mayra-document-test-apk-1458`
Previous APK artifact ZIP SHA-256: `75751ce1848b73618adfcb10627420cb88756c0dd2125d164451d07d37d4b869`
Previous reports artifact SHA-256: `931d86bcc5bfdf9f8037ba7e90c8c21d1ae15783f9e5f4b67ab1e327c69a7a69`

## Validation truth

Android CI #1505 is validating the durable memory-approval source batch. Until a full-green run exists for the governed source, compile, complete unit tests, lint, R8 APK and permission/component audit are not claimed. This work remains `IN_PROGRESS`, not `DONE`.

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, capability gates, audited runtime, confirmation, idempotency and persistent activity history are implemented.
- Main-chat typed bridge and exact-action confirmation remain pending owner-device verification.
- Personal memory now includes deterministic commands, approved-memory persistence, bounded pending-proposal persistence, process-death restoration, visual Save/Not now review, contradiction comparison and read-only relevant-memory answer context.
- Memory Center edit/expiry/search controls and protected at-rest migration remain pending.

## Completed in this coding batch

- Added `MayraPendingMemoryProposal` and a bounded pending-proposal store contract.
- Added Android SharedPreferences persistence with versioned Unicode-safe codec and corruption recovery.
- Added proposal TTL pruning, replay-safe removal and startup restoration.
- Added stale-conflict rejection when approved state changes before confirmation.
- Added same-key conflict detection and revision-preserving replacement.
- Added structured `PendingMemoryApproval` chat results.
- Added visual Save/Not now and Replace/Not now dialogs showing current and proposed values.
- Locked chat input, voice and clear actions while a memory approval is pending.
- Added `PersonalMemoryAwareMayraAssistant` to inject only approved, active, query-relevant memory as read-only context.
- Reordered application composition so memory is installed before the assistant decorator.
- Added regressions for structured approval, conflict replacement, restoration, persistence, corrupt records and approved-only context injection.
- Updated roadmap, blueprint and rolling snapshot.
- Added no Android permission, service, receiver or background component.

## Memory safety contract

- Conversation text is never silently stored.
- Only explicit Save/Replace or exact deterministic confirmation persists a safe proposal.
- Prohibited and sensitive candidates are rejected before approval.
- Pending proposals are bounded, local, expiring and replay-safe.
- Same-key contradictory values require visible comparison.
- Stale conflicts are rejected rather than overwriting newer state.
- Answer context uses only approved active relevant records and cannot write memory.
- Delete and clear take effect immediately.
- The conversational provider cannot bypass deterministic memory controls.

## Known limitations and risks

- Only the newest pending proposal is automatically restored into chat after process death; older bounded proposals remain stored but have no management UI yet.
- Memory Center still lacks direct edit, expiry and search/filter controls.
- Approved-memory use is not yet visibly badged in each answer.
- Approved and pending records use private app storage but are not yet migrated to encrypted/Keystore-backed storage.
- Physical-device validation of save, replace, restart restoration and context behavior has not occurred.

## Physical-device truth

Owner previously verified APK installation/launch and PDF selection/metadata persistence in an earlier build. Memory Center, memory commands, Save/Replace dialogs, restart restoration, answer context, PDF/DOCX search, freshness, Activity History, system picker and chat confirmation remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`, `docs/MAYRA_ROADMAP.md` and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft/unmerged.
3. Check Android CI #1505 and later governed-head runs before claiming validation.
4. Use CI #1458 as the newest fully green evidence until a newer complete pipeline succeeds.
5. Do not overclaim physical validation.
6. Keep memory approval explicit and exclusions conservative.
7. Update this snapshot after every coding batch.

## Next step

Fix any CI regression, then add Memory Center edit/expiry/search controls and protected at-rest migration with backward-compatible recovery. After that, perform owner-device tests for save, replace, process restart and memory-grounded answers.
