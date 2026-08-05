# ADR-032 — Self-learning uses Mayra's canonical owner-approved memory

Status: Accepted
Date: 2026-08-05

## Decision

Mayra self-learning must feed the existing `MayraPersonalMemoryManager` approval pipeline before any learned preference can become trusted context.

The canonical production memory remains the existing protected personal-memory system used by chat, Memory Center and assistant retrieval. Self-learning is a proposal source, not a separate authority.

## Required behavior

1. A deterministic learner may recognize only narrow, explicit preference statements at first.
2. Recognized preferences become `MayraMemoryCandidate` proposals with `sourceType = self-learning`.
3. A learned proposal is never considered trusted merely because confidence is high.
4. The owner must still see and approve the proposal through the same pending-memory UI used by explicit `remember` commands.
5. Replacements show the previous value before approval.
6. Rejected, cancelled, expired or forgotten proposals/memories must never enter assistant context.
7. Secrets and sensitive categories continue through the canonical memory privacy policy before a proposal is created.
8. Ordinary requests that merely contain a language word (for example, asking a Hindi question) must not be treated as learning events.

## Initial learned preferences

The first production-facing deterministic learner is intentionally small:

- response language: Hindi, Hinglish or English;
- response length: short or detailed.

Examples such as `Hinglish me baat karo` may create a pending proposal. A mixed request such as `Hindi me Delhi ka weather batao` must remain an ordinary request and must not silently create memory.

## Relation to the experimental learning package

The `ai.mayra.app.learning` policy/store/review components remain an engineering boundary for evaluating richer future learning flows. They are not a second production source of truth. Before any such richer candidate affects production replies, it must cross into the canonical owner-approved personal-memory pipeline described above.

This prevents split-brain memory, duplicate owner controls and inconsistent forgetting semantics.

## Future gate

Broader repeated-behavior learning may be added only after:

- deterministic false-positive tests;
- owner-visible provenance;
- bounded confidence/repetition rules;
- physical-device UX verification;
- proof that one owner action can inspect, edit or forget every learned item from Memory Center.
