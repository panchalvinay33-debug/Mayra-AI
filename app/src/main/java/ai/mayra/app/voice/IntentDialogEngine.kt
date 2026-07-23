package ai.mayra.app.voice

import java.util.Locale
import java.util.UUID
import kotlin.math.min

enum class MayraIntentType {
    OPEN_APP,
    CALL_CONTACT,
    SEND_MESSAGE,
    CREATE_REMINDER,
    CREATE_NOTE,
    ADD_TO_LIST,
    SEARCH_MEMORY,
    DEVICE_CONTROL,
    CONTINUE_PREVIOUS,
    CONFIRM,
    REJECT,
    CANCEL,
    HELP,
    UNKNOWN
}

data class ExtractedEntity(
    val name: String,
    val value: String,
    val confidence: Double,
    val source: String = "local_rule"
) {
    init {
        require(name.isNotBlank())
        require(value.isNotBlank())
        require(confidence in 0.0..1.0)
    }
}

data class ResolvedIntent(
    val id: String = UUID.randomUUID().toString(),
    val type: MayraIntentType,
    val rawText: String,
    val normalizedText: String,
    val entities: Map<String, ExtractedEntity> = emptyMap(),
    val confidence: Double,
    val inheritedContext: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val sensitive: Boolean = false,
    val missingSlots: Set<String> = emptySet(),
    val reasons: List<String> = emptyList()
) {
    init {
        require(confidence in 0.0..1.0)
        require(reasons.size <= 10)
    }

    val complete: Boolean get() = missingSlots.isEmpty()
}

data class ConversationContext(
    val topic: String? = null,
    val previousIntent: ResolvedIntent? = null,
    val slots: Map<String, String> = emptyMap(),
    val pendingQuestion: String? = null,
    val pendingConfirmationAction: String? = null,
    val locale: String = "hi-IN"
)

data class ClarificationRequest(
    val question: String,
    val missingSlots: Set<String>,
    val suggestions: List<String> = emptyList(),
    val originalIntentId: String? = null
)

data class DialogDecision(
    val intent: ResolvedIntent,
    val responsePlan: VoiceResponsePlan,
    val clarification: ClarificationRequest? = null,
    val shouldExecute: Boolean = false,
    val actionKey: String? = null,
    val actionPayload: Map<String, String> = emptyMap()
)

class LocalIntentResolver {
    fun resolve(text: String, context: ConversationContext = ConversationContext()): ResolvedIntent {
        val normalized = normalize(text)
        require(normalized.isNotBlank())
        val lower = normalized.lowercase(Locale.ROOT)

        val direct = detectDirectIntent(lower)
        val inherited = if (direct.first == MayraIntentType.UNKNOWN) inheritIntent(lower, context) else null
        val type = inherited ?: direct.first
        val entities = extractEntities(normalized, lower, type, context)
        val missing = requiredSlots(type).filterNot { entities.containsKey(it) }.toSet()
        val inheritedContext = inherited != null || entities.values.any { it.source == "conversation_context" }
        val sensitive = type in setOf(MayraIntentType.SEND_MESSAGE, MayraIntentType.CALL_CONTACT)
        val requiresConfirmation = when (type) {
            MayraIntentType.SEND_MESSAGE -> entities.containsKey("message") && entities.containsKey("contact")
            MayraIntentType.CALL_CONTACT -> entities.containsKey("contact")
            MayraIntentType.DEVICE_CONTROL -> lower.contains("delete") || lower.contains("reset") || lower.contains("factory")
            else -> false
        }
        val confidence = score(type, direct.second, entities, missing, inheritedContext)
        val reasons = buildList {
            add(if (direct.first != MayraIntentType.UNKNOWN) "matched_local_pattern" else "no_direct_pattern")
            if (inheritedContext) add("used_conversation_context")
            if (missing.isNotEmpty()) add("missing_${missing.joinToString("_")}")
            if (requiresConfirmation) add("confirmation_required")
        }

        return ResolvedIntent(
            type = type,
            rawText = text,
            normalizedText = normalized,
            entities = entities,
            confidence = confidence,
            inheritedContext = inheritedContext,
            requiresConfirmation = requiresConfirmation,
            sensitive = sensitive,
            missingSlots = missing,
            reasons = reasons
        )
    }

