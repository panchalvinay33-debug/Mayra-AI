package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolSelectionEngineTest {
    @Test
    fun `selects best matching tool and respects limit`() {
        val registry = ToolRegistry().apply {
            register(FakeTool("device.launch_app", "Launch App", "Open an installed application", setOf("app")))
            register(FakeTool("device.share", "Share Content", "Share text with another application", setOf("share")))
            register(FakeTool("device.clipboard", "Clipboard", "Copy text to clipboard", setOf("clipboard")))
        }

        val selections = ToolSelectionEngine(registry).select("please launch app", limit = 1)

        assertEquals(1, selections.size)
        assertEquals("device.launch_app", selections.single().manifest.id)
        assertTrue(selections.single().score > 0)
    }

    @Test
    fun `required tags constrain candidate tools`() {
        val registry = ToolRegistry().apply {
            register(FakeTool("device.launch_app", "Launch App", "Open application", setOf("device", "app")))
            register(FakeTool("web.launch_page", "Launch Page", "Open website", setOf("web")))
        }

        val result = ToolSelectionEngine(registry).select("launch", requiredTags = setOf("web"))

        assertEquals(listOf("web.launch_page"), result.map { it.manifest.id })
    }

    private class FakeTool(
        id: String,
        name: String,
        description: String,
        tags: Set<String>
    ) : MayraTool {
        override val manifest = ToolManifest(id, name, description, tags = tags)
        override suspend fun execute(invocation: ToolInvocation) =
            ToolResult(manifest.id, ToolExecutionStatus.SUCCESS)
    }
}
