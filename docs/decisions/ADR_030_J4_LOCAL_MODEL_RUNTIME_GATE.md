# ADR-030 — J4 local-model bytes and runtime toolchain are separate gates

Date: 2026-08-04
Status: Accepted / active implementation
Parent log: `docs/MAYRA_DECISIONS.md`

## Decision

Mayra will separate local-LLM integration into two trust gates before any model output reaches the assistant:

1. **model-byte gate** — owner-selected `.litertlm` bytes are validated, copied atomically to app-private storage, checksum-pinned and independently re-verifiable;
2. **runtime/toolchain gate** — the exact LiteRT-LM Android SDK version, AAR hash, dependency metadata and Java/Kotlin compatibility are proven in CI before the SDK is linked to J4.

The first runtime proof uses a current upstream-supported **Gemma3-1B chat-ready `.litertlm`** candidate because it is substantially smaller than the multi-GB reference alternatives. It is a runtime proof candidate, not the final Mayra language-quality decision.

## Why

The J3 neural-TTS work demonstrated that packaging success does not prove native/runtime compatibility on the target Motorola. J3 became stable only after failure boundaries were isolated and model assets were moved into app-private filesystem paths.

Local LLMs add even larger storage, RAM and toolchain risks. Linking a moving SDK or blindly upgrading the whole project before proving model lifecycle would couple an experimental feature to mature J1/J2/J3/full-app code.

## Consequences

- J4 L0/L1 remains zero-permission and does not need Internet.
- Large model files remain owner-managed data, never base-APK weight.
- `.partial` imports are never accepted as runnable models.
- SHA-256 mismatch blocks runtime initialization.
- Current Mayra Java/Kotlin toolchain is not broadly upgraded from assumption.
- J4 CI probes the current LiteRT-LM Maven AAR before dependency wiring.
- After compatibility evidence, one exact SDK version is pinned to J4 only.
- CPU is the first runtime compatibility backend; GPU is a later measured comparison.
- Engine initialization runs off the UI thread and must have explicit close/release behavior.
- Free-form model text remains outside action execution, memory writes and document provenance authority.
- Missing/corrupt/killed model falls back to deterministic Mayra.

## Validation ladder

1. J4 package compile/lint/zero-permission audit.
2. Local-model integrity unit tests.
3. LiteRT-LM Maven/AAR provenance + class-file compatibility probe.
4. Motorola `.litertlm` import/re-verify/reopen/remove/re-import evidence.
5. Exact SDK pin + J4-only runtime compile/package audit.
6. Motorola CPU engine load/close/reload evidence.
7. Fixed Hindi/Hinglish/English prompt benchmark in Airplane mode.
8. RAM/latency/thermal/cancellation/recovery evidence.
9. Only then evaluate stronger model/GPU and possible final-app provider integration.

## Supersession note

This refines the earlier ADR-021 wording that named Qwen3-1.7B as an initial model candidate. The architectural decision to benchmark before model selection remains valid; the first concrete runtime-proof candidate is now current upstream-supported Gemma3-1B LiteRT-LM because it minimizes integration risk.
