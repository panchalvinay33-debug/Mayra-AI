package ai.mayra.app.execution

/** Kotlin compatibility helpers for ordered bounded queues, collection views and lazy sequences. */
internal fun <T> Collection<T>.takeLast(count: Int): List<T> {
    val safeCount = count.coerceAtLeast(0)
    if (safeCount == 0) return emptyList()
    val snapshot = ArrayList<T>(size)
    snapshot.addAll(this)
    return snapshot.drop((snapshot.size - safeCount).coerceAtLeast(0))
}

/** Materializes a sequence once and returns its bounded tail without recursive extension dispatch. */
internal fun <T> Sequence<T>.takeLast(count: Int): List<T> {
    val safeCount = count.coerceAtLeast(0)
    if (safeCount == 0) return emptyList()
    val snapshot = ArrayList<T>()
    for (item in this) snapshot += item
    return snapshot.drop((snapshot.size - safeCount).coerceAtLeast(0))
}
