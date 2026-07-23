package ai.mayra.app.personal

import ai.mayra.app.plugins.MayraPlugin
import ai.mayra.app.plugins.MayraPluginRegistry
import ai.mayra.app.plugins.MayraUnifiedSearch
import ai.mayra.app.plugins.PluginRequest
import ai.mayra.app.plugins.PluginResult
import ai.mayra.app.plugins.UnifiedSearchProvider
import ai.mayra.app.plugins.UnifiedSearchQuery
import ai.mayra.app.plugins.UnifiedSearchResponse
import ai.mayra.app.privacy.MayraPrivacyCenter
import ai.mayra.app.privacy.PersonalDataCategory
import ai.mayra.app.privacy.PrivacyDecision
import ai.mayra.app.privacy.PrivacySnapshot
import java.time.ZoneId

data class PersonalRuntimeSnapshot(
    val habitPatterns: Int,
    val suggestions: Int,
    val plugins: Int,
    val openPluginCircuits: Int,
    val privacy: PrivacySnapshot,
    val capturedAt: Long
)

data class PersonalRuntimeOutput(
    val suggestions: List<ProactiveSuggestion>,
    val briefing: PersonalBriefing,
    val dashboard: PersonalDashboardSnapshot
)

class MayraPersonalRuntime(
    val habits: PersonalHabitEngine = PersonalHabitEngine(),
    val briefing: MayraBriefingEngine = MayraBriefingEngine(),
    val dashboard: MayraPersonalDashboard = MayraPersonalDashboard(),
    val plugins: MayraPluginRegistry = MayraPluginRegistry(),
    val search: MayraUnifiedSearch = MayraUnifiedSearch(),
    val privacy: MayraPrivacyCenter = MayraPrivacyCenter(),
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun recordSignal(signal: PersonalSignal): Boolean {
        val category = categoryFor(signal)
        val decision = privacy.evaluate(
            purpose = "personalization",
            category = category,
            operation = "store_signal",
            userPresent = true
        )
        if (!decision.allowed || !privacy.shouldRetain(category, signal.timestamp, clock())) return false
        habits.record(
            signal.copy(
                key = privacy.sanitizeForStorage(signal.key, category),
                attributes = signal.attributes.mapValues { privacy.sanitizeForStorage(it.value, category) }
            )
        )
        return true
    }

    fun generate(
        context: PersonalAssistantContext,
        productivity: ProductivityInputs,
        completedToday: Int = productivity.tasksCompleted,
        focusMinutesToday: Int = productivity.focusMinutes
    ): PersonalRuntimeOutput {
        val patterns = habits.inferPatterns(context.now, context.zoneId)
        val suggestions = habits.suggestions(context, patterns)
        return PersonalRuntimeOutput(
            suggestions = suggestions,
            briefing = briefing.build(context, suggestions, completedToday, focusMinutesToday),
            dashboard = dashboard.calculate(productivity)
        )
    }

    fun registerPlugin(plugin: MayraPlugin) = plugins.register(plugin)
    fun registerSearchProvider(provider: UnifiedSearchProvider) = search.register(provider)

    suspend fun executePlugin(request: PluginRequest): PluginResult = plugins.executeBest(request)
    suspend fun unifiedSearch(query: UnifiedSearchQuery): UnifiedSearchResponse = search.search(query)

    fun privacyDecision(
        purpose: String,
        category: PersonalDataCategory,
        operation: String,
        userPresent: Boolean,
        pluginId: String? = null
    ): PrivacyDecision = privacy.evaluate(purpose, category, operation, userPresent, pluginId)

    fun snapshot(
        now: Long = clock(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        context: PersonalAssistantContext = PersonalAssistantContext(now = now, zoneId = zoneId)
    ): PersonalRuntimeSnapshot {
        val patterns = habits.inferPatterns(now, zoneId)
        val suggestions = habits.suggestions(context, patterns)
        val pluginSnapshots = plugins.snapshots()
        return PersonalRuntimeSnapshot(
            habitPatterns = patterns.size,
            suggestions = suggestions.size,
            plugins = pluginSnapshots.size,
            openPluginCircuits = pluginSnapshots.count { it.health.name == "OPEN_CIRCUIT" },
            privacy = privacy.snapshot(now),
            capturedAt = now
        )
    }

    fun resetPersonalization() {
        habits.clear()
        privacy.clearAudit()
    }

    private fun categoryFor(signal: PersonalSignal): PersonalDataCategory = when (signal.type) {
        PersonalSignalType.APP_OPENED,
        PersonalSignalType.COMMAND_USED -> PersonalDataCategory.APP_USAGE
        PersonalSignalType.CONTACT_INTERACTION -> PersonalDataCategory.CONTACT_REFERENCE
        PersonalSignalType.REMINDER_CREATED,
        PersonalSignalType.REMINDER_COMPLETED -> PersonalDataCategory.REMINDER
        PersonalSignalType.NOTE_CREATED -> PersonalDataCategory.NOTE
        PersonalSignalType.MEETING -> PersonalDataCategory.CALENDAR
        PersonalSignalType.WAKE_SESSION -> PersonalDataCategory.VOICE_TRANSCRIPT
        PersonalSignalType.TASK_COMPLETED,
        PersonalSignalType.CUSTOM -> PersonalDataCategory.HABIT
    }
}
