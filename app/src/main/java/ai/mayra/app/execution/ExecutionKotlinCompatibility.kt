package ai.mayra.app.execution

/** Kotlin compatibility helpers for ordered bounded queues, collection views and lazy sequences. */
internal fun <T> Collection<T>.takeLast(count: Int): List<T> =
    toList().takeLast(count.coerceAtLeast(0))

/**
 * Materializes a sequence before taking its tail. Kotlin's standard library exposes takeLast on
 * collections but not on Sequence, while execution diagnostics intentionally build lazy filters.
 */
internal fun <T> Sequence<T>.takeLast(count: Int): List<T> =
    toList().takeLast(count.coerceAtLeast(0))
