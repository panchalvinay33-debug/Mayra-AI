package ai.mayra.app.execution

/** Kotlin compatibility helper for ordered bounded queues and collection views. */
internal fun <T> Collection<T>.takeLast(count: Int): List<T> =
    toList().takeLast(count.coerceAtLeast(0))
