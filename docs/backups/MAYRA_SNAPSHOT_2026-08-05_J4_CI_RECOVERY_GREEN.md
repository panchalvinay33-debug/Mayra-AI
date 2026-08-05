# Mayra AI — J4 CI Recovery Green Snapshot

Date: 2026-08-05
Status: IMMUTABLE MILESTONE SNAPSHOT
Source: `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`
Protected recovery branch: `baseline/mayra-0.2.1-j4-ci-recovery-green-134`
Backup branch: `backup/j4-ci-recovery-2026-08-05`
PR: #12 — Draft/open/unmerged

## Why this snapshot exists

The J4 local-brain track was temporarily blocked by Room/KSP reading truncated or empty schema JSON while multiple Android variants shared `app/schemas`. The failure looked like a J4 problem first, but the same failure later reproduced in FullTest under the shared Android CI, proving a multi-variant Room schema output race rather than a LiteRT-LM runtime failure.

## Repair applied

- J4 compile, tests, lint and assembly are executed in isolated Gradle invocations.
- transient Room schema output is reset before isolated variant work;
- generated Room JSON is explicitly parsed/validated after J4 compile;
- Android CI serializes governed variant compilation/lint so Room/KSP variants do not write the shared schema directory concurrently;
- no broad Java/Kotlin migration was introduced;
- LiteRT-LM remains isolated to the J4 engineering package/process.

## Exact green evidence

At source `e72488a6f6dceb24950f9b0f574ae223d52bd8bb`:

- Android CI #2356 — SUCCESS;
- J1 Assistant Test #465 — SUCCESS;
- J2 Voice Test #361 — SUCCESS;
- J3 Neural TTS Test #183 — SUCCESS;
- J4 Local LLM Test #134 — SUCCESS;
- Project Governance #537 — SUCCESS.

Android CI #2356 passed governed variant compile, complete debug unit tests, all governed lint variants, Personal Alpha package/audit, minified Release/R8 package/audit, FullTest package/audit and isolated DocumentTest package/audit.

J4 #134 passed pinned LiteRT-LM 0.15.0 AAR provenance/hash/classfile checks, Maven-transitive isolation, isolated compile, Room JSON validation, focused local-model/learning tests, lint, J4 APK assembly, zero-permission/component/runtime-boundary audit and artifact upload.

J4 runtime APK artifact: `mayra-j4-litert-runtime-apk-134`, artifact ID `8917566340`.
J4 audit artifact: `mayra-j4-litert-runtime-audit-134`, artifact ID `8917567141`.

## Physical device truth preserved

Earlier Motorola Edge 70 Fusion / Android 16 evidence already proves:

- Gemma3-1B `.litertlm` model import + SHA verification;
- LiteRT-LM CPU initialization reaches Stage 5/5;
- fixed Hindi/Hinglish/English local generation works;
- isolated `:localbrain` close/reload works;
- launcher/test activity survives local-brain process close/failure boundary.

That evidence remains valid, but short fixed prompts are not enough to promote the model as Mayra's production conversational brain.

## Next gate

J4 quality/operability benchmark only:

1. longer useful Hindi/Hinglish/English prompts;
2. output length/approximate token metrics;
3. generation timing/decode estimate where defensible;
4. local-brain process RAM snapshots before/after load and generation;
5. explicit cancel-generation behavior;
6. 10 sequential prompts;
7. 5 close/reload cycles;
8. background, lock and process-kill recovery;
9. Airplane-mode repeat;
10. owner-observed battery/thermal behavior.

No J5 launcher implementation should invalidate this recovery point. If a later batch regresses core CI, return to the protected branch above.
