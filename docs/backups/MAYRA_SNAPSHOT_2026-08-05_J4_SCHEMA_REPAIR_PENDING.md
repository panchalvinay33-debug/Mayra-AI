# Mayra Snapshot — J4 Room/KSP Schema Repair Pending

Date: 2026-08-05
Type: Failure/repair checkpoint — NOT a stable baseline
Branch: `agent/document-library-foundation`
PR: #12 Draft/open/unmerged

## Trigger

J4 Local LLM Test run #102 failed in `kspJ4LocalLlmTestKotlin` while Room/KSP attempted to deserialize a truncated schema JSON. The payload ended after `"formatVersion"` and raised `Expected colon ':', but had 'EOF'`.

## What was already proven in the failed run

- LiteRT-LM Android AAR version pinned to 0.15.0;
- AAR SHA-256 verification passed;
- `Engine.class` classfile probe passed at Java 21 classfile major 65;
- Maven LiteRT module / Kotlin 2.2.21 did not leak onto J4 compile classpath.

Therefore this snapshot does not classify the failure as a LiteRT-LM runtime-provenance failure.

## Repair applied

Workflow source commit: `7ebabeec21e13da77fe81371a6284b1853c00f20`.

Repair:

- reset transient `app/schemas` before KSP-bearing stages;
- isolate J4 compilation from Debug unit-test KSP work;
- parse generated Room schema JSON immediately after J4 compile;
- run focused Debug tests separately;
- run J4 lint separately;
- assemble J4 separately;
- preserve zero-permission and runtime-isolation audit.

Governance semantics were then hardened so `START_HERE` section numbering can evolve while the required resume/completion headings remain mandatory, and Jarvis/J5 planning records became required governance files. Governance run #531 on source `8790ef6a896e43ff08c4c9fc4d9a1762c1a3d73e` passed.

## Current validation state when this snapshot was written

- Project Governance #531: SUCCESS.
- J4 Local LLM Test on the newest documentation/repair head: pending/running.
- J1/J2/J3/Android regressions on the newest head: pending/running.

No green J4 baseline is created from this checkpoint.

## Next decision

If J4 passes, preserve the exact run/artifact/digest evidence and continue the J4 device-quality gate before J5 implementation.

If J4 still fails with malformed Room schema JSON, inspect the exact generated file and database writer, then introduce variant-specific schema output rather than weakening KSP/Room validation.

## Recovery

Latest protected application recovery branch remains:

`baseline/mayra-0.2.1-j2-privacy-tts-green-136`

The Jarvis/Launcher planning snapshot remains separate:

`docs/backups/MAYRA_SNAPSHOT_2026-08-05_JARVIS_LAUNCHER_DIRECTION.md`
