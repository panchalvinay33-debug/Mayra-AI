package ai.mayra.app.memory

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.time.Instant

class AndroidMayraPersonalMemoryStore(
    context: Context,
    private val maxRecords: Int = 200,
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
    private val protector: MayraMemoryRecordProtector = AndroidKeystoreMayraMemoryProtector()
) : MayraPersonalMemoryStore {
    init { require(maxRecords in 1..2_000) }

    @Synchronized
    override fun all(): List<MayraPersonalMemory> {
        val rawRecords = preferences.getStringSet(KEY_RECORDS, emptySet()).orEmpty()
        var migrationNeeded = false
        val decoded = rawRecords.mapNotNull { raw ->
            val plaintext = when {
                protector.isProtected(raw) -> protector.unprotect(raw)
                else -> raw.also { migrationNeeded = true }
            }
            plaintext?.let(MayraMemoryCodec::decode)
        }.sortedByDescending { it.updatedAt }

        if (migrationNeeded && decoded.isNotEmpty()) {
            runCatching { persist(decoded) }
        }
        return decoded
    }

    @Synchronized
    override fun put(memory: MayraPersonalMemory) {
        val retained = all().filterNot { it.id == memory.id }.plus(memory)
            .sortedByDescending { it.updatedAt }
            .take(maxRecords)
        persist(retained)
    }

    @Synchronized
    override fun delete(id: String): Boolean {
        val before = all()
        val after = before.filterNot { it.id == id }
        if (after.size == before.size) return false
        persist(after)
        return true
    }

    @Synchronized
    override fun clear() {
        check(preferences.edit().remove(KEY_RECORDS).commit()) { "Unable to clear Mayra personal memory." }
    }

    fun exportText(): String = all().joinToString(separator = "\n\n") { memory ->
        buildString {
            append(memory.key).append(": ").append(memory.value).append('\n')
            append("Category: ").append(memory.category.name).append('\n')
            append("Source: ").append(memory.provenance.sourceType).append(" / ").append(memory.provenance.sourceReference).append('\n')
            append("Updated: ").append(memory.updatedAt)
            memory.expiresAt?.let { append('\n').append("Expires: ").append(it) }
        }
    }

    @Synchronized
    private fun persist(records: List<MayraPersonalMemory>) {
        val protectedRecords = records.map { protector.protect(MayraMemoryCodec.encode(it)) }.toSet()
        check(preferences.edit().putStringSet(KEY_RECORDS, protectedRecords).commit()) {
            "Unable to persist protected Mayra personal memory."
        }
    }

    private companion object {
        const val PREFS = "mayra_personal_memory_v1"
        const val KEY_RECORDS = "records"
    }
}

internal object MayraMemoryCodec {
    private const val VERSION = "1"

    fun encode(memory: MayraPersonalMemory): String = listOf(
        VERSION,
        b64(memory.id),
        b64(memory.key),
        b64(memory.value),
        memory.category.name,
        b64(memory.provenance.sourceType),
        b64(memory.provenance.sourceReference),
        memory.provenance.capturedAt.toEpochMilli().toString(),
        memory.createdAt.toEpochMilli().toString(),
        memory.updatedAt.toEpochMilli().toString(),
        memory.expiresAt?.toEpochMilli()?.toString().orEmpty(),
        memory.revision.toString()
    ).joinToString("|")

    fun decode(raw: String): MayraPersonalMemory? = runCatching {
        val p = raw.split('|')
        require(p.size == 12 && p[0] == VERSION)
        MayraPersonalMemory(
            id = unb64(p[1]),
            key = unb64(p[2]),
            value = unb64(p[3]),
            category = MayraMemoryCategory.valueOf(p[4]),
            provenance = MayraMemoryProvenance(
                sourceType = unb64(p[5]),
                sourceReference = unb64(p[6]),
                capturedAt = Instant.ofEpochMilli(p[7].toLong())
            ),
            createdAt = Instant.ofEpochMilli(p[8].toLong()),
            updatedAt = Instant.ofEpochMilli(p[9].toLong()),
            expiresAt = p[10].takeIf(String::isNotEmpty)?.toLong()?.let(Instant::ofEpochMilli),
            revision = p[11].toInt()
        )
    }.getOrNull()

    private fun b64(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private fun unb64(value: String): String = String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
}
