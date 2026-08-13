package ai.mayra.app.core

import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

enum class MayraActivityStatus {
    EXECUTED,
    CONFIRMATION_REQUIRED,
    CLARIFICATION_REQUIRED,
    BLOCKED,
    FAILED,
    DUPLICATE_BLOCKED
}

data class MayraActivityRecord(
    val id: String,
    val timestamp: Instant,
    val outcome: MayraRoutingOutcome,
    val disposition: MayraRouteDisposition,
    val status: MayraActivityStatus,
    val capability: MayraRequiredCapability,
    val idempotencyKey: String?,
    val detail: String
) {
    init {
        require(id.isNotBlank())
        require(detail.isNotBlank())
        require(idempotencyKey == null || outcome == MayraRoutingOutcome.ACT)
    }
}

fun interface MayraActivitySink {
    fun append(record: MayraActivityRecord)
}

class MayraInMemoryActivityLog : MayraActivitySink {
    private val records = CopyOnWriteArrayList<MayraActivityRecord>()

    override fun append(record: MayraActivityRecord) {
        records += record
    }

    fun snapshot(): List<MayraActivityRecord> = records.toList()
}

interface MayraIdempotencyStore {
    fun reserve(key: String): Boolean
    fun release(key: String)
}

class MayraInMemoryIdempotencyStore : MayraIdempotencyStore {
    private val keys = ConcurrentHashMap.newKeySet<String>()
    override fun reserve(key: String): Boolean = keys.add(key)
    override fun release(key: String) {
        keys.remove(key)
    }
}

object MayraActionIdempotency {
    fun key(message: String, decision: MayraRoutingDecision): String {
        require(decision.outcome == MayraRoutingOutcome.ACT)
        val canonical = message.trim().lowercase().replace(Regex("\\s+"), " ") + "|" +
            decision.requiredCapability.name + "|" + decision.matchedSignals.sorted().joinToString(",")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

class MayraActivityRecorder(
    private val sink: MayraActivitySink,
    private val clock: Clock = Clock.systemUTC()
) {
    fun record(
        plan: MayraRoutingPlan,
        status: MayraActivityStatus,
        detail: String,
        idempotencyKey: String? = null
    ) {
        val now = clock.instant()
        val seed = "$now|${plan.decision.outcome}|$status|$detail|${idempotencyKey.orEmpty()}"
        val id = MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it) }
        sink.append(
            MayraActivityRecord(
                id = id,
                timestamp = now,
                outcome = plan.decision.outcome,
                disposition = plan.disposition,
                status = status,
                capability = plan.decision.requiredCapability,
                idempotencyKey = idempotencyKey,
                detail = detail
            )
        )
    }
}