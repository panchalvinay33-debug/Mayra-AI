# Mayra AI — AI Caller Message-Taking Feasibility Preflight

Status: DIRECT CELLULAR AUDIO PATH REJECTED — SUPPORTED VOICEMAIL/VOIP/CALL-FORWARDING ARCHITECTURE REQUIRED
Date: 2026-08-03
Gate: Issue #14
Target owner experience: Mayra can tell a caller the owner is unavailable, capture the caller's message with consent/disclosure, then summarize it to the owner.

## Owner outcome

Desired experience:

1. caller reaches a Mayra-managed message-taking endpoint when the owner does not answer or explicitly chooses message mode;
2. Mayra gives a short disclosure/greeting;
3. caller speaks a message;
4. message audio/transcript is processed through the selected private architecture;
5. owner later sees/hears a concise message summary and source details.

## Hard Android cellular-call boundary

A normal third-party Android app cannot assume access to both sides of arbitrary SIM/PSTN call audio.

Android audio-input rules give the cellular call priority. Capturing the call uplink/downlink is limited to privileged/pre-installed apps with protected capabilities such as `CAPTURE_AUDIO_OUTPUT`. Accessibility is not an acceptable workaround for remote call audio recording.

Therefore Mayra must not claim it can simply answer a normal SIM call, inject arbitrary TTS into the remote caller's audio path and transcribe the remote caller through ordinary public app APIs.

## Rejected designs

- Accessibility automation to press call UI and record remote audio.
- Hidden call recording.
- Speakerphone + microphone acoustic loop presented as a reliable production feature.
- Root/privilege escalation as the normal owner architecture.
- Claiming `InCallService` alone grants raw remote audio capture/injection.

`InCallService` is still useful for answer/reject/mute/speaker control, but it does not by itself solve AI two-way cellular audio.

## Supported architecture options

### Option A — Carrier call forwarding to Mayra voicemail/AI endpoint

The owner's carrier forwards unanswered/busy/selected calls to a Mayra-controlled telephony endpoint.

Endpoint responsibilities:

- answer remote call;
- play disclosure/greeting;
- capture caller message lawfully for the configured jurisdiction/use;
- transcribe/summarize;
- deliver result securely to Mayra.

Pros:

- works when phone is offline/out of coverage depending on forwarding mode;
- caller audio exists at the telephony endpoint where it can be processed;
- clean separation from Android protected call audio.

Cons:

- requires carrier forwarding support;
- likely requires a telephony service/backend and recurring/usage cost;
- privacy/security/data-retention design required.

### Option B — Mayra-owned VoIP number/account

Calls placed to a Mayra/owner VoIP number are handled by a Mayra-controlled VoIP service integrated with Android Telecom where appropriate.

Pros:

- Mayra controls both application audio and message flow;
- easier to integrate AI greeting/transcription than arbitrary SIM calls.

Cons:

- caller must use/receive the VoIP number or forwarding must route to it;
- backend/provider/network dependency;
- emergency/PSTN behavior must remain separate.

### Option C — Traditional carrier voicemail integration

Use the carrier's voicemail as the recording source and import/notify/transcribe only where a supported carrier/API/export path exists.

Pros: lowest custom telephony burden.

Cons: carrier-specific; automation/access may be limited.

## Recommended direction

For the user's desired `Mayra talks to caller and takes a message` experience, evaluate **call forwarding → Mayra-controlled VoIP/telephony endpoint** first.

This is an optional advanced service layer, separate from the core on-device Mayra app. The phone app should continue working fully without it.

## Privacy and disclosure

Before any real deployment define:

- caller disclosure wording;
- recording/transcription consent requirements for the owner's location and likely callers;
- encryption in transit/at rest;
- retention/deletion period;
- owner-only authentication;
- whether raw audio is retained or immediately deleted after transcript generation;
- abuse/spam/rate limits.

No caller recording should be hidden behind vague behavior.

## Local/cloud split

Possible design:

- telephony endpoint captures caller audio;
- speech-to-text may run at the endpoint or a private service;
- message summary can be produced locally on the phone after encrypted transcript delivery when practical;
- full raw audio need not be sent to a general cloud LLM;
- owner chooses whether audio is retained.

A completely on-device solution for arbitrary SIM-call remote audio is not assumed.

## Integration with Phone-role Mayra

On-device Phone role can provide owner commands such as:

- reject;
- answer;
- silence;
- speaker;
- hang up.

A future explicit action like `send to Mayra messages` would require a proven carrier/telephony forwarding mechanism; it cannot be implemented by pretending the local app can hand the live remote cellular audio to the AI.

## Evidence required before implementation

- choose telephony/carrier architecture;
- document cost and account ownership;
- verify forwarding/VoIP behavior with a non-critical test number;
- define privacy/retention/disclosure;
- threat-model caller spoofing and owner authentication;
- define offline/failure behavior;
- prove message delivery reliability;
- prove delete/export controls;
- keep core Mayra functional when backend is unavailable.

## Entry decision

APPROVED NOW:

- architecture/provider research;
- mock message-taking UI and data model;
- local encrypted message inbox design;
- call-forwarding/VoIP proof with test numbers only after explicit setup.

BLOCKED:

- claiming ordinary SIM-call AI conversation is implemented;
- direct remote cellular audio recording through normal Android APIs;
- Accessibility-based recording/control hacks;
- production telephony backend without privacy/retention/security design.

## Sources reviewed

- Android audio-input sharing/call capture rules.
- Android InCallService/Telecom APIs.
- Google Play Accessibility restrictions on remote call audio recording.
