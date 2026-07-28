# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest implementation head before this snapshot: `041c6dbaab69ecd608343e4b8af8683d05461a07`
CI #1287: compile and complete tests passed; externally cancelled during lint, therefore not authoritative.

## Purpose

This rolling recovery snapshot is updated in every coding batch. Significant milestones also receive immutable dated snapshots.

## Product state

- Canonical blueprint, roadmap and mandatory update policy are present.
- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed core routing is implemented.
- Next active milestone: provider/tool eligibility gates.

## Completed in this batch

- Added typed outcomes: `ANSWER`, `RETRIEVE`, `ACT`, `CLARIFY`, `UNSUPPORTED`.
- Added explicit reason and confidence validation.
- Added required-capability metadata.
- Added destructive-action confirmation boundary.
- Preserved backward compatibility with `DOCUMENTS` and `DELEGATE` consumers.
- Routed blank input to clarification.
- Routed document evidence requests to retrieval.
- Routed normal conversation to answer.
- Routed device/file commands to actions.
- Routed unavailable scanned-PDF OCR and legacy DOC requests to explicit unsupported results.
- Added English/Hindi and contract regression tests.
- Updated global capability registry: typed routing DONE; provider eligibility IN_PROGRESS.

## Current routing contract

Every decision includes:

- compatibility route;
- typed outcome;
- confidence in the range 0–100;
- human-readable reason;
- matched deterministic signals;
- required capability;
- confirmation requirement for state-changing actions.

The router classifies intent only. It does not execute tools or bypass confirmation.

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

## CI truth

- CI #1287 is not authoritative because it was cancelled during lint.
- Compile: passed.
- Complete unit tests: passed.
- Lint/R8/APK audit: not completed in that run.
- A fresh full CI must pass on the latest snapshot head before artifact claims are updated.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`.
2. Read `docs/MAYRA_ROADMAP.md`.
3. Confirm PR #12 remains Draft/unmerged.
4. Use only the newest fully green CI head as authoritative.
5. Do not overclaim physical validation.
6. Update this file after every coding batch.

## Next step

Run full CI on the latest documentation head, then implement provider/tool eligibility: capability availability, permission/privacy, network/freshness and confirmation gates with deterministic fallback reasons.