package ai.mayra.app.core

import ai.mayra.app.core.actions.DevicePermission
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
    fun yesConfirmsPendingAction() = runTest {
        val executor = FakeExecutor(confirmResult = ActionExecutionResult.Success)
        val dispatcher = ActionDispatcher(executor)

        assertEquals("Action completed.", dispatcher.dispatch(AssistantIntent.Chat("haan")))
        assertEquals(1, executor.confirmCalls)
    }

    @Test
    fun noRejectsPendingAction() = runTest {
        val executor = FakeExecutor(rejectResult = ActionExecutionResult.Success)
        val dispatcher = ActionDispatcher(executor)

        assertEquals("Action cancelled.", dispatcher.dispatch(AssistantIntent.Chat("cancel")))
        assertEquals(1, executor.rejectCalls)
    }

    @Test
    fun permissionMessageIsPassedThrough() = runTest {
        val dispatcher = ActionDispatcher(
            FakeExecutor(
                callResult = ActionExecutionResult.PermissionRequired(
                    message = "Mayra needs contacts permission.",
                    permissions = setOf(DevicePermission.READ_CONTACTS)
                )
            )
        )

        assertEquals(
            "Mayra needs contacts permission.",
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
    fun ordinaryChatIntentIsNotDispatchedToDeviceExecutor() = runTest {
        val dispatcher = ActionDispatcher(FakeExecutor())

        assertNull(dispatcher.dispatch(AssistantIntent.Chat("Hello")))
    }

    private class FakeExecutor(
        private val openResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val callResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val messageResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val reminderResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val confirmResult: ActionExecutionResult = ActionExecutionResult.Success,
        private val rejectResult: ActionExecutionResult = ActionExecutionResult.Success
    ) : ActionExecutor {
        var confirmCalls = 0
        var rejectCalls = 0

        override suspend fun openApp(packageOrName: String) = openResult
        override suspend fun callContact(name: String) = callResult
        override suspend fun sendMessage(recipient: String, message: String?) = messageResult
        override suspend fun createReminder(request: String) = reminderResult
        override suspend fun confirmPending(): ActionExecutionResult {
            confirmCalls++
            return confirmResult
        }
        override suspend fun rejectPending(): ActionExecutionResult {
            rejectCalls++
            return rejectResult
        }
    }
}
