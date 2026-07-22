package ai.mayra.app.core.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContextStoreTest {

    @Test
    fun appendNormalizesWhitespaceAndRejectsBlankText() {
        val store = ConversationContextStore(capacity = 3, maxTextLength = 20)

        val turn = store.append(ConversationRole.USER, "  hello   Mayra  ", 10L)

        assertEquals("hello Mayra", turn.text)
        assertEquals(ConversationRole.USER, turn.role)

        val error = runCatching {
            store.append(ConversationRole.USER, "   ", 11L)
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun capacityDropsOldestTurnAndTracksMetrics() {
        val store = ConversationContextStore(capacity = 2)

        store.append(ConversationRole.USER, "one", 1L)
        store.append(ConversationRole.ASSISTANT, "two", 2L)
        store.append(ConversationRole.USER, "three", 3L)

        val snapshot = store.snapshot()
        assertEquals(listOf("two", "three"), snapshot.turns.map { it.text })
        assertEquals(3L, snapshot.totalAcceptedTurns)
        assertEquals(1L, snapshot.droppedTurns)
    }

    @Test
    fun consecutiveDuplicateDoesNotCreateAnotherTurn() {
        val store = ConversationContextStore()

        val first = store.append(ConversationRole.USER, "open camera", 1L)
        val duplicate = store.append(ConversationRole.USER, "open   camera", 2L)

        assertEquals(first, duplicate)
        assertEquals(1, store.snapshot().turns.size)
        assertEquals(1L, store.snapshot().totalAcceptedTurns)
    }

    @Test
    fun searchReturnsNewestMatchingTurnsFirst() {
        val store = ConversationContextStore()
        store.append(ConversationRole.USER, "Call Shiv", 1L)
        store.append(ConversationRole.ASSISTANT, "Calling Shiv", 2L)
        store.append(ConversationRole.USER, "Open camera", 3L)

        val results = store.search("shiv")

        assertEquals(listOf("Calling Shiv", "Call Shiv"), results.map { it.text })
    }

    @Test
    fun clearReturnsRemovedCountWithoutResettingMetrics() {
        val store = ConversationContextStore()
        store.append(ConversationRole.USER, "hello", 1L)
        store.append(ConversationRole.ASSISTANT, "hi", 2L)

        assertEquals(2, store.clear())
        assertTrue(store.recent().isEmpty())
        assertEquals(2L, store.snapshot().totalAcceptedTurns)
    }
}
