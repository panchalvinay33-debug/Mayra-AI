package ai.mayra.app.context

import java.time.LocalDateTime

/** Privacy-first J6 contacts aggregate. Names, numbers, emails and addresses are excluded. */
data class ContactsContextSnapshot(
    val capturedAt: LocalDateTime,
    val access: ContextValue<ContactsAggregate> = ContextValue.NotGranted
)

data class ContactsAggregate(
    val totalContacts: Int,
    val phoneCapableContacts: Int
) {
    init {
        require(totalContacts >= 0) { "totalContacts must be non-negative" }
        require(phoneCapableContacts in 0..totalContacts) {
            "phoneCapableContacts must be between zero and totalContacts"
        }
    }
}

data class ContactMetadata(
    val hasPhoneNumber: Boolean
)

fun aggregateContactMetadata(
    items: List<ContactMetadata>,
    capturedAt: LocalDateTime
): ContactsContextSnapshot = ContactsContextSnapshot(
    capturedAt = capturedAt,
    access = ContextValue.Available(
        value = ContactsAggregate(
            totalContacts = items.size,
            phoneCapableContacts = items.count(ContactMetadata::hasPhoneNumber)
        ),
        source = ContextSource.CONTACTS
    )
)

fun ContactsContextSnapshot.summaryLine(): String = when (val value = access) {
    ContextValue.NotGranted -> "Contacts not enabled"
    ContextValue.Unavailable -> "Contacts context unavailable"
    is ContextValue.Available -> {
        val aggregate = value.value
        when {
            aggregate.totalContacts == 0 -> "No contacts available"
            aggregate.phoneCapableContacts == aggregate.totalContacts ->
                "${aggregate.totalContacts} contacts available"
            else ->
                "${aggregate.totalContacts} contacts · ${aggregate.phoneCapableContacts} with phone number"
        }
    }
}
