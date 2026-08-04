package ai.mayra.app.learning

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraLearningReviewControllerTest {
    @Test fun refreshBuildsBoundedOwnerSnapshot() = runBlocking {
        val gateway = FakeGateway(
            pendingItems = listOf(memory("pending", LearnedMemoryState.PENDING, 2)),
            approvedItems = (1..30).map { memory("approved-$it", LearnedMemoryState.APPROVED, it.toLong()) }
        )
        val controller = MayraLearningReviewController(gateway, now = { 1_000L }, tokenSource = { "token" })

        val state = controller.refresh(maxPerSection = 99)

        assertTrue(state is LearningReviewControllerState.Ready)
        state as LearningReviewControllerState.Ready
        assertEquals(1, state.snapshot.pendingCount)
        assertEquals(20, state.snapshot.approvedCount)
    }

    @Test fun approveRejectAndForgetUseGateway() = runBlocking {
        val gateway = FakeGateway()
        val controller = MayraLearningReviewController(gateway, tokenSource = { "token" })

        assertTrue(controller.approve(" language ") is LearningReviewActionResult.Success)
        assertTrue(controller.reject("routine") is LearningReviewActionResult.Success)
        assertTrue(controller.forget("theme") is LearningReviewActionResult.Success)
        assertEquals(listOf("language"), gateway.approvedKeys)
        assertEquals(listOf("routine"), gateway.rejectedKeys)
        assertEquals(listOf("theme"), gateway.forgottenKeys)
    }

    @Test fun blankKeyIsDeniedWithoutMutation() = runBlocking {
        val gateway = FakeGateway()
        val controller = MayraLearningReviewController(gateway)

        val result = controller.approve("   ")

        assertTrue(result is LearningReviewActionResult.Denied)
        assertTrue(gateway.approvedKeys.isEmpty())
    }

    @Test fun forgetAllRequiresMatchingUnexpiredSingleUseToken() = runBlocking {
        var clock = 10_000L
        val gateway = FakeGateway(forgetAllCount = 4)
        val controller = MayraLearningReviewController(
            gateway,
            now = { clock },
            tokenSource = { "owner-token" }
        )

        val challenge = controller.requestForgetAll()
        assertTrue(controller.confirmForgetAll("wrong") is LearningReviewActionResult.Denied)
        assertEquals(0, gateway.forgetAllCalls)

        val success = controller.confirmForgetAll(challenge.token)
        assertEquals(LearningReviewActionResult.Success("Forgot 4 memories", 4), success)
        assertEquals(1, gateway.forgetAllCalls)
        assertTrue(controller.confirmForgetAll(challenge.token) is LearningReviewActionResult.Denied)
    }

    @Test fun expiredForgetAllTokenCannotDelete() = runBlocking {
        var clock = 1_000L
        val gateway = FakeGateway(forgetAllCount = 8)
        val controller = MayraLearningReviewController(
            gateway,
            now = { clock },
            tokenSource = { "short-lived" }
        )

        val challenge = controller.requestForgetAll()
        clock = challenge.expiresAtEpochMs + 1

        assertTrue(controller.confirmForgetAll(challenge.token) is LearningReviewActionResult.Denied)
        assertEquals(0, gateway.forgetAllCalls)
    }

    private fun memory(key: String, state: LearnedMemoryState, updated: Long) = LearnedMemoryEntity(
        normalizedKey = key,
        displayKey = key,
        value = "value",
        category = LearningCategory.OTHER.name,
        source = LearningSource.EXPLICIT_OWNER_STATEMENT.name,
        confidence = 1.0,
        persistence = LearningPersistence.LONG_TERM.name,
        state = state.name,
        policyReason = "test",
        createdAtEpochMs = updated,
        updatedAtEpochMs = updated
    )

    private class FakeGateway(
        private val pendingItems: List<LearnedMemoryEntity> = emptyList(),
        private val approvedItems: List<LearnedMemoryEntity> = emptyList(),
        private val forgetAllCount: Int = 0
    ) : LearningReviewGateway {
        val approvedKeys = mutableListOf<String>()
        val rejectedKeys = mutableListOf<String>()
        val forgottenKeys = mutableListOf<String>()
        var forgetAllCalls = 0

        override suspend fun pending() = pendingItems
        override suspend fun approved(limit: Int) = approvedItems.take(limit)
        override suspend fun approve(key: String): Boolean = approvedKeys.add(key)
        override suspend fun reject(key: String): Boolean = rejectedKeys.add(key)
        override suspend fun forget(key: String): Boolean = forgottenKeys.add(key)
        override suspend fun forgetAll(): Int {
            forgetAllCalls += 1
            return forgetAllCount
        }
    }
}
