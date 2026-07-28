# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Authoritative CI for this batch: pending

## Purpose

This rolling recovery snapshot is updated in every coding batch. Significant milestones also receive immutable dated snapshots.

## Product state

- Canonical blueprint, roadmap and mandatory update policy are present.
- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, provider eligibility and audited runtime boundary are implemented.
- Action idempotency, duplicate prevention and immutable activity records are implemented in-memory.
- Concrete assistant/document/action adapters and persisted user-visible history remain active work.

## Completed in this batch

- Added deterministic SHA-256 action idempotency keys normalized for case and whitespace.
- Added a thread-safe idempotency store using atomic reservation semantics.
- Successful actions retain their reservation and duplicate execution is blocked before handler invocation.
- Failed, blank-output and missing-handler actions release reservations so an explicit retry can proceed.
- Confirmation-required actions do not reserve a key before approval.
- Read-only answer and retrieval requests are not deduplicated.
- Added immutable `MayraActivityRecord` values with timestamp, outcome, disposition, status, capability, optional action key and detail.
- Added thread-safe in-memory activity log with defensive snapshots.
- Added typed `DuplicateBlocked` runtime result.
- Added eight idempotency/audit regression tests.
- Updated capability registry, registry tests, execution roadmap and this rolling backup.

## Safety contract

The router classifies intent. The policy gates capability and confirmation. The runtime invokes only an executable handler.

Action execution additionally requires an atomic idempotency reservation. A duplicate successful or in-progress action never reaches its handler. A failed action releases the reservation for a deliberate retry.

Activity records contain routing metadata and result detail; no hidden provider execution is performed by the audit layer.

## Current batch commits

- `b315e9c0a3fc654b936097c79cdd44ee7d970339` — activity/idempotency model
- `b768297a6dc2a47dc359ade7d5bcc82c36f8d705` — runtime enforcement
- `c0b9140f741fdccb7b9cd4857b54cceeed9a482f` — idempotency/audit tests
- `29d8252b0d6b70b9d8337ba4b944061a21f67f8a` — capability registry update
- `d80dee2626573d0bb0cba5d36bad8f77b1ec5d6b` — registry regression update
- `39be90c236ff01ba51804333beb92c7e703ec3da` — roadmap update

## Verified document capabilities

- Private local library metadata
- Plain text, text-based PDF and DOCX extraction
- Unicode search/snippets
- Local summaries and grounded Q&A
- Async indexing and parser limits
- Freshness and Current-only evidence
- Smart/force maintenance
- Transactional content/fingerprint commits
- Isolated zero-permission APK pipeline

## Physical-device truth

Owner verified APK installation/launch and PDF selection/metadata persistence. PDF text search, DOCX search, freshness UI, Smart refresh, transactional maintenance and scanned-PDF behavior remain unverified on phone.

## Validation truth

- Full Android CI on the latest snapshot head is required.
- Earlier green runs do not prove this batch.
- After CI passes, record exact head, run, artifacts and digest in PR metadata without moving the verified code head.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`.
2. Read `docs/MAYRA_ROADMAP.md`.
3. Confirm PR #12 remains Draft/unmerged.
4. Use only the newest fully green CI head as authoritative.
5. Do not overclaim physical validation.
6. Update this file after every coding batch.

## Next step

Run full CI, then add persisted activity history, confirmation-token lifecycle and concrete normal-answer, document-retrieval and confirmed-action adapters.