package ai.mayra.app.background

import android.content.Context
import java.util.UUID

enum class PendingActionType { CALL, MESSAGE, REMINDER, SECURITY_REVIEW, OPEN_APP, OTHER }
enum class PendingActionState { WAITING, APPROVED, REJECTED, EXECUTED, EXPIRED, FAILED }
enum class AuditOutcome { CREATED, APPROVED, REJECTED, EXECUTED, EXPIRED, FAILED, CLEARED }

data class PendingAction(
    val id: String = UUID.randomUUID().toString(),
    val type: PendingActionType,
    val title: String,
    val payload: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val state: PendingActionState = PendingActionState.WAITING,
    val requiresConfirmation: Boolean = true,
    val source: String = "mayra",
    val lastError: String? = null
) {
    fun isExpired(now: Long): Boolean = expiresAt?.let { now >= it } == true
    fun isPending(now: Long): Boolean = state == PendingActionState.WAITING && !isExpired(now)
}

data class AuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val actionId: String?,
    val actionType: String,
    val outcome: AuditOutcome,
    val summary: String,
    val timestamp: Long
)

class PendingActionStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val audit = TrustAuditStore(appContext)

    @Synchronized
    fun add(action: PendingAction): PendingAction {
        save(snapshot().filterNot { it.id == action.id } + action)
        audit.append(
            AuditEntry(
                actionId = action.id,
                actionType = action.type.name,
                outcome = AuditOutcome.CREATED,
                summary = action.title,
                timestamp = action.createdAt
            )
        )
        return action
    }

    fun snapshot(): List<PendingAction> = preferences.getStringSet(KEY_ACTIONS, emptySet()).orEmpty()
        .mapNotNull(::decode)
        .sortedByDescending(PendingAction::createdAt)

    fun waiting(now: Long = System.currentTimeMillis()): List<PendingAction> {
        expireDue(now)
        return snapshot().filter { it.isPending(now) }
    }

    @Synchronized
    fun approve(id: String, now: Long = System.currentTimeMillis()): PendingAction? = transition(
        id = id,
        now = now,
        allowedFrom = setOf(PendingActionState.WAITING),
        target = PendingActionState.APPROVED,
        outcome = AuditOutcome.APPROVED
    )

    @Synchronized
    fun reject(id: String, now: Long = System.currentTimeMillis()): PendingAction? = transition(
        id = id,
        now = now,
        allowedFrom = setOf(PendingActionState.WAITING, PendingActionState.APPROVED),
        target = PendingActionState.REJECTED,
        outcome = AuditOutcome.REJECTED
    )

    @Synchronized
    fun markExecuted(id: String, now: Long = System.currentTimeMillis()): PendingAction? = transition(
        id = id,
        now = now,
        allowedFrom = setOf(PendingActionState.APPROVED),
        target = PendingActionState.EXECUTED,
        outcome = AuditOutcome.EXECUTED
    )

    @Synchronized
    fun markFailed(id: String, error: String, now: Long = System.currentTimeMillis()): PendingAction? {
        var changed: PendingAction? = null
        val updated = snapshot().map { current ->
            if (current.id != id || current.state !in setOf(PendingActionState.APPROVED, PendingActionState.WAITING)) current
            else current.copy(state = PendingActionState.FAILED, lastError = error.take(200)).also { changed = it }
        }
        if (changed != null) {
            save(updated)
            audit.append(AuditEntry(actionId = id, actionType = changed!!.type.name, outcome = AuditOutcome.FAILED, summary = error.take(160), timestamp = now))
        }
        return changed
    }

    @Synchronized
    fun expireDue(now: Long = System.currentTimeMillis()): Int {
        var count = 0
        val updated = snapshot().map { action ->
            if (action.state == PendingActionState.WAITING && action.isExpired(now)) {
                count++
                audit.append(AuditEntry(actionId = action.id, actionType = action.type.name, outcome = AuditOutcome.EXPIRED, summary = action.title, timestamp = now))
                action.copy(state = PendingActionState.EXPIRED)
            } else action
        }
        if (count > 0) save(updated)
        return count
    }

    @Synchronized
    fun prune(maxEntries: Int = 200) {
        require(maxEntries > 0)
        val retained = snapshot().sortedWith(
            compareByDescending<PendingAction> { it.state == PendingActionState.WAITING }
                .thenByDescending { it.createdAt }
        ).take(maxEntries)
        save(retained)
    }

    private fun transition(
        id: String,
        now: Long,
        allowedFrom: Set<PendingActionState>,
        target: PendingActionState,
        outcome: AuditOutcome
    ): PendingAction? {
        var changed: PendingAction? = null
        val updated = snapshot().map { current ->
            if (current.id != id || current.state !in allowedFrom || current.isExpired(now)) current
            else current.copy(state = target, lastError = null).also { changed = it }
        }
        changed?.let {
            save(updated)
            audit.append(AuditEntry(actionId = it.id, actionType = it.type.name, outcome = outcome, summary = it.title, timestamp = now))
        }
        return changed
    }

    private fun save(actions: List<PendingAction>) {
        preferences.edit().putStringSet(KEY_ACTIONS, actions.map(::encode).toSet()).apply()
    }

    private fun encode(action: PendingAction): String = listOf(
        action.id, action.type.name, action.title, action.payload, action.createdAt,
        action.expiresAt ?: -1L, action.state.name, action.requiresConfirmation,
        action.source, action.lastError.orEmpty()
    ).joinToString(SEPARATOR) { it.toString().replace(SEPARATOR, " ") }

    private fun decode(value: String): PendingAction? {
        val parts = value.split(SEPARATOR)
        if (parts.size != 10) return null
        return PendingAction(
            id = parts[0],
            type = runCatching { PendingActionType.valueOf(parts[1]) }.getOrNull() ?: return null,
            title = parts[2],
            payload = parts[3],
            createdAt = parts[4].toLongOrNull() ?: return null,
            expiresAt = parts[5].toLongOrNull()?.takeIf { it >= 0L },
            state = runCatching { PendingActionState.valueOf(parts[6]) }.getOrNull() ?: return null,
            requiresConfirmation = parts[7].toBooleanStrictOrNull() ?: return null,
            source = parts[8],
            lastError = parts[9].ifBlank { null }
        )
    }

    private companion object {
        const val FILE_NAME = "mayra_pending_actions"
        const val KEY_ACTIONS = "actions"
        const val SEPARATOR = "\u001D"
    }
}

class TrustAuditStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun append(entry: AuditEntry) {
        val retained = (snapshot() + entry).sortedByDescending(AuditEntry::timestamp).take(MAX_ENTRIES)
        preferences.edit().putStringSet(KEY_ENTRIES, retained.map(::encode).toSet()).apply()
    }

    fun snapshot(limit: Int = MAX_ENTRIES): List<AuditEntry> = preferences
        .getStringSet(KEY_ENTRIES, emptySet()).orEmpty()
        .mapNotNull(::decode)
        .sortedByDescending(AuditEntry::timestamp)
        .take(limit.coerceIn(1, MAX_ENTRIES))

    @Synchronized
    fun clear(now: Long = System.currentTimeMillis()) {
        preferences.edit().remove(KEY_ENTRIES).apply()
        append(AuditEntry(actionId = null, actionType = "AUDIT", outcome = AuditOutcome.CLEARED, summary = "Audit history cleared by user", timestamp = now))
    }

    private fun encode(entry: AuditEntry): String = listOf(
        entry.id, entry.actionId.orEmpty(), entry.actionType, entry.outcome.name,
        entry.summary, entry.timestamp
    ).joinToString(SEPARATOR) { it.toString().replace(SEPARATOR, " ") }

    private fun decode(value: String): AuditEntry? {
        val parts = value.split(SEPARATOR)
        if (parts.size != 6) return null
        return AuditEntry(
            id = parts[0],
            actionId = parts[1].ifBlank { null },
            actionType = parts[2],
            outcome = runCatching { AuditOutcome.valueOf(parts[3]) }.getOrNull() ?: return null,
            summary = parts[4],
            timestamp = parts[5].toLongOrNull() ?: return null
        )
    }

    private companion object {
        const val FILE_NAME = "mayra_trust_audit"
        const val KEY_ENTRIES = "entries"
        const val SEPARATOR = "\u001C"
        const val MAX_ENTRIES = 500
    }
}
