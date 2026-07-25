package ai.mayra.app.memory

import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraMemoryBackupTest {
    private lateinit var memory: MayraPersonalMemory

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("mayra_personal_memory", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        memory = MayraPersonalMemory(context)
    }

    @Test fun excludesSensitiveMarkedNotes() {
        memory.saveNote(PersonalNote(title = "Language", body = "I prefer Hindi"))
        memory.saveNote(PersonalNote(title = "Private", body = "hidden reference", sensitive = true))

        val backup = MayraMemoryBackupFormatter.format(memory, now = 0L)

        assertTrue(backup.contains("Language"))
        assertTrue(backup.contains("I prefer Hindi"))
        assertFalse(backup.contains("Private"))
        assertFalse(backup.contains("hidden reference"))
    }
}
