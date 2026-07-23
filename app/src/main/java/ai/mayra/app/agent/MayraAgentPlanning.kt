package ai.mayra.app.agent

import java.util.Locale
import java.util.UUID

data class AgentObjective(
    val text: String,
    val locale: String = "hi-IN",
    val variables: Map<String, String> = emptyMap(),
    val requestedAt: Long = System.currentTimeMillis()
) {
    init {
        require(text.isNotBlank())
        require(text.length <= 4_000)
        require(variables.size <= 50)
    }
}

data class AgentPlanningResult(
    val plan: AgentPlan?,
    val clarification: String? = null,
    val recognizedCapabilities: Set<String> = emptySet(),
    val warnings: List<String> = emptyList()
)

class MayraAgentToolRegistry(tools: Collection<MayraAgentTool> = emptyList()) {
    private val tools = linkedMapOf<String, MayraAgentTool>()

    init { tools.forEach(::register) }

    @Synchronized
    fun register(tool: MayraAgentTool) {
        require(tool.descriptor.id !in tools) { "Duplicate tool id: ${tool.descriptor.id}" }
        tools[tool.descriptor.id] = tool
    }

    @Synchronized
    fun replace(tool: MayraAgentTool) { tools[tool.descriptor.id] = tool }

    @Synchronized
    fun remove(id: String): Boolean = tools.remove(id) != null

    @Synchronized
    fun get(id: String): MayraAgentTool? = tools[id]

    @Synchronized
    fun snapshot(): List<MayraAgentTool> = tools.values.toList()

    @Synchronized
    fun descriptors(): List<AgentToolDescriptor> = tools.values.map(MayraAgentTool::descriptor)

    @Synchronized
    fun findOperation(operation: String): List<MayraAgentTool> = tools.values
        .filter { operation in it.descriptor.operations }
        .sortedWith(compareBy<MayraAgentTool> { it.descriptor.risk }.thenBy { it.descriptor.id })
}

/**
 * Deterministic local planner for common assistant chains. It intentionally asks for clarification
 * instead of inventing missing time/contact/event details.
 */
