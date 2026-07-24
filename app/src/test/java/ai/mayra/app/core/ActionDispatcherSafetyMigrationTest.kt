package ai.mayra.app.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionDispatcherSafetyMigrationTest {
    @Test
    fun `mayra stop uses global kill switch without rejecting pending action`() = runTest {
        var stopped = false
        var rejected = false
        val dispatcher = ActionDispatcher(
            executor = FakeExecutor(onReject = { rejected = true }),
            stopAllActions = { stopped = true },
            resumeActions = {}
        )

        val reply = dispatcher.dispatch(AssistantIntent.Chat("Mayra stop"))

        assertTrue(stopped)
        assertFalse(rejected)
        assertEquals(
            "All Mayra phone actions are stopped. Chat and phone awareness remain available.",
            reply
        )
    }

    @Test
    fun `mayra resume enables shared action path`() = runTest {
        var resumed = false
        val dispatcher = ActionDispatcher(
            executor = FakeExecutor(),
            stopAllActions = {},
            resumeActions = { resumed = true }
        )

        val reply = dispatcher.dispatch(AssistantIntent.Chat("Mayra resume actions"))

        assertTrue(resumed)
        assertEquals("Mayra phone actions are enabled again.", reply)
    }

    @Test
    fun `plain stop still rejects only pending confirmation`() = runTest {
        var globalStop = false
        var rejected = false
        val dispatcher = ActionDispatcher(
            executor = FakeExecutor(onReject = { rejected = true }),
            stopAllActions = { globalStop = true },
            resumeActions = {}
        )

        val reply = dispatcher.dispatch(AssistantIntent.Chat("stop"))

        assertTrue(rejected)
        assertFalse(globalStop)
        assertEquals("Action cancelled.", reply)
    }

    @Test
    fun `message reply describes draft handoff not a sent message`() = runTest {
        val dispatcher = ActionDispatcher(
            executor = FakeExecutor(),
            stopAllActions = {},
            resumeActions = {}
        )

        val reply = dispatcher.dispatch(
            AssistantIntent.ComposeMessage(recipient = "Rahul", message = "I am late")
        )

        assertEquals("Message prepared for Rahul. Review it before sending.", reply)
        assertFalse(reply.orEmpty().contains("sent", ignoreCase = true))
    }

    @Test
    fun `call reply does not claim connection`() = runTest {
        val dispatcher = ActionDispatcher(
            executor = FakeExecutor(),
            stopAllActions = {},
            resumeActions = {}
        )

        val reply = dispatcher.dispatch(AssistantIntent.CallContact("Mummy"))

        assertEquals("Call flow opened for Mummy. Connection is not claimed yet.", reply)
    }

    private class FakeExecutor(
        private val onReject: () -> Unit = {}
    ) : ActionExecutor {
        override suspend fun openApp(packageOrName: String): ActionExecutionResult = ActionExecutionResult.Success
        override suspend fun callContact(name: String): ActionExecutionResult = ActionExecutionResult.Success
        override suspend fun sendMessage(recipient: String, message: String?): ActionExecutionResult = ActionExecutionResult.Success
        override suspend fun createReminder(request: String): ActionExecutionResult = ActionExecutionResult.Success
        override suspend fun confirmPending(): ActionExecutionResult = ActionExecutionResult.Success
        override suspend fun rejectPending(): ActionExecutionResult {
            onReject()
            return ActionExecutionResult.Success
        }
    }
}
