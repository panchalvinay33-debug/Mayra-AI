package ai.mayra.app.assistant

/**
 * Production gate for downloadable/local neural voice packs.
 *
 * A model being technically runnable or free to download is not enough. Mayra only enables a
 * neural pack automatically when its redistribution/use terms are explicitly approved and the
 * pack has passed the target-device benchmark. Benchmark-only packs can still be evaluated in a
 * dedicated engineering build without becoming a production dependency.
 */
object MayraVoicePackPolicy {
    enum class LicenseGate {
        APPROVED,
        BENCHMARK_ONLY,
        BLOCKED
    }

    data class Candidate(
        val id: String,
        val displayName: String,
        val languageTag: String,
        val engine: String,
        val approximateModelBytes: Long?,
        val licenseGate: LicenseGate,
        val requiresReferenceAudio: Boolean = false,
        val notes: String
    ) {
        val isProductionEligible: Boolean
            get() = licenseGate == LicenseGate.APPROVED && !requiresReferenceAudio
    }

    /**
     * Initial shortlist. These entries are metadata only; no model binary is bundled here.
     * Exact legal notices/model hashes must be pinned before any production download is enabled.
     */
    val candidates: List<Candidate> = listOf(
        Candidate(
            id = "sherpa-vits-piper-hi-in-priyamvada-medium",
            displayName = "Hindi Priyamvada Medium",
            languageTag = "hi-IN",
            engine = "sherpa-onnx VITS/Piper",
            approximateModelBytes = 63_516_050L,
            licenseGate = LicenseGate.BENCHMARK_ONLY,
            notes = "Excellent Android-sized baseline; model repository is permissive but its model card cites a non-commercial dataset, so do not ship until redistribution/use is cleared."
        ),
        Candidate(
            id = "indic-parler-tts",
            displayName = "Indic Parler TTS",
            languageTag = "hi-IN",
            engine = "Parler-TTS",
            approximateModelBytes = 3_750_000_000L,
            licenseGate = LicenseGate.APPROVED,
            notes = "Apache-2.0 model with strong Hindi/style control, but current checkpoint size is far above the preferred phone budget; benchmark only after a practical mobile quantization/runtime exists."
        ),
        Candidate(
            id = "indic-f5",
            displayName = "IndicF5",
            languageTag = "hi-IN",
            engine = "F5-TTS",
            approximateModelBytes = null,
            licenseGate = LicenseGate.APPROVED,
            requiresReferenceAudio = true,
            notes = "MIT high-quality Indic TTS candidate; reference-audio workflow and mobile runtime cost make it a later research path, not the first Mayra phone voice."
        )
    )

    fun candidate(id: String): Candidate? = candidates.firstOrNull { it.id == id }

    fun productionCandidates(): List<Candidate> = candidates.filter(Candidate::isProductionEligible)

    fun benchmarkCandidates(): List<Candidate> = candidates.filter {
        it.licenseGate != LicenseGate.BLOCKED
    }
}
