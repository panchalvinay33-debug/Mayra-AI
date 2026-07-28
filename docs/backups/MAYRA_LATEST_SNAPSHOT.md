# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest batch head before final validation: `70f989a34a17906b3a81fdd8a1274e86fbb47577`
Authoritative CI for this head: pending

## Purpose

This rolling recovery snapshot is updated in every coding batch. Significant milestones also receive immutable dated snapshots.

## Product state

- Canonical blueprint, roadmap and mandatory update policy are present.
- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed core routing and provider/tool eligibility policy are implemented.
- An audited typed routing runtime boundary is now implemented and awaiting full CI.
- Next active milestone after validation: idempotency, activity records and concrete adapters.

## Completed in this batch

- Detected that a newer integrated typed router and provider policy already existed on the branch.
- Removed a duplicate standalone typed-router implementation and duplicate tests before they could create conflicting enum/data-class declarations.
- Preserved the integrated `MayraRoutingDecision` compatibility contract.
- Added `MayraRoutingRuntimeResult` variants: `Executed`, `ConfirmationRequired`, `ClarificationRequired`, `Blocked`, `Failed`.
- Added `MayraRouteHandler` and `MayraRuntimeHandlers` adapter boundary.
- Added `MayraRoutingRuntime` dispatcher after classification and policy planning.
- Guaranteed confirmation, clarification and blocked plans do not invoke handlers.
- Converted missing handlers, blank handler output and thrown exceptions into typed failures.
- Added ten runtime regression tests covering answer, retrieval, action, confirmation, capability blocking, OCR blocking and failure behavior.
- Updated the execution roadmap and this rolling backup.

## Safety contract

The router classifies intent. The policy plans eligibility and confirmation. The runtime dispatcher invokes only the handler selected by an executable plan.

A handler is never invoked when:

- clarification is required;
- the capability is blocked or unavailable;
- the feature is explicitly unsupported;
- destructive-action confirmation is still pending.

Provider exceptions and empty outputs are returned as typed failures rather than crashing or silently falling through.

## Current batch commits

- `d8d42059219ee674e0de46867516636c12b5cabc` — remove duplicate typed router
- `971586b9ddb415d7c51763e3bccb2248af22d30d` — remove duplicate tests
- `01139d2d12f7e29edfcad9cfb7768efd9b49324d` — audited runtime boundary
- `8bd9d695502f970c6df775aa41e57482707e2344` — runtime regression suite
- `70f989a34a17906b3a81fdd8a1274e86fbb47577` — roadmap update

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
- Do not treat earlier runs or earlier heads as proof for this batch.
- After CI passes, record exact head, run, artifacts and digest without moving the code head again.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`.
2. Read `docs/MAYRA_ROADMAP.md`.
3. Confirm PR #12 remains Draft/unmerged.
4. Use only the newest fully green CI head as authoritative.
5. Do not overclaim physical validation.
6. Update this file after every coding batch.

## Next step

Run full CI, then implement idempotency and immutable activity records before connecting concrete answer, document and confirmed-action adapters.
