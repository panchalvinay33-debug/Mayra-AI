# Mayra AI — Execution Roadmap

Last updated: 2026-08-02
Canonical blueprint: `docs/MAYRA_BLUEPRINT.md`
Latest backup: `docs/backups/MAYRA_LATEST_SNAPSHOT.md`
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Overall program view

| Track | Status | Progress signal | Next gate |
|---|---|---|---|
| Blueprint and backup discipline | DONE | Canonical blueprint, roadmap and rolling snapshot maintained | Keep updated after every authoritative green milestone |
| Single-app packaging | CI_VERIFIED | Personal Alpha, safe Full Test and isolated Document Test all audited in CI #1753 | Owner-device install/launch validation |
| Personal Alpha packaging | CI_VERIFIED | `ai.mayra.app.alpha`, one launcher, version 0.2.0-alpha, debug-signed owner candidate | Physical Motorola acceptance; production signing later |
| Document intelligence | DEVICE_VERIFY | TXT/PDF/DOCX extraction, search, summaries, grounded Q&A and health tools implemented | PDF/DOCX owner-device acceptance; OCR/legacy DOC deferred |
| Core routing and eligibility | DONE | Typed outcomes, capability gates, trusted assistant metadata and safe confirmation flow | Keep full regression suite green |
| Personal memory | DEVICE_VERIFY | Explicit approval, provenance, edit/delete/expiry and protected storage implemented | Motorola save/use/edit/delete/recovery checks |
| Conversational provider | CI_VERIFIED | Owner-disabled-by-default HTTPS provider, OpenAI Responses compatibility, bounded transport and Keystore credential storage | Owner API-key connection test and offline-fallback validation |
| Search and fresh knowledge | PLANNED | No web-search completion claim | Add source/citation/freshness contract after provider device validation |
| Phone actions | DEVICE_VERIFY | Calls use dialer handoff; messages use composer handoff; no direct CALL_PHONE/SEND_SMS permission | Contact resolution + confirmation + handoff checks on device |
| Mayra reminders | CI_VERIFIED | Persistent store, Hindi/Hinglish parser, WorkManager schedule, notifications, Complete/Snooze, follow-up and reboot/update recovery | Physical timing/notification/reboot acceptance |
| Voice intelligence | DEVICE_VERIFY | Microphone permission is requested on use; speech/TTS path preserved | Hindi/Hinglish physical evaluation |
| Privacy and release | IN_PROGRESS | Separate capability-audited Personal Alpha, microphone-only Full Test and zero-permission Document Test | Production signing, release artifact provenance and owner acceptance |

## Authoritative verification truth

### Personal Alpha milestone

Android CI #1753 passed on feature head `1aa031f8bb5213a9f3cacd88b7bfa3528489b132`.

It verified on the same governed source state:

1. Debug, `personalAlpha` and `fullTest` Kotlin compilation.
2. Complete debug unit-test suite: 346 tests passed.
3. Debug, Personal Alpha, Full Test and Document Test lint.
4. Personal Alpha APK assembly.
5. Personal Alpha package: `ai.mayra.app.alpha`.
6. Personal Alpha label: `Mayra AI Personal Alpha`.
7. Exactly one launcher: `ai.mayra.app.MainActivity`.
8. Required Personal Alpha capabilities present: INTERNET, microphone, contacts, notifications and boot recovery.
9. Direct CALL_PHONE, SEND_SMS, SMS-read/receive, WRITE_CONTACTS, exact-alarm, package-install and overlay permissions absent.
10. Main Chat, Document Library, Memory Center, Provider Settings, Activity History, Boot Receiver, Notification Listener and reminder action receiver present.
11. Safe Full Test APK assembly and permission/component audit remained green.
12. Isolated minified Document Test APK and zero-permission/component audit remained green.
13. All three APK artifacts plus CI reports were uploaded.

### Artifact evidence

`mayra-personal-alpha-apk-1753`:

- GitHub artifact id: `8834650772`
- ZIP size: 18,712,293 bytes
- ZIP SHA-256: `d81c1bb8a1ffa1c95e75481dad97213477530df53eb2219bc8ce162e91d1b5d9`
- extracted APK: `app-personalAlpha.apk`
- APK size: 19,160,566 bytes
- APK SHA-256: `72411a46f39064db1a518fc9992a09138a501ef3afd490a6a84112ddf2bc42cb`

Other #1753 artifacts:

- `mayra-full-test-apk-1753` — ZIP SHA-256 `8d87855c8e399962e049d5e8a994aef7e789aee9f8c8a481f9537a156875d64a`
- `mayra-document-test-apk-1753` — ZIP SHA-256 `957a4735aeb132cb68a9e88ce1078add2a73e4f78ec884b58f05c06b532c5e77`
- `android-reports-1753` — ZIP SHA-256 `5774a5f4b6fca26a9e0e125adc2164e452741ffbef119ab0a040518b76c73dcc`

## Current capability boundary

### Personal Alpha

Designed for owner-device functional testing with real Mayra capabilities. It is debug-signed and installs as a separate package; it is **not** a production release.

Present by design:

- INTERNET for owner-enabled conversational provider
- RECORD_AUDIO for voice
- READ_CONTACTS for contact resolution
- POST_NOTIFICATIONS for Mayra reminders
- RECEIVE_BOOT_COMPLETED for reminder/background recovery
- notification-listener component behind Android special-access consent
- one launcher and all core internal Mayra screens

Absent by design:

- direct CALL_PHONE
- direct SEND_SMS
- SMS read/receive
- contact writes
- exact-alarm permission
- overlay permission
- package-install permission

Calls and messages are review-first Android handoffs; the user performs the final dial/send action.

### Safe Full Test

Keeps the full visible Mayra UI plus microphone while stripping network, contacts, notifications and background recovery for lower-risk sideload testing.

### Document Test

Remains an isolated, zero-permission regression APK. It is not the complete application.

## Implemented in the 0.2.0 milestone

- versionCode 3 / versionName 0.2.0
- OpenAI Responses-compatible bounded HTTPS conversational provider
- default provider endpoint `https://api.openai.com/v1/responses`
- owner-editable model/endpoint and remote enable/disable
- API key encrypted with Android Keystore-backed AES-GCM storage
- offline local fallback when provider is disabled, missing or unavailable
- typed personal-memory attribution outside visible model text
- review-first dialer and message-composer handoff instead of direct call/SMS privileges
- Mayra-owned reminder parser and persistent store
- WorkManager reminder scheduling with revision-safe stale-worker rejection
- Complete and Snooze notification actions
- reminder follow-up and reboot/app-update recovery
- dedicated Personal Alpha CI packaging/audit path

## Immediate next priority

1. Install `Mayra AI Personal Alpha 0.2.0-alpha` on the Motorola owner device.
2. Verify first launch and request permissions only when related features are invoked.
3. Configure an API key in Provider Settings and test one English, Hindi and Hinglish online response plus offline fallback.
4. Create `drinking water after 3 min`, `kal subah 7 baje` and a missing-time reminder; verify notification, Complete and Snooze.
5. Reboot with a future reminder pending and verify recovery without duplicate alerts.
6. Test contact resolution, call dialer handoff and message composer handoff; ensure Mayra never claims delivery/connection.
7. Run Library, Memory, Activity History and voice acceptance checks.
8. Only after device acceptance: add production signing/provenance and a non-debug release candidate.

PR #12 remains Draft, open and unmerged. No ready-for-review or merge transition is authorized.
