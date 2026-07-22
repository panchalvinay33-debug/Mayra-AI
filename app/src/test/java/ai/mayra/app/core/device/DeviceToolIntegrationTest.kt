package ai.mayra.app.core.device

import ai.mayra.app.core.intelligence.ToolExecutionContext
import ai.mayra.app.core.intelligence.ToolExecutionStatus
import ai.mayra.app.core.intelligence.ToolInvocation
import ai.mayra.app.core.intelligence.ToolInvocationPipeline
import ai.mayra.app.core.intelligence.ToolRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceToolIntegrationTest {

    @Test
    fun `registers only tools supported by current capabilities`() {
        val registry = ToolRegistry()
        val capabilities = AndroidCapabilityRegistry(
            setOf(DeviceCapability.OPEN_URL, DeviceCapability.CLIPBOARD_WRITE)
        )

        val manifests = DeviceToolRegistrar(registry, successHandler(), capabilities)
            .registerAvailable()

        assertEquals(setOf(DeviceToolCatalog.OPEN_URL, DeviceToolCatalog.COPY_CLIPBOARD), manifests.map { it.id }.toSet())
        assertNotNull(registry.resolve(DeviceToolCatalog.OPEN_URL))
        assertNotNull(registry.resolve(DeviceToolCatalog.COPY_CLIPBOARD))
        assertNull(registry.resolve(DeviceToolCatalog.LAUNCH_APP))
    }

    @Test
    fun `synchronizes registry when device capabilities change`() {
        val registry = ToolRegistry()
        val capabilities = AndroidCapabilityRegistry(setOf(DeviceCapability.OPEN_URL))
        val registrar = DeviceToolRegistrar(registry, successHandler(), capabilities)

        registrar.registerAvailable()
        capabilities.unregister(DeviceCapability.OPEN_URL)
        capabilities.register(DeviceCapability.SHARE_TEXT)

        val result = registrar.synchronize()

        assertEquals(setOf(DeviceToolCatalog.SHARE_TEXT), result.added)
        assertEquals(1, result.removedCount)
        assertNull(registry.resolve(DeviceToolCatalog.OPEN_URL))
        assertNotNull(registry.resolve(DeviceToolCatalog.SHARE_TEXT))
    }

    @Test
    fun `pipeline converts invocation to device action and returns device result`() = runTest {
        var capturedAction: DeviceAction? = null
        var capturedContext: DeviceActionContext? = null
        val handler = DeviceActionHandler { action, context ->
            capturedAction = action
            capturedContext = context
            DeviceActionResult(DeviceActionStatus.SUCCESS, message = "opened")
        }
        val registry = ToolRegistry()
        DeviceToolRegistrar(
            registry,
            handler,
            AndroidCapabilityRegistry(setOf(DeviceCapability.OPEN_URL))
        ).registerAvailable()

        val result = ToolInvocationPipeline(registry).invoke(
            ToolInvocation(
                toolId = DeviceToolCatalog.OPEN_URL,
                arguments = mapOf("url" to "https://example.com"),
                context = ToolExecutionContext(sessionId = "session-1")
            )
        )

        assertEquals(ToolExecutionStatus.SUCCESS, result.status)
        assertEquals("opened", result.output)
        assertEquals(DeviceAction.OpenUrl("https://example.com"), capturedAction)
        assertEquals("session-1", capturedContext?.sessionId)
        assertFalse(capturedContext?.confirmed ?: true)
        assertEquals("OPEN_URL", result.metadata["deviceCapability"])
    }

    @Test
    fun `confirmed high risk invocation reaches device handler as confirmed`() = runTest {
        var confirmed = false
        val handler = DeviceActionHandler { _, context ->
            confirmed = context.confirmed
            DeviceActionResult(DeviceActionStatus.SUCCESS)
        }
        val registry = ToolRegistry()
        DeviceToolRegistrar(
            registry,
            handler,
            AndroidCapabilityRegistry(setOf(DeviceCapability.APP_LAUNCH))
        ).registerAvailable()

        val result = ToolInvocationPipeline(registry).invoke(
            invocation = ToolInvocation(
                toolId = DeviceToolCatalog.LAUNCH_APP,
                arguments = mapOf("packageName" to "com.example.app"),
                context = ToolExecutionContext(sessionId = "session-2")
            ),
            confirmed = true
        )

        assertEquals(ToolExecutionStatus.SUCCESS, result.status)
        assertTrue(confirmed)
    }

    @Test
    fun `device permission denial maps to denied tool result`() = runTest {
        val handler = DeviceActionHandler { _, _ ->
            DeviceActionResult(DeviceActionStatus.PERMISSION_DENIED)
        }
        val definition = DeviceToolCatalog.definitions().first { it.manifest.id == DeviceToolCatalog.OPEN_URL }
        val tool = DeviceToolAdapter(definition.manifest, definition.actionFactory, handler)

        val result = tool.execute(
            ToolInvocation(
                toolId = definition.manifest.id,
                arguments = mapOf("url" to "https://example.com"),
                context = ToolExecutionContext(sessionId = "session-3")
            )
        )

        assertEquals(ToolExecutionStatus.DENIED, result.status)
        assertEquals("device_permission_denied", result.errorCode)
    }

    private fun successHandler(): DeviceActionHandler = DeviceActionHandler { _, _ ->
        DeviceActionResult(DeviceActionStatus.SUCCESS)
    }
}
