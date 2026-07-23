package ai.mayra.app.core

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantIntentEngineTest {

    private val engine = AssistantIntentEngine(Locale.ENGLISH)

    @Test
    fun emptyInputReturnsInvalidIntent() {
        val result = engine.parse("   ")

        assertEquals(
            AssistantIntent.Invalid("Please say or type a command."),
            result
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
    fun callCommandReturnsContactIntent() {
        assertEquals(
            AssistantIntent.CallContact("rahul"),
            engine.parse("Call Rahul")
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
