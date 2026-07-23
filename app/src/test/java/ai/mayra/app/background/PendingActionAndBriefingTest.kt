package ai.mayra.app.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingActionAndBriefingTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_pending_actions", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_trust_audit", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_briefing_cache", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun actionMustBeApprovedBeforeExecution() {
        val store = PendingActionStore(context)
        val action = store.add(
            PendingAction(
                id = "call-1",
                type = PendingActionType.CALL,
                title = "Call Mummy",
                payload = "+919999999999",
                createdAt = 1_000L
            )
        )

        assertNull(store.markExecuted(action.id, now = 1_100L))
        assertEquals(PendingActionState.APPROVED, store.approve(action.id, 1_200L)?.state)
        assertEquals(PendingActionState.EXECUTED, store.markExecuted(action.id, 1_300L)?.state)

        val outcomes = TrustAuditStore(context).snapshot().map(AuditEntry::outcome)
        assertTrue(AuditOutcome.CREATED in outcomes)
        assertTrue(AuditOutcome.APPROVED in outcomes)
        assertTrue(AuditOutcome.EXECUTED in outcomes)
    }

    @Test
    fun expiredActionCannotBeApproved() {
        val store = PendingActionStore(context)
        store.add(
            PendingAction(
                id = "otp-action",
                type = PendingActionType.SECURITY_REVIEW,
                title = "Review security alert",
                payload = "alert",
                createdAt = 1_000L,
                expiresAt = 2_000L
            )
        )

        assertNull(store.approve("otp-action", now = 2_100L))
        assertEquals(1, store.expireDue(now = 2_100L))
        assertEquals(PendingActionState.EXPIRED, store.snapshot().single().state)
    }

    @Test
    fun failedActionStoresSanitizedError() {
        val store = PendingActionStore(context)
        store.add(
            PendingAction(
                id = "message-1",
                type = PendingActionType.MESSAGE,
                title = "Message Papa",
                payload = "I am late",
                createdAt = 1_000L
            )
        )

        val failed = store.markFailed("message-1", "network unavailable", now = 1_500L)
        assertEquals(PendingActionState.FAILED, failed?.state)
        assertEquals("network unavailable", failed?.lastError)
        assertEquals(AuditOutcome.FAILED, TrustAuditStore(context).snapshot().first().outcome)
    }

    @Test
    fun briefingCacheKeepsMorningAndEveningSeparately() {
        val cache = BriefingCache(context)
        cache.save(BriefingKind.MORNING, "Morning text", 1_000L)
        cache.save(BriefingKind.EVENING, "Evening text", 2_000L)

        assertEquals("Morning text", cache.read(BriefingKind.MORNING)?.text)
        assertEquals(1_000L, cache.read(BriefingKind.MORNING)?.generatedAt)
        assertEquals("Evening text", cache.read(BriefingKind.EVENING)?.text)
    }

    @Test
    fun clearingAuditLeavesTransparentClearMarker() {
        val audit = TrustAuditStore(context)
        audit.append(
            AuditEntry(
                actionId = "x",
                actionType = "CALL",
                outcome = AuditOutcome.CREATED,
                summary = "Call created",
                timestamp = 1_000L
            )
        )

        audit.clear(now = 2_000L)
        val entries = audit.snapshot()
        assertEquals(1, entries.size)
        assertEquals(AuditOutcome.CLEARED, entries.single().outcome)
    }
}
