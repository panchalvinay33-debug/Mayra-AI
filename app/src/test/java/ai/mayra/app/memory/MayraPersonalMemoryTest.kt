package ai.mayra.app.memory

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraPersonalMemoryTest {
    private val now = Instant.parse("2026-07-28T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val source = MayraMemoryProvenance("chat", "message-1", now)

    @Test fun allowedMemoryRequiresExplicitApproval() {
        val store = MayraInMemoryPersonalMemoryStore()
        val manager = MayraPersonalMemoryManager(store, clock)
        val result = manager.propose(MayraMemoryCandidate("tea", "likes masala chai", MayraMemoryCategory.PREFERENCE, source))
        assertTrue(result is MayraMemoryProposalResult.ApprovalRequired)
        assertTrue(store.all().isEmpty())
        val saved = manager.approve((result as MayraMemoryProposalResult.ApprovalRequired).proposalId)
        assertTrue(saved is MayraMemoryApprovalResult.Saved)
        assertEquals(1, store.all().size)
    }

    @Test fun rejectedProposalNeverSaves() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val result = manager.propose(MayraMemoryCandidate("language", "prefers Hindi", MayraMemoryCategory.PREFERENCE, source))
            as MayraMemoryProposalResult.ApprovalRequired
        assertTrue(manager.reject(result.proposalId))
        assertTrue(manager.approve(result.proposalId) is MayraMemoryApprovalResult.Rejected)
        assertEquals(0, manager.activeMemories().size)
    }

    @Test fun secretsAreProhibited() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val result = manager.propose(MayraMemoryCandidate("bank pin", "1234", MayraMemoryCategory.OTHER, source))
        assertTrue(result is MayraMemoryProposalResult.Rejected)
        assertEquals(MayraMemorySensitivity.PROHIBITED, (result as MayraMemoryProposalResult.Rejected).sensitivity)
    }

    @Test fun sensitiveHealthMemoryIsRejected() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val result = manager.propose(MayraMemoryCandidate("medical", "diabetes diagnosis", MayraMemoryCategory.PROFILE, source))
        assertTrue(result is MayraMemoryProposalResult.Rejected)
        assertEquals(MayraMemorySensitivity.SENSITIVE, (result as MayraMemoryProposalResult.Rejected).sensitivity)
    }

    @Test fun approvalTokenIsOneTime() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val proposal = manager.propose(MayraMemoryCandidate("city", "Indore", MayraMemoryCategory.PROFILE, source))
            as MayraMemoryProposalResult.ApprovalRequired
        assertTrue(manager.approve(proposal.proposalId) is MayraMemoryApprovalResult.Saved)
        assertTrue(manager.approve(proposal.proposalId) is MayraMemoryApprovalResult.Rejected)
    }

    @Test fun sameKeyUpdatesExistingMemoryAndRevision() {
        val store = MayraInMemoryPersonalMemoryStore()
        val manager = MayraPersonalMemoryManager(store, clock)
        fun save(value: String) {
            val proposal = manager.propose(MayraMemoryCandidate("drink", value, MayraMemoryCategory.PREFERENCE, source))
                as MayraMemoryProposalResult.ApprovalRequired
            manager.approve(proposal.proposalId)
        }
        save("tea")
        save("coffee")
        assertEquals(1, store.all().size)
        assertEquals("coffee", store.all().single().value)
        assertEquals(2, store.all().single().revision)
    }

    @Test fun expiredMemoryIsPrunedAndNotRetrieved() {
        val store = MayraInMemoryPersonalMemoryStore()
        store.put(MayraPersonalMemory("id", "trip", "Jaipur", MayraMemoryCategory.PROJECT, source, now.minusSeconds(20), now.minusSeconds(10), now, 1))
        val manager = MayraPersonalMemoryManager(store, clock)
        assertTrue(manager.activeMemories().isEmpty())
        assertTrue(store.all().isEmpty())
    }

    @Test fun expiredProposalCannotBeApproved() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val proposal = manager.propose(MayraMemoryCandidate("trip", "Jaipur", MayraMemoryCategory.PROJECT, source, now.minusSeconds(1)))
            as MayraMemoryProposalResult.ApprovalRequired
        assertTrue(manager.approve(proposal.proposalId) is MayraMemoryApprovalResult.Rejected)
    }

    @Test fun retrievalIsDeterministicAndRelevant() {
        val store = MayraInMemoryPersonalMemoryStore()
        val manager = MayraPersonalMemoryManager(store, clock)
        listOf(
            MayraMemoryCandidate("food", "likes spicy Indian food", MayraMemoryCategory.PREFERENCE, source),
            MayraMemoryCandidate("travel", "planning Jaipur trip", MayraMemoryCategory.PROJECT, source),
            MayraMemoryCandidate("language", "prefers Hindi replies", MayraMemoryCategory.PREFERENCE, source)
        ).forEach { candidate ->
            val p = manager.propose(candidate) as MayraMemoryProposalResult.ApprovalRequired
            manager.approve(p.proposalId)
        }
        val result = manager.retrieve("Indian food preference")
        assertEquals(1, result.size)
        assertEquals("food", result.single().key)
    }

    @Test fun updateRejectsSensitiveReplacement() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val p = manager.propose(MayraMemoryCandidate("note", "likes cricket", MayraMemoryCategory.PREFERENCE, source))
            as MayraMemoryProposalResult.ApprovalRequired
        val memory = (manager.approve(p.proposalId) as MayraMemoryApprovalResult.Saved).memory
        assertNull(manager.update(memory.id, "medical diagnosis", source.copy(sourceReference = "message-2")))
        assertEquals("likes cricket", manager.activeMemories().single().value)
    }

    @Test fun deleteAndClearAreUserControlled() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val p = manager.propose(MayraMemoryCandidate("city", "Bhopal", MayraMemoryCategory.PROFILE, source))
            as MayraMemoryProposalResult.ApprovalRequired
        val memory = (manager.approve(p.proposalId) as MayraMemoryApprovalResult.Saved).memory
        assertTrue(manager.delete(memory.id))
        assertFalse(manager.delete(memory.id))
        manager.clear()
        assertTrue(manager.activeMemories().isEmpty())
    }
}
