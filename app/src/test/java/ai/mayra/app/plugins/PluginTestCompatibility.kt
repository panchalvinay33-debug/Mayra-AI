package ai.mayra.app.plugins

import java.util.UUID

/** Source-compatible factory for tests written before requestId became the first constructor field. */
internal fun PluginRequest(
    operation: String,
    parameters: Map<String, String> = emptyMap(),
    context: PluginContext = PluginContext()
): PluginRequest = PluginRequest(
    requestId = UUID.randomUUID().toString(),
    operation = operation,
    parameters = parameters,
    context = context,
    timeoutMillis = PluginRequest.DEFAULT_TIMEOUT_MS
)
