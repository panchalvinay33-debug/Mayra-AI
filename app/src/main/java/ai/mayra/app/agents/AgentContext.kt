package ai.mayra.app.agents

import java.util.concurrent.ConcurrentHashMap

/**
 * Request-scoped shared state used by cooperating agents.
 */
class AgentContext(
    initialValues: Map<String, Any?> = emptyMap()
) {
    private val values = ConcurrentHashMap<String, Any>()

    init {
        initialValues.forEach { (key, value) ->
            if (value != null) values[key] = value
        }
    }

    fun put(key: String, value: Any?) {
        require(key.isNotBlank()) { "Context key cannot be blank" }
        if (value == null) {
            values.remove(key)
        } else {
            values[key] = value
        }
    }

    operator fun get(key: String): Any? = values[key]

    inline fun <reified T> getAs(key: String): T? = get(key) as? T

    fun remove(key: String): Any? = values.remove(key)

    fun contains(key: String): Boolean = values.containsKey(key)

    fun snapshot(): Map<String, Any> = values.toMap()
}