    private fun detectDirectIntent(lower: String): Pair<MayraIntentType, Double> {
        if (matches(lower, "yes", "haan", "ha", "kar do", "confirm", "theek hai")) return MayraIntentType.CONFIRM to 0.94
        if (matches(lower, "no", "nahi", "mat karo", "reject")) return MayraIntentType.REJECT to 0.94
        if (matches(lower, "cancel", "ruk jao", "stop", "band karo")) return MayraIntentType.CANCEL to 0.95
        if (matches(lower, "help", "madad", "kya kar sakti")) return MayraIntentType.HELP to 0.90
        if (containsAny(lower, "open ", "khol", "launch ")) return MayraIntentType.OPEN_APP to 0.88
        if (containsAny(lower, "call ", "phone lag", "dial ")) return MayraIntentType.CALL_CONTACT to 0.90
        if (containsAny(lower, "message ", "msg ", "whatsapp ", "text ", "bhej")) return MayraIntentType.SEND_MESSAGE to 0.88
        if (containsAny(lower, "remind", "reminder", "yaad dil")) return MayraIntentType.CREATE_REMINDER to 0.89
        if (containsAny(lower, "note ", "likh lo", "save this", "yaad rakh")) return MayraIntentType.CREATE_NOTE to 0.86
        if (containsAny(lower, "list me", "list mein", "shopping list", "add to list")) return MayraIntentType.ADD_TO_LIST to 0.88
        if (containsAny(lower, "search", "dhundo", "find ", "memory me", "yaad hai")) return MayraIntentType.SEARCH_MEMORY to 0.82
        if (containsAny(lower, "wifi", "bluetooth", "torch", "volume", "brightness", "silent mode")) return MayraIntentType.DEVICE_CONTROL to 0.84
        if (matches(lower, "continue", "aage", "phir", "usme", "isme", "wahi")) return MayraIntentType.CONTINUE_PREVIOUS to 0.72
        return MayraIntentType.UNKNOWN to 0.30
    }

    private fun inheritIntent(lower: String, context: ConversationContext): MayraIntentType? {
        val previous = context.previousIntent ?: return null
        if (lower.startsWith("usme") || lower.startsWith("isme") || lower.startsWith("wahi") || lower.startsWith("aur ")) {
            return previous.type
        }
        if (context.pendingQuestion != null && previous.missingSlots.isNotEmpty()) return previous.type
        return null
    }

    private fun extractEntities(
        original: String,
        lower: String,
        type: MayraIntentType,
        context: ConversationContext
    ): Map<String, ExtractedEntity> {
        val entities = linkedMapOf<String, ExtractedEntity>()
        fun put(name: String, value: String, confidence: Double, source: String = "local_rule") {
            val clean = value.trim().trim('.', ',', '?', '!', '।').take(500)
            if (clean.isNotBlank()) entities[name] = ExtractedEntity(name, clean, confidence, source)
        }

        when (type) {
            MayraIntentType.OPEN_APP -> {
                val value = removePrefixes(original, listOf("open", "launch", "khol do", "kholo", "khol"))
                put("app", value, 0.88)
            }
            MayraIntentType.CALL_CONTACT -> {
                val value = removePrefixes(original, listOf("call", "dial", "phone lagao", "phone laga do", "phone lag"))
                put("contact", value, 0.86)
            }
            MayraIntentType.SEND_MESSAGE -> {
                val messageMarker = Regex("(?i)(?:message|msg|text|whatsapp)\\s+(.+?)\\s+(?:to|ko)\\s+(.+)")
                val reversedMarker = Regex("(?i)(.+?)\\s+ko\\s+(.+?)\\s+(?:message|msg|bhej(?:o| do)?)")
                val first = messageMarker.find(original)
                val second = reversedMarker.find(original)
                when {
                    first != null -> {
                        put("message", first.groupValues[1], 0.84)
                        put("contact", first.groupValues[2], 0.84)
                    }
                    second != null -> {
                        put("contact", second.groupValues[1], 0.78)
                        put("message", second.groupValues[2], 0.72)
                    }
                    else -> context.slots["contact"]?.let { put("contact", it, 0.68, "conversation_context") }
                }
            }
            MayraIntentType.CREATE_REMINDER -> {
                val time = Regex("(?i)\\b(today|tomorrow|kal|aaj|tonight|morning|evening|subah|shaam|\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\b").find(original)
                time?.value?.let { put("time", it, 0.78) }
                val content = original
                    .replace(Regex("(?i)remind(?:er)?(?: me)?(?: to)?"), "")
                    .replace(Regex("(?i)yaad dila(?:na| dena)?"), "")
                    .replace(time?.value ?: "", "")
                put("content", content, 0.75)
            }
            MayraIntentType.CREATE_NOTE -> {
                put("content", removePrefixes(original, listOf("note", "note karo", "likh lo", "save this", "yaad rakh")), 0.82)
            }
            MayraIntentType.ADD_TO_LIST -> {
                val list = if (lower.contains("shopping")) "shopping" else context.slots["list"] ?: "default"
                put("list", list, 0.80, if (lower.contains("shopping")) "local_rule" else "conversation_context")
                val content = original
                    .replace(Regex("(?i)(shopping )?list (?:me|mein)"), "")
                    .replace(Regex("(?i)add(?: to list)?"), "")
                put("content", content, 0.72)
            }
            MayraIntentType.SEARCH_MEMORY -> put("query", removePrefixes(original, listOf("search", "find", "dhundo", "memory me")), 0.82)
            MayraIntentType.DEVICE_CONTROL -> {
                val control = listOf("wifi", "bluetooth", "torch", "volume", "brightness", "silent mode").firstOrNull { lower.contains(it) }
                control?.let { put("control", it, 0.90) }
                val operation = when {
                    containsAny(lower, " on", "chalu", "enable") -> "on"
                    containsAny(lower, " off", "band", "disable") -> "off"
                    containsAny(lower, "increase", "badha") -> "increase"
                    containsAny(lower, "decrease", "kam") -> "decrease"
                    else -> "toggle"
                }
                put("operation", operation, 0.72)
            }
            else -> Unit
        }

        requiredSlots(type).forEach { slot ->
            if (!entities.containsKey(slot)) context.slots[slot]?.let { put(slot, it, 0.64, "conversation_context") }
        }
        return entities
    }

