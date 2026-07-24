# Mayra Contact & Identity Engine — Implementation Status

## Implemented

- Persistent relationship identities that map natural names to the exact Android contact name.
- Relationship terms such as Mummy, Papa, Boss, Doctor and custom labels.
- Multiple aliases per identity with case, punctuation and whitespace normalization.
- Preferred communication channel: phone, SMS, WhatsApp or ask every time.
- Trust levels: standard, trusted and sensitive.
- Exact-match resolution first, then unique partial-match resolution.
- Duplicate alias detection with mandatory disambiguation; Mayra never guesses between multiple people.
- Android Contacts remains the source of the actual phone number. Mayra does not privately duplicate phone numbers in the identity store.
- Real call and message production paths resolve relationship identity before the existing Android contact resolver.
- Identity metadata reaches the Action Safety Engine.
- Trusted identities are marked for future trusted-contact policy.
- Sensitive identities set sensitive action metadata so confirmation protections remain active.
- Internal People & Relationships management screen.
- Living Presence launcher entry.
- Add/remove identity without modifying Android Contacts.
- Deterministic resolution tests and Robolectric persistence tests.

## Safety guarantees

- Ambiguous aliases are not guessed.
- A missing canonical Android contact stops the action with a recovery instruction.
- Sensitive identities cannot silently use trusted-owner bypass.
- Removing a Mayra identity never deletes or edits the Android contact.
- Phone numbers remain owned by Android Contacts and are read only after permission is granted.

## Remaining gaps

1. Edit existing identity in-place rather than remove/recreate.
2. Android Contacts picker for selecting the canonical contact instead of typing the exact saved name.
3. Recent-interaction ranking and recency context.
4. Separate trusted rules per action: call, message draft, notification reply and data sharing.
5. Contact photo and organization metadata.
6. Notification sender-to-identity linking.
7. Voice commands to create or update an identity.
8. Encrypted backup/export and future cross-device sync.
9. Physical-device validation with duplicate contacts, multiple phone numbers and OEM contact providers.
10. Fresh successful Gradle compile, tests and lint after the current GitHub Actions runner issue is resolved.

## Validation status

Source and tests are committed. This implementation must not be described as build-verified until GitHub Actions starts Checkout and completes Gradle compile, unit tests and lint, or the same validation is run locally in Android Studio.
