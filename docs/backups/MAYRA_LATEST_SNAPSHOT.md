# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-29
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest authoritative single-app green head: `496f5d043b4adf5c446f14d14e4adfd13a7c0918`
Authoritative single-app CI: Android CI #1667
Current typed-response head: `f52d764a92aef9321cbe311c00119b32aba1b78b`
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Current package truth

Mayra is one Android application package for owner testing. Only `MainActivity` is launchable. Document Library, Memory Center, Provider Settings and Activity History remain internal screens opened from the main app.

CI #1667 verified exactly one `launchable-activity` entry and required it to be `ai.mayra.app.MainActivity`.

## Safe full-test boundary

The owner-device `fullTest` package keeps the complete visible application and microphone-based voice testing while removing high-risk declarations that caused Play Protect review in the earlier sideload build.

Present:

- Main Chat
- Document Library
- Memory Center
- Provider Settings
- Activity History
- microphone permission

Absent:

- contacts permission
- direct call permission
- SMS permission
- notification permission/listener
- exact alarm permission
- boot permission/receiver
- INTERNET permission

The isolated `documentTest` package remains a separate CI regression artifact and is not the complete application.

## Typed assistant response migration

The current coding batch replaces text-embedded memory-use markers with trusted structured metadata.

Implemented:

1. `MayraAssistantResponse` carries visible text and `usedPersonalMemoryKeys` separately.
2. `MayraStructuredAssistant` exposes `replyStructured` while preserving a text-only compatibility method.
3. `LocalMayraAssistant` implements the structured contract.
4. `PersonalMemoryAwareMayraAssistant` attaches approved-memory keys out-of-band.
5. `ChatViewModel` consumes structured responses directly.
6. The legacy marker parser and Base64 marker protocol were removed.
7. Tests cover text normalization, key deduplication, legacy text isolation and the local structured contract.

Security effect: provider output, document text or user content can no longer become trusted memory-attribution metadata merely by containing a special marker string.

## Verification status

Authoritative baseline CI #1667 passed:

- Kotlin compilation
- complete unit tests
- Android lint
- safe full-test APK assembly
- safe permission/component audit
- exactly-one-launcher audit
- isolated document-test R8 build and audit

The typed-response/documentation batch is awaiting its latest CI run. Do not treat it as green until that run completes successfully.

## Historical correction

CI #1631 produced only the intentionally isolated `documentTest` APK. CI #1647/#1653 created the first complete full-test path. The earlier privileged build was blocked for review by Play Protect, which led to the current microphone-only safe full-test boundary.

## Next gate

1. Obtain a green CI result for the typed-response head.
2. Download the latest `mayra-full-test-apk-<run>` artifact.
3. Remove older test packages from the Motorola device once.
4. Install the latest artifact and confirm exactly one Mayra launcher icon.
5. Test capability reply, Library, Memory, Provider, History and voice.
6. Keep PR #12 Draft and unmerged until explicit owner approval.
