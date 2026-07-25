package ai.mayra.app.memory

import ai.mayra.app.knowledge.ChecklistItem
import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.knowledge.PersonalNoteType
import ai.mayra.app.knowledge.TimelineEvent
import ai.mayra.app.knowledge.TimelineEventType
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraMemoryBackupFormatterTest {
    private lateinit var context: Context
    private lateinit var memory: MayraPersonalMemory

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_personal_memory", Context.MODE_PRIVATE).edit().clear().commit()
        memory = MayraPersonalMemory(context)
    }

    @After fun tearDown() {
        context.getSharedPreferences("mayra_personal_memory", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun exportsNormalNotesAndChecklistState() {
        memory.saveNote(
            PersonalNote(
                type = PersonalNoteType.CHECKLIST,
                title = "Trip preparation",
                checklist = listOf(
                    ChecklistItem(text = "Pack charger", completed = true),
                    ChecklistItem(text = "Carry ID", completed = false)
                ),
                tags = setOf("travel"),
                pinned = true
            )
        )

        val text = MayraMemoryBackupFormatter.format(memory, now = 0L)

        assertTrue(text.contains("Trip preparation"))
        assertTrue(text.contains("[x] Pack charger"))
        assertTrue(text.contains("[ ] Carry ID"))
        assertTrue(text.contains("Tags: travel"))
        assertTrue(text.contains("Pinned: yes"))
    }

    @Test fun excludesSensitiveNotesAndTimelineEvents() {
        memory.saveNote(PersonalNote(title = "Normal preference", body = "Morning reminders", sensitive = false))
        memory.saveNote(PersonalNote(title = "Private reference", body = "Do not export", sensitive = true))
        memory.appendEvent(TimelineEvent(type = TimelineEventType.CUSTOM, title = "Visible event", sensitive = false))
        memory.appendEvent(TimelineEvent(type = TimelineEventType.CUSTOM, title = "Hidden event", sensitive = true))

        val text = MayraMemoryBackupFormatter.format(memory, now = 0L)

        assertTrue(text.contains("Normal preference"))
        assertTrue(text.contains("Visible event"))
        assertFalse(text.contains("Private reference"))
        assertFalse(text.contains("Hidden event"))
    }
}
