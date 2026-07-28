# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest source head before governance commits: `94b9f295316ada0b5796a2b0473047fd83c957c6`
Latest fully validated functional head: `abfa2711f01bb526d1c6fdb93364aa8ea148c6af`
Authoritative previous green CI: Android CI #1458

## Validation truth

Android CI #1601 compiled application sources successfully but failed compiling provider unit tests. The exact cause was Kotlin SAM construction against a normal `MayraProviderCredentialSource` interface. The contract is now a `fun interface`; runtime behavior is unchanged. A newer full pipeline is required before any green claim.

## Completed in this batch

- Fixed the exact CI #1601 unit-test compilation regression.
- Added non-secret owner provider settings model and Android SharedPreferences store.
- Persisted endpoint, model, enable state and bounded transport limits only.
- Explicitly excluded bearer tokens, authorization and secrets from settings persistence.
- Added HTTPS validation with failure-safe preservation of the previous valid settings.
- Added owner-facing Remote Provider screen.
- Added default-off toggle, settings validation, save feedback and emergency disable.
- Registered the settings activity without adding INTERNET permission.
- Added Robolectric tests for defaults, persistence, secret exclusion, invalid-HTTP rollback and emergency disable.
- Kept concrete remote provider uninstalled from `MayraApplication`.

## Safety contract

- Remote provider remains owner-disabled by default.
- Credentials are supplied separately at runtime and are never stored by the settings screen.
- Invalid settings cannot destroy the previous valid configuration.
- Emergency disable preserves endpoint/model while switching remote use off.
- No network permission, live-provider claim or physical-device claim is made.

## Known limitations

- The newest source has not yet passed compile, complete tests, lint, R8 and component audit.
- No audited network-enabled release flavor exists yet.
- No secure runtime credential implementation is installed.
- Provider settings do not dynamically recompose the active assistant until restart/future runtime controller work.
- Live Hindi/Hinglish quality and Motorola validation remain pending.

## Recovery instructions

1. Read the blueprint, roadmap and Jarvis North Star issue #13.
2. Confirm PR #12 remains Draft and unmerged.
3. Check the newest governed-head CI before claiming validation.
4. Use CI #1458 as authoritative green evidence until superseded.
5. Do not add INTERNET permission or provider composition without an audited release decision.
6. Keep credentials outside source, chat, memory and ordinary preferences.
7. Update this snapshot after every coding batch.

## Next step

Stabilize full CI, then add an audited network-enabled release flavor, secure runtime credentials, provider runtime composition/health controls and Hindi/Hinglish evaluation.
