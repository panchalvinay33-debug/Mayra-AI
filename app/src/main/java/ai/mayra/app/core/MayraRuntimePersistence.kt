package ai.mayra.app.core

import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** Versioned, bounded persistence for runtime activity records. */
class MayraPersistentActivityLog(
    private val preferences: SharedPreferences,
    private val maxRecords: Int = 200
) : MayraActivitySink {
    init {
        require(maxRecords in 1..2_000)
    }

    @Synchronized
    override fun append(record: MayraActivityRecord) {
        val updated = (snapshot() + record).takeLast(maxRecords)
        preferences.edit()
            .putString(KEY, updated.joinToString("\n", transform = MayraActivityCodec::encode))
            .commit()
    }

    @Synchronized
    fun snapshot(): List<MayraActivityRecord> = preferences.getString(KEY, null)
        .orEmpty()
        .lineSequence()
        .filter(String::isNotBlank)
        .mapNotNull(MayraActivityCodec::decode)
        .toList()
        .takeLast(maxRecords)

    @Synchronized
    fun clear(): Boolean = preferences.edit().remove(KEY).commit()

    fun exportText(): String = snapshot().joinToString("\n") { record ->
        "${record.timestamp}\t${record.status}\t${record.outcome}\t${record.capability}\t${record.detail}"
    }

    private companion object {
        const val KEY = "mayra.runtime.activity.v1"
    }
}

private object MayraActivityCodec {
    private const val VERSION = "1"

    fun encode(record: MayraActivityRecord): String = listOf(
        VERSION,
        record.id,
        record.timestamp.toEpochMilli().toString(),
        record.outcome.name,
        record.disposition.name,
        record.status.name,
        record.capability.name,
        encodeText(record.idempotencyKey.orEmpty()),
        encodeText(record.detail)
    ).joinToString("|")

    fun decode(value: String): MayraActivityRecord? = runCatching {
        val parts = value.split('|')
        require(parts.size == 9 && parts[0] == VERSION)
        MayraActivityRecord(
            id = parts[1],
            timestamp = Instant.ofEpochMilli(parts[2].toLong()),
            outcome = MayraRoutingOutcome.valueOf(parts[3]),
            disposition = MayraRouteDisposition.valueOf(parts[4]),
            status = MayraActivityStatus.valueOf(parts[5]),
            capability = MayraRequiredCapability.valueOf(parts[6]),
            idempotencyKey = decodeText(parts[7]).ifBlank { null },
            detail = decodeText(parts[8])
        )
    }.getOrNull()

    private fun encodeText(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8
    )
}

data class MayraConfirmationToken(
    val value: String,
    val actionKey: String,
    val expiresAt: Instant
)

enum class MayraConfirmationResult {
    ACCEPTED,
    UNKNOWN,
    EXPIRED,
    MISMATCH,
    ALREADY_USED
}

/** One-time, expiring confirmation tokens bound to one exact action idempotency key. */
class MayraConfirmationTokenStore(
    private val clock: Clock = Clock.systemUTC(),
    private val ttl: Duration = Duration.ofMinutes(2)
) {
    init {
        require(!ttl.isNegative && !ttl.isZero)
        require(ttl <= Duration.ofHours(1))
    }

    private data class Stored(val actionKey: String, val expiresAt: Instant, var used: Boolean = false)
    private val tokens = ConcurrentHashMap<String, Stored>()

    fun issue(message: String, decision: MayraRoutingDecision): MayraConfirmationToken {
        require(decision.outcome == MayraRoutingOutcome.ACT && decision.requiresConfirmation)
        purgeExpired()
        val actionKey = MayraActionIdempotency.key(message, decision)
        val now = clock.instant()
        val seed = "$actionKey|$now|${tokens.size}"
        val value = MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val expiresAt = now.plus(ttl)
        tokens[value] = Stored(actionKey, expiresAt)
        return MayraConfirmationToken(value, actionKey, expiresAt)
    }

    fun consume(token: String, message: String, decision: MayraRoutingDecision): MayraConfirmationResult {
        val stored = tokens[token] ?: return MayraConfirmationResult.UNKNOWN
        synchronized(stored) {
            if (stored.used) return MayraConfirmationResult.ALREADY_USED
            if (!clock.instant().isBefore(stored.expiresAt)) {
                tokens.remove(token, stored)
                return MayraConfirmationResult.EXPIRED
            }
            val expected = runCatching { MayraActionIdempotency.key(message, decision) }.getOrNull()
                ?: return MayraConfirmationResult.MISMATCH
            if (expected != stored.actionKey) return MayraConfirmationResult.MISMATCH
            stored.used = true
            return MayraConfirmationResult.ACCEPTED
        }
    }

    fun purgeExpired(): Int {
        val now = clock.instant()
        val before = tokens.size
        tokens.entries.removeIf { !now.isBefore(it.value.expiresAt) }
        return before - tokens.size
    }
}
