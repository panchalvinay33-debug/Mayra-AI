# Mayra AI — Full App Acceptance Checklist

Date prepared: 2026-07-28
Target device: Motorola owner device, Android 16
Branch: `agent/document-library-foundation`
PR: #12 (Draft, open, unmerged)

## Evidence rule

For every failed item, record: screen, exact steps, expected result, actual result, screenshot/screen recording, app version/build SHA, and whether the failure survives app restart. Do not mark a device-only flow verified from CI alone.

## 1. Installation and launch

- [ ] APK installs without package/signature error.
- [ ] Main Mayra launcher opens without crash or blank screen.
- [ ] App survives background/foreground transition.
- [ ] App survives portrait rotation without duplicate messages or dialogs.
- [ ] Force-stop and reopen restores only state that is designed to persist.
- [ ] No unexpected notification, background service, or permission prompt appears at first launch.

## 2. Main chat

- [ ] Send a simple English request and receive a visible reply.
- [ ] Send a Hindi request and receive usable Devanagari output.
- [ ] Send a Hinglish request and receive a coherent response.
- [ ] Send multiple messages; conversation order remains correct.
- [ ] Rapid double-tap on Send does not duplicate the request.
- [ ] Clear conversation works only when no confirmation is pending.
- [ ] Error state is visible and recoverable; input is not permanently locked.
- [ ] Text-to-speech reads only answer text, not internal metadata markers.

## 3. Voice

- [ ] Voice button asks for microphone permission only when used.
- [ ] Denying microphone permission does not crash chat.
- [ ] Start/stop listening state is visible.
- [ ] Hindi and Hinglish speech populate usable text.
- [ ] Voice is disabled while an action or memory approval is pending.

## 4. Personal memory approval

- [ ] A deterministic “remember” command creates a review dialog, not an immediate write.
- [ ] Save persists a safe fact.
- [ ] Not now leaves approved storage unchanged.
- [ ] A prohibited secret such as PIN/OTP/password is rejected before approval.
- [ ] Same-key contradictory value shows current and proposed values.
- [ ] Replace increments revision and updates the value.
- [ ] A stale conflict cannot overwrite a newer approved value.
- [ ] Pending approval survives process death only within its expiry window.

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

## 6. Protected memory storage

- [ ] New approved memory survives app restart.
- [ ] New pending proposal survives app restart.
- [ ] Storage Health shows EMPTY on a clean install.
- [ ] Protected records show HEALTHY when readable.
- [ ] Legacy test data migrates only after successful protected rewrite.
- [ ] Retry safe migration never deletes data or resets Keystore keys.
- [ ] DEGRADED state is visible when a record cannot be decrypted/decoded.
- [ ] Degraded storage is not falsely shown as empty history.
- [ ] Device lock/unlock does not corrupt readable memory.

## 7. Memory use in answers

- [ ] Relevant approved memory can influence an answer.
- [ ] Unapproved, expired, or irrelevant memory is not injected.
- [ ] Message shows trusted personal-memory chips.
- [ ] Answer text does not contain the old appended disclosure line.
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

## 9. Actions and confirmations

- [ ] Registered safe action shows exact confirmation prompt.
- [ ] Confirm executes only the displayed request.
- [ ] Cancel executes nothing.
- [ ] Confirmation token is one-time and duplicate confirm is rejected.
- [ ] Destructive or unregistered action remains unsupported.
- [ ] Input, voice, clear, and duplicate sends are locked while confirmation is pending.
- [ ] Activity History records typed outcome and result.

## 10. Provider settings

- [ ] Remote provider is disabled by default.
- [ ] Plain HTTP endpoint is rejected.
- [ ] Valid HTTPS endpoint/model settings persist after restart.
- [ ] Invalid save preserves previous valid settings.
- [ ] Emergency disable turns remote use off while preserving endpoint/model.
- [ ] No bearer token or authorization value appears in ordinary preferences/export/chat/memory.
- [ ] Current offline build clearly states that network permission/composition is not active.

## 11. Permissions and components

- [ ] Permission prompts occur only when the related feature is invoked.
- [ ] Denied permission produces a recoverable explanation.
- [ ] No INTERNET permission exists in the isolated offline verification APK.
- [ ] No unexpected exported activity, service, or receiver is present.
- [ ] Notification listener and boot receiver behavior match owner-enabled design only.

## 12. Reliability and recovery

- [ ] App restart during indexing does not leave Current evidence partially committed.
- [ ] App restart during memory approval does not create an approved record silently.
- [ ] App restart during action confirmation does not execute the action.
- [ ] Corrupt local records are isolated and do not crash startup.
- [ ] Offline mode remains usable when remote provider is disabled/unavailable.
- [ ] No failure path clears recoverable user data automatically.

## Release decision

The build may be called CI-verified only after compile, complete unit tests, lint, minified APK assembly, and permission/component audit pass on the same governed head. It may be called device-verified only after all applicable checks above are completed on the owner device with recorded evidence.
