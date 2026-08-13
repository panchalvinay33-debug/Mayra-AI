package ai.mayra.app.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidMayraMemoryStorageHealthReaderTest {
    private lateinit var context: Context
    private val protector = PrefixProtector()

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_pending_memory_proposals_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun emptyStorageIsReported() {
        assertEquals(MayraMemoryStorageState.EMPTY, reader().read().state)
    }

    @Test fun legacyRecordsRequestMigration() {
        approvedPrefs().edit().putStringSet("records", setOf(MayraMemoryCodec.encode(memory()))).commit()
        val health = reader().read()
        assertEquals(MayraMemoryStorageState.MIGRATION_NEEDED, health.state)
        assertEquals(1, health.approvedLegacy)
    }

    @Test fun unreadableProtectedRecordIsDegraded() {
        approvedPrefs().edit().putStringSet("records", setOf("p:broken")).commit()
        val health = reader().read()
        assertEquals(MayraMemoryStorageState.DEGRADED, health.state)
        assertEquals(1, health.approvedUnreadable)
    }

    @Test fun validProtectedApprovedAndPendingRecordsAreHealthy() {
        approvedPrefs().edit().putStringSet("records", setOf(protector.protect(MayraMemoryCodec.encode(memory())))).commit()
        pendingPrefs().edit().putStringSet("records", setOf(protector.protect(MayraPendingMemoryProposalCodec.encode(proposal())))).commit()
        val health = reader().read()
        assertEquals(MayraMemoryStorageState.HEALTHY, health.state)
        assertEquals(1, health.approvedProtected)
        assertEquals(1, health.pendingProtected)
    }

    private fun reader() = AndroidMayraMemoryStorageHealthReader(context, protector, protector)
    private fun approvedPrefs() = context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE)
    private fun pendingPrefs() = context.getSharedPreferences("mayra_pending_memory_proposals_v1", Context.MODE_PRIVATE)

    private fun memory(): MayraPersonalMemory {
        val now = Instant.parse("2026-07-28T10:00:00Z")
        return MayraPersonalMemory(
            "id", "city", "Indore", MayraMemoryCategory.PROFILE,
            MayraMemoryProvenance("chat", "owner-command", now), now, now, null, 1
        )
    }

    private fun proposal(): MayraPendingMemoryProposal {
        val now = Instant.parse("2026-07-28T10:00:00Z")
        return MayraPendingMemoryProposal(
            "proposal",
            MayraMemoryCandidate("food", "poha", MayraMemoryCategory.PREFERENCE, MayraMemoryProvenance("chat", "owner-command", now)),
            now
        )
    }

    private class PrefixProtector : MayraMemoryRecordProtector {
        override fun protect(plaintext: String): String = "p:$plaintext"
        override fun unprotect(payload: String): String? = payload.takeIf { it.startsWith("p:") && it != "p:broken" }?.removePrefix("p:")
        override fun isProtected(payload: String): Boolean = payload.startsWith("p:")
    }
}
