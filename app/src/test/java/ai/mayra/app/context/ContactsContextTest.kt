package ai.mayra.app.context

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ContactsContextTest {
    private val capturedAt = LocalDateTime.of(2026, 8, 6, 10, 0)

    @Test
    fun aggregatesOnlyCoarseContactMetadata() {
        val snapshot = aggregateContactMetadata(
            items = listOf(
                ContactMetadata(hasPhoneNumber = true),
                ContactMetadata(hasPhoneNumber = false),
                ContactMetadata(hasPhoneNumber = true)
            ),
            capturedAt = capturedAt
        )

        val available = snapshot.access as ContextValue.Available
        assertEquals(3, available.value.totalContacts)
        assertEquals(2, available.value.phoneCapableContacts)
        assertEquals(ContextSource.CONTACTS, available.source)
        assertEquals("3 contacts · 2 with phone number", snapshot.summaryLine())
    }

    @Test
    fun accessStatesRemainExplicit() {
        assertEquals(
            "Contacts not enabled",
            ContactsContextSnapshot(capturedAt, ContextValue.NotGranted).summaryLine()
        )
        assertEquals(
            "Contacts context unavailable",
            ContactsContextSnapshot(capturedAt, ContextValue.Unavailable).summaryLine()
        )
    }

    @Test
    fun metadataContractContainsNoPrivateContactFields() {
        val names = ContactMetadata::class.java.declaredFields.map { it.name }.toSet()

        assertFalse("name" in names)
        assertFalse("displayName" in names)
        assertFalse("phoneNumber" in names)
        assertFalse("email" in names)
        assertFalse("address" in names)
        assertFalse("notes" in names)
        assertFalse("accountName" in names)
    }
}
