# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-27
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest code-and-governance head validated before documentation finalization: `3d829bfa57851141b64fe7933f54b9d0e21ced9e`
Authoritative CI: Android CI #1275
APK artifact: `mayra-document-test-apk-1275`
Reports artifact: `android-reports-1275`
APK artifact ZIP SHA-256: `d72817f2eb98a8831aed56cede243cd7b2b2ac5a5ac9a13940b5df497316789b`

## Purpose

This is the rolling recovery snapshot. It must be updated in every coding batch. Significant milestones also receive an immutable dated snapshot beside this file.

## Product state

- Canonical product blueprint exists at `docs/MAYRA_BLUEPRINT.md`.
- Canonical execution roadmap exists at `docs/MAYRA_ROADMAP.md`.
- Mandatory update policy exists at `docs/BLUEPRINT_UPDATE_POLICY.md`.
- Immutable document-foundation snapshot is preserved.
- Global machine-testable `MayraCapabilityRegistry` is implemented and validated.
- Document intelligence foundation remains 16 of 18 implemented features.
- Document work is not the default active module; only device-found defects should return immediately.
- Next active program track: typed core assistant routing outcomes.

## Global capability registry state

The registry tracks 13 top-level capabilities across Core assistant, Documents, Memory, Search, Actions, Voice, Privacy, Release and Governance. It uses `DONE`, `DEVICE_VERIFY`, `IN_PROGRESS`, `PLANNED`, and `DEFERRED`, enforces unique IDs, validates complete counts, and keeps OCR/legacy DOC explicitly deferred rather than allowing them to block broader development.

## Verified document capabilities

- Local private library metadata
- Plain text, text-based PDF and DOCX extraction
- Unicode Hindi/Hinglish/English search and snippets
- Local summaries and grounded Q&A
- Async indexing and parser limits
- Freshness metadata using parser version, source size and provider modified time
- Current-only evidence policy
- Smart refresh, force rebuild and health inventory
- Transactional content/fingerprint commits with rollback
- Isolated, minified, zero-permission document-test APK pipeline

## Physical-device evidence

Verified by owner:
- isolated document-test APK installed and launched on Motorola phone;
- PDF selected and saved in the library;
- PDF metadata persisted/displayed.

Not yet independently verified on phone:
- PDF text re-index/search in latest build;
- DOCX add/index/search;
- freshness badge and same-size modified-time change;
- Smart refresh and transactional maintenance UX;
- scanned-PDF behavior.

## Deferred document milestones

- On-device OCR for scanned pages and images
- Legacy binary `.doc` parsing

## Completed in this governance batch

- Added canonical product blueprint
- Added full multi-module execution roadmap
- Added rolling latest recovery snapshot
- Added immutable document-foundation milestone snapshot
- Added mandatory blueprint/roadmap/backup update policy
- Added global capability registry
- Added registry uniqueness/count/status regression tests
- Passed compile, complete tests, Android lint, R8 isolated APK assembly and zero-permission/component audit in CI #1275

## Current documentation-finalization head

The roadmap and this snapshot were updated after CI #1275, producing a documentation-only newer head. A final CI run on that latest head is required before it becomes the authoritative latest-head proof.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md` for product boundaries.
2. Read `docs/MAYRA_ROADMAP.md` for current status and next sequence.
3. Confirm PR #12 remains Draft and unmerged.
4. Use the most recent green CI head as source of truth.
5. Do not claim physical validation beyond the evidence listed above.
6. Update this snapshot after every coding batch with new head, CI, artifacts, completed work, risks and next step.

## Next step

Run final CI on the documentation-finalization head, then begin a focused typed-routing milestone only after confirming branch/PR strategy. The routing model should distinguish `ANSWER`, `RETRIEVE`, `ACT`, `CLARIFY`, and `UNSUPPORTED` with explicit reason and confidence metadata.