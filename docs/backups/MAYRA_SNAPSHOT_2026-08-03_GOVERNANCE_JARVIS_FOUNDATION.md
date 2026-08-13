# Mayra AI — Immutable Snapshot: Governance + Jarvis Foundation

Snapshot date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft, open, unmerged
App version: 0.2.1 / versionCode 4

## Stable baseline entering this transition

Android CI #1795 completed successfully before the Jarvis Assistant-role commits. It verified the 0.2.1 application foundation, complete tests, lint, Personal Alpha audits, minified final release audit, Full Test and Document Test.

## Transition delivered after that baseline

### Jarvis foundation

- Android VoiceInteractionService foundation.
- VoiceInteractionSessionService/session foundation.
- Animated Mayra assistant-session orb foundation.
- RecognitionService shell and assistant metadata.
- Lock-screen assistant session declaration.
- Assistant components excluded from low-permission Full Test.

These items were not yet Motorola-verified at snapshot creation.

### Governance foundation

- Added root `START_HERE.md` as mandatory first-read document.
- Replaced stale repository README with canonical navigation.
- Added `docs/MAYRA_IDEA_LEDGER.md`.
- Added `docs/MAYRA_DECISIONS.md`.
- Added `docs/MAYRA_CHANGELOG.md`.
- Rebuilt blueprint for local-first brain, Assistant role and future Phone role architecture.
- Rebuilt roadmap for 0.2.1 and phased Jarvis delivery.
- Expanded mandatory update/resume policy.
- Added `scripts/verify_project_governance.sh`.
- Added `.github/workflows/project-governance.yml`.

## Accepted architecture direction

- Mayra is local-first and cloud-optional.
- Official Android Assistant role is the preferred always-available path.
- Offline wake phrase and local LLM are separate benchmarked milestones.
- Advanced incoming-call control will require optional default Phone/Call Screening roles.
- Standard cellular call audio injection/recording is not assumed.
- Owner Mode may streamline routine actions but retains recovery for broad destructive actions.

## Deferred or constrained

- OCR and legacy binary DOC.
- Exact alarms unless device tests justify them.
- Root/accessibility-based unrestricted automation.
- Hidden SIM-call recording or arbitrary AI speech injection.

## Validation truth

- Pre-transition code baseline: Android CI #1795 green.
- Governance/Jarvis transition: latest-head Android CI and Project Governance CI pending at snapshot creation.
- No physical Assistant-role, wake-word, local-model or call-role success claim exists yet.

## Recovery procedure

1. Read root `START_HERE.md`.
2. Read rolling `docs/backups/MAYRA_LATEST_SNAPSHOT.md` for the newer exact head/run state.
3. Check PR #12 remains Draft/open/unmerged.
4. Repair latest-head CI/governance failures before extending Jarvis work.
5. Continue Phase J1 from the roadmap.

## Secret and merge truth

No credentials, API keys, signing passwords or private owner data belong in this snapshot. This snapshot does not authorize merging or marking PR #12 ready.
