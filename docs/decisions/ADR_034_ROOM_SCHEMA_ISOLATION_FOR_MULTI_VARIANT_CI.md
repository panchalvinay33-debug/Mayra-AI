# ADR-034 — Isolate Room schema generation across multi-variant CI

Date: 2026-08-05
Status: Accepted / implementation under exact-head CI validation

## Context

The J4 Local LLM workflow failed in `kspJ4LocalLlmTestKotlin` while Room attempted to deserialize a truncated schema JSON. The observed JSON ended immediately after `"formatVersion"`, producing `Expected colon ':', but had 'EOF'`.

In the failing Gradle invocation, J4 compilation and Debug unit-test work were requested together. Both variants use KSP/Room and the project-level `room.schemaLocation` points at the same `app/schemas` directory. The logs showed J4 and Debug KSP work active in the same build window.

The LiteRT-LM 0.15.0 AAR provenance steps had already passed: download, SHA-256, classfile level and compile-classpath isolation were not the failing boundary.

## Decision

J4 CI will treat Room schema output as transient shared build state and will not allow J4 compile and Debug KSP work to contend for it in one Gradle invocation.

The J4 workflow therefore:

1. removes/recreates `app/schemas` before each KSP-bearing stage;
2. compiles J4 in its own Gradle invocation;
3. parses every generated Room schema JSON after J4 compilation and fails with the exact file if malformed;
4. runs focused Debug unit tests in a separate Gradle invocation;
5. runs J4 lint separately;
6. assembles J4 separately;
7. preserves the existing zero-permission/package/runtime audit.

## Why this is preferred

- It repairs the observed shared build-state failure without weakening Room/KSP checks.
- It does not change the production database schema or migration contract.
- It does not broaden J4 permissions or runtime authority.
- It provides precise diagnostics if malformed schema output returns.
- It avoids a broad Kotlin/Room/KSP/toolchain change while the actual failure is localized.

## Promotion gate

This decision is not considered proven merely because the workflow source changed. Promotion requires the exact-head J4 workflow to pass together with applicable J1/J2/J3/Android/Governance regressions.

If the isolated workflow still produces malformed JSON, the next step is to identify the exact Room database/schema writer and move from workflow isolation to variant-specific Room schema directories or a Room Gradle-plugin configuration. Tests/checks must not be disabled to force green.

## Rollback

The workflow-only repair can be reverted independently. The last protected application baseline remains `baseline/mayra-0.2.1-j2-privacy-tts-green-136`; this ADR does not move any application baseline.