    private fun requiredSlots(type: MayraIntentType): Set<String> = when (type) {
        MayraIntentType.OPEN_APP -> setOf("app")
        MayraIntentType.CALL_CONTACT -> setOf("contact")
        MayraIntentType.SEND_MESSAGE -> setOf("contact", "message")
        MayraIntentType.CREATE_REMINDER -> setOf("content", "time")
        MayraIntentType.CREATE_NOTE -> setOf("content")
        MayraIntentType.ADD_TO_LIST -> setOf("list", "content")
        MayraIntentType.SEARCH_MEMORY -> setOf("query")
        MayraIntentType.DEVICE_CONTROL -> setOf("control", "operation")
        else -> emptySet()
    }

    private fun score(
        type: MayraIntentType,
        patternScore: Double,
        entities: Map<String, ExtractedEntity>,
        missing: Set<String>,
        inherited: Boolean
    ): Double {
        if (type == MayraIntentType.UNKNOWN) return 0.25
        val entityAverage = if (entities.isEmpty()) patternScore else entities.values.map { it.confidence }.average()
        val missingPenalty = min(0.45, missing.size * 0.18)
        val inheritancePenalty = if (inherited) 0.08 else 0.0
        return ((patternScore * 0.6 + entityAverage * 0.4) - missingPenalty - inheritancePenalty).coerceIn(0.0, 1.0)
    }

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(4_000)
    private fun matches(value: String, vararg options: String): Boolean = options.any { value == it || value.startsWith("$it ") }
    private fun containsAny(value: String, vararg options: String): Boolean = options.any(value::contains)
    private fun removePrefixes(value: String, prefixes: List<String>): String {
        var result = value.trim()
        prefixes.sortedByDescending { it.length }.forEach { prefix ->
            result = result.replace(Regex("(?i)^${Regex.escape(prefix)}\\s*"), "")
        }
        return result.trim()
    }
}

