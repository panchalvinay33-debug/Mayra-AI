# Mayra Personal Workspace + Background Assistant Blueprint

**Status:** Locked companion blueprint to `MAYRA_AI_MASTER_BLUEPRINT.md`  
**Delivery:** Personal owner APK first  
**Implementation status:** Vertical-slice development; no fake completion claims

This document is now part of the Mayra source of truth and must be read with:

- `MAYRA_AI_MASTER_BLUEPRINT.md`
- `MAYRA_LIVING_INTELLIGENCE_VISION.md`
- `MAYRA_SOURCE_OF_TRUTH_AND_BACKUP_MAP.md`
- `PERSONAL_WORKSPACE_AND_BACKGROUND_ASSISTANT_STATUS.md`

## Locked direction

Mayra's primary surface is the unified **Mayra + Chat** screen. A dedicated **Personal Workspace** opens for visible structured work and remains connected to the same Mayra identity, memory, voice, safety and action engine.

The Workspace must support live transcript, task state, sources, notes, tables, document/PDF preview, email draft, call controls, confirmation cards, task history, pause/continue, undo/redo, save/export/send/cancel, microphone and keyboard.

## Background Assistant contract

Permitted entry modes are app microphone, visible notification Talk action, supported headset/Bluetooth entry, future wake-word prototype and future default-assistant exploration. Background activity must be visible, stoppable and privacy-gated.

Runtime states are:

`LISTENING -> UNDERSTANDING -> SEARCHING -> PROCESSING -> WAITING_FOR_CONFIRMATION -> SPEAKING -> COMPLETED`, with `PAUSED`, `CANCELLED`, `FAILED` and retry paths.

Lock-screen modes are Strict, Balanced, Trusted/private environment and Always authenticate. Sensitive files, payments, OTP, private messages, destructive actions and protected app data require unlock or explicit confirmation.

## File Intelligence contract

Mayra may index only Android-authorized MediaStore content and SAF-granted documents/folders. It must preserve original URI/path reference, modified time, MIME type, extraction source/page and confidence. It may never bypass scoped storage, private encrypted app databases, banking boundaries, secure folders, PINs, passwords or OTP protections.

Required searchable bill fields include file name, URI/path, type, created/modified date, vendor, bill date, invoice number, item, quantity, rate, tax, total, payment status, source page and OCR confidence.

## Workspace action contract

All commands become typed actions:

- `SEARCH_FILE`
- `ANALYSE_DOCUMENT`
- `CREATE_TABLE`
- `UPDATE_TABLE`
- `EXPORT_DOCUMENT`
- `SEND_EMAIL`
- `PREPARE_WHATSAPP`
- `PLACE_CALL`
- `CONTROL_CALL`
- `CREATE_REMINDER`
- `CREATE_NOTE`
- `OPEN_APP`
- `READ_NOTIFICATION`
- `SEARCH_CONTACT`

Each action records request, entities, permission requirements, risk, confirmation requirement, progress, sources, result verification and owner-visible report.

## Export contract

Workspace source data and rendered artifacts remain separate. Supported targets are staged in this order: CSV, PDF, XLSX, DOCX, TXT and image report. Templates include professional report, invoice, quotation, payment statement, business letter, Shree Shyam Event Management letterhead and custom saved templates.

## Email and external bridge contract

Email requires recipient resolution, preview, subject, body, attachments, confirmation, send adapter and verified result/failure report. WhatsApp and external apps use official intents/deep links/share first; optional Accessibility remains narrow, deterministic, user-commanded, visible and blocked for banking/OTP/sensitive flows.

## Calling contract

Review-first Android dialer remains the current fallback. Default-dialer role, Telecom and InCallService are separate later vertical slices. Emergency/system restrictions are never bypassed.

## Backup integration

The existing `.mayrabackup` encrypted envelope remains authoritative. A future backup schema version must add:

- Workspace sessions and revision history;
- user-created tables and formulas;
- file-grant metadata (not external file bytes by default);
- indexed metadata and source references;
- parsed bill records and confidence;
- generated artifact metadata;
- email drafts and confirmation state;
- background privacy modes;
- bounded workspace audit history.

Sensitive-folder exclusions, selective restore, schema migration, duplicate protection and rollback remain mandatory. External source files are referenced, not silently copied. Before Room schema migration, export the schema and create a dated immutable-style backup branch recorded in the backup map.

## Milestones

A. Workspace screen, voice/text input, typed intents, live task state, encrypted autosave.  
B. File grants, inventory, incremental index, PDF/OCR, bill parsing, source-attributed voice result.  
C. Voice table editing, corrections, totals, formulas, undo/redo, CSV/PDF export.  
D. Email draft, attachment, preview, confirmation and send.  
E. Default-dialer prototype and supported call controls.  
F. WhatsApp bridge and optional narrow private accessibility adapter.  
G. Background/lock-screen privacy modes and wake-word research.

## First end-to-end acceptance

`XYZ bill -> background authorized search -> PDF/image analysis -> source-verified date/rate/total -> spoken answer -> Workspace table -> spoken corrections -> professional PDF -> recipient/subject/body/attachment preview -> confirmed email -> verified voice report`.

Nothing in this flow is considered complete until the relevant source compiles, tests pass, the verified APK is installed and the owner-phone physical test succeeds.