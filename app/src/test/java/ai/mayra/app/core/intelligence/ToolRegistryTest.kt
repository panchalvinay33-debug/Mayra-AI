package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ToolRegistryTest {
    @Test
    fun `register resolve disable and enable tool`() {
        val registry = ToolRegistry()
        val tool = FakeTool("device.launch_app", setOf("device", "app"))
        registry.register(tool)

        assertEquals(tool, registry.resolve("device.launch_app"))
        registry.disable("device.launch_app")
        assertNull(registry.resolve("device.launch_app"))
        registry.enable("device.launch_app")
        assertEquals(tool, registry.resolve("device.launch_app"))
    }

    @Test
    fun `duplicate registration fails unless replacement requested`() {
        val registry = ToolRegistry()
        registry.register(FakeTool("device.share"))
        assertFailsWith<IllegalArgumentException> { registry.register(FakeTool("device.share")) }
        registry.register(FakeTool("device.share"), replace = true)
        assertEquals(1, registry.manifests().size)
    }

    @Test
    fun `discovery ranks id and name matches`() {
        val registry = ToolRegistry()
        registry.register(FakeTool("device.launch_app", setOf("app"), "Launch App"))
        registry.register(FakeTool("device.clipboard", setOf("clipboard"), "Clipboard"))

        val results = registry.discover("launch app")

        assertEquals("device.launch_app", results.first().id)
    }

    private class FakeTool(
        id: String,
        tags: Set<String> = emptySet(),
        name: String = id
    ) : MayraTool {
        override val manifest = ToolManifest(id, name, "Test tool for $name", tags = tags)
        override suspend fun execute(invocation: ToolInvocation) =
            ToolResult(manifest.id, ToolExecutionStatus.SUCCESS, output = "ok")
    }
}
