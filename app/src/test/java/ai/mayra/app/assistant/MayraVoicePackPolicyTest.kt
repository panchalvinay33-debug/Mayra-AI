package ai.mayra.app.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraVoicePackPolicyTest {
    @Test
    fun `piper hindi candidate stays benchmark only until model terms are cleared`() {
        val candidate = requireNotNull(
            MayraVoicePackPolicy.candidate("sherpa-vits-piper-hi-in-priyamvada-medium")
        )

        assertTrue(candidate.licenseGate == MayraVoicePackPolicy.LicenseGate.BENCHMARK_ONLY)
        assertFalse(candidate.isProductionEligible)
        assertTrue(candidate.approximateModelBytes == 63_516_050L)
    }

    @Test
    fun `reference audio model is not automatically production eligible`() {
        val candidate = requireNotNull(MayraVoicePackPolicy.candidate("indic-f5"))

        assertTrue(candidate.licenseGate == MayraVoicePackPolicy.LicenseGate.APPROVED)
        assertTrue(candidate.requiresReferenceAudio)
        assertFalse(candidate.isProductionEligible)
    }

    @Test
    fun `benchmark list excludes blocked packs`() {
        assertTrue(
            MayraVoicePackPolicy.benchmarkCandidates().none {
                it.licenseGate == MayraVoicePackPolicy.LicenseGate.BLOCKED
            }
        )
    }
}
