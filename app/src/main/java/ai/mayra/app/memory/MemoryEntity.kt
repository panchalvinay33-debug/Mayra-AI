package ai.mayra.app.memory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memories",
    indices = [
        Index(value = ["category"]),
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val category: String = CATEGORY_GENERAL,
    val source: String = SOURCE_CONVERSATION,
    val importance: Int = DEFAULT_IMPORTANCE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(content.isNotBlank()) { "Memory content cannot be blank." }
        require(importance in MIN_IMPORTANCE..MAX_IMPORTANCE) {
            "Importance must be between $MIN_IMPORTANCE and $MAX_IMPORTANCE."
        }
    }

    companion object {
        const val CATEGORY_GENERAL = "general"
        const val SOURCE_CONVERSATION = "conversation"
        const val DEFAULT_IMPORTANCE = 3
        const val MIN_IMPORTANCE = 1
        const val MAX_IMPORTANCE = 5
    }
}
