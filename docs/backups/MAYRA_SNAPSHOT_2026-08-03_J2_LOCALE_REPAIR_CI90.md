# Mayra AI — Immutable Snapshot: J2 Locale Repair / CI #90

Date: 2026-08-03
Status: EXACT APPLICATION SOURCE GREEN — MOTOROLA RETEST NEXT

## Source and rollback

- Active PR: #12, Draft/open/unmerged
- Application source commit: `e706bdfb8f53006825404db99a51f466aa251fc4`
- Protected baseline: `baseline/mayra-0.2.1-j2-locale-repair-green-90`
- Previous J2 baseline retained: `baseline/mayra-0.2.1-j2-voice-green-18`

## Exact-source validation

- J2 Voice Test #90 — success
- J1 Assistant Test #194 — success
- Android CI #2085 — success
- Project Governance #266 — success

J2 #90 passed compile, unit tests, lint, APK assembly, exactly-one-permission audit and component/launcher audit. J1 #194 preserved the zero-permission Assistant boundary. Android CI #2085 passed the complete unit suite, lint, Personal Alpha audit, minified final release audit, Full Test audit and isolated Document Test audit.

## Why this milestone exists

Physical Motorola J2 CI #18 testing proved:

- microphone permission works;
- Android reports on-device speech recognition available;
- J2 can be the Digital assistant and is invoked by the configured Power-button trigger;
- microphone privacy indicator appears;
- first speech attempt fails with `Speech language unavailable`.

The failure was bounded: no false transcript and no crash.

## Repair

The recognition request now uses bounded locale negotiation:

1. current device locale;
2. `hi-IN`;
3. `en-IN`;
4. `en-US`.

Locale tags are canonicalized to BCP-47 form, duplicates removed, blank/`und` skipped, and only language-not-supported/language-unavailable errors advance to the next candidate. There is no endless retry loop and no cloud STT fallback.

An explicit Android 12/API-31 guard protects `createOnDeviceSpeechRecognizer()` for minSdk 26 variants.

## CI discoveries preserved

- J1 #179 caught the missing static API-31 guard in lint; repaired without suppression/baseline weakening.
- J2 #82 caught non-canonical locale output (`HI-in` preserved instead of `hi-IN`); production policy was fixed rather than weakening the test.

## J2 #90 artifact provenance

- Artifact: `mayra-j2-voice-apk-90`
- Artifact ID: `8865632199`
- Package: `ai.mayra.app.j2`
- APK size: `19,192,945` bytes
- APK SHA-256: `2c1e00db4a2bfd98993eb87fe091c5373931153eb3b5ac2252914d4441ac230c`
- Artifact ZIP SHA-256: `bbe4936bc5caec8a08d244ea28f82cf09daabceb49bde47b20ad1678933521b9`
- Requested Android permission: exactly `android.permission.RECORD_AUDIO`

## Motorola retest gate

Install the CI #90 candidate and re-select J2 as Digital assistant if Android resets selection after reinstall. Then test:

- `Mayra namaste`
- `kal subah saat baje`
- `open WhatsApp`
- one short English phrase

Record visible transcript/error. If recognition succeeds, continue direct tap dismissal, 20 repeated cycles, already-locked invocation and reboot recovery.

If all bounded locales remain unavailable, do not silently switch to cloud speech. Record the language-pack limitation and evaluate a separately gated local STT engine.

## Scope not proven by this milestone

This does not prove production wake phrase, local conversational LLM, Phone/InCallService control, AI caller message-taking or full-app owner distribution. Those remain separately gated.
