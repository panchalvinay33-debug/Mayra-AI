# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest batch head before validation: `431fb1e839a3b98560c7d834069fc271ff9c417e`
Authoritative CI for this batch: pending

## Purpose

This rolling recovery snapshot is updated in every coding batch. Significant milestones also receive immutable dated snapshots.

## Product state

- Canonical blueprint, roadmap and mandatory update policy are present.
- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, provider eligibility, audited runtime boundary, idempotency and duplicate prevention are implemented.
- Bounded persistent activity history and replay-safe confirmation tokens are now implemented and awaiting authoritative CI.
- Concrete normal-answer/document/action adapters and user-facing history remain active work.

## Completed in this batch

- Added SharedPreferences-backed, versioned runtime activity persistence.
- Added bounded retention from 1 to 2,000 records, defaulting to 200.
- Added Unicode-safe Base64 field encoding and corrupt-row skipping.
- Added history clear and human-readable export support.
- Added one-time confirmation tokens bound to the exact action idempotency key.
- Added configurable expiry with a strict maximum of one hour.
- Added mismatch, unknown, expired and replay detection.
- Confirmation tokens cannot be issued for non-destructive/safe actions.
- Added persistence, retention, corruption, Unicode, expiry, binding and replay regression tests.
- Updated the capability registry and this rolling backup.

## Safety contract

- Persistent history stores routing/audit metadata and result detail; it does not grant execution permission.
- A destructive action still requires capability eligibility, an explicit confirmation token and an atomic idempotency reservation.
- A token is single-use, expires, and is bound to the exact normalized action.
- Corrupt persisted rows are ignored instead of crashing runtime recovery.
- History retention is bounded to prevent unbounded local growth.

## Physical-device truth

Owner verified APK installation/launch and PDF selection/metadata persistence. PDF text search, DOCX search, freshness UI, Smart refresh, transactional maintenance, persistent runtime history and confirmation-token flows remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md`.
2. Read `docs/MAYRA_ROADMAP.md`.
3. Confirm PR #12 remains Draft/unmerged.
4. Use only the newest fully green CI head as authoritative.
5. Do not overclaim physical validation.
6. Update this file after every coding batch.

## Next step

Run full CI, then connect concrete normal-answer and Current-only document-retrieval adapters. After that, add confirmed action execution and a user-visible activity/history screen.
