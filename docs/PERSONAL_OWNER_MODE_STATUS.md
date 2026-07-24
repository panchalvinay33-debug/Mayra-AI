# Mayra Personal Owner Mode — Locked Development Strategy

## Goal

Mayra is first being built as a private sideloaded companion for the owner's own Android phone. The immediate target is maximum useful access that Android legitimately exposes after explicit user setup. Play Store distribution hardening will be a later profile after the personal build proves stable and valuable.

## Owner-first rules

- Current development optimizes the private owner build before Play Store declarations and restricted-permission minimization.
- The owner explicitly enables permissions, Notification Access, default-app roles, battery exemptions and any optional assistive service.
- Mayra never claims to bypass Android sandboxing, secure fields, OEM restrictions, app security or root requirements.
- Global `Mayra stop` and the Action Controls kill switch remain available.
- Critical actions never become silent: financial operations, OTP/password handling, destructive actions, public posting, legal acceptance and protected account changes remain confirmed or user-completed.
- Notification reply and third-party messaging remain limited to official intents, APIs, deep links or exposed Android notification actions.

## Implemented

- Persistent Personal Owner Mode preferences.
- Owner Setup Center reachable from Living Presence.
- Readiness score and capability checklist for microphone, contacts, camera, calls, SMS, Mayra notifications, Notification Access, Accessibility settings, battery/background settings and default-app roles.
- Direct shortcuts to relevant Android settings surfaces.
- Direct low-risk and medium-risk personal action preferences.
- Optional trusted direct call/message handoffs.
- Production action bridge reads Owner Mode preferences on every action, so changes apply without restarting Mayra.
- Trusted handoffs can auto-confirm ordinary high-risk call/message handoffs.
- Sensitive, destructive, financial, public-post, legal and critical metadata always disables Owner bypass.
- Owner Mode can be disabled to restore standard confirmation behavior.
- Deterministic tests for persistence, trusted handoffs, protected-action exclusions and readiness scoring.

## Android hard limits

- Full incoming-call answer/reject/speaker/mute/end control requires a complete default-dialer implementation using Android Telecom/InCallService.
- Accessibility must be manually enabled and cannot legally or technically bypass secure fields or app security.
- Root-only controls require a rooted device and are not part of the normal-phone guarantee.
- Android and OEM battery management may delay background work even after battery settings are adjusted.
- Third-party apps decide which intents, app links, APIs and notification reply actions they expose.
- Process death removes memory-only notification reply handles.

## Later Play Store hardening

After the owner build is stable:

1. Introduce separate personal and Play distribution profiles.
2. Disable trusted direct handoffs by default in the Play profile.
3. Minimize restricted permissions and remove any permission not essential to the declared core feature.
4. Add policy-specific disclosures, data-safety declarations and review documentation.
5. Keep official Android/app integrations as the default; gate optional assistive control behind transparent consent and policy review.
6. Run Play pre-launch reports, OEM compatibility tests and privacy/security review.

## Next owner-build priorities

1. Contact & Identity Engine for Mummy/Papa/Boss/Doctor/nicknames and wrong-contact protection.
2. Real Mayra-owned reminders and calendar storage instead of handoff-only reminders.
3. Default-assistant role exploration and voice invocation entry points.
4. Default-dialer prototype for controlled incoming-call features.
5. Physical-device setup and compatibility dashboard for the owner's phone.
6. Optional deterministic assistive-control prototype only after explicit owner enablement.

## Validation status

Source and tests are committed. GitHub Actions has recently failed before Checkout with an empty step list, so the new Owner Mode batch is not claimed as build-verified until compile, unit tests and lint run successfully.
