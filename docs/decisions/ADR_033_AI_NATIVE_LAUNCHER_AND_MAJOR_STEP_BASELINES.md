# ADR 033 — AI-Native Launcher Shell and Major-Step Baselines

Date: 2026-08-05
Status: Accepted

## Context

The owner wants Mayra to behave like a practical Jarvis-style personal Android companion. A normal app surface alone does not provide the desired persistent Home presence, contextual dashboard or natural entry point for voice and actions. At the same time, making the launcher responsible for heavy AI execution would create unacceptable reliability risk.

The owner also requires that ideas, blueprints, roadmaps, backups and recovery points are updated continuously and that every major step is treated with the same discipline as a protected baseline.

## Decision

1. Mayra will evolve toward an AI-native Android launcher/Home shell while keeping the Mayra brain, memory, voice and action layers modular and independent from critical Home rendering.
2. The launcher is a primary interaction shell, not a security bypass and not the privileged authority for actions.
3. Heavy model/runtime failure must not make the launcher unusable.
4. Existing Android Digital Assistant integration remains a parallel always-available entry point and permanent fallback.
5. Free-form model output never directly executes privileged actions. Typed deterministic policy/adapters remain the execution authority.
6. Every major capability must have an explicit pre-implementation planning checkpoint and a post-validation recovery checkpoint.
7. Protected `baseline/*` branches are created only from exact green commits. Planning snapshots and failure snapshots may be immutable records but must never be labeled stable code baselines.
8. Before a major step begins, applicable Idea Ledger, Blueprint, Roadmap, Decision and test/preflight records must describe the goal, trust boundary, validation gate and rollback path.
9. After a major step becomes green, Latest Snapshot, Changelog, test evidence and immutable milestone snapshot must be synchronized before the next risky phase.

## Consequences

- Jarvis development gains a stable product shell without coupling phone usability to the local LLM.
- Launcher work receives its own isolated feasibility and device-validation phase after the current J4 head is repaired and promoted from green evidence.
- Major feature work becomes slower to start but much safer to recover, audit and continue across conversations/sessions.
- No red or pending head can be promoted as a baseline or used as justification for stacking unrelated speculative features.

## Validation gates for the launcher phase

At minimum, Motorola device evidence must prove:

- Mayra can be selected as default Home;
- Home gesture/button consistently returns to Mayra;
- selection persists through reboot where Android permits;
- installed apps remain reachable/searchable;
- launcher remains usable with local model disabled/killed/corrupt;
- crash/restart recovery is bounded;
- owner can switch back to another launcher;
- voice/Digital Assistant coexistence does not create a loop or unusable navigation state;
- battery/RAM remain acceptable during normal Home idle.

## Related record

`docs/MAYRA_JARVIS_LAUNCHER_MASTER_PLAN.md`
