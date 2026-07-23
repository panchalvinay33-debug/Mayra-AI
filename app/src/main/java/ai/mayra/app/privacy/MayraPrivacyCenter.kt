package ai.mayra.app.privacy

import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class PersonalDataCategory {
    CONVERSATION,
    VOICE_TRANSCRIPT,
    CONTACT_REFERENCE,
    CALENDAR,
    LOCATION,
    FILE_METADATA,
    APP_USAGE,
    HABIT,
    NOTE,
    REMINDER,
    PLUGIN_DATA,
    DIAGNOSTIC,
    SENSITIVE_REFERENCE
}

enum class RetentionMode { SESSION_ONLY, DAYS, UNTIL_USER_DELETES, NEVER_STORE }
enum class ConsentState { GRANTED, DENIED, ASK_EVERY_TIME, NOT_SET }
enum class PrivacyRisk { LOW, MODERATE, HIGH, CRITICAL }

data class RetentionPolicy(
    val category: PersonalDataCategory,
    val mode: RetentionMode,
    val retentionDays: Int? = null,
    val includeInSearch: Boolean = true,
    val allowProactiveUse: Boolean = true,
    val allowPluginAccess: Boolean = false
) {
    init {
        if (mode == RetentionMode.DAYS) require(retentionDays in 1..3650)
        else require(retentionDays == null)
        if (mode == RetentionMode.NEVER_STORE) {
            require(!includeInSearch)
            require(!allowProactiveUse)
            require(!allowPluginAccess)
        }
    }

    fun expiresAt(createdAt: Long): Long? = when (mode) {
        RetentionMode.SESSION_ONLY -> createdAt
        RetentionMode.DAYS -> createdAt + retentionDays!! * DAY_MS
        RetentionMode.UNTIL_USER_DELETES -> null
        RetentionMode.NEVER_STORE -> createdAt
    }

    companion object { const val DAY_MS = 24L * 60 * 60 * 1000 }
}

data class ConsentRecord(
    val id: String = UUID.randomUUID().toString(),
    val purpose: String,
    val category: PersonalDataCategory,
    val state: ConsentState,
    val grantedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val source: String = "user"
) {
    init {
        require(purpose.isNotBlank())
        if (state == ConsentState.GRANTED) require(grantedAt != null)
    }

    fun active(now: Long = System.currentTimeMillis()): Boolean =
        state == ConsentState.GRANTED && (expiresAt == null || expiresAt > now)
}

data class PrivacyAuditEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String,
    val category: PersonalDataCategory,
    val purpose: String,
    val result: String,
    val actor: String,
    val sensitive: Boolean,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(operation.isNotBlank())
        require(purpose.isNotBlank())
        require(actor.isNotBlank())
        require(metadata.size <= 20)
    }
}

data class PrivacyDecision(
    val allowed: Boolean,
    val requiresConfirmation: Boolean,
    val risk: PrivacyRisk,
    val reason: String,
    val policy: RetentionPolicy,
    val consent: ConsentState
)

data class PrivacySnapshot(
    val policies: List<RetentionPolicy>,
    val consentRecords: List<ConsentRecord>,
    val recentAudit: List<PrivacyAuditEvent>,
    val deniedPurposes: Int,
    val expiringConsents: Int,
    val capturedAt: Long
)

class SensitiveTextRedactor {
    private val phone = Regex("(?<!\\d)(?:\\+?91[- ]?)?[6-9]\\d{9}(?!\\d)")
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val card = Regex("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)")
    private val upi = Regex("(?i)\\b[a-z0-9._-]{2,}@[a-z]{2,}\\b")
    private val otp = Regex("(?i)\\b(?:otp|one[ -]?time password|verification code)\\D{0,12}(\\d{4,8})\\b")
    private val pin = Regex("(?i)\\b(?:pin|passcode|password)\\D{0,8}([A-Za-z0-9@#$%^&*!_-]{4,})")

    fun redact(text: String): String {
        var result = text
        result = otp.replace(result) { match -> match.value.replace(match.groupValues[1], "[REDACTED_CODE]") }
        result = pin.replace(result) { match -> match.value.replace(match.groupValues[1], "[REDACTED_SECRET]") }
        result = email.replace(result, "[REDACTED_EMAIL]")
        result = phone.replace(result, "[REDACTED_PHONE]")
        result = upi.replace(result, "[REDACTED_UPI]")
        result = card.replace(result) { match ->
            val digits = match.value.filter(Char::isDigit)
            if (passesLuhn(digits)) "[REDACTED_CARD]" else match.value
        }
        return result
    }

