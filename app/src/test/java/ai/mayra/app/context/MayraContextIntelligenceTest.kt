package ai.mayra.app.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraContextIntelligenceTest {
    @Test
    fun `security otp notification is highly sensitive and masked on lock screen`() {
        val engine = NotificationAttentionEngine(now = { 1_000L })
        val notification = notification(
            title = "Security alert",
            text = "Your OTP 123456 expires in 5 min",
            postedAt = 1_000L
        )

        val insight = engine.analyze(notification, AttentionContext(now = 1_000L, deviceLocked = true))

        assertEquals(NotificationCategory.SECURITY, insight.category)
        assertEquals(NotificationSensitivity.HIGHLY_SENSITIVE, insight.sensitivity)
        assertFalse(insight.maskedText.contains("123456"))
        assertTrue(insight.action in setOf(AttentionAction.ASK, AttentionAction.DEFER))
    }

    @Test
    fun `promotion is ignored or stored without interruption`() {
        val engine = NotificationAttentionEngine(now = { 1_000L })
        val insight = engine.analyze(
            notification(title = "Big Sale", text = "50% off coupon today", postedAt = 1_000L),
            AttentionContext(now = 1_000L)
        )

        assertEquals(NotificationCategory.PROMOTION, insight.category)
        assertTrue(insight.action in setOf(AttentionAction.IGNORE, AttentionAction.STORE_ONLY))
    }

    @Test
    fun `duplicate notification is suppressed`() {
        var clock = 1_000L
        val engine = NotificationAttentionEngine(now = { clock })
        val first = notification(id = "first", title = "Message", text = "Shiv sent you a message", postedAt = clock)
        val firstInsight = engine.analyze(first, AttentionContext(now = clock))
        clock += 500
        val second = first.copy(id = "second", postedAt = clock)

        val duplicate = engine.analyze(second, AttentionContext(now = clock))

        assertEquals(AttentionAction.STORE_ONLY, duplicate.action)
        assertEquals(firstInsight.notificationId, duplicate.duplicateOf)
    }

    @Test
    fun `quiet hours defer normal messages`() {
        val engine = NotificationAttentionEngine(now = { 1_000L })
        val insight = engine.analyze(
            notification(title = "WhatsApp", text = "New message from friend", postedAt = 1_000L, conversationKey = "friend"),
            AttentionContext(now = 1_000L, quietHours = true)
        )

        assertEquals(NotificationCategory.MESSAGE, insight.category)
        assertEquals(AttentionAction.DEFER, insight.action)
    }

    @Test
    fun `incoming call may interrupt while driving`() {
        val engine = NotificationAttentionEngine(now = { 1_000L })
        val insight = engine.analyze(
            notification(title = "Incoming call", text = "Shiv calling", postedAt = 1_000L),
            AttentionContext(now = 1_000L, driving = true)
        )

        assertEquals(NotificationCategory.CALL, insight.category)
        assertEquals(AttentionAction.INTERRUPT, insight.action)
    }

    @Test
    fun `app policy can disable notifications`() {
        val engine = NotificationAttentionEngine(now = { 1_000L })
        val insight = engine.analyze(
            notification(postedAt = 1_000L),
            AttentionContext(now = 1_000L),
            AppNotificationPolicy("com.test", enabled = false)
        )

        assertEquals(AttentionAction.IGNORE, insight.action)
        assertTrue("app_policy_disabled" in insight.reasons)
    }

    @Test
    fun `conversation resolves pronoun to recent person`() {
        var clock = 1_000L
        val engine = ConversationContextEngine(now = { clock })
        val shiv = engine.upsertEntity(
            ContextEntity(type = ContextEntityType.PERSON, canonicalName = "Shiv", aliases = setOf("beta"), updatedAt = clock)
        )
        engine.recordTurn(ContextTurn(source = ContextSource.USER, text = "Shiv ko call karna hai", timestamp = clock, entityIds = setOf(shiv.id)))
        clock += 100

        val resolution = engine.resolveReference("usko message bhejo", ContextEntityType.PERSON)

        assertFalse(resolution.clarificationNeeded)
        assertEquals("Shiv", resolution.entity?.canonicalName)
        assertTrue(resolution.resolvedText.contains("Shiv"))
    }

    @Test
    fun `ambiguous references request clarification`() {
        var clock = 1_000L
        val engine = ConversationContextEngine(now = { clock })
        engine.upsertEntity(ContextEntity(type = ContextEntityType.PERSON, canonicalName = "Shiv", aliases = setOf("Shiv"), updatedAt = clock))
        engine.upsertEntity(ContextEntity(type = ContextEntityType.PERSON, canonicalName = "Shiva", aliases = setOf("Shiv"), updatedAt = clock))

        val resolution = engine.resolveReference("Shiv ko message bhejo", ContextEntityType.PERSON)

        assertTrue(resolution.clarificationNeeded)
    }

    @Test
    fun `pending action collects required fields`() {
        var clock = 1_000L
        val engine = ConversationContextEngine(now = { clock })
        engine.setPendingAction(
            PendingConversationAction(
                description = "Create reminder",
                requiredFields = setOf("time", "title"),
                expiresAt = 10_000L
            )
        )

        val partial = engine.providePendingField("time", "8 AM")
        val complete = engine.providePendingField("title", "Medicine")

        assertFalse(partial?.complete ?: true)
        assertTrue(complete?.complete == true)
    }

    @Test
    fun `interrupted turn can be resumed once`() {
        val engine = ConversationContextEngine(now = { 1_000L })
        val turn = ContextTurn(source = ContextSource.USER, text = "Travel plan bana rahe the", timestamp = 1_000L, interrupted = true)
        engine.recordTurn(turn)

        assertEquals(turn.id, engine.resumeInterrupted()?.id)
        assertNull(engine.resumeInterrupted())
    }

    @Test
    fun `expired session starts fresh`() {
        var clock = 1_000L
        val engine = ConversationContextEngine(sessionTtlMillis = 100L, now = { clock })
        engine.recordTurn(ContextTurn(source = ContextSource.USER, text = "Old topic", timestamp = clock, topic = "travel"))
        clock += 200

        val snapshot = engine.snapshot()

        assertTrue(snapshot.turns.isEmpty())
        assertNull(snapshot.activeTopic)
    }

    @Test
    fun `sensitive turns and entities are excluded from safe snapshot`() {
        val engine = ConversationContextEngine(now = { 1_000L })
        engine.upsertEntity(ContextEntity(type = ContextEntityType.DOCUMENT, canonicalName = "Bank statement", sensitive = true, updatedAt = 1_000L))
        engine.recordTurn(ContextTurn(source = ContextSource.USER, text = "My account details", timestamp = 1_000L, sensitive = true))

        val safe = engine.snapshot()
        val full = engine.snapshot(includeSensitive = true)

        assertTrue(safe.turns.isEmpty())
        assertTrue(safe.entities.isEmpty())
        assertEquals(1, full.turns.size)
        assertEquals(1, full.entities.size)
    }

    @Test
    fun `fusion blocks speech when locked and sensitive interruption exists`() {
        val insight = NotificationInsight(
            notificationId = "n1",
            category = NotificationCategory.SECURITY,
            sensitivity = NotificationSensitivity.SENSITIVE,
            urgency = 0.95,
            importance = 0.95,
            attentionScore = 0.95,
            action = AttentionAction.INTERRUPT,
            maskedTitle = "Security",
            maskedText = "Hidden",
            summary = "Security alert",
            fingerprint = "abc",
            expiresAt = 10_000L
        )

        val fused = ContextFusionEngine().fuse(ContextFusionInput(notifications = listOf(insight), deviceLocked = true))

        assertFalse(fused.safeForSpeech)
        assertEquals(1, fused.attentionItems.size)
    }

    @Test
    fun `fusion keeps only useful notifications`() {
        fun insight(id: String, action: AttentionAction, score: Double) = NotificationInsight(
            notificationId = id,
            category = NotificationCategory.MESSAGE,
            sensitivity = NotificationSensitivity.PERSONAL,
            urgency = score,
            importance = score,
            attentionScore = score,
            action = action,
            maskedTitle = id,
            maskedText = id,
            summary = id,
            fingerprint = id,
            expiresAt = 10_000L
        )
        val fused = ContextFusionEngine().fuse(
            ContextFusionInput(notifications = listOf(
                insight("ignore", AttentionAction.IGNORE, 0.1),
                insight("summary", AttentionAction.SUMMARIZE, 0.6),
                insight("ask", AttentionAction.ASK, 0.8)
            ))
        )

        assertEquals(listOf("ask", "summary"), fused.attentionItems.map(NotificationInsight::notificationId))
    }

    private fun notification(
        id: String = "n1",
        title: String = "Message",
        text: String = "Hello",
        postedAt: Long,
        conversationKey: String? = null
    ) = ContextNotification(
        id = id,
        sourcePackage = "com.test",
        appLabel = "Test App",
        title = title,
        text = text,
        postedAt = postedAt,
        conversationKey = conversationKey
    )
}
