package ai.mayra.app.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MayraOwnedReminderCommandTest {
    @Test
    fun `successful reminder reports Mayra-owned save`() = runTest {
        val dispatcher = ActionDispatcher(
            executor = FakeReminderExecutor(ActionExecutionResult.Success),
            stopAllActions = {},
            resumeActions = {}
        )

        val reply = dispatcher.dispatch(AssistantIntent.CreateReminder("Medicine in 20 minutes")).orEmpty()

        assertTrue(reply.contains("Reminder saved by Mayra"))
        assertTrue(reply.contains("Medicine in 20 minutes"))
        assertFalse(reply.contains("creation opened", ignoreCase = true))
    }

    @Test
    fun `unclear reminder time returns exact clarification`() = runTest {
        val clarification = "What time should I remind you?"
        val dispatcher = ActionDispatcher(
            executor = FakeReminderExecutor(ActionExecutionResult.NotSupported(clarification)),
            stopAllActions = {},
            resumeActions = {}
        )

        assertEquals(
            clarification,
            dispatcher.dispatch(AssistantIntent.CreateReminder("Pay bill tomorrow"))
        )
    }

    private class FakeReminderExecutor(
        private val result: ActionExecutionResult
    ) : ActionExecutor {
        override suspend fun openApp(packageOrName: String) = ActionExecutionResult.Success
        override suspend fun callContact(name: String) = ActionExecutionResult.Success
        override suspend fun sendMessage(recipient: String, message: String?) = ActionExecutionResult.Success
        override suspend fun createReminder(request: String) = result
        override suspend fun confirmPending() = ActionExecutionResult.Success
        override suspend fun rejectPending() = ActionExecutionResult.Success
    }
}
