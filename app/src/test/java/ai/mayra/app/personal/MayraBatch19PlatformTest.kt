package ai.mayra.app.personal

import ai.mayra.app.plugins.MayraCapability
import ai.mayra.app.plugins.MayraPlugin
import ai.mayra.app.plugins.MayraPluginRegistry
import ai.mayra.app.plugins.MayraUnifiedSearch
import ai.mayra.app.plugins.PluginContext
import ai.mayra.app.plugins.PluginDescriptor
import ai.mayra.app.plugins.PluginRequest
import ai.mayra.app.plugins.PluginResult
import ai.mayra.app.plugins.UnifiedSearchDomain
import ai.mayra.app.plugins.UnifiedSearchItem
import ai.mayra.app.plugins.UnifiedSearchProvider
import ai.mayra.app.plugins.UnifiedSearchQuery
import ai.mayra.app.privacy.ConsentRecord
import ai.mayra.app.privacy.ConsentState
import ai.mayra.app.privacy.MayraPrivacyCenter
import ai.mayra.app.privacy.PersonalDataCategory
import ai.mayra.app.privacy.RetentionMode
import ai.mayra.app.privacy.RetentionPolicy
import ai.mayra.app.privacy.SensitiveTextRedactor
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraBatch19PlatformTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun habitEngineInfersRepeatedMorningAppRoutine() {
        val engine = PersonalHabitEngine()
        val base = LocalDateTime.of(2026, 7, 1, 8, 10).atZone(zone).toInstant().toEpochMilli()
        repeat(8) { day ->
            engine.record(
                PersonalSignal(
                    type = PersonalSignalType.APP_OPENED,
                    key = "WhatsApp",
                    timestamp = base + day * DAY_MS,
                    successful = true
                )
            )
        }

        val patterns = engine.inferPatterns(
            now = base + 9 * DAY_MS,
            zoneId = zone,
            minimumObservations = 3
        )

        assertEquals(1, patterns.size)
        assertEquals("WhatsApp", patterns.single().key)
        assertEquals(8, patterns.single().preferredHour)
        assertTrue(patterns.single().confidence >= 0.60)
    }

    @Test
    fun quietHoursDeferRoutineSuggestion() {
        val engine = PersonalHabitEngine()
        val now = LocalDateTime.of(2026, 7, 20, 22, 0).atZone(zone).toInstant().toEpochMilli()
        val pattern = HabitPattern(
            id = "app:music",
            type = PersonalSignalType.APP_OPENED,
            key = "Music",
            preferredHour = 22,
            preferredDays = emptySet(),
            observationCount = 12,
            successfulCount = 12,
            confidence = 0.90,
            averageDurationMillis = 10_000,
            lastObservedAt = now - DAY_MS,
            sensitive = false
        )

        val suggestions = engine.suggestions(
            PersonalAssistantContext(now = now, zoneId = zone, quietHours = true),
            listOf(pattern)
        )

        assertEquals(SuggestionDisposition.DEFER, suggestions.single().disposition)
    }

    @Test
    fun briefingCombinesCalendarRemindersTasksAndSuggestions() {
        val now = LocalDateTime.of(2026, 7, 20, 8, 0).atZone(zone).toInstant().toEpochMilli()
        val context = PersonalAssistantContext(
            now = now,
            zoneId = zone,
            pendingReminderCount = 2,
            pendingTaskCount = 3,
            nextEventTitle = "School meeting",
            nextEventAt = now + 60 * 60 * 1000L
        )
        val suggestion = ProactiveSuggestion(
            type = ProactiveSuggestionType.ROUTINE,
            title = "WhatsApp kholna hai?",
            explanation = "Morning routine",
            actionKey = "device.open_app",
            score = 0.9,
            disposition = SuggestionDisposition.SHOW,
            expiresAt = now + 60_000
        )

        val briefing = MayraBriefingEngine().build(context, listOf(suggestion))

        assertEquals(BriefingPeriod.MORNING, briefing.period)
        assertTrue(briefing.items.any { it.title == "School meeting" })
        assertTrue(briefing.items.any { it.title == "Pending reminders" })
        assertTrue(briefing.items.any { it.title == "Aaj ke tasks" })
        assertTrue(briefing.items.any { it.title.contains("WhatsApp") })
    }

    @Test
    fun dashboardRewardsCompletionAndPenalizesOverdueWork() {
        val dashboard = MayraPersonalDashboard()
        val strong = dashboard.calculate(
            ProductivityInputs(10, 9, 5, 5, 120, 1, 0, 0.9)
        )
        val weak = dashboard.calculate(
            ProductivityInputs(10, 2, 5, 1, 10, 8, 4, 0.2)
        )

        assertTrue(strong.productivityScore > weak.productivityScore)
        assertTrue(strong.productivityScore >= 75)
        assertTrue(weak.attentionPenalty > strong.attentionPenalty)
    }

    @Test
    fun pluginRequiresDeclaredPermission() = runBlocking {
        val registry = MayraPluginRegistry()
        registry.register(successPlugin(requiredPermission = "gmail.read"))

        val missing = registry.executeBest(PluginRequest("search", mapOf("query" to "invoice")))
        val allowed = registry.executeBest(
            PluginRequest(
                "search",
                mapOf("query" to "invoice"),
                PluginContext(grantedPermissions = setOf("gmail.read"))
            )
        )

        assertTrue(missing is PluginResult.MissingPermission)
        assertTrue(allowed is PluginResult.Success)
    }

    @Test
    fun pluginCircuitOpensAfterRepeatedFailures() = runBlocking {
        var clock = 1_000L
        val registry = MayraPluginRegistry(failureThreshold = 2, circuitResetMillis = 10_000L) { clock }
        registry.register(failingPlugin())
        val request = PluginRequest("search")

        assertTrue(registry.executeBest(request) is PluginResult.Failure)
        assertTrue(registry.executeBest(request) is PluginResult.Failure)
        val blocked = registry.executeBest(request)

        assertTrue(blocked is PluginResult.Unsupported || blocked is PluginResult.Failure)
        assertEquals("OPEN_CIRCUIT", registry.snapshots().single().health.name)
        clock += 11_000L
        registry.executeBest(request)
        assertFalse(registry.snapshots().single().health.name == "OPEN_CIRCUIT")
    }

    @Test
    fun unifiedSearchFiltersSensitiveAndRanksExactTitle() = runBlocking {
        val search = MayraUnifiedSearch { 10_000L }
        search.register(object : UnifiedSearchProvider {
            override val id = "local.notes"
            override val domains = setOf(UnifiedSearchDomain.NOTE)
            override suspend fun search(query: UnifiedSearchQuery) = listOf(
                UnifiedSearchItem("1", UnifiedSearchDomain.NOTE, "Mayra release plan", score = 0.60, source = id),
                UnifiedSearchItem("2", UnifiedSearchDomain.NOTE, "Mayra", score = 0.55, source = id),
                UnifiedSearchItem("3", UnifiedSearchDomain.NOTE, "Mayra private", score = 0.99, sensitive = true, source = id)
            )
        })

        val response = search.search(UnifiedSearchQuery("Mayra", domains = setOf(UnifiedSearchDomain.NOTE)))

        assertEquals(2, response.items.size)
        assertEquals("Mayra", response.items.first().title)
        assertFalse(response.items.any { it.sensitive })
    }

    @Test
    fun redactorRemovesPhoneEmailOtpAndValidCard() {
        val redactor = SensitiveTextRedactor()
        val text = "Call 9876543210, mail a@b.com, OTP 123456 and card 4111 1111 1111 1111"
        val redacted = redactor.redact(text)

        assertTrue(redacted.contains("[REDACTED_PHONE]"))
        assertTrue(redacted.contains("[REDACTED_EMAIL]"))
        assertTrue(redacted.contains("[REDACTED_CODE]"))
        assertTrue(redacted.contains("[REDACTED_CARD]"))
        assertFalse(redacted.contains("9876543210"))
        assertFalse(redacted.contains("123456"))
    }

    @Test
    fun privacyCenterBlocksPluginWhenPolicyDisallowsAccess() {
        val privacy = MayraPrivacyCenter { 1_000L }
        val decision = privacy.evaluate(
            purpose = "external search",
            category = PersonalDataCategory.NOTE,
            operation = "read",
            userPresent = true,
            pluginId = "drive.plugin"
        )

        assertFalse(decision.allowed)
        assertTrue(decision.reason.contains("Plugin access"))
    }

    @Test
    fun explicitConsentAllowsSensitiveShareButKeepsConfirmation() {
        val privacy = MayraPrivacyCenter { 1_000L }
        privacy.setPolicy(
            RetentionPolicy(
                PersonalDataCategory.CONTACT_REFERENCE,
                RetentionMode.SESSION_ONLY,
                includeInSearch = false,
                allowProactiveUse = false,
                allowPluginAccess = true
            )
        )
        privacy.recordConsent(
            ConsentRecord(
                purpose = "contact export",
                category = PersonalDataCategory.CONTACT_REFERENCE,
                state = ConsentState.GRANTED,
                grantedAt = 900L,
                updatedAt = 900L
            )
        )

        val decision = privacy.evaluate(
            purpose = "contact export",
            category = PersonalDataCategory.CONTACT_REFERENCE,
            operation = "share",
            userPresent = true,
            pluginId = "contacts.plugin"
        )

        assertTrue(decision.allowed)
        assertTrue(decision.requiresConfirmation)
    }

    @Test
    fun retentionPolicyExpiresOldHabitData() {
        val now = 100 * DAY_MS
        val privacy = MayraPrivacyCenter { now }
        privacy.setPolicy(RetentionPolicy(PersonalDataCategory.HABIT, RetentionMode.DAYS, 30))

        assertFalse(privacy.shouldRetain(PersonalDataCategory.HABIT, now - 31 * DAY_MS, now))
        assertTrue(privacy.shouldRetain(PersonalDataCategory.HABIT, now - 29 * DAY_MS, now))
    }

    @Test
    fun deletionAndExportPlansExcludeSensitiveByDefault() {
        val privacy = MayraPrivacyCenter { 5_000L }
        val categories = setOf(PersonalDataCategory.NOTE, PersonalDataCategory.SENSITIVE_REFERENCE)

        val deletion = privacy.deletionPlan(categories)
        val export = privacy.exportPlan(categories, includeSensitive = false)

        assertEquals(2, deletion.operations.size)
        assertTrue(PersonalDataCategory.NOTE in export.included)
        assertTrue(PersonalDataCategory.SENSITIVE_REFERENCE in export.excluded)
    }

    private fun successPlugin(requiredPermission: String? = null): MayraPlugin = object : MayraPlugin {
        override val descriptor = PluginDescriptor(
            id = "test.success",
            displayName = "Test Search",
            version = "1",
            description = "test",
            capabilities = setOf(MayraCapability.SEARCH),
            requiredPermissions = requiredPermission?.let(::setOf).orEmpty()
        )

        override fun canHandle(request: PluginRequest) = if (request.operation == "search") 0.9 else 0.0
        override suspend fun execute(request: PluginRequest): PluginResult = PluginResult.Success("done", mapOf("result" to "value"))
    }

    private fun failingPlugin(): MayraPlugin = object : MayraPlugin {
        override val descriptor = PluginDescriptor(
            id = "test.failure",
            displayName = "Failing Search",
            version = "1",
            description = "test",
            capabilities = setOf(MayraCapability.SEARCH)
        )

        override fun canHandle(request: PluginRequest) = 1.0
        override suspend fun execute(request: PluginRequest): PluginResult = PluginResult.Failure("offline", true)
    }

    private companion object { const val DAY_MS = 24L * 60 * 60 * 1000 }
}
