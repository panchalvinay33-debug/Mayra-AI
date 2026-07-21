package ai.mayra.app.memory

/** A lightweight in-memory conversation item used by the Phase 2 brain. */
data class MemoryEntry(
    val role: Role,
    val text: String,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    enum class Role { USER, MAYRA, SYSTEM }
}
