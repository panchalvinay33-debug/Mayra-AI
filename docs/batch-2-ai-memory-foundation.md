# Batch 2: AI Memory Foundation

This batch starts the clean Phase 7 reconstruction from `main` after the Phase 5 foundation was squash-merged.

## Included

- Bounded, thread-safe recent conversation context
- Duplicate suppression and normalized text storage
- Context search, snapshots, dropped-turn accounting, and clearing
- Framework-independent long-term memory records
- Deterministic normalized IDs and upsert behavior
- Confidence and memory-kind filtering
- Ranked memory retrieval
- Namespace clearing, forgetting, and snapshots
- JVM unit tests for both engines

## Next slice

- Task planner and validated execution plans
- Mayra AI orchestrator
- RuntimeKernel integration
- Persistent adapters between Room and the framework-independent memory engine
