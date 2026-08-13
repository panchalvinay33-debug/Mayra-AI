package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraDocumentModuleReadinessTest {
    @Test
    fun roadmapHasStableImplementedAndRemainingCounts() {
        val readiness = MayraDocumentModuleRoadmap.readiness

        assertEquals(18, readiness.total)
        assertEquals(12, readiness.complete)
        assertEquals(4, readiness.needsDeviceTest)
        assertEquals(2, readiness.planned)
        assertEquals(16, readiness.implemented)
        assertEquals(88, readiness.implementationPercent)
        assertEquals(66, readiness.signedOffPercent)
    }

    @Test
    fun onlyOcrAndLegacyDocRemainPlanned() {
        val planned = MayraDocumentModuleRoadmap.readiness.features
            .filter { it.stage == DocumentFeatureStage.PLANNED }
            .map { it.id }
            .toSet()

        assertEquals(setOf("ocr", "legacy-doc"), planned)
    }

    @Test
    fun everyFeatureHasUniqueIdAndUsefulCopy() {
        val features = MayraDocumentModuleRoadmap.readiness.features

        assertEquals(features.size, features.map { it.id }.distinct().size)
        assertTrue(features.all { it.label.isNotBlank() && it.note.isNotBlank() })
        assertTrue(MayraDocumentModuleRoadmap.readiness.summary().contains("16/18"))
    }
}
