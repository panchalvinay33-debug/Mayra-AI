package ai.mayra.app.knowledge

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoryAwareMayraAssistantTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun clearMemory() {
        context.getSharedPreferences("mayra_personal_memory", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun relevantNormalMemoryIsAddedWithoutChangingUserMessage() = runTest {
        MayraMemoryRecall(context).saveConfirmedNote(
            title = "Language preference",
            body = "I prefer Hindi responses"
        )
        val fake = RecordingAssistant()
        val assistant = MemoryAwareMayraAssistant(context, fake)

        assistant.reply("Which language do I prefer?", emptyList()).getOrThrow()

        assertTrue(fake.lastMessage == "Which language do I prefer?")
        assertTrue(fake.lastConversation.first().text.contains("Hindi responses"))
    }

    @Test
    fun unrelatedOrSensitiveMemoryIsNotExposed() = runTest {
        val memory = MayraPersonalMemory(context)
        memory.saveNote(PersonalNote(title = "Secret", body = "OTP 123456", sensitive = true))
        val fake = RecordingAssistant()
        val assistant = MemoryAwareMayraAssistant(context, fake)

        assistant.reply("Tell me about my day", emptyList()).getOrThrow()

        assertFalse(fake.lastConversation.any { it.text.contains("123456") })
    }

    @Test
    fun personalBriefingExcludesSensitiveNotes() {
        val memory = MayraPersonalMemory(context)
        memory.saveNote(PersonalNote(title = "Visible task", body = "Call the electrician", pinned = true))
        memory.saveNote(PersonalNote(title = "Secret task", body = "Private", pinned = true, sensitive = true))

        val briefing = MayraPersonalBriefing(context).compose()

        assertTrue(briefing.highlights.any { it.contains("Visible task") })
        assertFalse(briefing.highlights.any { it.contains("Secret task") })
    }

    private class RecordingAssistant : MayraAssistant {
        var lastMessage: String = ""
        var lastConversation: List<MayraMessage> = emptyList()

        override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> {
            lastMessage = message
            lastConversation = conversation
            return Result.success("ok")
        }
    }
}
