package ai.mayra.app.memory

import android.content.Context

enum class MayraMemoryStorageState { HEALTHY, MIGRATION_NEEDED, DEGRADED, EMPTY }

data class MayraMemoryStorageHealth(
    val state: MayraMemoryStorageState,
    val approvedProtected: Int,
    val approvedLegacy: Int,
    val approvedUnreadable: Int,
    val pendingProtected: Int,
    val pendingLegacy: Int,
    val pendingUnreadable: Int
) {
    val totalUnreadable: Int get() = approvedUnreadable + pendingUnreadable
    val totalLegacy: Int get() = approvedLegacy + pendingLegacy
}

/** Read-only diagnostics. It never deletes records or resets a Keystore key. */
class AndroidMayraMemoryStorageHealthReader(
    context: Context,
    private val approvedProtector: MayraMemoryRecordProtector = AndroidKeystoreMayraMemoryProtector(),
    private val pendingProtector: MayraMemoryRecordProtector = AndroidKeystoreMayraMemoryProtector(
        alias = "mayra.pending.memory.aes.v1"
    )
) {
    private val appContext = context.applicationContext

    fun read(): MayraMemoryStorageHealth {
        val approved = inspect(
            appContext.getSharedPreferences("mayra_personal_memory_v1", Context.MODE_PRIVATE)
                .getStringSet("records", emptySet()).orEmpty(),
            approvedProtector,
            MayraMemoryCodec::decode
        )
        val pending = inspect(
            appContext.getSharedPreferences("mayra_pending_memory_proposals_v1", Context.MODE_PRIVATE)
                .getStringSet("records", emptySet()).orEmpty(),
            pendingProtector,
            MayraPendingMemoryProposalCodec::decode
        )
        val state = when {
            approved.total + pending.total == 0 -> MayraMemoryStorageState.EMPTY
            approved.unreadable + pending.unreadable > 0 -> MayraMemoryStorageState.DEGRADED
            approved.legacy + pending.legacy > 0 -> MayraMemoryStorageState.MIGRATION_NEEDED
            else -> MayraMemoryStorageState.HEALTHY
        }
        return MayraMemoryStorageHealth(
            state,
            approved.protected,
            approved.legacy,
            approved.unreadable,
            pending.protected,
            pending.legacy,
            pending.unreadable
        )
    }

    private fun <T> inspect(
        records: Set<String>,
        protector: MayraMemoryRecordProtector,
        decoder: (String) -> T?
    ): Counts {
        var protected = 0
        var legacy = 0
        var unreadable = 0
        records.forEach { raw ->
            if (protector.isProtected(raw)) {
                val plain = protector.unprotect(raw)
                if (plain != null && decoder(plain) != null) protected++ else unreadable++
            } else if (decoder(raw) != null) {
                legacy++
            } else {
                unreadable++
            }
        }
        return Counts(protected, legacy, unreadable)
    }

    private data class Counts(val protected: Int, val legacy: Int, val unreadable: Int) {
        val total: Int get() = protected + legacy + unreadable
    }
}
