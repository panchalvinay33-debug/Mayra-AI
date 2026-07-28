package ai.mayra.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraCapabilityRegistryTest {
    @Test
    fun registryHasUniqueCompleteCapabilities() {
        MayraCapabilityRegistry.requireValid()
        val capabilities = MayraCapabilityRegistry.capabilities
        assertEquals(capabilities.size, capabilities.map { it.id }.distinct().size)
        assertTrue(capabilities.all { it.note.isNotBlank() })
    }

    @Test
    fun snapshotCountsEveryCapabilityExactlyOnce() {
        val snapshot = MayraCapabilityRegistry.snapshot()
        assertEquals(
            snapshot.total,
            snapshot.done + snapshot.deviceVerify + snapshot.inProgress + snapshot.planned + snapshot.deferred
        )
        assertEquals(snapshot.total, snapshot.byModule.values.sumOf(List<MayraCapability>::size))
    }

    @Test
    fun governanceAndDocumentFoundationRemainTracked() {
        val byId = MayraCapabilityRegistry.capabilities.associateBy { it.id }
        assertEquals(MayraCapabilityStatus.DONE, byId.getValue("governance.blueprint-backup").status)
        assertEquals(MayraCapabilityStatus.DONE, byId.getValue("documents.foundation").status)
        assertEquals(MayraCapabilityStatus.DEVICE_VERIFY, byId.getValue("documents.device-validation").status)
    }

    @Test
    fun routingAndEligibilityAreDoneAndRuntimeIntegrationIsNext() {
        val byId = MayraCapabilityRegistry.capabilities.associateBy { it.id }
        assertEquals(MayraCapabilityStatus.DONE, byId.getValue("core.query-routing").status)
        assertEquals(MayraCapabilityStatus.DONE, byId.getValue("core.provider-eligibility").status)
        assertEquals(MayraCapabilityStatus.IN_PROGRESS, byId.getValue("core.runtime-integration").status)
    }

    @Test
    fun deferredDocumentFeaturesDoNotBlockBroaderRoadmap() {
        val deferred = MayraCapabilityRegistry.capabilities
            .filter { it.status == MayraCapabilityStatus.DEFERRED }
            .map { it.id }
            .toSet()
        assertEquals(setOf("documents.ocr", "documents.legacy-doc"), deferred)
    }
}
