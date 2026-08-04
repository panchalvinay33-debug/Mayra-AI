# ADR 031 — Owner-controlled self-learning for Mayra

Status: **Accepted as product direction; implementation gated**
Date: 2026-08-04

## Decision

Mayra should become more useful over time, but "self-learning" will not mean silently retraining model weights from every conversation.

The first production-safe meaning of self-learning is an **owner-controlled adaptive memory and preference layer** that learns from explicit corrections, repeated choices and confirmed routines.

## What Mayra may learn

- preferred language and Hinglish style;
- response length and tone preferences;
- frequently used contacts, apps and locations after owner confirmation;
- recurring routines and reminder patterns;
- corrections such as names, relationships and preferred wording;
- task-specific preferences, for example default reminder times or preferred calling app;
- document facts only with source/provenance attached.

## What Mayra must not learn automatically

- passwords, OTPs, payment credentials or authentication secrets;
- health, legal, financial or intimate facts without explicit save confirmation;
- raw private conversations as permanent memory by default;
- unverified model guesses or hallucinations;
- instructions that bypass Android permissions, confirmations or the deterministic action router;
- hidden personality or behavior changes that the owner cannot inspect and undo.

## Learning pipeline

1. Observe a candidate preference or correction.
2. Classify it as temporary context, suggested memory or blocked sensitive data.
3. Ask for confirmation when permanence or sensitivity is involved.
4. Save a typed memory record with source, timestamp, confidence and scope.
5. Apply it only in matching contexts.
6. Expose it in a Memory/Learning screen.
7. Allow edit, forget, pause learning, export and clear-all.
8. Decay or re-confirm stale and conflicting memories.

## Trust boundary

The local LLM may propose a memory candidate, but it cannot directly write trusted memory.

A deterministic validator remains authoritative for:

- memory type;
- sensitivity;
- confirmation requirement;
- conflict resolution;
- retention/expiry;
- action permissions.

Local model text still cannot directly place calls, send messages, create reminders, change settings or execute device actions.

## On-device adaptation phases

### Phase A — Explicit learning

- "Remember this" / "Forget this" commands;
- owner-approved preferences and corrections;
- inspectable local database records;
- no model-weight training.

### Phase B — Suggested learning

- Mayra detects repeated choices;
- suggests one concise memory candidate;
- saves only after owner approval;
- duplicate/conflict detection.

### Phase C — Local personalization

- retrieve relevant memories for prompts;
- rank by recency, confidence and context;
- personalized response style and routing defaults;
- measurable quality and privacy tests.

### Phase D — Experimental adapter training

Only after device benchmarks, battery/thermal testing, rollback support, data minimization and explicit owner opt-in. Any LoRA/adapter or embedding update must be local, versioned, reversible and never silently uploaded.

## Required controls

- Learning: On / Ask every time / Off;
- show what was learned and why;
- delete one memory or all memory;
- private/sensitive categories disabled by default;
- exportable audit log without secrets;
- reset personalization independently from model files;
- deterministic fallback when memory is missing or corrupt.

## Acceptance gates

- no silent sensitive-memory writes;
- every persistent item has provenance;
- corrections override older conflicting entries predictably;
- owner can inspect and remove learned items;
- airplane-mode operation;
- no network transmission unless separately enabled and disclosed;
- memory poisoning and prompt-injection tests;
- local LLM cannot claim an action occurred when it did not;
- J1/J2/J3/J4 regressions remain green.

## Current sequencing

J4 has physically proven local Gemma3-1B CPU initialization, generation, close/rebind and reload on Motorola Edge 70 Fusion. The next engineering sequence is:

1. improve Hindi/Hinglish quality and benchmark longer responses;
2. add generation cancellation, RAM/thermal and repeated-cycle measurements;
3. implement typed owner-controlled memory candidates;
4. add confirmation, inspect/edit/forget and provenance UI;
5. connect approved memories to local-brain prompt retrieval;
6. only later evaluate optional local adapter training.

## Consequence

Mayra can genuinely become more personal over time without turning every model output into trusted truth. Learning remains transparent, reversible and under the owner's control.