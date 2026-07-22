package ai.mayra.app.core.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContextManagerTest {
    @Test
    fun `oldest non-system messages are evicted first`() {
        val manager = ConversationContextManager(maxMessages = 3, maxCharacters = 1_000)

        manager.append("session", ConversationMessage(role = ConversationRole.SYSTEM, content = "system"))
        manager.append("session", ConversationMessage(role = ConversationRole.USER, content = "first"))
        manager.append("session", ConversationMessage(role = ConversationRole.ASSISTANT, content = "second"))
        manager.append("session", ConversationMessage(role = ConversationRole.USER, content = "third"))

        val snapshot = manager.snapshot("session")

        assertEquals(listOf("system", "second", "third"), snapshot.messages.map { it.content })
    }

    @Test
    fun `character limit trims context deterministically`() {
        val manager = ConversationContextManager(maxMessages = 10, maxCharacters = 8)

        manager.append("session", ConversationMessage(role = ConversationRole.USER, content = "1234"))
        manager.append("session", ConversationMessage(role = ConversationRole.ASSISTANT, content = "5678"))
        val snapshot = manager.append("session", ConversationMessage(role = ConversationRole.USER, content = "90"))

        assertEquals(listOf("5678", "90"), snapshot.messages.map { it.content })
        assertEquals(6, snapshot.estimatedCharacters)
    }

    @Test
    fun `sessions remain isolated and can be cleared`() {
        val manager = ConversationContextManager()
        manager.append("one", ConversationMessage(role = ConversationRole.USER, content = "hello"))
        manager.append("two", ConversationMessage(role = ConversationRole.USER, content = "namaste"))

        assertEquals(setOf("one", "two"), manager.activeSessionIds())
        assertTrue(manager.clear("one"))
        assertFalse(manager.clear("one"))
        assertTrue(manager.snapshot("one").messages.isEmpty())
        assertEquals(listOf("namaste"), manager.snapshot("two").messages.map { it.content })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank message is rejected`() {
        ConversationMessage(role = ConversationRole.USER, content = "   ")
    }
}
