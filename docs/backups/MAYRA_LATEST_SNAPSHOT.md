# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest source head before governance commits: `24fdd22aa1142d09c45734e3bc3d646881cf610f`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458

## Validation truth

Android CI #1589 failed during debug compilation. The exact error was in `MayraMemoryCenterActivity`: Compose `ColumnScope.this` was passed to `AndroidMayraMemoryStorageHealthReader`, which requires Android `Context`. The implementation now captures `LocalContext.current` and uses it for initial and retry diagnostics. A newer full pipeline is required before any green claim.

## Completed in this batch

- Fixed the exact CI #1589 Compose-context compile regression.
- Preserved the non-destructive Memory Center health and migration-retry UX.
- Added `MayraHttpConversationalProvider`, a concrete HTTPS POST transport.
- Added owner-controlled enabled state, HTTPS endpoint validation and model selection.
- Added runtime authorization-source integration without storing authorization in memory or chat.
- Added bounded connect/read timeouts and maximum response bytes.
- Added bounded serialization of recent conversation and message text.
- Added JSON escaping and extraction of a required string `text` response field.
- Classified HTTP 408, 429 and 5xx as temporary; other non-2xx responses as permanent.
- Added provider health states for disabled, missing authorization, ready, temporary failure and permanent failure.
- Added tests for disabled/missing configuration, successful Unicode response, HTTP classification and oversized response rejection.
- Kept the transport uninstalled by default and added no Android network permission in this batch.

## Safety contract

- The network provider returns text only and cannot execute actions or write memory.
- Remote use is owner-disabled by default.
- Non-HTTPS endpoints are rejected at configuration construction.
- Response bodies larger than the configured cap are rejected.
- Missing runtime authorization prevents opening a connection.
- Offline fallback remains owned by `ResilientMayraProviderAssistant`.
- No live-provider or device validation is claimed.

## Known limitations

- The newest source has not yet passed compile, complete tests, lint, R8 and component audit.
- A production network permission/release-flavor strategy is not yet approved or implemented.
- No owner-facing provider settings screen exists yet.
- No concrete provider is installed into `MayraApplication`.
- Live Hindi/Hinglish provider quality remains untested.
- Motorola memory-health and provenance-chip validation remains pending.

## Recovery instructions

1. Read the blueprint, roadmap and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft and unmerged.
3. Check the newest governed-head CI before claiming validation.
4. Use CI #1458 as authoritative green evidence until superseded.
5. Do not enable network composition or add permissions without an audited release decision.
6. Keep runtime authorization outside source, chat and personal memory.
7. Update this snapshot after every coding batch.

## Next step

Stabilize the newest full CI. Then add secure owner-facing provider settings and an audited network-enabled release flavor, followed by live provider health checks and Hindi/Hinglish evaluation.
