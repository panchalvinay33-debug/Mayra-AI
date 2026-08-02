# Mayra AI — Full App Acceptance Checklist

Last updated: 2026-08-02
Primary target: `Mayra AI Personal Alpha` 0.2.0-alpha (`ai.mayra.app.alpha`)
Target device: Motorola owner device, Android 16
Branch: `agent/document-library-foundation`
PR: #12 (Draft, open, unmerged)

## Evidence rule

For every failed item, record: screen, exact steps, expected result, actual result, screenshot/screen recording, app version/build SHA, and whether the failure survives app restart. Do not mark a device-only flow verified from CI alone.

The Personal Alpha APK is debug-signed for owner testing. A CI-verified Personal Alpha is **not** a production-signed release and is **not** device-verified until applicable checks below pass on the physical device.

## 1. Installation and launch

- [ ] APK installs without package/signature error.
- [ ] Package shown by Android is `ai.mayra.app.alpha`.
- [ ] Launcher label is `Mayra AI Personal Alpha`.
- [ ] Exactly one Mayra Personal Alpha launcher icon is present.
- [ ] Main Mayra launcher opens without crash or blank screen.
- [ ] App survives background/foreground transition.
- [ ] App survives portrait rotation without duplicate messages or dialogs.
- [ ] Force-stop and reopen restores only state that is designed to persist.
- [ ] No unrelated permission prompt appears automatically at first launch.

## 2. Main chat

- [ ] Send a simple English request and receive a visible reply.
- [ ] Send a Hindi request and receive usable Devanagari output.
- [ ] Send a Hinglish request and receive a coherent response.
- [ ] Send multiple messages; conversation order remains correct.
- [ ] Rapid double-tap on Send does not duplicate the request.
- [ ] Clear conversation works only when no confirmation is pending.
- [ ] Error state is visible and recoverable; input is not permanently locked.
- [ ] Text-to-speech reads only answer text, not trusted metadata fields.

## 3. Voice

- [ ] Voice button asks for microphone permission only when used.
- [ ] Denying microphone permission does not crash chat.
- [ ] Start/stop listening state is visible.
- [ ] Hindi and Hinglish speech populate usable text.
- [ ] Voice is disabled while an action or memory approval is pending.
- [ ] TTS can be interrupted/reused without duplicate speaking.

## 4. Personal memory approval

- [ ] A deterministic “remember” command creates a review dialog, not an immediate write.
- [ ] Save persists a safe fact.
- [ ] Not now leaves approved storage unchanged.
- [ ] A prohibited secret such as PIN/OTP/password is rejected before approval.
- [ ] Same-key contradictory value shows current and proposed values.
- [ ] Replace increments revision and updates the value.
- [ ] A stale conflict cannot overwrite a newer approved value.
- [ ] Pending approval/process-recovery behavior matches the currently implemented persistence policy and never silently approves data.

## 5. Memory Center

- [ ] Approved memories list opens.
- [ ] Search matches key and value text, including Hindi Unicode.
- [ ] Every category filter works.
- [ ] Direct edit updates value and revision.
- [ ] Sensitive/prohibited edit is rejected without destroying the old value.
- [ ] Expiry presets: 1 day, 7 days, 30 days, never.
- [ ] Delete removes one memory immediately.
- [ ] Clear all requires confirmation and clears approved plus pending records.
- [ ] Export is owner-triggered and clearly readable; it is not described as protected backup.

## 6. Protected memory and provider-secret storage

- [ ] New approved memory survives app restart.
- [ ] New pending proposal survives app restart when persistence policy says it should.
- [ ] Storage Health shows EMPTY on a clean install.
- [ ] Protected records show HEALTHY when readable.
- [ ] Legacy test data migrates only after successful protected rewrite.
- [ ] Retry safe migration never deletes data or resets Keystore keys.
- [ ] DEGRADED state is visible when a record cannot be decrypted/decoded.
- [ ] Degraded storage is not falsely shown as empty history.
- [ ] Device lock/unlock does not corrupt readable memory.
- [ ] Provider API key is never displayed back in plaintext after saving.
- [ ] Provider API key is not present in ordinary SharedPreferences values, chat, memory export or activity history.
- [ ] Removing provider credential makes online provider unavailable without damaging local data.

## 7. Memory use in answers

- [ ] Relevant approved memory can influence an answer.
- [ ] Unapproved, expired, or irrelevant memory is not injected.
- [ ] Message shows trusted personal-memory chips.
- [ ] Answer text does not contain the old appended disclosure marker/protocol.
- [ ] More than three keys show bounded chips plus a “more” count.
- [ ] Model-written lookalike text cannot create a trusted chip.

## 8. Document Library

- [ ] Select TXT through system picker.
- [ ] Select PDF through system picker.
- [ ] Select DOCX through system picker.
- [ ] Metadata persists after reopening the app.
- [ ] Unicode search returns correct snippets.
- [ ] Summary is grounded in indexed file text.
- [ ] Document Q&A distinguishes supported answer from unsupported claim.
- [ ] Deleted or changed source is not silently treated as Current.
- [ ] Smart refresh updates stale evidence.
- [ ] Library Health reports index issues without deleting good records.
- [ ] Large/corrupt file produces a bounded error rather than app crash.

## 9. Phone actions and confirmations

