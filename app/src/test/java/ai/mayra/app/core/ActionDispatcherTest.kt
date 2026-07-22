package ai.mayra.app.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionDispatcherTest {

    @Test
    fun successfulOpenAppReturnsFriendlyReply() = runTest {
        val executor = FakeExecutor(openResult = ActionExecutionResult.Success)
        val dispatcher = ActionDispatcher(executor)

        assertEquals(
            "Opening youtube.",
            dispatcher.dispatch(AssistantIntent.OpenApp("youtube"))
        )
    }

    @Test
    fun confirmationMessageIsPassedThrough() = runTest {
        val executor = FakeExecutor(
            callResult = ActionExecutionResult.ConfirmationRequired(
                "Please confirm calling Rahul."
            )
        )
        val dispatcher = ActionDispatcher(executor)

        assertEquals(
            "Please confirm calling Rahul.",
            dispatcher.dispatch(AssistantIntent.CallContact("Rahul"))
        )
    }

    @Test
    fun unsupportedActionReturnsReason() = runTest {
        val executor = FakeExecutor(
            reminderResult = ActionExecutionResult.NotSupported(
                "Reminder scheduling is unavailable."
            )
        )
        val dispatcher = ActionDispatcher(executor)

        assertEquals(
            "Reminder scheduling is unavailable.",
            dispatcher.dispatch(AssistantIntent.CreateReminder("tomorrow"))
        )
    }

    @Test
    fun failureIsWrappedInSafeReply() = runTest {
        val executor = FakeExecutor(
            messageResult = ActionExecutionResult.Failure("SMS app crashed")
        )
        val dispatcher = ActionDispatcher(executor)

        assertEquals(
            "I couldn't complete that action: SMS app crashed",
            dispatcher.dispatch(
                AssistantIntent.ComposeMessage("9999999999", "Hello")
            )
        )
    }

    @Test
    fun chatIntentIsNotDispatchedToDeviceExecutor() = runTest {
        val dispatcher = ActionDispatcher(FakeExecutor())

        assertNull(dispatcher.dispatch(AssistantIntent.Chat("Hello")))
    }

    private class FakeExecutor(
        private val openResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val callResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val messageResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val reminderResult: ActionExecutionResult = ActionExecutionResult.Success
    ) : ActionExecutor {
        override suspend fun openApp(packageOrName: String) = openResult
        override suspend fun callContact(name: String) = callResult
        override suspend fun sendMessage(recipient: String, message: String?) = messageResult
        override suspend fun createReminder(request: String) = reminderResult
    }
}