class MayraAgentPlanner(
    private val registry: MayraAgentToolRegistry,
    private val now: () -> Long = System::currentTimeMillis
) {
    fun plan(objective: AgentObjective): AgentPlanningResult {
        val text = objective.text.trim().replace(Regex("\\s+"), " ")
        val lower = text.lowercase(Locale.ROOT)
        val capabilities = linkedSetOf<String>()
        val steps = mutableListOf<AgentStep>()
        val warnings = mutableListOf<String>()

        fun supports(toolId: String, operation: String): Boolean = registry.get(toolId)
            ?.descriptor?.operations?.contains(operation) == true

        fun addTool(
            title: String,
            toolId: String,
            operation: String,
            args: Map<String, String>,
            dependencies: Set<String> = emptySet(),
            requiresConfirmation: Boolean = false,
            failurePolicy: AgentFailurePolicy = AgentFailurePolicy.STOP,
            outputBindings: Map<String, String> = emptyMap()
        ): AgentStep? {
            if (!supports(toolId, operation)) {
                warnings += "$toolId.$operation provider available nahi hai"
                return null
            }
            val step = AgentStep(
                order = steps.size,
                title = title,
                kind = AgentStepKind.TOOL,
                call = AgentToolCall(toolId, operation, args),
                dependencies = dependencies,
                requiresConfirmation = requiresConfirmation,
                failurePolicy = failurePolicy,
                outputBindings = outputBindings
            )
            steps += step
            capabilities += "$toolId.$operation"
            return step
        }

        val wantsReminder = containsAny(lower, "reminder", "remind", "yaad dila", "याद दिल")
        val wantsCalendar = containsAny(lower, "calendar", "event", "meeting", "कैलेंडर", "मीटिंग")
        val wantsWeather = containsAny(lower, "weather", "mausam", "मौसम")
        val wantsMessage = containsAny(lower, "message", "whatsapp", "sms", "msg", "संदेश")
        val wantsCall = containsAny(lower, "call", "phone laga", "कॉल")
        val wantsSearch = containsAny(lower, "search", "find", "dhundo", "ढूंढ")
        val wantsVision = containsAny(lower, "photo", "image", "document", "bill", "receipt", "फोटो", "दस्तावेज")
        val wantsNote = containsAny(lower, "note", "likh lo", "save", "नोट")

        val timePhrase = extractTimePhrase(text)
        val contact = extractContact(text)
        val content = extractContent(text)

        if ((wantsReminder || wantsCalendar) && timePhrase == null) {
            return AgentPlanningResult(
                plan = null,
                clarification = "Reminder ya event kis samay ke liye banana hai?",
                recognizedCapabilities = capabilities,
                warnings = warnings
            )
        }
        if ((wantsMessage || wantsCall) && contact == null) {
            return AgentPlanningResult(
                plan = null,
                clarification = "Kis contact ke liye action karna hai?",
                recognizedCapabilities = capabilities,
                warnings = warnings
            )
        }

        val weatherStep = if (wantsWeather) addTool(
            title = "Check weather",
            toolId = "search",
            operation = "weather",
            args = mapOf("query" to text, "time" to timePhrase.orEmpty()),
            failurePolicy = AgentFailurePolicy.CONTINUE,
            outputBindings = mapOf("weather.summary" to "summary")
        ) else null

        val reminderStep = if (wantsReminder) addTool(
            title = "Create reminder",
            toolId = "personal",
            operation = "create_reminder",
            args = mapOf("content" to content, "time" to timePhrase.orEmpty()),
            dependencies = weatherStep?.let { setOf(it.id) }.orEmpty(),
            failurePolicy = AgentFailurePolicy.RETRY
        ) else null

        val calendarStep = if (wantsCalendar) addTool(
            title = "Create calendar event",
            toolId = "calendar",
            operation = "create_event",
            args = mapOf("title" to inferEventTitle(text), "time" to timePhrase.orEmpty()),
            dependencies = emptySet(),
            requiresConfirmation = false,
            failurePolicy = AgentFailurePolicy.RETRY,
            outputBindings = mapOf("calendar.event_id" to "event_id")
        ) else null

        if (wantsMessage) {
            val deps = setOfNotNull(reminderStep?.id, calendarStep?.id, weatherStep?.id)
            addTool(
                title = "Prepare message for $contact",
                toolId = "communication",
                operation = if (lower.contains("whatsapp")) "compose_whatsapp" else "compose_message",
                args = mapOf("contact" to contact.orEmpty(), "message" to content),
                dependencies = deps,
                requiresConfirmation = true,
                failurePolicy = AgentFailurePolicy.STOP
            )
        }

        if (wantsCall) {
            addTool(
                title = "Call $contact",
                toolId = "communication",
                operation = "call",
                args = mapOf("contact" to contact.orEmpty()),
                requiresConfirmation = true
            )
        }

        if (wantsSearch && !wantsWeather) {
            addTool(
                title = "Search requested information",
                toolId = "search",
                operation = "unified_search",
                args = mapOf("query" to content),
                failurePolicy = AgentFailurePolicy.CONTINUE,
                outputBindings = mapOf("search.summary" to "summary")
            )
        }

        if (wantsVision) {
            addTool(
                title = "Analyze selected image",
                toolId = "vision",
                operation = "analyze",
                args = mapOf("prompt" to text),
                failurePolicy = AgentFailurePolicy.CONTINUE,
                outputBindings = mapOf("vision.summary" to "summary")
            )
        }

        if (wantsNote) {
            val deps = steps.filter { it.kind == AgentStepKind.TOOL }.map(AgentStep::id).toSet()
            addTool(
                title = "Save note",
                toolId = "personal",
                operation = "create_note",
                args = mapOf("content" to content),
                dependencies = deps,
                failurePolicy = AgentFailurePolicy.CONTINUE
            )
        }

        if (steps.isEmpty()) {
            return AgentPlanningResult(
                plan = null,
                clarification = "Is request me kaunsi action karni hai—reminder, calendar, search, message, call, note ya image analysis?",
                warnings = warnings
            )
        }

        val checkpointDependencies = steps.map(AgentStep::id).toSet()
        steps += AgentStep(
            order = steps.size,
            title = "Prepare final summary",
            kind = AgentStepKind.CHECKPOINT,
            dependencies = checkpointDependencies
        )

        return AgentPlanningResult(
            plan = AgentPlan(
                title = text.take(100),
                objective = text,
                createdAt = objective.requestedAt,
                expiresAt = objective.requestedAt + AgentPlan.DEFAULT_PLAN_TTL,
                maxExecutions = minOf(100, steps.size * 5),
                allowParallel = true,
                steps = steps
            ),
            recognizedCapabilities = capabilities,
            warnings = warnings
        )
    }

    private fun extractTimePhrase(text: String): String? {
        val regex = Regex(
            "(?i)\\b(today|tomorrow|tonight|aaj|kal|subah|shaam|raat|morning|evening|night|\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\b"
        )
        return regex.findAll(text).joinToString(" ") { it.value }.trim().takeIf(String::isNotBlank)
    }

    private fun extractContact(text: String): String? {
        val patterns = listOf(
            Regex("(?i)(?:call|message|whatsapp|sms|msg)\\s+([\\p{L}][\\p{L} .'-]{1,60}?)(?:\\s+(?:ko|to)|$)"),
            Regex("(?i)([\\p{L}][\\p{L} .'-]{1,60}?)\\s+ko\\s+(?:call|message|whatsapp|sms|msg)")
        )
        return patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1)?.trim() }
    }

    private fun extractContent(text: String): String = text
        .replace(Regex("(?i)\\b(reminder|remind me|calendar|event|meeting|message|whatsapp|sms|call|search|find|note|save)\\b"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(1_000)
        .ifBlank { text.take(1_000) }

    private fun inferEventTitle(text: String): String {
        val match = Regex("(?i)([\\p{L}\\p{N} ]{2,80})\\s+(?:meeting|event)").find(text)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)?.let { "$it meeting" }
            ?: "Mayra event"
    }

    private fun containsAny(value: String, vararg terms: String) = terms.any(value::contains)
    private fun setOfNotNull(vararg values: String?): Set<String> = values.filterNotNull().toSet()
}

/** Safe unavailable tool used until a real provider/adapter is installed. */
class UnavailableAgentTool(
    override val descriptor: AgentToolDescriptor,
    private val reason: String
) : MayraAgentTool {
    override suspend fun execute(call: AgentToolCall, context: AgentExecutionContext): AgentToolResult =
        AgentToolResult.NotSupported(reason)
}

/** Small functional adapter useful for wiring existing Mayra services without inheritance. */
class FunctionalAgentTool(
    override val descriptor: AgentToolDescriptor,
    private val executor: suspend (AgentToolCall, AgentExecutionContext) -> AgentToolResult,
    private val compensator: (suspend (AgentToolCall, AgentToolResult.Success, AgentExecutionContext) -> AgentToolResult)? = null
) : MayraAgentTool {
    override suspend fun execute(call: AgentToolCall, context: AgentExecutionContext): AgentToolResult = executor(call, context)

    override suspend fun compensate(
        call: AgentToolCall,
        result: AgentToolResult.Success,
        context: AgentExecutionContext
    ): AgentToolResult = compensator?.invoke(call, result, context)
        ?: AgentToolResult.NotSupported("Rollback is not configured for ${descriptor.id}")
}
