package ai.mayra.app.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidMayraPendingMemoryProposalStoreTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_pending_memory_proposals_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun proposalSurvivesStoreRecreation() {
        val proposal = proposal("p1", "भाषा", "हिंदी")
        AndroidMayraPendingMemoryProposalStore(context).put(proposal)
        assertEquals(proposal, AndroidMayraPendingMemoryProposalStore(context).all().single())
    }

    @Test fun removeAndClearPersist() {
        val store = AndroidMayraPendingMemoryProposalStore(context)
        store.put(proposal("p1", "city", "Indore"))
        assertEquals("p1", store.remove("p1")?.id)
        assertTrue(AndroidMayraPendingMemoryProposalStore(context).all().isEmpty())
        store.put(proposal("p2", "food", "poha"))
        store.clear()
        assertTrue(AndroidMayraPendingMemoryProposalStore(context).all().isEmpty())
    }

    @Test fun corruptEntriesAreSkipped() {
        val prefs = context.getSharedPreferences("mayra_pending_memory_proposals_v1", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("records", setOf("broken", MayraPendingMemoryProposalCodec.encode(proposal("p1", "city", "Indore")))).commit()
        assertEquals(1, AndroidMayraPendingMemoryProposalStore(context).all().size)
    }

    private fun proposal(id: String, key: String, value: String): MayraPendingMemoryProposal {
        val now = Instant.parse("2026-07-28T12:00:00Z")
        return MayraPendingMemoryProposal(
            id = id,
            candidate = MayraMemoryCandidate(
                key,
                value,
                MayraMemoryCategory.OTHER,
                MayraMemoryProvenance("chat", "owner-command", now)
            ),
            createdAt = now
        )
    }
}
