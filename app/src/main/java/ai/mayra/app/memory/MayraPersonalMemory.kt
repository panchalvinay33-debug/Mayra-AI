package ai.mayra.app.memory

import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class MayraMemoryCategory { PREFERENCE, PROFILE, ROUTINE, RELATIONSHIP, PROJECT, OTHER }

enum class MayraMemorySensitivity { ALLOWED, SENSITIVE, PROHIBITED }

data class MayraMemoryProvenance(
    val sourceType: String,
    val sourceReference: String,
    val capturedAt: Instant
) {
    init {
        require(sourceType.isNotBlank())
        require(sourceReference.isNotBlank())
    }
}

data class MayraMemoryCandidate(
    val key: String,
    val value: String,
    val category: MayraMemoryCategory,
    val provenance: MayraMemoryProvenance,
    val expiresAt: Instant? = null
) {
    init {
        require(key.isNotBlank())
        require(value.isNotBlank())
    }
}

data class MayraPersonalMemory(
    val id: String,
    val key: String,
    val value: String,
    val category: MayraMemoryCategory,
    val provenance: MayraMemoryProvenance,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant?,
    val revision: Int
) {
    init {
        require(id.isNotBlank())
        require(key.isNotBlank())
        require(value.isNotBlank())
        require(revision >= 1)
        require(!updatedAt.isBefore(createdAt))
    }

    fun isExpired(now: Instant): Boolean = expiresAt?.let { !it.isAfter(now) } == true
}

sealed interface MayraMemoryProposalResult {
    data class ApprovalRequired(val proposalId: String, val candidate: MayraMemoryCandidate) : MayraMemoryProposalResult
    data class Rejected(val reason: String, val sensitivity: MayraMemorySensitivity) : MayraMemoryProposalResult
}

sealed interface MayraMemoryApprovalResult {
    data class Saved(val memory: MayraPersonalMemory) : MayraMemoryApprovalResult
    data class Rejected(val reason: String) : MayraMemoryApprovalResult
}

object MayraMemoryPrivacyPolicy {
    private val prohibitedPatterns = listOf(
        Regex("\\b(?:password|passcode|pin|otp)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:cvv|credit card|debit card)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:aadhaar|aadhar)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:private key|seed phrase|recovery phrase)\\b", RegexOption.IGNORE_CASE)
    )
    private val sensitivePatterns = listOf(
        Regex("\\b(?:medical|diagnosis|disease|pregnant|pregnancy|medicine)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:religion|caste|political|sexual orientation)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:salary|income|bank account)\\b", RegexOption.IGNORE_CASE)
    )

    fun classify(candidate: MayraMemoryCandidate): MayraMemorySensitivity {
        val text = "${candidate.key} ${candidate.value}"
        return when {
            prohibitedPatterns.any { it.containsMatchIn(text) } -> MayraMemorySensitivity.PROHIBITED
            sensitivePatterns.any { it.containsMatchIn(text) } -> MayraMemorySensitivity.SENSITIVE
            else -> MayraMemorySensitivity.ALLOWED
        }
    }

    fun rejectionReason(sensitivity: MayraMemorySensitivity): String = when (sensitivity) {
        MayraMemorySensitivity.PROHIBITED -> "Mayra will not store secrets, authentication data or high-risk financial identifiers."
        MayraMemorySensitivity.SENSITIVE -> "Sensitive personal information is excluded from automatic personal memory."
        MayraMemorySensitivity.ALLOWED -> ""
    }
}

interface MayraPersonalMemoryStore {
    fun all(): List<MayraPersonalMemory>
    fun put(memory: MayraPersonalMemory)
    fun delete(id: String): Boolean
    fun clear()
}

class MayraInMemoryPersonalMemoryStore : MayraPersonalMemoryStore {
    private val records = ConcurrentHashMap<String, MayraPersonalMemory>()
    override fun all(): List<MayraPersonalMemory> = records.values.toList()
    override fun put(memory: MayraPersonalMemory) { records[memory.id] = memory }
    override fun delete(id: String): Boolean = records.remove(id) != null
    override fun clear() = records.clear()
}

