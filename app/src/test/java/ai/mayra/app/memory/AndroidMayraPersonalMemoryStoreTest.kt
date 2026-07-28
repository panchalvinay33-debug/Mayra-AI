package ai.mayra.app.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidMayraPersonalMemoryStoreTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun survivesStoreRecreationWithUnicode() {
        val memory = memory("1", "भाषा", "हिंदी में जवाब पसंद है", 1)
        AndroidMayraPersonalMemoryStore(context).put(memory)
        val restored = AndroidMayraPersonalMemoryStore(context).all().single()
        assertEquals(memory, restored)
    }

    @Test fun retentionKeepsNewestRecords() {
        val store = AndroidMayraPersonalMemoryStore(context, maxRecords = 2)
        store.put(memory("1", "one", "a", 1))
        store.put(memory("2", "two", "b", 2))
        store.put(memory("3", "three", "c", 3))
        assertEquals(listOf("3", "2"), store.all().map { it.id })
    }

    @Test fun corruptRecordsAreSkipped() {
        val prefs = context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("records", setOf("broken", MayraMemoryCodec.encode(memory("1", "city", "Indore", 1)))).commit()
        assertEquals(1, AndroidMayraPersonalMemoryStore(context).all().size)
    }

    @Test fun deleteAndClearPersist() {
        val store = AndroidMayraPersonalMemoryStore(context)
        store.put(memory("1", "city", "Indore", 1))
        assertTrue(store.delete("1"))
        assertFalse(store.delete("1"))
        store.put(memory("2", "food", "poha", 2))
        store.clear()
        assertTrue(AndroidMayraPersonalMemoryStore(context).all().isEmpty())
    }

    @Test fun exportContainsProvenanceAndNoCodecPayload() {
        val store = AndroidMayraPersonalMemoryStore(context)
        store.put(memory("1", "city", "Indore", 1))
        val export = store.exportText()
        assertTrue(export.contains("city: Indore"))
        assertTrue(export.contains("Source: chat / message-1"))
        assertFalse(export.contains("|"))
    }

    private fun memory(id: String, key: String, value: String, seconds: Long): MayraPersonalMemory {
        val time = Instant.parse("2026-07-28T10:00:00Z").plusSeconds(seconds)
        return MayraPersonalMemory(
            id = id,
            key = key,
            value = value,
            category = MayraMemoryCategory.PREFERENCE,
            provenance = MayraMemoryProvenance("chat", "message-1", time),
            createdAt = time,
            updatedAt = time,
            expiresAt = null,
            revision = 1
        )
    }
}
