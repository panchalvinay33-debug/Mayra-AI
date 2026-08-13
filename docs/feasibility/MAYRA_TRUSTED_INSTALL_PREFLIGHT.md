# Mayra AI — Trusted Installation and Update Feasibility Preflight

Status: ARCHITECTURE APPROVED — PRIVATE SIGNING + PLAY INTERNAL TESTING/OWNER CHANNEL MUST BE COMPLETED BEFORE FULL-APP DEVICE PROMOTION
Date: 2026-08-03
Gate: Issue #14
Target owner device: Motorola Edge 70 Fusion / Android 16

## Owner outcome

The owner should install Mayra once and receive future test/final updates without repeated uninstall, signature conflict, confusing APK hunting or Play Protect bypass.

Existing local Mayra data should survive normal compatible upgrades.

## Current failure evidence

Earlier Personal Alpha/J1 update attempts exposed two real distribution problems:

1. temporary GitHub-hosted runner debug certificates changed across builds, causing Android package-signature conflicts;
2. a broad sideloaded full Personal Alpha was blocked by Google Play Protect.

Both are treated as architecture requirements, not one-off annoyances.

## Signing architecture

- One long-lived private owner signing certificate is required for repeat owner test APKs.
- Production release signing remains separate where appropriate, but package/certificate strategy must be deliberate before public/internal distribution.
- Keystore/password/private key never enters Git history, documentation or ordinary build logs.
- GitHub/environment secrets provide signing inputs at build time.
- Every promoted artifact records package, version, source SHA, certificate fingerprint where available and APK/AAB SHA-256.

Hosted-runner debug signing is disposable only and must never be called update-compatible.

## Distribution direction

Preferred full-app owner testing path: **Google Play Internal Testing** once the Play Console app/signing setup is ready.

Reasons:

- Play Store installation/update flow;
- controlled tester access;
- reduced sideload friction;
- internal test can distribute quickly to a small trusted group;
- up to 100 internal testers are supported, while the owner can remain the only actual tester.

A direct owner-signed APK may remain available for controlled recovery/testing, but it must use the same intended owner certificate for upgrade compatibility.

## Package strategy

Engineering packages remain distinct:

- `ai.mayra.app.j1` — zero-permission Assistant proof;
- `ai.mayra.app.j2` — microphone-only voice proof;
- `ai.mayra.app.alpha` — full owner engineering candidate;
- `ai.mayra.app` — final application identity.

J1/J2 are disposable proof packages and are not personal-data containers.

The final owner experience remains one Mayra app, not multiple launcher products.

## Update/data-retention contract

Before full-app promotion prove A→B upgrade using the same certificate:

1. install signed build A;
2. create representative local state: chat/settings, approved memory, imported document metadata and reminder where safe;
3. install signed build B over A without uninstall;
4. verify app launches;
5. verify expected data remains/migrates correctly;
6. verify Assistant/system-role status behavior;
7. verify no duplicate launcher/package;
8. verify rollback expectations are documented.

Any schema migration must include failure/recovery tests before owner data is put at risk.

## Play Protect rule

Mayra development never instructs the owner to disable or bypass Play Protect.

If a sideloaded test package is blocked:

- isolate the capability into a minimal package when useful for diagnosis;
- improve signing/distribution;
- use Play Internal Testing/trusted delivery for the full app;
- do not weaken Android security settings as the solution.

## Play Internal Testing setup gate

Before calling the trusted channel ready:

- Play Console app created/configured;
- correct package/application identity selected;
- app signing ownership/recovery plan documented;
- tester account added;
- first AAB uploaded to Internal Testing;
- owner opts in through the official test link;
- clean install works from Play Store;
- update B over A works;
- versionCode/versionName progression verified;
- uninstall/reinstall data expectations documented;
- private signing/recovery material stored outside repository.

## CI/provenance requirements

For every owner candidate:

- compile/tests/lint/package audit green;
- no unexpected dangerous/broad permissions;
- one launcher;
- package/version recorded;
- source commit recorded;
- artifact digest recorded;
- stable certificate path confirmed for update candidate;
- release/Personal Alpha must not silently fall back to ephemeral signing while being presented as stable.

## Failure behavior

- signing secrets missing → stable-owner workflow must fail clearly or mark artifact ephemeral; never silently claim stable signing;
- certificate mismatch → stop update test; do not uninstall a real data-bearing full Mayra app without explicit backup expectation;
- Play release unavailable → remain on last verified installed build;
- migration failure → rollback/recovery procedure, no automatic destructive reset.

## Entry decision

APPROVED NOW:

- stable owner-signing workflow/scaffold;
- certificate fingerprint/provenance audit;
- A→B update test design;
- Play Console/Internal Testing setup;
- AAB build/upload preparation.

BLOCKED until proof:

- calling full Mayra installation/update solved;
- storing important owner data in a repeatedly debug-signed test package;
- disabling Play Protect;
- production rollout.

## Sources reviewed

- Google Play Internal Testing documentation.
- Google Play release/testing documentation.
- Android package/signing update compatibility behavior as observed and enforced by the platform.
