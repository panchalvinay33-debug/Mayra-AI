package ai.mayra.app.memory

import ai.mayra.app.chat.MayraReplyMetadataParser
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalMemoryAwareMayraAssistantTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneOffset.UTC)

    @Test fun injectsOnlyApprovedRelevantMemoryAndReturnsStructuredUseMetadata() = runBlocking {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val pending = manager.propose(candidate("favorite tea", "masala chai")) as MayraMemoryProposalResult.ApprovalRequired
        manager.approve(pending.proposalId)
        var received = ""
        val delegate = object : MayraAssistant {
            override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> {
                received = message
                return Result.success("You like masala chai.")
            }
        }

        val raw = PersonalMemoryAwareMayraAssistant(delegate, manager)
            .reply("Which tea do I like?").getOrThrow()
        val parsed = MayraReplyMetadataParser.parse(raw)

        assertTrue(received.contains("favorite tea: masala chai"))
        assertTrue(received.contains("approved personal context"))
        assertEquals("You like masala chai.", parsed.text)
        assertEquals(listOf("favorite tea"), parsed.usedPersonalMemoryKeys)
        assertFalse(parsed.text.contains("Used approved personal memory"))
    }

    @Test fun doesNotInjectOrAttachMetadataForUnapprovedOrIrrelevantMemory() = runBlocking {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        manager.propose(candidate("favorite tea", "masala chai"))
        var received = ""
        val delegate = object : MayraAssistant {
            override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> {
                received = message
                return Result.success("ok")
            }
        }

        val parsed = MayraReplyMetadataParser.parse(
            PersonalMemoryAwareMayraAssistant(delegate, manager).reply("Open calculator").getOrThrow()
        )

        assertFalse(received.contains("masala chai"))
        assertFalse(received.contains("approved personal context"))
        assertEquals("ok", parsed.text)
        assertTrue(parsed.usedPersonalMemoryKeys.isEmpty())
    }

    private fun candidate(key: String, value: String) = MayraMemoryCandidate(
        key,
        value,
        MayraMemoryCategory.PREFERENCE,
        MayraMemoryProvenance("chat", "owner-command", clock.instant())
    )
}
