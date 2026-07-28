# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest source head before governance commits: `5429ff38727fd8e16b98e95fd56ad5719aba91e8`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458

## Validation truth

Android CI #1571 compiled debug sources successfully and ran 317 unit tests; one legacy assertion failed because it expected the removed text disclosure rather than structured provenance metadata. The test was corrected to parse the trusted marker and verify `usedPersonalMemoryKeys`. Lint, R8 and APK audit were skipped after the test failure. A newer full pipeline is required before any green claim.

## Completed in this batch

- Added trusted personal-memory provenance chips to chat message cards.
- Kept model answer text clean; structured keys live on `MayraMessage`.
- Added Memory Center storage-health card.
- Displayed protected, legacy and unreadable counts separately for approved and pending records.
- Added owner-triggered `Retry safe migration`.
- Retry performs normal protected reads/rewrites and never clears records or resets Keystore keys.
- Added explicit degraded-state explanation and safe failure messaging.
- Added `MayraConversationalProvider` text-only boundary.
- Added bounded timeout, bounded retry and deterministic offline fallback.
- Added cancellation propagation and permanent-failure no-retry behavior.
- Added bounded provider request history and runtime credential-source contract.
- Added provider tests for success, retry, permanent fallback, exhausted fallback, cancellation and request bounds.
- Diagnosed CI #1571 from its uploaded reports and corrected the exact stale test assertion.
- Added no Android permission, service, receiver or background component.

## Safety contract

- Unreadable protected records are never presented as empty history without warning.
- Mayra never automatically resets Keystore keys.
- Migration retry is owner-triggered and non-destructive.
- Model-written text cannot create trusted memory provenance chips.
- Remote provider answers cannot bypass action confirmation or memory approval boundaries.
- Provider credentials cannot come from personal memory or source control.
- Cancellation remains cancellation; it is not converted into a misleading fallback answer.

## Known limitations

- The newest source has not yet passed the full governed CI chain.
- Memory health and provenance chips remain unverified on the Motorola owner device.
- The provider boundary has no concrete production HTTP adapter yet.
- Network eligibility, response-size enforcement and provider health UI remain pending.
- No portable encrypted backup format exists.

## Recovery instructions

1. Read the blueprint, roadmap and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft and unmerged.
3. Check the newest governed-head CI before claiming validation.
4. Use CI #1458 as authoritative green evidence until superseded by a newer complete run.
5. Never auto-reset Keystore keys or clear unreadable records.
6. Do not enable a concrete remote provider without secure runtime credentials and eligibility checks.
7. Update this snapshot after every coding batch.

## Next step

Run and stabilize full CI for the latest head. Then implement a concrete provider transport with response-size limits, network eligibility, provider diagnostics and secure runtime configuration, followed by Motorola acceptance for memory health, migration retry and provenance chips.