    fun containsLikelySecret(text: String): Boolean =
        otp.containsMatchIn(text) || pin.containsMatchIn(text) ||
            card.findAll(text).any { passesLuhn(it.value.filter(Char::isDigit)) }

    fun fingerprint(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun passesLuhn(value: String): Boolean {
        if (value.length !in 13..19 || value.toSet().size == 1) return false
        var sum = 0
        var doubleDigit = false
        for (index in value.indices.reversed()) {
            var digit = value[index].digitToIntOrNull() ?: return false
            if (doubleDigit) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            doubleDigit = !doubleDigit
        }
        return sum % 10 == 0
    }
}

class MayraPrivacyCenter(
    private val clock: () -> Long = System::currentTimeMillis,
    private val redactor: SensitiveTextRedactor = SensitiveTextRedactor(),
    private val maxAuditEvents: Int = 1_000
) {
    private val policies = ConcurrentHashMap<PersonalDataCategory, RetentionPolicy>()
    private val consents = ConcurrentHashMap<String, ConsentRecord>()
    private val audit = ArrayDeque<PrivacyAuditEvent>()

    init {
        defaultPolicies().forEach { policies[it.category] = it }
    }

    fun policy(category: PersonalDataCategory): RetentionPolicy =
        policies[category] ?: error("Missing privacy policy for $category")

    fun setPolicy(policy: RetentionPolicy) {
        policies[policy.category] = policy
        recordAudit(
            operation = "policy_updated",
            category = policy.category,
            purpose = "privacy_configuration",
            result = policy.mode.name,
            actor = "user",
            sensitive = policy.category == PersonalDataCategory.SENSITIVE_REFERENCE
        )
    }

    fun recordConsent(record: ConsentRecord): ConsentRecord {
        val key = consentKey(record.purpose, record.category)
        consents[key] = record.copy(
            purpose = normalize(record.purpose),
            source = normalize(record.source)
        )
        recordAudit(
            operation = "consent_updated",
            category = record.category,
            purpose = record.purpose,
            result = record.state.name,
            actor = record.source,
            sensitive = false
        )
        return consents.getValue(key)
    }

    fun consent(purpose: String, category: PersonalDataCategory, now: Long = clock()): ConsentState {
        val record = consents[consentKey(purpose, category)] ?: return ConsentState.NOT_SET
        return if (record.expiresAt != null && record.expiresAt <= now) ConsentState.NOT_SET else record.state
    }

    fun evaluate(
        purpose: String,
        category: PersonalDataCategory,
        operation: String,
        userPresent: Boolean,
        pluginId: String? = null,
        now: Long = clock()
    ): PrivacyDecision {
        val policy = policy(category)
        val consent = consent(purpose, category, now)
        val pluginAccess = pluginId != null
        val highRiskCategory = category in HIGH_RISK_CATEGORIES
        val writeOrShare = operation.lowercase().let { it.contains("share") || it.contains("send") || it.contains("export") || it.contains("write") }

        val decision = when {
            policy.mode == RetentionMode.NEVER_STORE && operation.contains("store", ignoreCase = true) -> PrivacyDecision(
                false, false, PrivacyRisk.HIGH, "This category is configured as never-store.", policy, consent
            )
            pluginAccess && !policy.allowPluginAccess -> PrivacyDecision(
                false, false, PrivacyRisk.HIGH, "Plugin access is disabled for this data category.", policy, consent
            )
            consent == ConsentState.DENIED -> PrivacyDecision(
                false, false, PrivacyRisk.HIGH, "User consent is denied for this purpose.", policy, consent
            )
            consent == ConsentState.ASK_EVERY_TIME -> PrivacyDecision(
                userPresent, true, if (highRiskCategory) PrivacyRisk.CRITICAL else PrivacyRisk.MODERATE,
                "Explicit confirmation is required every time.", policy, consent
            )
            highRiskCategory && writeOrShare && consent != ConsentState.GRANTED -> PrivacyDecision(
                userPresent, true, PrivacyRisk.CRITICAL,
                "Sensitive data sharing requires explicit consent.", policy, consent
            )
            !userPresent && (highRiskCategory || writeOrShare) -> PrivacyDecision(
                false, false, PrivacyRisk.HIGH, "User presence is required for this operation.", policy, consent
            )
            else -> PrivacyDecision(
                true,
                requiresConfirmation = highRiskCategory && writeOrShare,
                risk = if (highRiskCategory) PrivacyRisk.HIGH else if (writeOrShare) PrivacyRisk.MODERATE else PrivacyRisk.LOW,
                reason = "Operation is permitted by the active privacy policy.",
                policy = policy,
                consent = consent
            )
        }

        recordAudit(
            operation = normalize(operation),
            category = category,
            purpose = normalize(purpose),
            result = if (decision.allowed) "allowed" else "blocked",
            actor = pluginId ?: "mayra_core",
            sensitive = highRiskCategory,
            metadata = mapOf("confirmation" to decision.requiresConfirmation.toString(), "risk" to decision.risk.name)
        )
        return decision
    }

    fun sanitizeForStorage(text: String, category: PersonalDataCategory): String {
        val policy = policy(category)
        if (policy.mode == RetentionMode.NEVER_STORE) return ""
        return if (category in HIGH_RISK_CATEGORIES || redactor.containsLikelySecret(text)) redactor.redact(text) else text
    }

    fun shouldRetain(category: PersonalDataCategory, createdAt: Long, now: Long = clock()): Boolean {
        val policy = policy(category)
        return when (policy.mode) {
            RetentionMode.NEVER_STORE -> false
            RetentionMode.SESSION_ONLY -> false
            RetentionMode.UNTIL_USER_DELETES -> true
            RetentionMode.DAYS -> createdAt + policy.retentionDays!! * RetentionPolicy.DAY_MS > now
        }
    }

    fun deletionPlan(categories: Set<PersonalDataCategory>): PrivacyDeletionPlan {
        require(categories.isNotEmpty())
        val operations = categories.sortedBy(Enum<*>::name).map { category ->
            PrivacyDeletionOperation(
                category = category,
                storageKeys = storageKeys(category),
                requiresPluginDisconnect = category == PersonalDataCategory.PLUGIN_DATA,
                irreversible = true
            )
        }
        return PrivacyDeletionPlan(UUID.randomUUID().toString(), categories, operations, clock())
    }

    fun exportPlan(categories: Set<PersonalDataCategory>, includeSensitive: Boolean): PrivacyExportPlan {
        require(categories.isNotEmpty())
        val allowed = categories.filterNot { category ->
            category in HIGH_RISK_CATEGORIES && !includeSensitive
        }.toSet()
        return PrivacyExportPlan(
            id = UUID.randomUUID().toString(),
            requested = categories,
            included = allowed,
            excluded = categories - allowed,
            redactSensitive = !includeSensitive,
            createdAt = clock()
        )
    }

    @Synchronized
    fun recentAudit(limit: Int = 100, includeSensitive: Boolean = false): List<PrivacyAuditEvent> = audit
        .asSequence()
        .filter { includeSensitive || !it.sensitive }
        .toList()
        .takeLast(limit.coerceIn(1, maxAuditEvents))
        .sortedByDescending(PrivacyAuditEvent::timestamp)

    fun snapshot(now: Long = clock()): PrivacySnapshot {
        val records = consents.values.sortedByDescending(ConsentRecord::updatedAt)
        return PrivacySnapshot(
            policies = policies.values.sortedBy { it.category.name },
            consentRecords = records,
            recentAudit = recentAudit(100),
            deniedPurposes = records.count { it.state == ConsentState.DENIED },
            expiringConsents = records.count { it.expiresAt?.let { expiry -> expiry in (now + 1)..(now + 7 * RetentionPolicy.DAY_MS) } == true },
            capturedAt = now
        )
    }

    @Synchronized
    fun clearAudit() = audit.clear()

    @Synchronized
    private fun recordAudit(
        operation: String,
        category: PersonalDataCategory,
        purpose: String,
        result: String,
        actor: String,
        sensitive: Boolean,
        metadata: Map<String, String> = emptyMap()
    ) {
        audit += PrivacyAuditEvent(
            timestamp = clock(),
            operation = operation,
            category = category,
            purpose = purpose,
            result = result,
            actor = actor,
            sensitive = sensitive,
            metadata = metadata.entries.take(20).associate { normalize(it.key) to normalize(it.value) }
        )
        while (audit.size > maxAuditEvents) audit.removeFirst()
    }

    private fun consentKey(purpose: String, category: PersonalDataCategory) = "${category.name}:${normalize(purpose).lowercase()}"
    private fun normalize(value: String) = value.trim().replace(Regex("\\s+"), " ").take(300)

    private fun storageKeys(category: PersonalDataCategory): Set<String> = when (category) {
        PersonalDataCategory.CONVERSATION -> setOf("mayra_conversations", "mayra_context_memory")
        PersonalDataCategory.VOICE_TRANSCRIPT -> setOf("voice_turns", "voice_transcripts")
        PersonalDataCategory.CONTACT_REFERENCE -> setOf("contact_references")
        PersonalDataCategory.CALENDAR -> setOf("calendar_cache")
        PersonalDataCategory.LOCATION -> setOf("location_context")
        PersonalDataCategory.FILE_METADATA -> setOf("file_index")
        PersonalDataCategory.APP_USAGE -> setOf("app_usage_signals")
        PersonalDataCategory.HABIT -> setOf("habit_patterns", "personal_signals")
        PersonalDataCategory.NOTE -> setOf("mayra_personal_memory")
        PersonalDataCategory.REMINDER -> setOf("reminders", "timeline_events")
        PersonalDataCategory.PLUGIN_DATA -> setOf("plugin_tokens", "plugin_cache", "plugin_audit")
        PersonalDataCategory.DIAGNOSTIC -> setOf("diagnostics", "runtime_audit")
        PersonalDataCategory.SENSITIVE_REFERENCE -> setOf("secure_references")
    }

    private fun defaultPolicies(): List<RetentionPolicy> = listOf(
        RetentionPolicy(PersonalDataCategory.CONVERSATION, RetentionMode.DAYS, 30, true, true, false),
        RetentionPolicy(PersonalDataCategory.VOICE_TRANSCRIPT, RetentionMode.DAYS, 7, true, false, false),
        RetentionPolicy(PersonalDataCategory.CONTACT_REFERENCE, RetentionMode.SESSION_ONLY, includeInSearch = false, allowProactiveUse = false),
        RetentionPolicy(PersonalDataCategory.CALENDAR, RetentionMode.DAYS, 14, true, true, false),
        RetentionPolicy(PersonalDataCategory.LOCATION, RetentionMode.NEVER_STORE, includeInSearch = false, allowProactiveUse = false, allowPluginAccess = false),
        RetentionPolicy(PersonalDataCategory.FILE_METADATA, RetentionMode.DAYS, 14, true, false, false),
        RetentionPolicy(PersonalDataCategory.APP_USAGE, RetentionMode.DAYS, 30, false, true, false),
        RetentionPolicy(PersonalDataCategory.HABIT, RetentionMode.DAYS, 90, true, true, false),
        RetentionPolicy(PersonalDataCategory.NOTE, RetentionMode.UNTIL_USER_DELETES, includeInSearch = true, allowProactiveUse = true),
        RetentionPolicy(PersonalDataCategory.REMINDER, RetentionMode.DAYS, 90, true, true, false),
        RetentionPolicy(PersonalDataCategory.PLUGIN_DATA, RetentionMode.DAYS, 14, false, false, true),
        RetentionPolicy(PersonalDataCategory.DIAGNOSTIC, RetentionMode.DAYS, 14, false, false, false),
        RetentionPolicy(PersonalDataCategory.SENSITIVE_REFERENCE, RetentionMode.NEVER_STORE, includeInSearch = false, allowProactiveUse = false, allowPluginAccess = false)
    )

    companion object {
        val HIGH_RISK_CATEGORIES = setOf(
            PersonalDataCategory.LOCATION,
            PersonalDataCategory.CONTACT_REFERENCE,
            PersonalDataCategory.PLUGIN_DATA,
            PersonalDataCategory.SENSITIVE_REFERENCE
        )
    }
}

data class PrivacyDeletionOperation(
    val category: PersonalDataCategory,
    val storageKeys: Set<String>,
    val requiresPluginDisconnect: Boolean,
    val irreversible: Boolean
)

data class PrivacyDeletionPlan(
    val id: String,
    val categories: Set<PersonalDataCategory>,
    val operations: List<PrivacyDeletionOperation>,
    val createdAt: Long
)

data class PrivacyExportPlan(
    val id: String,
    val requested: Set<PersonalDataCategory>,
    val included: Set<PersonalDataCategory>,
    val excluded: Set<PersonalDataCategory>,
    val redactSensitive: Boolean,
    val createdAt: Long
)
