package ai.mayra.app.memory

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.time.Instant

class AndroidMayraPendingMemoryProposalStore(
    context: Context,
    private val maxRecords: Int = 20,
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
    private val protector: MayraMemoryRecordProtector = AndroidKeystoreMayraMemoryProtector(PENDING_KEY_ALIAS)
) : MayraPendingMemoryProposalStore {
    init { require(maxRecords in 1..100) }

    @Synchronized
    override fun all(): List<MayraPendingMemoryProposal> {
        val rawRecords = preferences.getStringSet(KEY_RECORDS, emptySet()).orEmpty()
        var migrationNeeded = false
        val decoded = rawRecords.mapNotNull { raw ->
            val plaintext = when {
                protector.isProtected(raw) -> protector.unprotect(raw)
                else -> raw.also { migrationNeeded = true }
            }
            plaintext?.let(MayraPendingMemoryProposalCodec::decode)
        }.sortedByDescending { it.createdAt }
        if (migrationNeeded && decoded.isNotEmpty()) runCatching { persist(decoded) }
        return decoded
    }

    @Synchronized
    override fun put(proposal: MayraPendingMemoryProposal) {
        val records = all().filterNot { it.id == proposal.id }.plus(proposal)
            .sortedByDescending { it.createdAt }
            .take(maxRecords)
        persist(records)
    }

    @Synchronized
    override fun remove(id: String): MayraPendingMemoryProposal? {
        val records = all()
        val removed = records.firstOrNull { it.id == id } ?: return null
        persist(records.filterNot { it.id == id })
        return removed
    }

    @Synchronized
    override fun clear() {
        check(preferences.edit().remove(KEY_RECORDS).commit()) {
            "Unable to clear pending Mayra memory approvals."
        }
    }

    private fun persist(records: List<MayraPendingMemoryProposal>) {
        val protectedRecords = records.map {
            protector.protect(MayraPendingMemoryProposalCodec.encode(it))
        }.toSet()
        check(preferences.edit().putStringSet(KEY_RECORDS, protectedRecords).commit()) {
            "Unable to persist protected pending Mayra memory approval."
        }
    }

    private companion object {
        const val PREFS = "mayra_pending_memory_proposals_v1"
        const val KEY_RECORDS = "records"
        const val PENDING_KEY_ALIAS = "mayra.pending.memory.aes.v1"
    }
}

internal object MayraPendingMemoryProposalCodec {
    private const val VERSION = "1"

    fun encode(proposal: MayraPendingMemoryProposal): String = listOf(
        VERSION,
        b64(proposal.id),
        b64(proposal.candidate.key),
        b64(proposal.candidate.value),
        proposal.candidate.category.name,
        b64(proposal.candidate.provenance.sourceType),
        b64(proposal.candidate.provenance.sourceReference),
        proposal.candidate.provenance.capturedAt.toEpochMilli().toString(),
        proposal.candidate.expiresAt?.toEpochMilli()?.toString().orEmpty(),
        proposal.createdAt.toEpochMilli().toString(),
        proposal.conflictingMemoryId?.let(::b64).orEmpty()
    ).joinToString("|")

    fun decode(raw: String): MayraPendingMemoryProposal? = runCatching {
        val p = raw.split('|')
        require(p.size == 11 && p[0] == VERSION)
        MayraPendingMemoryProposal(
            id = unb64(p[1]),
            candidate = MayraMemoryCandidate(
                key = unb64(p[2]),
                value = unb64(p[3]),
                category = MayraMemoryCategory.valueOf(p[4]),
                provenance = MayraMemoryProvenance(
                    sourceType = unb64(p[5]),
                    sourceReference = unb64(p[6]),
                    capturedAt = Instant.ofEpochMilli(p[7].toLong())
                ),
                expiresAt = p[8].takeIf(String::isNotEmpty)?.toLong()?.let(Instant::ofEpochMilli)
            ),
            createdAt = Instant.ofEpochMilli(p[9].toLong()),
            conflictingMemoryId = p[10].takeIf(String::isNotEmpty)?.let(::unb64)
        )
    }.getOrNull()

    private fun b64(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    private fun unb64(value: String): String = String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
}
