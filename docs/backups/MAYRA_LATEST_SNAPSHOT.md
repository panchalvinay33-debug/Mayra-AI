# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest verified head: `0753ea4051548a30bccc103c22c736a0c6e29015`
Authoritative CI: Android CI #1333
APK artifact: `mayra-document-test-apk-1333`
APK artifact ZIP SHA-256: `2f8acec002e8e62f10b77f9ff032bdf4c9782863a501b4b1783b24bcf2202072`

## Purpose

This rolling recovery snapshot is updated in every coding batch. Significant milestones also receive immutable dated snapshots.

## Product state

- Canonical blueprint, roadmap and mandatory update policy are present.
- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, provider eligibility and audited runtime boundary are implemented.
- Action idempotency, duplicate prevention and immutable activity records are implemented in-memory and CI-verified.
- Concrete assistant/document/action adapters and persisted user-visible history remain active work.

## Completed in this batch

- Added deterministic SHA-256 action idempotency keys normalized for case and whitespace.
- Added a thread-safe idempotency store using atomic reservation semantics.
- Successful actions retain their reservation and duplicate execution is blocked before handler invocation.
- Failed, blank-output and missing-handler actions release reservations so an explicit retry can proceed.
- Confirmation-required actions do not reserve a key before approval.
- Read-only answer and retrieval requests are not deduplicated.
- Added immutable `MayraActivityRecord` values and a thread-safe in-memory activity log with defensive snapshots.
- Added typed `DuplicateBlocked` runtime result.
- Added idempotency/audit regression tests.
- Updated capability registry, registry tests, execution roadmap and this rolling backup.

## Validation

Android CI #1333 passed on `0753ea4051548a30bccc103c22c736a0c6e29015`:

- Kotlin compile passed
- complete unit-test suite passed
- Android lint passed
- isolated R8 APK assembly passed
- manifest/permission/component audit passed
- requested Android permissions: none
- APK and reports upload passed

Artifacts:

- `mayra-document-test-apk-1333` — `sha256:2f8acec002e8e62f10b77f9ff032bdf4c9782863a501b4b1783b24bcf2202072`
- `android-reports-1333` — `sha256:cb519d4f279643fbb3df8b44b6b57e44f9ea7317760a447bef7816dad763c5c4`

## Safety contract

The router classifies intent. The policy gates capability and confirmation. The runtime invokes only an executable handler.

Action execution additionally requires an atomic idempotency reservation. A duplicate successful or in-progress action never reaches its handler. A failed action releases the reservation for a deliberate retry.

## Physical-device truth

Owner verified APK installation/launch and PDF selection/metadata persistence. PDF text search, DOCX search, freshness UI, Smart refresh, transactional maintenance and scanned-PDF behavior remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`.
2. Read `docs/MAYRA_ROADMAP.md`.
3. Confirm PR #12 remains Draft/unmerged.
4. Use Android CI #1333 on head `0753ea4051548a30bccc103c22c736a0c6e29015` as the latest authoritative proof.
5. Do not overclaim physical validation.
6. Update this file after every coding batch.

## Next step

Add persisted activity history, confirmation-token lifecycle and concrete normal-answer, document-retrieval and confirmed-action adapters.