package ai.mayra.app.memory

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraMemoryChatControllerTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC)
    private val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
    private val controller = MayraMemoryChatController(manager, clock)

    @Test
    fun rememberRequiresExplicitConfirmationThenSaves() {
        val proposal = controller.handle("remember favorite tea: masala chai") as MayraMemoryChatResult.Reply
        assertEquals(0, manager.activeMemories().size)
        val id = Regex("confirm memory ([a-f0-9]{64})").find(proposal.text)!!.groupValues[1]

        val saved = controller.handle("confirm memory $id") as MayraMemoryChatResult.Reply

        assertTrue(saved.text.startsWith("Saved:"))
        assertEquals("masala chai", manager.activeMemories().single().value)
        assertEquals(MayraMemoryCategory.PREFERENCE, manager.activeMemories().single().category)
    }

    @Test
    fun prohibitedSecretsAreRejectedWithoutProposal() {
        val result = controller.handle("remember bank pin: 1234") as MayraMemoryChatResult.Reply
        assertTrue(result.text.contains("will not store", ignoreCase = true))
        assertEquals(0, manager.pendingCount())
    }

    @Test
    fun cancelConsumesProposalAndPreventsSave() {
        val proposal = controller.handle("remember project name: Mayra") as MayraMemoryChatResult.Reply
        val id = Regex("cancel memory ([a-f0-9]{64})").find(proposal.text)!!.groupValues[1]
        val cancelled = controller.handle("cancel memory $id") as MayraMemoryChatResult.Reply
        assertEquals("Memory proposal cancelled.", cancelled.text)
        assertTrue((controller.handle("confirm memory $id") as MayraMemoryChatResult.Reply).text.contains("missing"))
    }

    @Test
    fun listAndForgetUseOnlyApprovedMemories() {
        val proposal = controller.handle("remember business project: NearMeU") as MayraMemoryChatResult.Reply
        val id = Regex("confirm memory ([a-f0-9]{64})").find(proposal.text)!!.groupValues[1]
        controller.handle("confirm memory $id")

        val listed = controller.handle("what do you remember") as MayraMemoryChatResult.Reply
        assertTrue(listed.text.contains("NearMeU"))

        val forgotten = controller.handle("forget NearMeU") as MayraMemoryChatResult.Reply
        assertTrue(forgotten.text.startsWith("Forgot:"))
        assertTrue(manager.activeMemories().isEmpty())
    }

    @Test
    fun unrelatedChatIsNotHandled() {
        assertEquals(MayraMemoryChatResult.NotHandled, controller.handle("How is the weather?"))
    }
}
