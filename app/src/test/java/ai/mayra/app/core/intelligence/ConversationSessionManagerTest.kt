package ai.mayra.app.core.intelligence

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversationSessionManagerTest {

    private val fixedInstant = Instant.parse("2026-07-22T12:00:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    @Test
    fun `create normalizes title and stores metadata`() {
        val manager = managerWithIds("session-1")

        val session = manager.create(
            title = "  Mayra chat  ",
            metadata = mapOf("source" to "voice")
        )

        assertEquals("session-1", session.id)
        assertEquals("Mayra chat", session.title)
        assertEquals(ConversationSessionStatus.ACTIVE, session.status)
        assertEquals(fixedInstant, session.createdAt)
        assertEquals("voice", session.metadata["source"])
    }

    @Test
    fun `append stores messages and refreshes session`() {
        val manager = managerWithIds("session-1")
        val session = manager.create()

        val snapshot = manager.append(
            session.id,
            ConversationMessage(role = ConversationRole.USER, content = "Hello Mayra")
        )

        assertEquals(1, snapshot.messages.size)
        assertEquals("Hello Mayra", snapshot.messages.single().content)
        assertEquals(fixedInstant, manager.get(session.id)?.updatedAt)
    }

    @Test
    fun `closed session rejects new messages`() {
        val manager = managerWithIds("session-1")
        val session = manager.create()
        manager.close(session.id)

        val error = assertFailsWith<IllegalStateException> {
            manager.append(
                session.id,
                ConversationMessage(role = ConversationRole.USER, content = "Too late")
            )
        }

        assertTrue(error.message.orEmpty().contains("closed"))
        assertEquals(ConversationSessionStatus.CLOSED, manager.get(session.id)?.status)
    }

    @Test
    fun `rename and metadata update preserve existing values`() {
        val manager = managerWithIds("session-1")
        val session = manager.create(metadata = mapOf("channel" to "text"))

        manager.rename(session.id, "  Daily planning ")
        val updated = manager.updateMetadata(session.id, mapOf("language" to "hi"))

        assertEquals("Daily planning", updated.title)
        assertEquals("text", updated.metadata["channel"])
        assertEquals("hi", updated.metadata["language"])
    }

    @Test
    fun `delete removes both session and context`() {
        val manager = managerWithIds("session-1")
        val session = manager.create()
        manager.append(
            session.id,
            ConversationMessage(role = ConversationRole.USER, content = "Remember this")
        )

        assertTrue(manager.delete(session.id))
        assertFalse(manager.delete(session.id))
        assertNull(manager.get(session.id))
        assertFailsWith<NoSuchElementException> { manager.snapshot(session.id) }
    }

    @Test
    fun `list can filter by status`() {
        val ids = ArrayDeque(listOf("active", "closed"))
        val manager = ConversationSessionManager(
            clock = fixedClock,
            idFactory = { ids.removeFirst() }
        )
        manager.create()
        val closed = manager.create()
        manager.close(closed.id)

        assertEquals(listOf("active"), manager.list(ConversationSessionStatus.ACTIVE).map { it.id })
        assertEquals(listOf("closed"), manager.list(ConversationSessionStatus.CLOSED).map { it.id })
        assertEquals(2, manager.list().size)
    }

    @Test
    fun `duplicate generated id is rejected`() {
        val manager = ConversationSessionManager(
            clock = fixedClock,
            idFactory = { "same-id" }
        )
        manager.create()

        assertFailsWith<IllegalArgumentException> { manager.create() }
    }

    private fun managerWithIds(vararg ids: String): ConversationSessionManager {
        val queue = ArrayDeque(ids.toList())
        return ConversationSessionManager(
            clock = fixedClock,
            idFactory = { queue.removeFirst() }
        )
    }
}