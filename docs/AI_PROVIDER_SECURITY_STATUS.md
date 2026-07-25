# AI Provider Security — Implementation Status

## Implemented

- Local-only mode remains the default and requires no API key or network provider.
- OpenAI is limited to normal conversational replies; phone actions remain in Mayra's local action, permission and confirmation layers.
- API keys are encrypted with an Android Keystore AES-GCM key before app-private persistence.
- A stored key is considered configured only when it can be successfully decrypted and passes bounded format validation.
- Corrupted, undecryptable or invalid encrypted key material is removed instead of remaining in a false configured state.
- API-key input, model names, user messages, context messages, request bodies, response bodies and displayed assistant text are bounded.
- Model names are normalized and restricted to a conservative character set before use in configuration or model lookup URLs.
- Online endpoints must use HTTPS and HTTP redirects are disabled.
- Connection and server diagnostics redact bearer tokens and OpenAI-shaped key material before display or persistence.
- Empty, invalid and oversized online requests fail safely.
- Oversized online responses are rejected without reading an unbounded response into memory.
- Remote factory, request, parsing or connection failures fall back to Mayra's offline assistant for ordinary chat.
- Local device actions are routed locally before online chat selection.
- Provider UI catches storage and connection failures instead of crashing and asks the owner to re-enter an unavailable encrypted key.

## Security boundaries

- The API key is never displayed after it is saved.
- The encrypted API-key ciphertext is excluded from Android automatic backup because app automatic/full backup is disabled.
- API keys, request authorization headers and raw provider secrets must not enter source control, diagnostics, action audit logs or backup exports.
- Android Keystore protects the local encryption key, but a rooted or fully compromised device remains outside this guarantee.
- Network content sent to an enabled provider leaves the phone. The owner must deliberately select OpenAI mode and provide a key.
- Mayra does not claim that an online response performed a phone action.

## Deterministic validation added

- valid and invalid model normalization;
- path/whitespace-shaped model rejection;
- bounded API-key validation;
- bearer-token and key redaction from diagnostics;
- request/context input limits;
- HTTPS endpoint enforcement;
- provider configuration behavior for local, missing-key, stored-key and invalid-model states.

## Remaining verification

1. Fresh Kotlin compilation.
2. Complete unit-test execution.
3. Android lint.
4. Keystore save/read/delete on the owner's Motorola device.
5. Corrupted ciphertext recovery test on a physical or instrumented Android environment.
6. Successful provider connection with an owner-supplied key.
7. Offline fallback under timeout, DNS failure, non-2xx response and malformed JSON.
8. Memory and battery behavior for maximum-sized allowed requests and responses.
9. Confirmation that no OEM backup or device-transfer path copies provider ciphertext unexpectedly.

## Validation status

The source, safety policy, deterministic tests, UI containment and preflight markers are committed. Build, unit-test, lint, network and physical-device success are not claimed until fresh evidence is produced.
