package ai.mayra.app.memory

import ai.mayra.app.knowledge.ChecklistItem
import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.knowledge.PersonalNoteType
import ai.mayra.app.knowledge.TimelineEvent
import ai.mayra.app.knowledge.TimelineEventType
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.security.SecureRandom
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MayraMemoryBackupEngineTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @BeforeTest
    fun resetMemory() {
        context.getSharedPreferences("mayra_personal_memory", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `encrypted envelope round trips notes checklist and timeline`() {
        val payload = MayraMemoryBackupEngine.BackupPayload(
            notes = listOf(
                PersonalNote(
                    id = "note-1",
                    type = PersonalNoteType.CHECKLIST,
                    title = "Launch Mayra",
                    body = "Stable owner alpha",
                    tags = setOf("project", "alpha"),
                    checklist = listOf(ChecklistItem(id = "item-1", text = "Build APK", completed = true)),
                    pinned = true,
                    createdAt = 100L,
                    updatedAt = 200L
                )
            ),
            events = listOf(
                TimelineEvent(
                    id = "event-1",
                    type = TimelineEventType.GOAL,
                    title = "Personal alpha",
                    description = "First phone validation",
                    occurredAt = 300L
                )
            ),
            generatedAt = 400L
        )

        val encrypted = MayraMemoryBackupEngine.encrypt(payload, "Mayra2026".toCharArray(), deterministicRandom())
        val restored = MayraMemoryBackupEngine.decrypt(encrypted, "Mayra2026".toCharArray())

        assertTrue(encrypted.startsWith("MAYRA_ENCRYPTED_BACKUP_V1"))
        assertFalse(encrypted.contains("Launch Mayra"))
        assertEquals(payload, restored)
    }

    @Test
    fun `wrong password and modified ciphertext are rejected`() {
        val payload = MayraMemoryBackupEngine.BackupPayload(emptyList(), emptyList(), generatedAt = 1L)
        val encrypted = MayraMemoryBackupEngine.encrypt(payload, "Mayra2026".toCharArray(), deterministicRandom())

        assertFailsWith<IllegalArgumentException> {
            MayraMemoryBackupEngine.decrypt(encrypted, "Wrong123".toCharArray())
        }
        val modified = encrypted.dropLast(1) + if (encrypted.last() == 'A') "B" else "A"
        assertFailsWith<IllegalArgumentException> {
            MayraMemoryBackupEngine.decrypt(modified, "Mayra2026".toCharArray())
        }
    }

    @Test
    fun `restore is additive and duplicate safe`() {
        val memory = MayraPersonalMemory(context)
        val payload = MayraMemoryBackupEngine.BackupPayload(
            notes = listOf(PersonalNote(id = "note-1", title = "Remember this")),
            events = listOf(TimelineEvent(id = "event-1", type = TimelineEventType.CUSTOM, title = "Imported event")),
            generatedAt = 1L
        )

        val firstPreview = MayraMemoryBackupEngine.preview(payload, memory)
        val first = MayraMemoryBackupEngine.restore(payload, memory)
        val secondPreview = MayraMemoryBackupEngine.preview(payload, memory)
        val second = MayraMemoryBackupEngine.restore(payload, memory)

        assertEquals(1, firstPreview.newNotes)
        assertEquals(1, first.notesAdded)
        assertEquals(1, first.eventsAdded)
        assertEquals(1, secondPreview.duplicateNotes)
        assertEquals(0, second.notesAdded)
        assertEquals(0, second.eventsAdded)
        assertEquals(1, memory.notes(includeArchived = true).size)
        assertEquals(1, memory.timeline(includeSensitive = true).size)
    }

    @Test
    fun `password policy requires a letter number and minimum length`() {
        assertFailsWith<IllegalArgumentException> { MayraMemoryBackupEngine.requireStrongPassword("short1".toCharArray()) }
        assertFailsWith<IllegalArgumentException> { MayraMemoryBackupEngine.requireStrongPassword("onlyletters".toCharArray()) }
        assertFailsWith<IllegalArgumentException> { MayraMemoryBackupEngine.requireStrongPassword("12345678".toCharArray()) }
        MayraMemoryBackupEngine.requireStrongPassword("Mayra2026".toCharArray())
    }

    private fun deterministicRandom(): SecureRandom = object : SecureRandom() {
        private var value = 1
        override fun nextBytes(bytes: ByteArray) {
            bytes.indices.forEach { index -> bytes[index] = (value++ and 0x7F).toByte() }
        }
    }
}
