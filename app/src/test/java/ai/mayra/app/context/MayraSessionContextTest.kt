package ai.mayra.app.context

import ai.mayra.app.MayraEntryContract
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraSessionContextTest {
    private val now = LocalDateTime.of(2026, 8, 6, 15, 0)

    @Test
    fun recentVoiceSessionHasCoarseLocalSummary() {
        val snapshot = SessionContextSnapshot(
            now,
            ContextValue.Available(
                SessionAggregate(MayraEntryContract.Source.VOICE_SESSION, 4),
                ContextSource.SESSION
            )
        )
        assertEquals("Voice session · 4 min ago", snapshot.summaryLine())
    }

    @Test
    fun staleSessionIsNotSurfaced() {
        val snapshot = SessionContextSnapshot(
            now,
            ContextValue.Available(
                SessionAggregate(MayraEntryContract.Source.LAUNCHER, 181),
                ContextSource.SESSION
            )
        )
        assertNull(snapshot.summaryLine())
    }

    @Test
    fun futureEntryTimeIsRejectedInsteadOfLookingRecent() {
        assertNull(sessionMinutesSince(now.plusMinutes(1), now))
        assertEquals(15, sessionMinutesSince(now.minusMinutes(15), now))
    }

    @Test
    fun sessionContractContainsNoConversationOrExternalAppFields() {
        val fields = SessionAggregate::class.java.declaredFields.map { it.name.lowercase() }
        assertTrue(fields.contains("source"))
        assertTrue(fields.contains("minutessinceentry"))
        assertFalse(fields.any { "message" in it || "conversation" in it || "package" in it || "app" in it || "text" in it })
    }
}
