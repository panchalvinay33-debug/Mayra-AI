package ai.mayra.app.document

enum class DocumentFeatureStage {
    COMPLETE,
    IMPLEMENTED_NEEDS_DEVICE_TEST,
    PLANNED
}

data class DocumentFeatureReadiness(
    val id: String,
    val label: String,
    val stage: DocumentFeatureStage,
    val note: String
)

data class DocumentModuleReadiness(
    val features: List<DocumentFeatureReadiness>
) {
    val total: Int get() = features.size
    val complete: Int get() = features.count { it.stage == DocumentFeatureStage.COMPLETE }
    val needsDeviceTest: Int
        get() = features.count { it.stage == DocumentFeatureStage.IMPLEMENTED_NEEDS_DEVICE_TEST }
    val planned: Int get() = features.count { it.stage == DocumentFeatureStage.PLANNED }
    val implemented: Int get() = complete + needsDeviceTest
    val implementationPercent: Int get() = if (total == 0) 0 else implemented * 100 / total
    val signedOffPercent: Int get() = if (total == 0) 0 else complete * 100 / total

    fun summary(): String =
        "$implemented/$total features implemented ($implementationPercent%); " +
            "$complete signed off, $needsDeviceTest need phone validation, $planned planned."
}

/** Single source of truth for the remaining document-foundation roadmap. */
object MayraDocumentModuleRoadmap {
    val readiness = DocumentModuleReadiness(
        listOf(
            feature("library", "Persisted private document library", DocumentFeatureStage.COMPLETE,
                "Persistable URI metadata and local-only storage are implemented."),
            feature("plain-text", "Plain-text extraction", DocumentFeatureStage.COMPLETE,
                "TXT, Markdown, CSV, JSON, XML and logs are indexed locally."),
            feature("search", "Unicode-aware document search", DocumentFeatureStage.COMPLETE,
                "English, Hinglish and Hindi terms, phrases and snippets are covered."),
            feature("summaries", "Local summaries", DocumentFeatureStage.COMPLETE,
                "Summaries use current on-device indexes only."),
            feature("qa", "Grounded document Q&A", DocumentFeatureStage.COMPLETE,
                "Answers are blocked when evidence is missing, legacy or stale."),
            feature("async", "Background-safe indexing", DocumentFeatureStage.COMPLETE,
                "Parsing and index writes run away from the UI thread."),
            feature("limits", "Parser safety limits", DocumentFeatureStage.COMPLETE,
                "PDF and DOCX size, page, XML and indexed-character limits are enforced."),
            feature("freshness-core", "Index freshness engine", DocumentFeatureStage.COMPLETE,
                "Parser version, source size and modified-time changes are detected."),
            feature("evidence", "Current-only evidence policy", DocumentFeatureStage.COMPLETE,
                "Stale or partial indexes cannot reach search, summaries or Q&A."),
            feature("health", "Library Health diagnostics", DocumentFeatureStage.COMPLETE,
                "Inventory, parser readiness and repair reports are available."),
            feature("transactions", "Transactional local index commits", DocumentFeatureStage.COMPLETE,
                "Content and fingerprint writes are verified and rolled back on failure."),
            feature("ci", "Isolated APK and CI audit", DocumentFeatureStage.COMPLETE,
                "Compile, tests, lint, R8 and zero-permission binary checks run in CI."),
            feature("pdf", "Text-based PDF indexing", DocumentFeatureStage.IMPLEMENTED_NEEDS_DEVICE_TEST,
                "Parser and generated-PDF tests pass; real phone re-index/search still needs confirmation."),
            feature("docx", "DOCX indexing", DocumentFeatureStage.IMPLEMENTED_NEEDS_DEVICE_TEST,
                "Body, tables, headers, footers and notes are implemented; phone search needs confirmation."),
            feature("freshness-ui", "Freshness badges and Smart refresh UX", DocumentFeatureStage.IMPLEMENTED_NEEDS_DEVICE_TEST,
                "Code and automated tests pass; real provider metadata behavior needs phone validation."),
            feature("maintenance-device", "Transactional maintenance UX", DocumentFeatureStage.IMPLEMENTED_NEEDS_DEVICE_TEST,
                "Smart and forced rebuild flows need an end-to-end phone run."),
            feature("ocr", "On-device OCR for scanned PDFs and images", DocumentFeatureStage.PLANNED,
                "Requires a dedicated recognition provider, performance limits and device validation."),
            feature("legacy-doc", "Legacy binary DOC parsing", DocumentFeatureStage.PLANNED,
                "Users currently need to export old DOC files as DOCX.")
        )
    )

    private fun feature(
        id: String,
        label: String,
        stage: DocumentFeatureStage,
        note: String
    ) = DocumentFeatureReadiness(id, label, stage, note)
}
