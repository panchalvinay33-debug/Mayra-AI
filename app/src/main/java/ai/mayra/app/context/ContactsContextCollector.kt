package ai.mayra.app.context

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.time.LocalDateTime

/**
 * Reads only contact-row availability metadata and immediately reduces it to counts. Display names,
 * phone numbers, emails, postal addresses, notes and account fields are never projected.
 */
fun collectContactsContext(
    context: Context,
    capturedAt: LocalDateTime = LocalDateTime.now()
): ContactsContextSnapshot {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return ContactsContextSnapshot(capturedAt, ContextValue.NotGranted)
    }

    val metadata: List<ContactMetadata> = try {
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val hasPhoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            buildList<ContactMetadata> {
                while (cursor.moveToNext()) {
                    add(ContactMetadata(hasPhoneNumber = cursor.getInt(hasPhoneIndex) > 0))
                }
            }
        } ?: emptyList()
    } catch (_: SecurityException) {
        return ContactsContextSnapshot(capturedAt, ContextValue.NotGranted)
    } catch (_: RuntimeException) {
        return ContactsContextSnapshot(capturedAt, ContextValue.Unavailable)
    }

    return aggregateContactMetadata(metadata, capturedAt)
}
