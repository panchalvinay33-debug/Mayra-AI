package ai.mayra.app.core

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraRuntimeAuditTest {
    private val capabilities = MayraRuntimeCapabilities(deviceActions = true)

    @Test
    fun actionKeyIsStableAcrossWhitespaceAndCase() {
        val first = MayraQueryRouter.route("Open file manager")
        val second = MayraQueryRouter.route("  OPEN   FILE   MANAGER  ")

        assertEquals(
            MayraActionIdempotency.key("Open file manager", first),
            MayraActionIdempotency.key("  OPEN   FILE   MANAGER  ", second)
        )
    }

    @Test
    fun differentActionsHaveDifferentKeys() {
        val open = MayraQueryRouter.route("Open file manager")
        val launch = MayraQueryRouter.route("Launch file manager")

        assertNotEquals(
            MayraActionIdempotency.key("Open file manager", open),
            MayraActionIdempotency.key("Launch file manager", launch)
        )
    }

    @Test
    fun successfulActionBlocksDuplicateBeforeHandler() {
        var calls = 0
        val log = MayraInMemoryActivityLog()
        val runtime = MayraRoutingRuntime(
            capabilities = capabilities,
            handlers = MayraRuntimeHandlers(act = MayraRouteHandler { _, _ -> calls++; "opened" }),
            activityRecorder = MayraActivityRecorder(log)
        )

        val first = runtime.dispatch("Open file manager")
        val second = runtime.dispatch("Open file manager")

        assertTrue(first is MayraRoutingRuntimeResult.Executed)
        assertTrue(second is MayraRoutingRuntimeResult.DuplicateBlocked)
        assertEquals(1, calls)
        assertEquals(
            listOf(MayraActivityStatus.EXECUTED, MayraActivityStatus.DUPLICATE_BLOCKED),
            log.snapshot().map { it.status }
        )
    }

    @Test
    fun failedActionReleasesReservationForRetry() {
        var calls = 0
        val runtime = MayraRoutingRuntime(
            capabilities = capabilities,
            handlers = MayraRuntimeHandlers(act = MayraRouteHandler { _, _ ->
                calls++
                if (calls == 1) error("temporary") else "opened"
            })
        )

        assertTrue(runtime.dispatch("Open file manager") is MayraRoutingRuntimeResult.Failed)
        assertTrue(runtime.dispatch("Open file manager") is MayraRoutingRuntimeResult.Executed)
        assertEquals(2, calls)
    }

    @Test
    fun confirmationDoesNotReserveActionKey() {
        val store = MayraInMemoryIdempotencyStore()
        val runtime = MayraRoutingRuntime(
            capabilities = capabilities,
            handlers = MayraRuntimeHandlers(act = MayraRouteHandler { _, _ -> "deleted" }),
            idempotencyStore = store
        )

        val confirmation = runtime.dispatch("Delete file report.pdf")
        assertTrue(confirmation is MayraRoutingRuntimeResult.ConfirmationRequired)

        val confirmedDecision = MayraQueryRouter.route("Delete file report.pdf").copy(requiresConfirmation = false)
        val confirmedPlan = MayraRoutingPolicy.plan(confirmedDecision, capabilities)
        assertTrue(runtime.dispatch("Delete file report.pdf", confirmedPlan) is MayraRoutingRuntimeResult.Executed)
    }

    @Test
    fun readOnlyRequestsAreNotDeduplicated() {
        var calls = 0
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(coreAssistant = true),
            handlers = MayraRuntimeHandlers(answer = MayraRouteHandler { _, _ -> "answer-${++calls}" })
        )

        assertTrue(runtime.dispatch("Hello") is MayraRoutingRuntimeResult.Executed)
        assertTrue(runtime.dispatch("Hello") is MayraRoutingRuntimeResult.Executed)
        assertEquals(2, calls)
    }

    @Test
    fun activityRecordUsesInjectedClockAndSnapshotIsDefensive() {
        val instant = Instant.parse("2026-07-28T12:00:00Z")
        val log = MayraInMemoryActivityLog()
        val recorder = MayraActivityRecorder(log, Clock.fixed(instant, ZoneOffset.UTC))
        val plan = MayraRoutingPolicy.routeAndPlan("Hello", MayraRuntimeCapabilities())

        recorder.record(plan, MayraActivityStatus.EXECUTED, "ok")
        val first = log.snapshot()
        val second = log.snapshot()

        assertEquals(instant, first.single().timestamp)
        assertEquals(first, second)
        assertFalse(first === second)
        assertTrue(first.single().id.isNotBlank())
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonActionRecordRejectsIdempotencyKey() {
        MayraActivityRecord(
            id = "id",
            timestamp = Instant.EPOCH,
            outcome = MayraRoutingOutcome.ANSWER,
            disposition = MayraRouteDisposition.EXECUTE,
            status = MayraActivityStatus.EXECUTED,
            capability = MayraRequiredCapability.CORE_ASSISTANT,
            idempotencyKey = "bad",
            detail = "ok"
        )
    }
}