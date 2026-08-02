# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-02
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Authoritative Personal Alpha feature head: `1aa031f8bb5213a9f3cacd88b7bfa3528489b132`
Authoritative feature CI: Android CI #1753
Owner-device checklist: `docs/MAYRA_FULL_APP_ACCEPTANCE.md`

## Current package truth

Mayra now has three governed Android test artifacts with deliberately different capability boundaries:

1. **Personal Alpha** — `ai.mayra.app.alpha`, label `Mayra AI Personal Alpha`, version `0.2.0-alpha`. This is the owner-device functional candidate with provider, contacts, reminders and background recovery enabled. It is debug-signed and is not a production release.
2. **Full Test** — `ai.mayra.app.fulltest`, label `Mayra AI Full Test`. This keeps the complete visible UI and microphone while stripping network, contacts, notifications and background recovery for lower-risk sideload testing.
3. **Document Test** — `ai.mayra.app.documenttest`. This remains the isolated zero-permission document regression artifact and is not the complete application.

Personal Alpha and Full Test each expose exactly one launcher activity: `ai.mayra.app.MainActivity`.

## Personal Alpha 0.2.0 capability boundary

CI #1753 verified the Personal Alpha APK with these required permissions present:

- `android.permission.INTERNET`
- `android.permission.RECORD_AUDIO`
- `android.permission.READ_CONTACTS`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.RECEIVE_BOOT_COMPLETED`

The following high-risk permissions were explicitly audited absent:

- `android.permission.CALL_PHONE`
- `android.permission.SEND_SMS`
- `android.permission.READ_SMS`
- `android.permission.RECEIVE_SMS`
- `android.permission.WRITE_CONTACTS`
- `android.permission.SCHEDULE_EXACT_ALARM`
- `android.permission.REQUEST_INSTALL_PACKAGES`
- `android.permission.SYSTEM_ALERT_WINDOW`

Calls use a dialer handoff and messages use a composer handoff. Mayra does not directly place a call or send an SMS and must not claim connection/delivery.

## Conversational provider status

Implemented and CI-verified:

- owner-disabled-by-default remote provider
- HTTPS-only endpoint validation
- OpenAI Responses-compatible request/response shape
- default endpoint `https://api.openai.com/v1/responses`
- default model `gpt-5.6`
- bounded request/response sizes and timeouts
- bounded retry with deterministic local fallback
- `store:false` request behavior
- owner-editable endpoint/model
- API key stored encrypted with Android Keystore-backed AES-GCM
- key is not read back into the settings UI and is not stored in ordinary plaintext preferences

Still device-only:

- real API-key connection/generation validation
- Hindi/Hinglish answer quality
- offline transition under real network loss
- provider error UX on the Motorola device

## Mayra-owned reminder status

Implemented and CI-verified:

- Hindi/Hinglish/English reminder parser
- relative-minute/hour parsing
- day/time parsing including `kal subah 7 baje`
- clarification when time is missing
- persistent reminder store
- revision-safe WorkManager scheduling
- stale worker rejection
- notification channel and guarded runtime permission check
- Complete action
- Snooze 10 min action
- 30-minute follow-up for unresolved due reminders
- reboot/app-update recovery
- duplicate/stale action protection through revision matching

The exact regression phrase `drinking water after 3 min` is covered by tests. Reminder timing, OEM battery behavior, notification presentation and reboot recovery still require physical-device acceptance.

## Trusted assistant and memory status

The trusted structured response boundary remains active:

- `MayraAssistantResponse` separates visible answer text from `usedPersonalMemoryKeys`
- `MayraStructuredAssistant` exposes structured replies
- approved memory keys are attached out-of-band
- provider/document/user text cannot manufacture trusted memory chips merely by containing a marker-like string

Personal memory remains approval-first with protected storage, provenance, edit/delete/expiry and user-facing controls. Physical recovery and attribution checks remain pending.

## CI #1753 evidence

Android CI #1753 passed on feature head `1aa031f8bb5213a9f3cacd88b7bfa3528489b132`.

Passed gates:

- debug, Personal Alpha and Full Test Kotlin compilation
- complete debug unit-test suite: 346 tests passed
- lint for Debug, Personal Alpha, Full Test and Document Test
- Personal Alpha APK assembly
- Personal Alpha package/label/one-launcher audit
- Personal Alpha required/forbidden permission audit
- Personal Alpha required-component audit
- safe Full Test APK assembly and audit
- isolated minified Document Test APK assembly and zero-permission/component audit
- report and APK artifact upload

Artifacts:

- `mayra-personal-alpha-apk-1753`
  - artifact id `8834650772`
  - ZIP size 18,712,293 bytes
  - ZIP SHA-256 `d81c1bb8a1ffa1c95e75481dad97213477530df53eb2219bc8ce162e91d1b5d9`
  - APK size 19,160,566 bytes
  - APK SHA-256 `72411a46f39064db1a518fc9992a09138a501ef3afd490a6a84112ddf2bc42cb`
- `mayra-full-test-apk-1753`
  - artifact id `8834651004`
  - ZIP SHA-256 `8d87855c8e399962e049d5e8a994aef7e789aee9f8c8a481f9537a156875d64a`
- `mayra-document-test-apk-1753`
  - artifact id `8834651181`
  - ZIP SHA-256 `957a4735aeb132cb68a9e88ce1078add2a73e4f78ec884b58f05c06b532c5e77`
- `android-reports-1753`
  - artifact id `8834650536`
  - ZIP SHA-256 `5774a5f4b6fca26a9e0e125adc2164e452741ffbef119ab0a040518b76c73dcc`

## Historical corrections retained

CI #1631 produced only the isolated `documentTest` APK and was never evidence of complete-app packaging. CI #1647/#1653 established the early complete Full Test path. An earlier broadly privileged sideload build was blocked by Play Protect review; that led to the safe Full Test boundary and later to the separately audited Personal Alpha package.

## Next gate

1. Install `Mayra AI Personal Alpha 0.2.0-alpha` on the Motorola owner device.
2. Verify launch, permission-on-use behavior and one launcher icon.
3. Test real provider setup/generation and local fallback.
4. Test reminder creation, notification, Complete, Snooze and reboot recovery.
5. Test contact resolution plus dialer/composer handoff.
6. Run Memory, Library, History and voice checks.
7. Do not call the build device-verified until the applicable checklist evidence is recorded.
8. Do not create a production-signed release or merge PR #12 without explicit owner approval.
