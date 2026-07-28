# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest batch documentation head: `1cb982d0f7fb6508ccd82bac0be9f4648023fcd8`
Authoritative CI for this head: pending

## Purpose

This rolling recovery snapshot is updated in every coding batch. Significant milestones also receive immutable dated snapshots.

## Product state

- Canonical blueprint, roadmap and mandatory update policy are present.
- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed core routing is implemented.
- Runtime provider/tool eligibility policy is implemented and awaiting full CI.
- Next active milestone after validation: audited runtime result and adapter integration.

## Completed in this batch

- Added `MayraRuntimeCapabilities` as a deterministic availability snapshot.
- Added route dispositions: `EXECUTE`, `CONFIRM`, `CLARIFY`, `FALLBACK`, `BLOCK`.
- Added `MayraRoutingPolicy` after intent classification and before any provider/action execution.
- Blocked document retrieval when the local library capability is unavailable.
- Blocked device actions when action capability is unavailable.
- Required explicit confirmation for destructive actions even when capability exists.
- Kept unsupported OCR/legacy DOC requests blocked even if a runtime flag is accidentally enabled.
- Preserved blank-input clarification and normal answer execution.
- Added nine routing-policy regression tests.
- Advanced `core.provider-eligibility` to DONE.
- Added `core.runtime-integration` as the active IN_PROGRESS capability.
- Updated execution roadmap and this rolling backup.

## Safety contract

The router classifies intent only. The policy layer only plans execution. Neither layer invokes providers, changes device state or bypasses confirmation.

A route may execute only when:

- its required capability is available;
- it is not explicitly unsupported/deferred;
- clarification is not required;
- destructive action confirmation is not pending.

## Current batch commits

- `2c1b66a10b1f26024b814d54ed8536b92ed05214` — routing policy
- `2667ec29043fc714b5b49de2154e0500ea65ce2e` — policy regressions
- `097ff5d671fe7797f9ddea2610c6d3548db0198f` — capability registry advancement
- `e3510335900344bd9a6df93e9599c181fef0c89c` — registry test advancement
- `1cb982d0f7fb6508ccd82bac0be9f4648023fcd8` — roadmap update

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
- Do not treat earlier cancelled or earlier-head runs as proof for this batch.
- After CI passes, record exact head, run, artifacts and digest without moving the code head again.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`.
2. Read `docs/MAYRA_ROADMAP.md`.
3. Confirm PR #12 remains Draft/unmerged.
4. Use only the newest fully green CI head as authoritative.
5. Do not overclaim physical validation.
6. Update this file after every coding batch.

## Next step

Run full CI, then implement a typed runtime result envelope, provider/action adapter boundary, idempotency and end-to-end assistant runtime tests.