class MayraPersonalMemoryManager(
    private val store: MayraPersonalMemoryStore,
    private val clock: Clock = Clock.systemUTC()
) {
    private val pending = ConcurrentHashMap<String, MayraMemoryCandidate>()

    fun propose(candidate: MayraMemoryCandidate): MayraMemoryProposalResult {
        val sensitivity = MayraMemoryPrivacyPolicy.classify(candidate)
        if (sensitivity != MayraMemorySensitivity.ALLOWED) {
            return MayraMemoryProposalResult.Rejected(
                MayraMemoryPrivacyPolicy.rejectionReason(sensitivity),
                sensitivity
            )
        }
        val proposalId = stableId("proposal|${candidate.key}|${candidate.value}|${candidate.provenance.sourceReference}|${clock.instant()}")
        pending[proposalId] = candidate
        return MayraMemoryProposalResult.ApprovalRequired(proposalId, candidate)
    }

    fun approve(proposalId: String): MayraMemoryApprovalResult {
        val candidate = pending.remove(proposalId)
            ?: return MayraMemoryApprovalResult.Rejected("This memory proposal is missing, expired or already handled.")
        val now = clock.instant()
        if (candidate.expiresAt?.let { !it.isAfter(now) } == true) {
            return MayraMemoryApprovalResult.Rejected("This memory proposal has already expired.")
        }
        val normalizedKey = normalize(candidate.key)
        val existing = activeMemories().firstOrNull { normalize(it.key) == normalizedKey }
        val memory = if (existing == null) {
            MayraPersonalMemory(
                id = stableId("memory|$normalizedKey|${candidate.provenance.sourceReference}|$now"),
                key = candidate.key.trim(),
                value = candidate.value.trim(),
                category = candidate.category,
                provenance = candidate.provenance,
                createdAt = now,
                updatedAt = now,
                expiresAt = candidate.expiresAt,
                revision = 1
            )
        } else {
            existing.copy(
                value = candidate.value.trim(),
                category = candidate.category,
                provenance = candidate.provenance,
                updatedAt = now,
                expiresAt = candidate.expiresAt,
                revision = existing.revision + 1
            )
        }
        store.put(memory)
        return MayraMemoryApprovalResult.Saved(memory)
    }

    fun reject(proposalId: String): Boolean = pending.remove(proposalId) != null

    fun update(id: String, newValue: String, provenance: MayraMemoryProvenance): MayraPersonalMemory? {
        require(newValue.isNotBlank())
        val current = activeMemories().firstOrNull { it.id == id } ?: return null
        val candidate = MayraMemoryCandidate(current.key, newValue, current.category, provenance, current.expiresAt)
        if (MayraMemoryPrivacyPolicy.classify(candidate) != MayraMemorySensitivity.ALLOWED) return null
        val updated = current.copy(
            value = newValue.trim(),
            provenance = provenance,
            updatedAt = clock.instant(),
            revision = current.revision + 1
        )
        store.put(updated)
        return updated
    }

    fun delete(id: String): Boolean = store.delete(id)
    fun clear() = store.clear()

    fun activeMemories(): List<MayraPersonalMemory> {
        val now = clock.instant()
        val all = store.all()
        all.filter { it.isExpired(now) }.forEach { store.delete(it.id) }
        return all.filterNot { it.isExpired(now) }.sortedByDescending { it.updatedAt }
    }

    fun retrieve(query: String, limit: Int = 5): List<MayraPersonalMemory> {
        require(limit in 1..50)
        val terms = tokenize(query)
        if (terms.isEmpty()) return emptyList()
        return activeMemories()
            .map { memory ->
                val haystack = tokenize("${memory.key} ${memory.value}")
                val overlap = terms.count { it in haystack }
                memory to overlap
            }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<MayraPersonalMemory, Int>> { it.second }
                .thenByDescending { it.first.updatedAt }
                .thenBy { it.first.id })
            .take(limit)
            .map { it.first }
    }

    fun pendingCount(): Int = pending.size

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    private fun tokenize(value: String): Set<String> = normalize(value)
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 2 }
        .toSet()

    private fun stableId(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
