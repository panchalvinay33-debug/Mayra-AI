# Mayra AI — Phone Role / Incoming-Call Control Feasibility Preflight

Status: APPROVED FOR DESIGN/ISOLATED IMPLEMENTATION ONLY — DEFAULT PHONE ROLE MUST NOT BE REQUESTED UNTIL COMPLETE CALL UI IS READY
Date: 2026-08-03
Gate: Issue #14
Target: Motorola Edge 70 Fusion / Android 16
Rollback baseline before future implementation: `baseline/mayra-0.2.1-j2-voice-green-18`

## Owner outcome

When explicitly enabled by the owner, Mayra should be able to announce an incoming caller and support deterministic voice commands such as:

- answer;
- reject/hang up;
- silence ringing;
- mute/unmute;
- speaker on/off;
- route to an available Bluetooth/earpiece endpoint where Android exposes it.

The call must remain recoverable through a complete visible call UI even if Mayra voice recognition is unavailable.

## Correct Android architecture

Primary call-control path:

- user-selected default Phone/Dialer role;
- complete `InCallService` implementation;
- incoming/ringing and ongoing-call UI;
- Android `Call` methods for answer/disconnect;
- `InCallService.setMuted()` for mute state;
- API 34+ `requestCallEndpointChange()` using Android-provided CallEndpoints for speaker/Bluetooth/earpiece routing;
- emergency/system fallback behavior preserved.

Optional separate capability:

- `CallScreeningService` for quick allow/silence/reject/caller-identification decisions.

## Why CallScreening is not the voice-command controller

Android requires a CallScreeningService to respond to an incoming call within about 5 seconds. It is suitable for deterministic screening/spam rules but not for waiting while Mayra announces the caller and the owner verbally decides `answer` or `reject`.

Voice-driven ringing-call control belongs to the active default-dialer/InCallService flow.

## Hard boundary: no half-built default dialer

Mayra must **not** request the default Phone role until all of these already exist and pass automated tests:

- ACTION_DIAL-capable dialer activity;
- complete incoming-call UI;
- complete ongoing-call UI;
- answer/reject/hang-up controls;
- mute and audio-endpoint controls;
- lost-role/failure fallback;
- emergency-call handling compatible with Android requirements;
- Bluetooth/headset behavior and lifecycle handling;
- phone-process/reboot recovery.

Requesting the role before this can degrade normal calling, so the role gate is deliberately last.

## Voice command architecture

Mayra voice must feed a deterministic call command state machine, not a free-form LLM action executor.

Allowed examples:

- `answer` → answer the current ringing call only;
- `reject` → disconnect/reject the current ringing call only;
- `speaker` → request the currently available speaker CallEndpoint;
- `mute` → set mute true;
- `unmute` → set mute false;
- `hang up` → disconnect the current active call.

Ambiguous transcript or no matching active call results in no action.

Local LLM may help conversational wording but must not directly control Telecom objects.

## Caller announcement

Caller identity can use call details and owner-approved contact lookup where permitted. Announcement should be concise and suppressible.

The caller announcement itself does not answer the call.

## Permission/setup burden

System-role changes require explicit owner selection.

Avoid broad call-log/SMS permissions unless a later concrete feature proves they are necessary and permitted. Contacts remain a separate owner-granted capability.

Setup target:

1. feature remains off by default while engineering;
2. owner opens Mayra Phone setup;
3. Mayra explains that it will become the default Phone app;
4. Android system role selector opens;
5. owner can restore previous Phone app at any time.

## Emergency and failure behavior

- Emergency calling must never depend on Mayra's local LLM, cloud provider or speech recognizer.
- If the Phone role is lost, Mayra immediately stops claiming call-control readiness.
- If voice recognition fails, visible answer/reject/call UI still works.
- If Mayra crashes/restarts, Telecom/System UI must remain recoverable.
- No hidden automatic answering unless the owner later defines a narrow explicit rule and platform behavior is proven.

## Evidence plan

Automated:

- role request/status;
- incoming/ringing/active/disconnected state machine;
- answer/reject/hang-up eligibility;
- mute/unmute;
- endpoint route request and failure callback;
- lost-role handling;
- multiple-call ambiguity;
- emergency-call exclusions;
- no Phone-role components in safe engineering variants where not intended.

Motorola:

- role selection/removal;
- known/unknown caller display;
- ringing UI;
- answer/reject by touch;
- answer/reject by bounded Mayra voice command;
- speaker/earpiece/Bluetooth;
- mute/unmute;
- incoming while screen locked;
- outgoing call;
- second-call/call-waiting behavior;
- reboot;
- restore previous Phone app;
- emergency fallback behavior without experimenting with an actual emergency call.

## Entry decision

APPROVED BEFORE ROLE REQUEST:

- isolated call state models;
- non-role UI prototypes;
- Telecom capability adapters behind interfaces;
- unit tests;
- manifest/test variants that do not take over the Phone role;
- physical testing only after a complete safe candidate exists.

BLOCKED:

- exposing `Make Mayra default Phone` to owner before full call UI/runtime is complete and CI-green;
- autonomous call decisions from free-form LLM output;
- Accessibility hacks for call buttons;
- claiming cellular call recording/audio injection.

## Sources reviewed

- Android `InCallService` API.
- Android `Call` API.
- Android `CallScreeningService` API.
