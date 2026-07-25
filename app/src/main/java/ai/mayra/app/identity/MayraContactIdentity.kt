package ai.mayra.app.identity

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

enum class MayraContactTrust { STANDARD, TRUSTED, SENSITIVE }
enum class MayraCommunicationChannel { PHONE, SMS, WHATSAPP, ASK_EVERY_TIME }

data class MayraContactIdentity(
    val id: String = UUID.randomUUID().toString(),
    val canonicalContactName: String,
    val relationship: String? = null,
    val aliases: Set<String> = emptySet(),
    val preferredChannel: MayraCommunicationChannel = MayraCommunicationChannel.ASK_EVERY_TIME,
    val trust: MayraContactTrust = MayraContactTrust.STANDARD,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank())
        require(canonicalContactName.trim().isNotBlank())
    }

    val normalizedTerms: Set<String>
        get() = buildSet {
            add(normalizeIdentityTerm(canonicalContactName))
            relationship?.let { add(normalizeIdentityTerm(it)) }
            aliases.forEach { add(normalizeIdentityTerm(it)) }
        }.filter(String::isNotBlank).toSet()
}

enum class MayraIdentityMatchConfidence { EXACT, STRONG_PARTIAL }

sealed interface MayraIdentityResolution {
    data class Resolved(
        val identity: MayraContactIdentity,
        val matchedTerm: String,
        val exact: Boolean,
        val confidence: MayraIdentityMatchConfidence = if (exact) {
            MayraIdentityMatchConfidence.EXACT
        } else {
            MayraIdentityMatchConfidence.STRONG_PARTIAL
        }
    ) : MayraIdentityResolution

    data class Ambiguous(
        val query: String,
        val candidates: List<MayraContactIdentity>
    ) : MayraIdentityResolution

    data class Unmapped(val query: String) : MayraIdentityResolution
}

/**
 * Conservative owner-defined identity resolver. Exact aliases are preferred. Partial matching is
 * allowed only for strong, sufficiently long token-prefix matches and never for short guesses.
 */
class MayraContactIdentityEngine(
    private val identities: () -> List<MayraContactIdentity>
) {
    fun resolve(query: String): MayraIdentityResolution {
        val normalized = normalizeIdentityTerm(query).take(MAX_QUERY_LENGTH)
        if (!ContactIdentitySafetyPolicy.isUsableQuery(normalized)) {
            return MayraIdentityResolution.Unmapped(query)
        }

        val all = identities().take(MAX_IDENTITIES).distinctBy(MayraContactIdentity::id)
        val exact = all.filter { normalized in it.normalizedTerms }
        if (exact.size == 1) {
            return MayraIdentityResolution.Resolved(
                identity = exact.single(),
                matchedTerm = normalized,
                exact = true
            )
        }
        if (exact.size > 1) return ambiguous(query, exact)

        if (!ContactIdentitySafetyPolicy.partialMatchingAllowed(normalized)) {
            return MayraIdentityResolution.Unmapped(query)
        }

        val scored = all.mapNotNull { identity ->
            val best = identity.normalizedTerms
                .mapNotNull { term -> ContactIdentitySafetyPolicy.partialScore(normalized, term) }
                .maxOrNull()
                ?: return@mapNotNull null
            identity to best
        }.sortedWith(
            compareByDescending<Pair<MayraContactIdentity, Int>> { it.second }
                .thenBy { it.first.canonicalContactName.lowercase(Locale.ROOT) }
        )

        if (scored.isEmpty()) return MayraIdentityResolution.Unmapped(query)
        val bestScore = scored.first().second
        val best = scored.filter { it.second == bestScore }.map(Pair<MayraContactIdentity, Int>::first)
        if (best.size != 1) return ambiguous(query, best)

        val runnerUp = scored.getOrNull(1)?.second
        if (runnerUp != null && bestScore - runnerUp < MIN_SCORE_MARGIN) {
            return ambiguous(query, scored.filter { bestScore - it.second < MIN_SCORE_MARGIN }.map { it.first })
        }

        return MayraIdentityResolution.Resolved(
            identity = best.single(),
            matchedTerm = normalized,
            exact = false,
            confidence = MayraIdentityMatchConfidence.STRONG_PARTIAL
        )
    }

    private fun ambiguous(query: String, candidates: List<MayraContactIdentity>) =
        MayraIdentityResolution.Ambiguous(
            query = query.take(MAX_QUERY_LENGTH),
            candidates = candidates.distinctBy(MayraContactIdentity::id)
                .sortedBy { it.canonicalContactName.lowercase(Locale.ROOT) }
                .take(MAX_AMBIGUOUS_CANDIDATES)
        )

    private companion object {
        const val MAX_IDENTITIES = 1_000
        const val MAX_QUERY_LENGTH = 120
        const val MAX_AMBIGUOUS_CANDIDATES = 8
        const val MIN_SCORE_MARGIN = 10
    }
}

object ContactIdentitySafetyPolicy {
    private const val MIN_PARTIAL_LENGTH = 4
    private const val MIN_CONTAINS_LENGTH = 6

    fun isUsableQuery(normalized: String): Boolean =
        normalized.isNotBlank() && normalized.any(Char::isLetter) && normalized.length <= 120

