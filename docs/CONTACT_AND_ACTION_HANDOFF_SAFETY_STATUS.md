# Contact Resolution and Action Handoff Safety — Status

## Implemented in the Personal Alpha stabilization branch

### Conservative contact identity resolution

- Owner-defined canonical names, relationships and aliases resolve before Android contact lookup.
- Exact canonical/relationship/alias matches remain preferred.
- Partial matching requires at least four normalized characters.
- Numeric-only identity queries are rejected.
- Tied or near-tied partial matches remain ambiguous and never select a person automatically.
- Ambiguous candidate lists are bounded.
- Identity terms, notes and aliases are normalized and control characters are removed.
- Owner identity metadata is attached to the action request without storing message content in identity records.

### Review-first phone handoffs

- Calls use `ACTION_DIAL`, not `ACTION_CALL`.
- Messages use `ACTION_SENDTO` with the `smsto:` scheme.
- The manifest no longer requests `CALL_PHONE` or `SEND_SMS`.
- The final call button and message send button remain visible owner actions in Android.
- Dial/message URI targets are sanitized and scheme/query injection is rejected.
- Output wording says that the dialer or composer opened; it never claims a call connected or a message was delivered.

### Confirmation and duplicate protection

- High-risk action confirmations are one-time and expire.
- Confirmation tickets carry the exact request fingerprint.
- Identical pending actions cannot create a second confirmation.
- Identical recently executed actions are blocked during a short duplicate window.
- Old requests and requests with invalid future timestamps are rejected.
- Only one call/message confirmation may be pending in each production executor.
- A new high-risk action cannot orphan an earlier confirmation token.
- Global Stop remains the higher-priority system boundary.

### Privacy-safe audit

- Action audit storage is bounded.
- Bearer tokens and OpenAI-shaped keys are redacted from audit details.
- Newline/control formatting is removed from audit diagnostics.
- Confirmation prompts include only a bounded target label and never include the message body.

## Deterministic tests

Tests cover:

- exact and strong-partial identity resolution;
- short partial query rejection;
- numeric-only query rejection;
- duplicate alias ambiguity;
- tied partial ambiguity;
- identity store normalization;
- one-time confirmation tokens;
- pending duplicate blocking;
- recent duplicate blocking and expiry;
- stale and future-dated action rejection;
- fingerprint normalization;
- bounded secret-redacted audit;
- dialer rather than direct-call intent;
- message composer handoff wording;
- URI/scheme injection rejection;
- invalid package rejection;
- removal of direct-call and direct-SMS permission requirements.

## Strict source preflight

The source preflight fails if it detects removal of:

- review-first dialer/message intents;
- manifest removal of `CALL_PHONE` and `SEND_SMS`;
- action fingerprints and duplicate blocking;
- stale action checks;
- one-pending confirmation serialization;
- owner identity integration;
- conservative partial-contact matching;
- the associated deterministic tests.

## Remaining validation

The source changes are committed but are not yet compile- or device-verified. Required evidence still includes:

1. Kotlin compilation and complete unit-test execution.
2. Android lint and debug APK assembly.
3. Physical contact permission flow.
4. Exact alias, ambiguous alias and Android duplicate-contact tests.
5. Dialer handoff on the owner's Motorola phone.
6. SMS/WhatsApp composer review behavior.
7. Rapid repeated voice/text command duplicate tests.
8. Confirmation expiry and Global Stop tests on a real device.

No successful call connection, message delivery, compilation or physical-device result is claimed by this document.
