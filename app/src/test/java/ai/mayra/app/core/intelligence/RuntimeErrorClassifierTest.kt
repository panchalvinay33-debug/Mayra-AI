package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuntimeErrorClassifierTest {
    private val classifier = RuntimeErrorClassifier()

    @Test
    fun `successful results do not produce runtime errors`() {
        val result = ToolResult("utility.echo", ToolExecutionStatus.SUCCESS, output = "ok")

        assertNull(classifier.classify(result))
    }

    @Test
    fun `validation failures are classified as non retryable`() {
        val error = classifier.classify(
            ToolResult(
                toolId = "utility.echo",
                status = ToolExecutionStatus.FAILED,
                errorCode = "missing_required_argument"
            )
        )!!

        assertEquals(RuntimeErrorCategory.VALIDATION, error.category)
        assertFalse(error.retryable)
    }

    @Test
    fun `permission denial has stable fallback code`() {
        val error = classifier.classify(
            ToolResult("device.call", ToolExecutionStatus.DENIED)
        )!!

        assertEquals(RuntimeErrorCategory.PERMISSION, error.category)
        assertEquals("permission_denied", error.code)
        assertFalse(error.retryable)
    }

    @Test
    fun `temporary network failure is retryable`() {
        val error = classifier.classify(
            ToolResult(
                toolId = "network.lookup",
                status = ToolExecutionStatus.FAILED,
                errorCode = "network_unavailable"
            )
        )!!

        assertEquals(RuntimeErrorCategory.NETWORK, error.category)
        assertTrue(error.retryable)
    }

    @Test
    fun `missing tool is classified as not found`() {
        val error = classifier.classify(
            ToolResult("missing.tool", ToolExecutionStatus.NOT_FOUND)
        )!!

        assertEquals(RuntimeErrorCategory.NOT_FOUND, error.category)
        assertEquals("tool_not_found", error.code)
    }

    @Test
    fun `throwables map to safe categories without exposing details`() {
        val validation = classifier.classify(IllegalArgumentException("secret invalid payload"))
        val permission = classifier.classify(SecurityException("private permission detail"))

        assertEquals(RuntimeErrorCategory.VALIDATION, validation.category)
        assertEquals("invalid_request", validation.code)
        assertEquals(RuntimeErrorCategory.PERMISSION, permission.category)
        assertEquals("permission_denied", permission.code)
        assertFalse(validation.userMessage.contains("secret"))
        assertFalse(permission.userMessage.contains("private"))
    }
}
