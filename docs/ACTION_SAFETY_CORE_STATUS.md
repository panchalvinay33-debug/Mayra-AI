# Mayra Action Safety Core — Implementation Status

This status file maps the first coding batches after the locked Master Blueprint.

## Implemented foundation

- Unified `MayraActionEngine` orchestration boundary.
- Explicit capability registry with available, setup-required, unsupported and policy-restricted states.
- Permission manager over the existing Android permission snapshot model.
- Risk classifier with low, medium, high and critical outcomes.
- Sensitive, destructive, financial, public-post and legal-acceptance policy metadata.
- Confirmation and strong-authentication recommendations.
- Honest result verification that distinguishes Android handoff from final completion.
- Action-specific fallback planning.
- Bounded visible audit history.
- User kill switch and resume control.
- Shared `MayraActionRuntime` service holder.
- Internal Action Controls screen exposed from the Living Presence launcher.
- Deterministic unit tests for the new core.

## Production command migration

The Android production constructor now installs and uses the shared `MayraActionRuntime`. Existing chat and voice commands therefore route these actions through the unified safety engine:

1. App opening.
2. Contact call initiation.
3. SMS/message composition.
4. Reminder creation handoff.

Target resolution still occurs before execution:

- Installed apps are resolved and ambiguous matches are rejected.
- Contacts are resolved only after contact permission is available.
- Multiple matching contacts require explicit disambiguation.
- Concrete requests then pass through capability, permission, confirmation, kill-switch, verification, fallback and audit layers.

The older injected `DeviceActionCoordinator` path remains available for deterministic tests and compatibility, but the real `AndroidActionExecutor(context)` path uses the shared engine.

## Voice safety controls

- `Mayra stop` and equivalent explicit phrases activate the global phone-action kill switch.
- `Mayra resume actions` enables the action path again.
- Plain `stop`, `no` or `cancel` rejects only the current pending confirmation.
- Chat and Phone Pulse remain usable while actions are stopped.

## Honest handoff wording

- A message composer opening is described as a prepared draft, never as a sent message.
- A call flow opening is not described as a connected call.
- A reminder screen opening is not described as a saved reminder.
- Android accepting an app-open intent is not proof of the destination app's final state.

## Existing system preserved

The new engine reuses `DeviceActionCoordinator`, `DeviceActionSafetyGate` and `AndroidDeviceActionRunner`. It does not introduce a parallel unrestricted executor and does not delete the existing action pipeline.

## Remaining action migrations

1. Notification direct-reply actions.
2. Relationship-aware contact identity aliases.
3. WhatsApp assisted messaging adapter.
4. Calendar event actions.
5. File-share and media actions.
6. Strong authentication for critical actions.
7. Persistent encrypted audit storage and optional retention controls.

Each migration must preserve user-visible handoff wording and must not claim final send/call/save completion without platform evidence.

## Validation status

GitHub Actions runs after the blueprint commit are failing before Checkout with no job steps or logs. This is an infrastructure/runner status, not a proven source compilation result. The last full source validation before the runner issue was Android CI #571, which passed compile, targeted tests, full unit tests and lint. The newly migrated Action Safety Core requires a fresh successful CI run before it can be called build-verified.
