package ai.mayra.app.memory

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraMemoryChatControllerTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC)
    private val proposals = MayraInMemoryPendingMemoryProposalStore()
    private val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock, proposals)
    private val controller = MayraMemoryChatController(manager, clock)

    @Test fun rememberRequiresVisualApprovalThenSaves() {
        val result = controller.handle("remember favorite tea: masala chai") as MayraMemoryChatResult.NeedsApproval
        assertEquals(0, manager.activeMemories().size)
        assertEquals("favorite tea", result.pending.key)
        assertNull(result.pending.previousValue)

        val saved = controller.approve(result.pending.proposalId)

        assertTrue(saved.text.startsWith("Saved:"))
        assertEquals("masala chai", manager.activeMemories().single().value)
        assertEquals(MayraMemoryCategory.PREFERENCE, manager.activeMemories().single().category)
    }

    @Test fun prohibitedSecretsAreRejectedWithoutProposal() {
        val result = controller.handle("remember bank pin: 1234") as MayraMemoryChatResult.Reply
        assertTrue(result.text.contains("will not store", ignoreCase = true))
        assertEquals(0, manager.pendingCount())
    }

    @Test fun cancelConsumesProposalAndPreventsSave() {
        val proposal = controller.handle("remember project name: Mayra") as MayraMemoryChatResult.NeedsApproval
        assertEquals("Memory proposal cancelled.", controller.cancel(proposal.pending.proposalId).text)
        assertTrue(controller.approve(proposal.pending.proposalId).text.contains("missing"))
    }

    @Test fun conflictingValueRequiresReplaceReview() {
        val first = controller.handle("remember favorite tea: masala chai") as MayraMemoryChatResult.NeedsApproval
        controller.approve(first.pending.proposalId)
        val replacement = controller.handle("remember favorite tea: ginger tea") as MayraMemoryChatResult.NeedsApproval

        assertEquals("masala chai", replacement.pending.previousValue)
        controller.approve(replacement.pending.proposalId)
        val memory = manager.activeMemories().single()
        assertEquals("ginger tea", memory.value)
        assertEquals(2, memory.revision)
    }

    @Test fun pendingApprovalCanBeRestoredAfterControllerRecreation() {
        val proposal = controller.handle("remember business project: NearMeU") as MayraMemoryChatResult.NeedsApproval
        val restored = MayraMemoryChatController(manager, clock).restoreLatestPending()
        assertEquals(proposal.pending.proposalId, restored?.proposalId)
    }

    @Test fun listAndForgetUseOnlyApprovedMemories() {
        val proposal = controller.handle("remember business project: NearMeU") as MayraMemoryChatResult.NeedsApproval
        controller.approve(proposal.pending.proposalId)
        assertTrue((controller.handle("what do you remember") as MayraMemoryChatResult.Reply).text.contains("NearMeU"))
        assertTrue((controller.handle("forget NearMeU") as MayraMemoryChatResult.Reply).text.startsWith("Forgot:"))
        assertTrue(manager.activeMemories().isEmpty())
    }

    @Test fun unrelatedChatIsNotHandled() {
        assertEquals(MayraMemoryChatResult.NotHandled, controller.handle("How is the weather?"))
    }
}