    fun partialMatchingAllowed(normalized: String): Boolean =
        normalized.length >= MIN_PARTIAL_LENGTH && normalized.any(Char::isLetter)

    fun partialScore(query: String, term: String): Int? {
        if (term.isBlank() || term == query) return null
        return when {
            term.startsWith("$query ") || term.startsWith(query) -> 90
            query.startsWith("$term ") || query.startsWith(term) -> 80
            query.length >= MIN_CONTAINS_LENGTH && term.contains(" $query ") -> 70
            query.length >= MIN_CONTAINS_LENGTH && term.endsWith(" $query") -> 70
            else -> null
        }
    }
}

class MayraContactIdentityStore(context: Context, private val maxEntries: Int = 500) {
    init { require(maxEntries in 20..5_000) }
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<MayraContactIdentity> = runCatching {
        val array = JSONArray(preferences.getString(KEY_IDENTITIES, "[]") ?: "[]")
        buildList {
            repeat(array.length().coerceAtMost(maxEntries)) { index ->
                runCatching { array.getJSONObject(index).toIdentity() }.getOrNull()?.let(::add)
            }
        }.distinctBy(MayraContactIdentity::id)
            .sortedBy { it.canonicalContactName.lowercase(Locale.ROOT) }
    }.getOrDefault(emptyList())

    @Synchronized
    fun upsert(identity: MayraContactIdentity) {
        val clean = identity.copy(
            canonicalContactName = normalizeDisplayTerm(identity.canonicalContactName).take(120),
            relationship = identity.relationship?.let(::normalizeDisplayTerm)?.take(80)?.takeIf(String::isNotBlank),
            aliases = identity.aliases.map(::normalizeDisplayTerm)
                .filter(String::isNotBlank)
                .filterNot { normalizeIdentityTerm(it) == normalizeIdentityTerm(identity.canonicalContactName) }
                .take(20)
                .toSet(),
            notes = identity.notes?.let(::sanitizeIdentityText)?.take(300)?.takeIf(String::isNotBlank),
            updatedAt = System.currentTimeMillis()
        )
        require(clean.canonicalContactName.isNotBlank())
        val updated = (all().filterNot { it.id == clean.id } + clean)
            .sortedByDescending(MayraContactIdentity::updatedAt)
            .take(maxEntries)
        saveAll(updated)
    }

    @Synchronized
    fun remove(id: String): Boolean {
        val before = all()
        val after = before.filterNot { it.id == id }
        if (before.size == after.size) return false
        saveAll(after)
        return true
    }

    @Synchronized
    fun clear(): Int = all().size.also { preferences.edit().remove(KEY_IDENTITIES).commit() }

    fun engine(): MayraContactIdentityEngine = MayraContactIdentityEngine(::all)

    private fun saveAll(values: List<MayraContactIdentity>) {
        val array = JSONArray()
        values.take(maxEntries).sortedBy(MayraContactIdentity::canonicalContactName).forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_IDENTITIES, array.toString()).commit()
    }

    private fun MayraContactIdentity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("canonical", canonicalContactName)
        .put("relationship", relationship)
        .put("aliases", JSONArray(aliases.toList()))
        .put("channel", preferredChannel.name)
        .put("trust", trust.name)
        .put("notes", notes)
        .put("updatedAt", updatedAt)

    private fun JSONObject.toIdentity(): MayraContactIdentity {
        val aliasArray = optJSONArray("aliases") ?: JSONArray()
        val aliases = buildSet {
            repeat(aliasArray.length().coerceAtMost(20)) {
                normalizeDisplayTerm(aliasArray.optString(it)).takeIf(String::isNotBlank)?.let(::add)
            }
        }
        return MayraContactIdentity(
            id = optString("id").ifBlank { UUID.randomUUID().toString() }.take(120),
            canonicalContactName = normalizeDisplayTerm(optString("canonical")).take(120),
            relationship = normalizeDisplayTerm(optString("relationship")).take(80).takeIf(String::isNotBlank),
            aliases = aliases,
            preferredChannel = enumValueOrDefault(optString("channel"), MayraCommunicationChannel.ASK_EVERY_TIME),
            trust = enumValueOrDefault(optString("trust"), MayraContactTrust.STANDARD),
            notes = sanitizeIdentityText(optString("notes")).take(300).takeIf(String::isNotBlank),
            updatedAt = optLong("updatedAt", 0L).coerceAtLeast(0L)
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private companion object {
        const val PREFS = "mayra_contact_identities"
        const val KEY_IDENTITIES = "identities_json"
    }
}

internal fun normalizeIdentityTerm(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
    .take(120)

private fun normalizeDisplayTerm(value: String): String = sanitizeIdentityText(value)
    .replace(Regex("\\s+"), " ")
    .trim()

private fun sanitizeIdentityText(value: String): String = value
    .replace(Regex("[\\p{Cc}&&[^\\n\\t]]"), " ")
    .replace('\n', ' ')
    .replace('\r', ' ')
    .replace('\t', ' ')
    .trim()

internal fun identitySafetySummary(identity: MayraContactIdentity): String = buildString {
    append(identity.relationship ?: identity.canonicalContactName)
    append(" · ")
    append(identity.trust.name.lowercase())
    append(" · ")
    append(identity.preferredChannel.name.replace('_', ' ').lowercase())
}.take(240)
