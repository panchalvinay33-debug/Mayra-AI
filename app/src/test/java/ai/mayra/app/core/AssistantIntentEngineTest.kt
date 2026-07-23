package ai.mayra.app.core

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantIntentEngineTest {

    private val engine = AssistantIntentEngine(Locale.ENGLISH)

    @Test
    fun emptyInputReturnsInvalidIntent() {
        assertEquals(
            AssistantIntent.Invalid("Please say or type a command."),
            engine.parse("   ")
        )
    }

    @Test
    fun openCommandReturnsStructuredOpenAppIntent() {
        assertEquals(
            AssistantIntent.OpenApp("youtube"),
            engine.parse("Open YouTube")
        )
    }

    @Test
    fun hindiOpenCommandReturnsStructuredOpenAppIntent() {
        assertEquals(
            AssistantIntent.OpenApp("camera"),
            engine.parse("Khol Camera")
        )
    }

    @Test
    fun naturalHinglishOpenCommandCanContainWordsBeforeAction() {
        assertEquals(
            AssistantIntent.OpenApp("whatsapp"),
            engine.parse("Mayra please mera WhatsApp open karo")
        )
    }

    @Test
    fun callCommandReturnsContactIntent() {
        assertEquals(
            AssistantIntent.CallContact("rahul"),
            engine.parse("Call Rahul")
        )
    }

    @Test
    fun reportedNaturalSentenceExtractsMummyAsCallTarget() {
        assertEquals(
            AssistantIntent.CallContact("mummy"),
            engine.parse("Mera open contact and call mummy")
        )
    }

    @Test
    fun hinglishCallSentenceRemovesCommandFillers() {
        assertEquals(
            AssistantIntent.CallContact("papa"),
            engine.parse("Mayra jara contact Papa ko call karo")
        )
    }

    @Test
    fun messageCommandSeparatesRecipientAndBody() {
        assertEquals(
            AssistantIntent.ComposeMessage(
                recipient = "Rahul",
                message = "meeting at five"
            ),
            engine.parse("Send message to Rahul: meeting at five")
        )
    }

    @Test
    fun hinglishMessageCommandSeparatesRecipientAndBody() {
        assertEquals(
            AssistantIntent.ComposeMessage(
                recipient = "Mummy",
                message = "main thodi der me aaunga"
            ),
            engine.parse("Mummy ko message likho main thodi der me aaunga")
        )
    }

    @Test
    fun messageWithoutBodyStillCreatesComposerIntent() {
        assertEquals(
            AssistantIntent.ComposeMessage("Rahul", null),
            engine.parse("Message Rahul")
        )
    }

    @Test
    fun reminderPreservesOriginalRequestText() {
        assertEquals(
            AssistantIntent.CreateReminder("Call the customer Tomorrow at 5 PM"),
            engine.parse("Remind me Call the customer Tomorrow at 5 PM")
        )
    }

    @Test
    fun deviceInformationCommandsAreRecognized() {
        assertEquals(
            AssistantIntent.DeviceInfo(DeviceInfoType.TIME),
            engine.parse("Time kya hai")
        )
        assertEquals(
            AssistantIntent.DeviceInfo(DeviceInfoType.BATTERY),
            engine.parse("Battery kitni hai")
        )
    }

    @Test
    fun unknownInputFallsBackToChat() {
        val result = engine.parse("Mujhe ek business idea do")

        assertTrue(result is AssistantIntent.Chat)
        assertEquals(
            "Mujhe ek business idea do",
            (result as AssistantIntent.Chat).message
        )
    }
}