package ai.mayra.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MemoryEntity::class,
        ConversationEntity::class,
        ReminderEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MayraDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun conversationDao(): ConversationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val DATABASE_NAME = "mayra.db"

        @Volatile
        private var instance: MayraDatabase? = null

        fun getInstance(context: Context): MayraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MayraDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }

        internal fun clearInstanceForTests() {
            instance = null
        }
    }
}
