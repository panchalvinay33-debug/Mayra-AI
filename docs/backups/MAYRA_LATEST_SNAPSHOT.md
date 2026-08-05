# Mayra AI — Latest Project Snapshot

Snapshot date: 2026-08-05
Branch: `agent/document-library-foundation`
PR: #12 — Draft/open/unmerged
App version: 0.2.1 / versionCode 4
Current phase: repair current J4 CI red head, then complete J4 green recovery baseline before J5 AI-native launcher implementation.

## Canonical product direction

Mayra is now explicitly targeting a practical **Jarvis-style personal Android operating layer**.

The final product combines:

- Android Digital Assistant / voice presence;
- local-first conversational brain with deterministic fallback;
- owner-approved memory and private documents;
- reminders, notification intelligence and people/context;
- AI-native default launcher/Home shell;
- trust-gated typed action engine;
- proactive My Day/pending-item assistance;
- later multimodal understanding and owner-defined routines through separate gates.

Canonical plan: `docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
Decision: `docs/decisions/ADR_033_AI_NATIVE_LAUNCHER_AND_MAJOR_STEP_BASELINES.md`
Immutable planning snapshot: `docs/backups/MAYRA_SNAPSHOT_2026-08-05_JARVIS_LAUNCHER_DIRECTION.md`

## Canonical truth

- Final product remains one Mayra app plus one AI-native launcher/Home shell; J1/J2/J3/J4 and future isolated engineering packages are test/proof packages only.
- The launcher is Mayra's Home surface, not the privileged action authority.
- Heavy AI/model/provider failure must not make Home unusable.
- PR #12 is not authorized for merge/ready.
- Protected `baseline/*` branches are immutable exact-green recovery markers.
- Planning/failure snapshots are not green application baselines.
- Device capability claims require Motorola evidence.
- Android offline TTS remains the production-safe speech fallback until a license-clear neural voice is selected.
- Free-form LLM output never directly executes privileged actions or writes trusted owner memory/context.

## Latest protected application baseline

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

- source `ef179cf4cb2395af2647be21dbacea6fb3c7cb62`
- J2 #136 / J1 #239 / Android CI #2131 / Governance #312: success
- artifact `mayra-j2-voice-apk-136`, ID `8868518898`
- APK SHA-256 `b2d129b2b40d8c2aef7eef21f1acf087daf0600224fd5ce4366b38f4aefad1d0`

## Motorola voice evidence

Physically proven:

- Android Digital assistant selection and Power-button invocation;
- offline Hindi/Hinglish/English recognition;
- direct dismissal paths and 20-cycle stability;
- already-locked invocation;
- reboot/no-speech/rapid-interaction checks reported OK;
- Android offline TTS speaks but sounds robotic to owner.

## J3 neural voice milestone

Passing source: `1ed0cd3e8d9ebe7671f3027fe0ab85b41da0294a`

- J3 #29 / Android CI #2202 / J2 #207 / J1 #311 / Governance #383: success;
- app-private model materialization: PASS;
- sherpa native constructor/model load/synthesis/playback: PASS;
- sample RTF: 0.72;
- all six phrases reported fine, phrase #1 preferred;
- Airplane mode / Stop cleanup / 20-reply stability reported pass.

The Priyamvada voice remains benchmark-only because its model-card dataset terms are not production-cleared. Keep system TTS fallback and reuse J3 architecture only with a license-clear voice pack.

## J4 local brain — current exact-head truth

J4 has accumulated model import/integrity, isolated local-brain and LiteRT-LM provenance work. However the latest inspected PR head before this documentation synchronization had one failing workflow:

- **J4 Local LLM Test — failure**;
- Android CI — success;
- J1 Assistant Test — success;
- J2 Voice Test — success;
- J3 Neural TTS Test — success;
- Project Governance — success.

Failure root cause observed in the J4 job:

- LiteRT-LM Android 0.15.0 AAR download and SHA-256 verification passed;
- classfile probe passed (`Engine.class`, Java 21 classfile major 65);
- compile classpath isolation check passed;
- `kspJ4LocalLlmTestKotlin` failed when Room/KSP deserialized a truncated schema JSON;
- error: `Expected colon ':', but had 'EOF'` with JSON input ending after `"formatVersion"`.

Conclusion: this is a localized Room/KSP schema-data/build failure in the current head, not evidence that the LiteRT-LM AAR probe itself failed. The head remains red and must not be promoted.

## Major-step baseline discipline

Before every major capability:

1. Idea Ledger update;
2. ADR/decision;
3. Blueprint update;
4. Roadmap gate;
5. preflight/test contract where needed;
6. rollback point;
7. immutable planning snapshot for material direction changes.

After implementation becomes exact-green:

1. all applicable CI/lint/unit/package/permission/component audits;
2. Motorola evidence for device claims;
3. Changelog + Latest Snapshot + test evidence synchronization;
4. immutable milestone snapshot;
5. protected `baseline/*` branch on the exact green commit;
6. next risky phase recorded.

## Jarvis phases accepted

- J5 AI-native launcher shell;
- J6 provenance-aware context fabric;
- J7 GREEN/AMBER/RED trust/action orchestration;
- J8 proactive My Day/pending-item intelligence;
- J9 multimodal Mayra;
- J10 owner-defined routines.

None of these should be stacked on the current red J4 head.

## Immediate next gate

1. repair the Room/KSP truncated-schema failure;
2. restore exact-head J4 + J1/J2/J3/Android/Governance green;
3. complete J4 longer-output/cancel/RAM/thermal/background/lock/Airplane regressions;
4. synchronize evidence and create a protected J4 recovery baseline only from exact green;
5. then begin J5 launcher preflight and isolated implementation;
6. prove default HOME selection, Home-button return, reboot persistence, app drawer/search, launcher switch-back and model/crash failure survival on Motorola.

## Trust boundary

- local LLM never directly executes calls/messages/device actions;
- local LLM never directly writes owner memory;
- context provenance remains structured;
- confirmation tokens remain typed/action-bound/expiring;
- local mode never silently sends owner context to network;
- missing/corrupt/killed local model falls back to deterministic Mayra;
- launcher remains usable when heavy AI is unavailable.

## Distribution truth

Hosted CI debug signatures may conflict. Stable owner signing/trusted distribution remains required. Never bypass Play Protect or Android security.
