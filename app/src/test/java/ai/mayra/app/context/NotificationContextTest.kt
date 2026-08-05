package ai.mayra.app.context

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationContextTest {
    private val capturedAt = LocalDateTime.of(2026, 8, 5, 22, 45)

    @Test
    fun aggregatesOnlySafeMetadataDeterministically() {
        val snapshot = aggregateNotificationMetadata(
            items = listOf(
                NotificationMetadata(NotificationCategory.MESSAGE, requestsAttention = true),
                NotificationMetadata(NotificationCategory.SYSTEM, requestsAttention = false),
                NotificationMetadata(NotificationCategory.MESSAGE, requestsAttention = false)
            ),
            capturedAt = capturedAt
        )

        val available = snapshot.access as ContextValue.Available
        assertEquals(3, available.value.activeCount)
        assertEquals(1, available.value.attentionCount)
        assertEquals(2, available.value.categoryCounts[NotificationCategory.MESSAGE])
        assertEquals(1, available.value.categoryCounts[NotificationCategory.SYSTEM])
        assertEquals(ContextSource.NOTIFICATION_ACCESS, available.source)
        assertEquals("3 active · 1 may need attention", snapshot.summaryLine())
    }

    @Test
    fun emptyMetadataProducesZeroAggregate() {
        val snapshot = aggregateNotificationMetadata(emptyList(), capturedAt)
        val available = snapshot.access as ContextValue.Available

        assertEquals(0, available.value.activeCount)
        assertEquals(0, available.value.attentionCount)
        assertTrue(available.value.categoryCounts.isEmpty())
        assertEquals("No active notifications", snapshot.summaryLine())
    }

    @Test
    fun accessStatesRemainExplicit() {
        assertEquals(
            "Notifications not enabled",
            NotificationContextSnapshot(capturedAt, ContextValue.NotGranted).summaryLine()
        )
        assertEquals(
            "Notification context unavailable",
            NotificationContextSnapshot(capturedAt, ContextValue.Unavailable).summaryLine()
        )
    }

    @Test
    fun metadataContractContainsNoPrivateTextFields() {
        val names = NotificationMetadata::class.java.declaredFields.map { it.name }.toSet()

        assertFalse("title" in names)
        assertFalse("text" in names)
        assertFalse("body" in names)
        assertFalse("sender" in names)
        assertFalse("conversation" in names)
    }
}
