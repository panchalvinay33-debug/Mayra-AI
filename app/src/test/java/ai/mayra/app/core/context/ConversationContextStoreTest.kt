package ai.mayra.app.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContextStoreTest {
    @Test
    fun `normalizes duplicates and keeps bounded recent history`() {
        val store = ConversationContextStore(capacity = 2, maxTextLength = 20)
        val first = store.append(ConversationRole.USER, "  hello   Mayra  ", 1L)
        val duplicate = store.append(ConversationRole.USER, "hello Mayra", 2L)
        store.append(ConversationRole.ASSISTANT, "Hi", 3L)
        store.append(ConversationRole.USER, "Next task", 4L)

        assertEquals(first, duplicate)
        assertEquals(listOf("Hi", "Next task"), store.recent().map { it.text })
        assertEquals(1L, store.snapshot().droppedTurns)
    }

    @Test
    fun `search returns newest matches first`() {
        val store = ConversationContextStore()
        store.append(ConversationRole.USER, "Hindi replies please", 1L)
        store.append(ConversationRole.ASSISTANT, "Hindi enabled", 2L)

        assertEquals(listOf("Hindi enabled", "Hindi replies please"), store.search("hindi").map { it.text })
    }

    @Test
    fun `clear preserves lifetime counters`() {
        val store = ConversationContextStore()
        store.append(ConversationRole.USER, "hello", 1L)

        assertEquals(1, store.clear())
        assertTrue(store.recent().isEmpty())
        assertEquals(1L, store.snapshot().totalAcceptedTurns)
    }
}
