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
class AndroidMayraProtectedMemoryStoreTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun newWritesAreProtectedAndReadable() {
        val prefs = context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE)
        val store = AndroidMayraPersonalMemoryStore(context, preferences = prefs, protector = TestProtector())
        store.put(memory("1", "language", "हिंदी"))

        val raw = prefs.getStringSet("records", emptySet()).orEmpty().single()
        assertTrue(raw.startsWith("test:"))
        assertFalse(raw.contains("हिंदी"))
        assertEquals("हिंदी", store.all().single().value)
    }

    @Test fun plaintextLegacyRecordMigratesOnRead() {
        val prefs = context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("records", setOf(MayraMemoryCodec.encode(memory("1", "city", "Indore")))).commit()

        val store = AndroidMayraPersonalMemoryStore(context, preferences = prefs, protector = TestProtector())
        assertEquals("Indore", store.all().single().value)
        assertTrue(prefs.getStringSet("records", emptySet()).orEmpty().single().startsWith("test:"))
    }

    @Test fun protectionFailureLeavesPreviousRecordsUntouched() {
        val prefs = context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE)
        val good = AndroidMayraPersonalMemoryStore(context, preferences = prefs, protector = TestProtector())
        good.put(memory("1", "city", "Indore"))
        val before = prefs.getStringSet("records", emptySet()).orEmpty().toSet()

        val failing = AndroidMayraPersonalMemoryStore(context, preferences = prefs, protector = FailingProtector())
        runCatching { failing.put(memory("2", "food", "poha")) }

        assertEquals(before, prefs.getStringSet("records", emptySet()).orEmpty())
        assertEquals("Indore", good.all().single().value)
    }

    @Test fun unreadableProtectedRecordIsSkipped() {
        val prefs = context.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("records", setOf("test:broken")).commit()
        assertTrue(AndroidMayraPersonalMemoryStore(context, preferences = prefs, protector = TestProtector()).all().isEmpty())
    }

    private fun memory(id: String, key: String, value: String): MayraPersonalMemory {
        val now = Instant.parse("2026-07-28T10:00:00Z")
        return MayraPersonalMemory(
            id, key, value, MayraMemoryCategory.PREFERENCE,
            MayraMemoryProvenance("test", "protected-store", now),
            now, now, null, 1
        )
    }

    private class TestProtector : MayraMemoryRecordProtector {
        override fun protect(plaintext: String): String = "test:" + plaintext.reversed()
        override fun unprotect(payload: String): String? = payload.removePrefix("test:").reversed().takeIf { payload.startsWith("test:") }
        override fun isProtected(payload: String): Boolean = payload.startsWith("test:")
    }

    private class FailingProtector : MayraMemoryRecordProtector {
        override fun protect(plaintext: String): String = error("simulated encryption failure")
        override fun unprotect(payload: String): String? = TestProtector().unprotect(payload)
        override fun isProtected(payload: String): Boolean = payload.startsWith("test:")
    }
}
