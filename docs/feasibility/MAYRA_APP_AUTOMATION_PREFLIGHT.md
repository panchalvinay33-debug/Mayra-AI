# Mayra AI — App Workflow Automation Feasibility Preflight

Status: APPROVED FOR OFFICIAL INTENTS/APIS AND NARROW DETERMINISTIC AUTOMATION ONLY
Date: 2026-08-03
Gate: Issue #14
Target: Motorola Edge 70 Fusion / Android 16

## Owner outcome

Mayra should help the owner complete common phone workflows with minimal friction: open apps, navigate to supported system/app actions, prepare calls/messages, create calendar/navigation/media actions and later connect to explicit APIs/integrations where available.

The goal is reliable assistance, not fragile blind screen tapping.

## Priority order

Every workflow must use the narrowest supported mechanism in this order:

1. direct Android framework API/role owned by Mayra;
2. documented app/provider API or connector;
3. standard Android implicit intent/deep link/app link;
4. user-visible handoff to the target app;
5. only if no supported path exists, evaluate a narrow deterministic Accessibility workflow under a separate per-workflow review.

## Intent rule

For Android implicit intents:

- build a typed intent for the requested action;
- verify a receiving activity exists before launching;
- pass only required data;
- handle no-handler/ambiguous-handler cases honestly;
- never report completion merely because the target app opened.

Existing review-first call/message handoffs remain the default pattern for actions where the target app owns the final irreversible step.

## Accessibility boundary

Accessibility is **not** Mayra's generic autonomous action engine.

If a future workflow truly needs Accessibility:

- it must be a static deterministic script tied to a specific owner-approved workflow;
- the owner explicitly enables the service and the workflow;
- no free-form LLM decides arbitrary taps/swipes;
- no authentication, banking, payment, password, OTP or security-setting screens;
- no bypass of Android permission/privacy/security prompts;
- abort immediately if expected UI state is not matched;
- every such workflow gets its own test/rollback record.

## LLM boundary

The local/cloud LLM may interpret natural language into a typed high-level intent, but cannot directly issue arbitrary UI actions.

Execution remains:

`natural language → typed Mayra intent → deterministic capability check → bounded adapter → result`

Sensitive actions still use owner confirmation policy where required.

## App-specific integrations

When stable official APIs/deep links exist, prefer explicit adapters with:

- capability detection;
- exact required data;
- failure/cancellation handling;
- version compatibility tests;
- no hidden scraping of unrelated app content.

Avoid reverse-engineered private APIs unless explicitly reviewed as an experimental owner-only integration with a fallback.

## Failure behavior

- target app not installed → concise unavailable result;
- intent unsupported → do not launch a random fallback;
- target app opens but user does not complete action → Mayra does not claim completion;
- deep link changes → fallback to app open/manual handoff;
- Accessibility script state mismatch → stop, do not keep tapping;
- device locked/authentication required → hand control to owner.

## Evidence plan

Automated:

- intent construction/resolution;
- routing collision tests;
- capability detection;
- no false success claims;
- exact confirmation binding for sensitive actions;
- deterministic Accessibility state machine tests if any are ever approved.

Motorola:

- common app opening;
- browser/maps/media intents;
- dialer/message composer;
- unsupported app/action fallback;
- locked-screen/authentication handoff;
- app version/update behavior.

## Entry decision

APPROVED:

- official APIs/roles;
- standard intents/deep links;
- typed app adapters;
- user-visible handoff flows;
- future narrow deterministic Accessibility only after per-workflow gate.

BLOCKED:

- generic autonomous screen control driven by LLM output;
- hidden authentication/payment flows;
- permission/security prompt bypass;
- claiming target-app completion without evidence.

## Sources reviewed

- Android common intents documentation.
- Google Play Accessibility API policy.