- [ ] READ_CONTACTS permission is requested only when a contact action needs it.
- [ ] Denying contacts permission produces a recoverable explanation.
- [ ] Exact contact match resolves correctly.
- [ ] Ambiguous contact does not get guessed; Mayra asks for clarification.
- [ ] Call request shows exact confirmation before handoff.
- [ ] Confirmed call opens Android dialer with the expected number; Mayra does not directly place the call.
- [ ] Message request shows exact confirmation before handoff.
- [ ] Confirmed message opens Android composer with expected recipient/body; Mayra does not directly send SMS.
- [ ] Cancel executes nothing.
- [ ] Confirmation token is one-time and duplicate confirm is rejected.
- [ ] Destructive or unregistered action remains unsupported.
- [ ] Input, voice, clear, and duplicate sends are locked while confirmation is pending.
- [ ] Activity History records typed outcome/result without claiming call connection or message delivery.

## 10. Provider settings and online/offline behavior

- [ ] Remote provider is disabled by default on a clean install.
- [ ] Plain HTTP endpoint is rejected.
- [ ] Default endpoint is HTTPS and usable for the intended provider.
- [ ] Valid HTTPS endpoint/model settings persist after restart.
- [ ] Invalid save preserves previous valid settings.
- [ ] API key can be stored without being read back into the text field.
- [ ] Enable provider only works when credential/configuration are usable.
- [ ] One English online generation succeeds.
- [ ] One Hindi online generation succeeds.
- [ ] One Hinglish online generation succeeds.
- [ ] Disable provider and verify the same chat surface remains usable with local fallback.
- [ ] Turn network off while provider is enabled; Mayra falls back safely instead of hanging/crashing.
- [ ] Provider error text never exposes Authorization header/API key.
- [ ] Emergency disable turns remote use off while preserving non-secret endpoint/model settings.

## 11. Mayra-owned reminders

- [ ] Grant notification permission only when reminder/notification capability is invoked or explicitly requested.
- [ ] Create `drinking water after 3 min`; due time is approximately three minutes in the future.
- [ ] Create `in 20 minutes` reminder; relative parsing is correct.
- [ ] Create `kal subah 7 baje` reminder; Hindi/Hinglish day/time parsing is correct.
- [ ] A request with no usable time asks for clarification rather than scheduling an arbitrary time.
- [ ] Reminder survives app restart before due time.
- [ ] Due reminder produces one Mayra reminder notification when notifications are allowed.
- [ ] Notification Complete action marks the reminder complete and removes/cancels further work.
- [ ] Notification Snooze 10 min moves the due time and does not let the stale worker fire again.
- [ ] An unresolved due reminder produces at most the designed follow-up behavior, not a notification loop.
- [ ] Reboot with a future reminder pending; reminder is recovered/rescheduled.
- [ ] App update/reinstall-over-existing behavior does not duplicate a valid reminder schedule.
- [ ] A stale notification action/revision is ignored.
- [ ] Denying notification permission does not crash reminder persistence/scheduling.

## 12. Permissions and components — Personal Alpha

Expected direct app permissions from the capability boundary:

- INTERNET
- RECORD_AUDIO
- READ_CONTACTS
- POST_NOTIFICATIONS
- RECEIVE_BOOT_COMPLETED

CI may also show normal library/runtime infrastructure permissions contributed by AndroidX; any new sensitive permission requires explicit review.

Physical checks:

- [ ] No CALL_PHONE permission is requested or shown for Mayra Personal Alpha.
- [ ] No SEND_SMS permission is requested or shown.
- [ ] No READ_SMS/RECEIVE_SMS permission is requested or shown.
- [ ] No WRITE_CONTACTS permission is requested or shown.
- [ ] No SCHEDULE_EXACT_ALARM permission is requested or shown.
- [ ] No package-install permission is requested.
- [ ] No system-overlay permission is requested.
- [ ] Microphone, contacts and notifications are requested only when relevant.
- [ ] Notification-listener special access is not silently enabled; Android owner consent is required.
- [ ] Boot receiver behavior is limited to recovery/scheduling work and causes no unexpected visible action.

## 13. Safe Full Test regression

- [ ] `Mayra AI Full Test` still installs separately when needed.
- [ ] It contains the complete visible core UI and microphone path.
- [ ] It has no INTERNET, contacts, notification, boot, direct-call or SMS permission.
- [ ] Notification listener and boot receiver are absent.
- [ ] It remains useful for lower-risk UI/voice regression testing.

## 14. Reliability and recovery

- [ ] App restart during indexing does not leave Current evidence partially committed.
- [ ] App restart during memory approval does not create an approved record silently.
- [ ] App restart during action confirmation does not execute the action.
- [ ] Corrupt local records are isolated and do not crash startup.
- [ ] Offline mode remains usable when remote provider is disabled/unavailable.
- [ ] No failure path clears recoverable user data automatically.
- [ ] Rapid reminder/action repeats do not create unintended duplicate execution.
- [ ] Provider timeout/network loss leaves input usable after fallback/error handling completes.

## Release decision

A feature head may be called **CI-verified Personal Alpha** only after compilation, the complete unit-test gate, all configured lint variants, Personal Alpha APK assembly, permission/component audit, safe Full Test regression audit and isolated Document Test regression audit pass on the same governed source state.

It may be called **device-verified** only after applicable checks above are completed on the owner device with recorded evidence.

It may be called a **production release candidate** only after device acceptance plus production signing/provenance, release-specific manifest review and a fresh CI/release audit. Debug-signed Personal Alpha artifacts do not satisfy that release gate.