class VoiceDialogManager(
    private val clarificationThreshold: Double = 0.62
) {
    fun decide(intent: ResolvedIntent, context: ConversationContext = ConversationContext()): DialogDecision {
        if (intent.type == MayraIntentType.CONFIRM && context.pendingConfirmationAction != null) {
            return DialogDecision(
                intent = intent,
                responsePlan = VoiceResponsePlan("Theek hai, main confirm ki gayi action process kar rahi hoon.", mode = VoiceOutputMode.SPEAK),
                shouldExecute = true,
                actionKey = context.pendingConfirmationAction,
                actionPayload = context.slots
            )
        }
        if (intent.type in setOf(MayraIntentType.REJECT, MayraIntentType.CANCEL)) {
            return DialogDecision(
                intent = intent,
                responsePlan = VoiceResponsePlan("Theek hai, maine action cancel kar di.", shouldEndSession = false)
            )
        }
        if (intent.type == MayraIntentType.HELP) {
            return DialogDecision(
                intent = intent,
                responsePlan = VoiceResponsePlan(
                    spokenText = "Aap app kholne, call, message, reminder, note, list, memory search aur device controls ke liye bol sakte hain.",
                    suggestions = listOf("WhatsApp kholo", "Kal subah reminder lagao", "Shopping list me doodh add karo")
                )
            )
        }
        if (intent.type == MayraIntentType.UNKNOWN || intent.confidence < clarificationThreshold || intent.missingSlots.isNotEmpty()) {
            val clarification = clarificationFor(intent)
            return DialogDecision(
                intent = intent,
                responsePlan = VoiceResponsePlan(
                    spokenText = clarification.question,
                    mode = VoiceOutputMode.ASK,
                    suggestions = clarification.suggestions
                ),
                clarification = clarification
            )
        }
        val actionKey = actionKey(intent.type)
        val payload = intent.entities.mapValues { it.value.value }
        if (intent.requiresConfirmation) {
            val prompt = confirmationPrompt(intent)
            val pending = PendingVoiceConfirmation(
                prompt = prompt,
                actionKey = actionKey,
                payload = payload,
                sensitive = intent.sensitive
            )
            return DialogDecision(
                intent = intent,
                responsePlan = VoiceResponsePlan(
                    spokenText = prompt,
                    mode = VoiceOutputMode.CONFIRM,
                    confirmation = pending,
                    sensitive = intent.sensitive
                ),
                actionKey = actionKey,
                actionPayload = payload
            )
        }
        return DialogDecision(
            intent = intent,
            responsePlan = VoiceResponsePlan(successPreview(intent), mode = VoiceOutputMode.SPEAK),
            shouldExecute = true,
            actionKey = actionKey,
            actionPayload = payload
        )
    }

    private fun clarificationFor(intent: ResolvedIntent): ClarificationRequest {
        val question = when {
            intent.type == MayraIntentType.UNKNOWN -> "Mujhe command poori tarah samajh nahi aayi. Aap kya karwana chahte hain?"
            "contact" in intent.missingSlots -> "Kis contact ke liye?"
            "message" in intent.missingSlots -> "Kya message bhejna hai?"
            "time" in intent.missingSlots -> "Reminder kis samay lagana hai?"
            "content" in intent.missingSlots -> "Kya likhna ya add karna hai?"
            "app" in intent.missingSlots -> "Kaunsi app kholni hai?"
            "query" in intent.missingSlots -> "Memory me kya dhundhna hai?"
            "control" in intent.missingSlots -> "Kaunsa device control badalna hai?"
            else -> "Thoda aur clearly batayenge?"
        }
        val suggestions = when (intent.type) {
            MayraIntentType.CREATE_REMINDER -> listOf("Kal subah 8 baje", "Aaj shaam 7 baje")
            MayraIntentType.OPEN_APP -> listOf("WhatsApp", "YouTube", "Settings")
            MayraIntentType.DEVICE_CONTROL -> listOf("Wi-Fi", "Bluetooth", "Torch")
            else -> emptyList()
        }
        return ClarificationRequest(question, intent.missingSlots, suggestions, intent.id)
    }

    private fun confirmationPrompt(intent: ResolvedIntent): String = when (intent.type) {
        MayraIntentType.SEND_MESSAGE -> {
            val contact = intent.entities["contact"]?.value ?: "is contact"
            val message = intent.entities["message"]?.value ?: "ye message"
            "$contact ko ‘${message.take(120)}’ bhejna hai. Confirm karun?"
        }
        MayraIntentType.CALL_CONTACT -> "${intent.entities["contact"]?.value} ko call lagani hai. Confirm karun?"
        else -> "Ye action sensitive ho sakti hai. Confirm karun?"
    }

    private fun successPreview(intent: ResolvedIntent): String = when (intent.type) {
        MayraIntentType.OPEN_APP -> "${intent.entities["app"]?.value} khol rahi hoon."
        MayraIntentType.CREATE_REMINDER -> "Reminder prepare kar rahi hoon."
        MayraIntentType.CREATE_NOTE -> "Note save kar rahi hoon."
        MayraIntentType.ADD_TO_LIST -> "List me add kar rahi hoon."
        MayraIntentType.SEARCH_MEMORY -> "Apni memory me dhundh rahi hoon."
        MayraIntentType.DEVICE_CONTROL -> "Device setting process kar rahi hoon."
        MayraIntentType.CONTINUE_PREVIOUS -> "Pichhli baat se continue kar rahi hoon."
        else -> "Command process kar rahi hoon."
    }

    private fun actionKey(type: MayraIntentType): String = when (type) {
        MayraIntentType.OPEN_APP -> "device.open_app"
        MayraIntentType.CALL_CONTACT -> "device.call_contact"
        MayraIntentType.SEND_MESSAGE -> "communication.send_message"
        MayraIntentType.CREATE_REMINDER -> "personal.create_reminder"
        MayraIntentType.CREATE_NOTE -> "personal.create_note"
        MayraIntentType.ADD_TO_LIST -> "personal.add_to_list"
        MayraIntentType.SEARCH_MEMORY -> "personal.search_memory"
        MayraIntentType.DEVICE_CONTROL -> "device.control"
        MayraIntentType.CONTINUE_PREVIOUS -> "conversation.continue"
        else -> "conversation.noop"
    }
}
