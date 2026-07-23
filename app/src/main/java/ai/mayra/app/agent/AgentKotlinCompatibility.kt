package ai.mayra.app.agent

/** Preserves iteration order while taking the newest bounded entries from a Set. */
internal fun <T> Set<T>.takeLast(count: Int): List<T> =
    toList().takeLast(count.coerceAtLeast(0))
