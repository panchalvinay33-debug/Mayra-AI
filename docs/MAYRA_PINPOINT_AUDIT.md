# Mayra AI — Full Project Pinpoint Audit

Audit date: 2026-08-03
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged

## Audit method

Each subsystem is checked against five truths:

1. Product requirement and owner intent.
2. Actual source/runtime wiring.
3. Automated tests and failure paths.
4. APK/manifest/permission/package boundary.
5. Motorola physical validation evidence.

## Executive result

- Pre-Jarvis protected baseline: `baseline/mayra-0.2.1-green-1795` at `065e22524c835f3ddd3b2f56215a3616f071d4b3`.
- Current Jarvis J1 protected baseline: `baseline/mayra-0.2.1-jarvis-j1-green-1851` at `0d9435adb92b425bfb47a710d4f4516a6aaac398`.
- Android CI #1851: success.
- Project Governance #32: success.
- Personal Alpha #1851 is provenance-recorded and ready for Motorola J1 testing.
- No merge/ready transition is authorized.

## Pinpoint module register

| Area | Source state | Automated/package evidence | Device evidence | Status / exact gap |
|---|---|---|---|---|
| One launcher / internal screens | Implemented | #1851 package/launcher audits | Latest candidate pending | DEVICE_VERIFY |
| Local deterministic chat | Implemented | Regression tests green | Partial old evidence | DEVICE_VERIFY |
| Local LLM brain | Not integrated | None | None | PLANNED after J1 |
| Optional cloud provider | Implemented | transport/settings/fallback + package audits | Real owner key pending | DEVICE_VERIFY |
| Provider credential security | Keystore AES-GCM | tests/lint/backup-off/HTTPS-only | Recovery pending | DEVICE_VERIFY |
| Personal memory | Implemented | lifecycle/provenance tests | Full Motorola lifecycle pending | DEVICE_VERIFY |
| TXT/PDF/DOCX library | Implemented | extraction/search/health + Document Test | Latest physical files pending | DEVICE_VERIFY |
| OCR / legacy DOC | Not implemented | explicit unsupported paths | None | DEFERRED |
| App opening | Implemented | routing collision tests | Latest test pending | DEVICE_VERIFY |
| Contact resolution | Implemented | resolver/action tests | Latest test pending | DEVICE_VERIFY |
| Calls/messages | Dialer/composer review-first | intent/confirmation + forbidden-permission audits | Pending | DEVICE_VERIFY |
| Reminder parser/store | Implemented | language/time/state tests | Pending | DEVICE_VERIFY |
| Reminder follow-up/recovery | Implemented/repaired | DUE→MISSED + remaining-delay tests | Doze/reboot pending | DEVICE_VERIFY |
| Confirmation expiry | Implemented | replay/mismatch/expiry tests | UI pending | DEVICE_VERIFY |
| Activity History | Implemented | persistence tests | Pending | DEVICE_VERIFY |
| Voice input/TTS | Foundation implemented | compile/lint | Quality pending | DEVICE_VERIFY |
| Animated assistant session | Implemented foundation | #1851 compile/tests/lint/package green | No physical invocation yet | DEVICE_VERIFY |
| Android Assistant role | Implemented foundation | #1851 source/package green | Role visibility/selection pending | DEVICE_VERIFY |
| Lock-screen assistant | Declaration foundation | #1851 manifest/package green | Locked invocation pending | DEVICE_VERIFY |
| Wake phrase / always listening | Not integrated | Recognition shell only | None | PLANNED |
| Notification intelligence | Listener foundation | package audit | Special-access behavior pending | DEVICE_VERIFY/PLANNED expansion |
| Default Phone role | Not implemented | None | None | PLANNED |
| Incoming answer/reject/speaker | Not implemented | None | None | PLANNED after Phone role |
| Call screening | Not implemented | None | None | PLANNED |
| Caller message-taking | Constrained architecture | None | None | PLANNED_WITH_CONSTRAINTS |
| Release minification | Implemented | #1851 final R8/manifest audit green | signed upgrade pending | IN_PROGRESS |
| Release signing | Environment scaffold | build config green | no signed artifact | PLANNED finalization |
| Project docs/governance | Implemented | Governance #32 green | N/A | DONE/continuous |
| Baseline/rollback | Two protected branches + playbook | Git references | device rollback not exercised | DONE code / DEVICE_VERIFY install |

## Key findings and sequencing

1. J1 is now CI/package verified, so the next valid step is Motorola testing—not wake-word speculation.
2. Local LLM and wake phrase remain blocked until role invocation, animation lifecycle, reboot and battery observations are recorded.
3. Phone role must be optional and isolated behind explicit owner selection.
4. Free-form models may propose intents but never receive direct memory/action authority.
5. Every install candidate requires package/version/source/CI/artifact/digest provenance.
6. Physical acceptance remains the largest evidence gap across mature core features.

## Failure and repair history

Android CI #1833 caught two Assistant compile issues:

- unavailable optional RecognitionService override;
- animation repeat property set through base Animator.

Both were repaired without weakening the design. Android CI #1851 then passed the complete governed pipeline.

## Current testing gate

Use `docs/testing/MAYRA_J1_MOTOROLA_ACCEPTANCE.md` with Personal Alpha #1851:

- source: `0d9435adb92b425bfb47a710d4f4516a6aaac398`;
- artifact ID: `8852147191`;
- APK SHA-256: `1459517f1aa375576afa353ba6683ceaf81ddbcb4e79fc6dd790a501f52307b8`.

Required evidence:

1. install/one-launcher sanity;
2. Assistant role visibility and owner selection/removal;
3. unlocked invocation;
4. locked invocation;
5. animation lifecycle and repeated invocation stability;
6. force-stop/reboot recovery;
7. no hidden role selection, overlay or continuous microphone use;
8. core regression smoke after role selection.

J2 cannot begin until device results are written back into acceptance, audit, roadmap and latest snapshot.
