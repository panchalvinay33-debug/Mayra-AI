package ai.mayra.app.personal

/** Bounded tail collection for sequences without materializing more than [count] elements. */
fun <T> Sequence<T>.takeLast(count: Int): List<T> {
    require(count >= 0)
    if (count == 0) return emptyList()
    val buffer = ArrayDeque<T>(count)
    for (item in this) {
        if (buffer.size == count) buffer.removeFirst()
        buffer.addLast(item)
    }
    return buffer.toList()
}
