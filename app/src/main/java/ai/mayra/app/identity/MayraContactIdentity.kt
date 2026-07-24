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

sealed interface MayraIdentityResolution {
    data class Resolved(
        val identity: MayraContactIdentity,
        val matchedTerm: String,
        val exact: Boolean
    ) : MayraIdentityResolution

    data class Ambiguous(
        val query: String,
        val candidates: List<MayraContactIdentity>
    ) : MayraIdentityResolution

    data class Unmapped(val query: String) : MayraIdentityResolution
}

class MayraContactIdentityEngine(
    private val identities: () -> List<MayraContactIdentity>
) {
    fun resolve(query: String): MayraIdentityResolution {
        val normalized = normalizeIdentityTerm(query)
        if (normalized.isBlank()) return MayraIdentityResolution.Unmapped(query)
        val all = identities()
        val exact = all.filter { normalized in it.normalizedTerms }
        if (exact.size == 1) return MayraIdentityResolution.Resolved(exact.single(), normalized, true)
        if (exact.size > 1) return MayraIdentityResolution.Ambiguous(query, exact.sortedBy { it.canonicalContactName })

        val partial = all.filter { identity ->
            identity.normalizedTerms.any { term -> term.contains(normalized) || normalized.contains(term) }
        }
        return when (partial.size) {
            0 -> MayraIdentityResolution.Unmapped(query)
            1 -> MayraIdentityResolution.Resolved(partial.single(), normalized, false)
            else -> MayraIdentityResolution.Ambiguous(query, partial.sortedBy { it.canonicalContactName })
        }
    }
}

class MayraContactIdentityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<MayraContactIdentity> = runCatching {
        val array = JSONArray(preferences.getString(KEY_IDENTITIES, "[]") ?: "[]")
        buildList {
            repeat(array.length()) { index -> add(array.getJSONObject(index).toIdentity()) }
        }.sortedBy { it.canonicalContactName.lowercase(Locale.ROOT) }
    }.getOrDefault(emptyList())

    @Synchronized
    fun upsert(identity: MayraContactIdentity) {
        val clean = identity.copy(
            canonicalContactName = identity.canonicalContactName.trim().take(120),
            relationship = identity.relationship?.trim()?.take(80)?.takeIf(String::isNotBlank),
            aliases = identity.aliases.map(::normalizeDisplayTerm).filter(String::isNotBlank).take(20).toSet(),
            notes = identity.notes?.trim()?.take(300)?.takeIf(String::isNotBlank),
            updatedAt = System.currentTimeMillis()
        )
        val updated = all().filterNot { it.id == clean.id } + clean
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
    fun clear(): Int = all().size.also { preferences.edit().remove(KEY_IDENTITIES).apply() }

    fun engine(): MayraContactIdentityEngine = MayraContactIdentityEngine(::all)

    private fun saveAll(values: List<MayraContactIdentity>) {
        val array = JSONArray()
        values.sortedBy(MayraContactIdentity::canonicalContactName).forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_IDENTITIES, array.toString()).apply()
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
        val aliases = buildSet { repeat(aliasArray.length()) { add(aliasArray.optString(it)) } }
        return MayraContactIdentity(
            id = optString("id").ifBlank { UUID.randomUUID().toString() },
            canonicalContactName = optString("canonical"),
            relationship = optString("relationship").takeIf(String::isNotBlank),
            aliases = aliases,
            preferredChannel = enumValueOrDefault(optString("channel"), MayraCommunicationChannel.ASK_EVERY_TIME),
            trust = enumValueOrDefault(optString("trust"), MayraContactTrust.STANDARD),
            notes = optString("notes").takeIf(String::isNotBlank),
            updatedAt = optLong("updatedAt", 0L)
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

private fun normalizeDisplayTerm(value: String): String = value.trim().replace(Regex("\\s+"), " ")

internal fun identitySafetySummary(identity: MayraContactIdentity): String = buildString {
    append(identity.relationship ?: identity.canonicalContactName)
    append(" · ")
    append(identity.trust.name.lowercase())
    append(" · ")
    append(identity.preferredChannel.name.replace('_', ' ').lowercase())
}
