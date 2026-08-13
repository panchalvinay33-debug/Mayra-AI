# Mayra J6 Calendar Context — Privacy-first foundation

## Goal

Add a deterministic calendar-context contract that can later support useful prompts such as whether the owner is busy, how many calendar blocks remain today, and roughly when the next event begins, without making Mayra Home depend on Calendar Provider access.

## First-slice privacy boundary

The trusted Home-facing context MUST NOT contain or display:

- event title or description
- location or meeting URL
- attendee names, emails, organizer, account or calendar name
- notes, conference metadata or free-form calendar text

The first slice carries only coarse metadata:

- number of remaining events today
- whether an event is active now
- minutes until the next event, when one exists
- captured-at timestamp and provenance

## Permission model

This foundation adds no runtime permission request and performs no Calendar Provider query. Calendar access remains an explicit later opt-in gate. Missing access is represented as `ContextValue.NotGranted`; provider/query failure is represented as `ContextValue.Unavailable`.

## Home critical path

Mayra Home must remain fully usable with calendar access denied, unavailable or broken. Calendar context is supplemental and must never initialize AI/model/provider infrastructure.

## Future runtime slice

A later, separately reviewed slice may add read-only Android Calendar Provider access for eligible variants. Raw calendar rows must be reduced immediately to the coarse contract before crossing the Context Fabric boundary. The isolated DocumentTest variant must remain permission-safe.

## Acceptance gates

1. Contract contains no free-form/private event content fields.
2. Deterministic summaries for NotGranted, Unavailable, empty day, busy-now and upcoming-event states.
3. Existing J1–J6 tests remain green.
4. No calendar permission or provider query is added by this contract-only slice.
5. PR #12 remains Draft/open/unmerged.
