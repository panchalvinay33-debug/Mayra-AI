package ai.mayra.app.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MemoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MayraDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        private const val DATABASE_NAME = "mayra.db"

        @Volatile
        private var instance: MayraDatabase? = null

        fun getInstance(context: Context): MayraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MayraDatabase::class.java,
                    DATABASE_NAME
                ).build().also { database ->
                    instance = database
                }
            }
    }
}
