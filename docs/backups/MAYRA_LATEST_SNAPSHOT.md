# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-07-28
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
Latest authoritative code head before this docs update: `d4286a237a4be8d2f66577e2522ad8fa1fb52080`
Authoritative CI: Android CI #1445
APK artifact: `mayra-document-test-apk-1445`
APK artifact ZIP SHA-256: `ad83a12d5f712ed7d216b7ff0ebe971dcf95ba3d3bbdd61c52390e585ef624e5`
Reports artifact SHA-256: `afaf373e6b3e3f831fdff516bb3615f460eb67b1d6d9ef480ecc5094b503320c`

## Product state

- Document foundation remains 16/18 implemented; OCR and legacy DOC are deferred.
- Typed routing, provider eligibility, audited runtime, idempotency and persistent activity history are implemented.
- Android composition root, Activity History and main-chat exact-action Confirm/Cancel UX are implemented and CI-verified.
- Consent-first personal-memory foundation is implemented and automated-test verified.
- Personal-memory user-facing controls and chat proposal UX remain active work.
- Fresh cited search, broader calendar/email/reminder actions and controlled Hindi/Hinglish voice remain major product tracks.

## Completed in this batch

- Added personal-memory candidate, stored-memory, category, sensitivity and provenance models.
- Added explicit proposal/approval flow; proposal creation does not write to persistent storage.
- Added one-time proposal consumption and explicit rejection.
- Added prohibited-memory exclusions for passwords, PIN/OTP, payment-card secrets, Aadhaar and cryptographic recovery secrets.
- Added sensitive-memory exclusions for health, religion/caste/politics/sexual orientation and salary/bank-account data.
- Added source type, source reference and capture-time provenance.
- Added optional expiry and automatic expired-record pruning.
- Added user-controlled update, delete and clear operations.
- Added same-key correction with stable identity and revision increment.
- Added deterministic relevance retrieval using only user-authored key/value evidence.
- Added bounded Android SharedPreferences persistence with a versioned Unicode-safe codec.
- Added corrupt-record skipping, configurable retention and readable export.
- Added pure JVM and Robolectric tests covering consent, exclusions, expiry, revision, retrieval, persistence, corruption, Unicode and user deletion.
- First CI #1443 found category-label relevance inflation; retrieval was corrected and full CI #1445 passed.

## Memory safety contract

- Mayra must not silently create personal memories from conversation text.
- A memory is stored only after explicit approval of a concrete proposal.
- Prohibited or sensitive candidates are rejected before a proposal can be approved.
- Retrieval uses only active, approved records and never expired records.
- Internal category labels do not count as user evidence during retrieval.
- Provenance accompanies every record and changes when the record is corrected.
- Delete and clear are direct user controls.
- Pending proposals are currently in-memory and do not survive process death.
- The Android store is local and permission-free; no network provider is introduced.

## Physical-device truth

Owner verified APK installation/launch and PDF selection/metadata persistence in an earlier build. PDF text search, DOCX search, freshness UI, Smart refresh, transactional maintenance, Activity History, export/clear, system-picker action, main-chat Confirm/Cancel, rotation retention and all personal-memory flows remain unverified on phone.

## Recovery instructions

1. Read `docs/MAYRA_BLUEPRINT.md` and `docs/MAYRA_ROADMAP.md`.
2. Confirm PR #12 remains Draft/unmerged.
3. Use Android CI #1445 on `d4286a237a4be8d2f66577e2522ad8fa1fb52080` as the newest fully verified functional head until a later full-green run exists.
4. Do not overclaim physical validation.
5. Do not connect memory to chat context until Memory Center controls and explicit proposal UX are implemented.
6. Keep sensitive-memory exclusions conservative.
7. Update this snapshot after every coding batch.

## Next step

Build the Memory Center, chat Save/Not now proposal dialog, visible provenance/expiry controls and approved-memory context injection. Then run full CI on the latest governed head.
