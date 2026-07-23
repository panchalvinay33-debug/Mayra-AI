package ai.mayra.app.plugins

/** Source-compatible factory for tests written before requestId became the first constructor field. */
internal fun PluginRequest(
    operation: String,
    parameters: Map<String, String> = emptyMap(),
    context: PluginContext = PluginContext()
): PluginRequest = PluginRequest(
    operation = operation,
    parameters = parameters,
    context = context
)
