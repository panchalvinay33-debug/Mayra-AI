# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest source head before governance commits: `72c39a6ad303e0a8fa7586034e03d87fd20dc24e`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458

## Validation truth

The protected-storage and expiry batch is newer than the latest fully green functional head. Compile, complete unit tests, lint, R8 APK and permission/component audit are pending on the newest governed head. Status remains `IN_PROGRESS`, not `DONE`.

## Completed in this batch

- Added AES-GCM per-record protection backed by Android Keystore for approved memories.
- Added a separate Keystore alias and protected envelopes for pending proposals.
- Added versioned encrypted envelopes with random IV and authenticated ciphertext.
- Added backward-compatible plaintext record decoding and migration to protected storage.
- Added failure-safe persistence: all records are protected before SharedPreferences replacement.
- Added corrupt/undecryptable record skipping to prevent startup crashes.
- Added deterministic injectable protector contracts for unit tests.
- Added migration, protected-write, unreadable-record and encryption-failure rollback tests.
- Updated existing Robolectric store tests to avoid depending on a real Android Keystore provider.
- Added Memory Center expiry presets: one day, seven days, thirty days and never expire.
- Expiry changes retain the existing value/category, record owner-expiry provenance and increment revision through the normal proposal/approval path.
- Added no Android permission, service, receiver or background component.

## Safety and privacy contract

- Conversation text is never silently stored.
- Approved and pending records are protected separately at rest.
- Legacy plaintext is deleted only after successful protected rewrite.
- Protection failure preserves the previous stored set.
- Tampered or undecryptable records are not used as memory evidence.
- Expired records are pruned and excluded from retrieval.
- Readable export remains an explicit owner action and is not a protected backup.

## Known limitations and risks

- Android Keystore behavior, key creation and migration are not yet verified on the Motorola owner device.
- Key invalidation/device-lock edge cases do not yet have a user-visible storage-health screen.
- The newest governed source does not yet have a full-green CI result.
- Answer memory provenance is text disclosure rather than a structured UI chip.
- No portable encrypted backup/export format exists yet.

## Recovery instructions

1. Read the blueprint, roadmap and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft and unmerged.
3. Check the newest governed-head CI before claiming validation.
4. Use CI #1458 as authoritative green evidence until superseded by a complete newer run.
5. Do not claim Motorola validation without actual device evidence.
6. Never clear legacy plaintext merely because Keystore protection failed.
7. Update this snapshot after every coding batch.

## Next step

Run and stabilize the full CI chain. Then add Keystore/storage-health diagnostics and owner-visible recovery states, followed by Motorola migration/restart/expiry acceptance and the production conversational-provider milestone.
