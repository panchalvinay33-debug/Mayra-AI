package ai.mayra.app.android.actions

/**
 * Runtime registry for Android actions.
 *
 * Duplicate IDs are rejected so a plugin cannot silently replace a core
 * capability. Deliberate replacement can be added later through a privileged
 * plugin API.
 */
class ActionRegistry(
    actions: Iterable<AndroidAction<*>> = emptyList(),
) {
    private val registered = linkedMapOf<String, AndroidAction<*>>()

    init {
        actions.forEach(::register)
    }

    @Synchronized
    fun register(action: AndroidAction<*>) {
        require(action.id.isNotBlank()) { "Action id must not be blank" }
        check(action.id !in registered) {
            "Android action '${action.id}' is already registered"
        }
        registered[action.id] = action
    }

    @Synchronized
    fun unregister(id: String): AndroidAction<*>? = registered.remove(id)

    @Synchronized
    fun find(id: String): AndroidAction<*>? = registered[id]

    @Synchronized
    fun contains(id: String): Boolean = id in registered

    @Synchronized
    fun snapshot(): List<AndroidAction<*>> = registered.values.toList()
}
