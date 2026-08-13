package ai.mayra.app.learning

/**
 * UI-neutral owner review model. Compose/voice surfaces can render this without directly exposing
 * DAO entities or allowing the model to mutate trusted memory state.
 */
data class LearningReviewItem(
    val key: String,
    val value: String,
    val category: String,
    val source: String,
    val confidencePercent: Int,
    val persistence: String,
    val state: LearnedMemoryState,
    val reason: String,
    val updatedAtEpochMs: Long
)

data class LearningReviewSnapshot(
    val pending: List<LearningReviewItem>,
    val approved: List<LearningReviewItem>
) {
    val pendingCount: Int get() = pending.size
    val approvedCount: Int get() = approved.size
}

object MayraLearningReviewModel {
    fun snapshot(
        pending: List<LearnedMemoryEntity>,
        approved: List<LearnedMemoryEntity>,
        maxPerSection: Int = 50
    ): LearningReviewSnapshot {
        val bounded = maxPerSection.coerceIn(1, 100)
        return LearningReviewSnapshot(
            pending = pending
                .asSequence()
                .filter { it.state == LearnedMemoryState.PENDING.name && it.value.isNotBlank() }
                .sortedByDescending { it.updatedAtEpochMs }
                .take(bounded)
                .map(::toItem)
                .toList(),
            approved = approved
                .asSequence()
                .filter { it.state == LearnedMemoryState.APPROVED.name && it.value.isNotBlank() }
                .sortedByDescending { it.updatedAtEpochMs }
                .take(bounded)
                .map(::toItem)
                .toList()
        )
    }

    private fun toItem(entity: LearnedMemoryEntity): LearningReviewItem = LearningReviewItem(
        key = entity.displayKey.take(80),
        value = entity.value.replace(Regex("\\s+"), " ").trim().take(300),
        category = entity.category,
        source = entity.source,
        confidencePercent = (entity.confidence.coerceIn(0.0, 1.0) * 100).toInt(),
        persistence = entity.persistence,
        state = runCatching { LearnedMemoryState.valueOf(entity.state) }
            .getOrDefault(LearnedMemoryState.REJECTED),
        reason = entity.policyReason.take(160),
        updatedAtEpochMs = entity.updatedAtEpochMs
    )
}
