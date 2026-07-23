package ai.mayra.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraVoiceEngineTest {

    @Test
    fun voiceRuntimeMovesThroughListeningProcessingAndSpeaking() {
        var clock = 1_000L
        val runtime = VoiceSessionRuntime { clock }

        assertEquals(VoiceState.LISTENING, runtime.start().state)
        assertTrue(runtime.acceptUserTurn("WhatsApp kholo", 0.91))
        assertEquals(VoiceState.PROCESSING, runtime.snapshot().state)

        runtime.enqueue(VoiceResponsePlan("WhatsApp khol rahi hoon."))
        assertEquals(VoiceState.SPEAKING, runtime.snapshot().state)
        assertNotNull(runtime.nextResponse())
        assertEquals(VoiceState.LISTENING, runtime.finishSpeaking().state)
    }

    @Test
    fun duplicateVoiceTurnIsSuppressedInsideWindow() {
        var clock = 1_000L
        val runtime = VoiceSessionRuntime { clock }
        runtime.start()

        assertTrue(runtime.acceptUserTurn("Torch on", 0.90))
        runtime.enqueue(VoiceResponsePlan("Processing"))
        runtime.nextResponse()
        runtime.finishSpeaking()

        clock += 500L
        assertFalse(runtime.acceptUserTurn("  Torch   on ", 0.90))
        assertEquals(1L, runtime.diagnostics().duplicateTurnsSuppressed)
    }

    @Test
    fun sensitiveTurnsAreHiddenByDefault() {
        val runtime = VoiceSessionRuntime()
        runtime.start()
        runtime.acceptUserTurn("Send private message", 0.90, sensitive = true)

        assertTrue(runtime.recentTurns().isEmpty())
        assertEquals(1, runtime.recentTurns(includeSensitive = true).size)
    }

    @Test
    fun resolverExtractsAppAndDeviceControl() {
        val resolver = LocalIntentResolver()

        val open = resolver.resolve("Open WhatsApp")
        assertEquals(MayraIntentType.OPEN_APP, open.type)
        assertEquals("WhatsApp", open.entities["app"]?.value)
        assertTrue(open.complete)

        val control = resolver.resolve("Bluetooth band karo")
        assertEquals(MayraIntentType.DEVICE_CONTROL, control.type)
        assertEquals("bluetooth", control.entities["control"]?.value)
        assertEquals("off", control.entities["operation"]?.value)
    }

    @Test
    fun messageIntentRequiresContactAndConfirmation() {
        val resolver = LocalIntentResolver()
        val intent = resolver.resolve("message ghar pahunch gaya to Shiv")

        assertEquals(MayraIntentType.SEND_MESSAGE, intent.type)
        assertEquals("ghar pahunch gaya", intent.entities["message"]?.value)
        assertEquals("Shiv", intent.entities["contact"]?.value)
        assertTrue(intent.requiresConfirmation)
        assertTrue(intent.sensitive)
    }

    @Test
    fun missingReminderTimeProducesClarification() {
        val resolver = LocalIntentResolver()
        val dialog = VoiceDialogManager()
        val intent = resolver.resolve("reminder dawa lena")
        val decision = dialog.decide(intent)

        assertEquals(MayraIntentType.CREATE_REMINDER, intent.type)
        assertTrue("time" in intent.missingSlots)
        assertNotNull(decision.clarification)
        assertEquals(VoiceOutputMode.ASK, decision.responsePlan.mode)
        assertFalse(decision.shouldExecute)
    }

    @Test
    fun coordinatorRecoversReminderAcrossTwoTurns() {
        val coordinator = MayraVoiceCoordinator()
        coordinator.startSession()

        val first = coordinator.handleTranscript("reminder dawa lena", 0.92)
        assertNull(first.action)
        assertEquals("clarification_required", first.reason)
        coordinator.nextResponse()
        coordinator.finishSpeaking()

        val second = coordinator.handleTranscript("kal subah", 0.94)
        assertEquals(MayraIntentType.CREATE_REMINDER, second.intent?.type)
        assertNotNull(second.action)
        assertEquals("personal.create_reminder", second.action?.actionKey)
        assertTrue(second.action?.payload?.get("time")?.contains("kal", ignoreCase = true) == true)
    }

    @Test
    fun messageActionIsBlockedUntilVoiceConfirmation() {
        val coordinator = MayraVoiceCoordinator()
        coordinator.startSession()

        val request = coordinator.handleTranscript("message ghar pahunch gaya to Shiv", 0.96)
        assertNull(request.action)
        assertEquals(VoiceOutputMode.CONFIRM, request.response?.mode)
        assertNotNull(request.session.pendingConfirmation)

        val confirmed = coordinator.handleTranscript("haan", 0.98)
        assertNotNull(confirmed.action)
        assertEquals("communication.send_message", confirmed.action?.actionKey)
        assertTrue(confirmed.action?.requiresConfirmation == true)
        assertEquals("confirmation_accepted", confirmed.reason)
    }

    @Test
    fun rejectedConfirmationNeverCreatesAction() {
        val coordinator = MayraVoiceCoordinator()
        coordinator.startSession()

        coordinator.handleTranscript("call Shiv", 0.96)
        val rejected = coordinator.handleTranscript("nahi", 0.96)

        assertNull(rejected.action)
        assertEquals("confirmation_rejected", rejected.reason)
        assertNull(rejected.session.pendingConfirmation)
    }

    @Test
    fun interruptionCanResumeResponsePlan() {
        val coordinator = MayraVoiceCoordinator()
        coordinator.startSession()
        val result = coordinator.handleTranscript("open YouTube", 0.93)
        assertNotNull(result.response)

        coordinator.interrupt()
        val resumed = coordinator.resume()
        assertNotNull(resumed)
        assertTrue(resumed?.spokenText?.contains("YouTube", ignoreCase = true) == true)
    }

    @Test
    fun lowConfidenceUnknownInputIsClarifiedInsteadOfExecuted() {
        val coordinator = MayraVoiceCoordinator()
        coordinator.startSession()

        val result = coordinator.handleTranscript("hmm woh wala karna", 0.30)

        assertNull(result.action)
        assertEquals(VoiceOutputMode.ASK, result.response?.mode)
        assertEquals("clarification_required", result.reason)
        assertEquals(1L, coordinator.diagnostics().unknownIntents)
    }

    @Test
    fun duplicateConfirmedActionIsNotPreparedTwice() {
        val coordinator = MayraVoiceCoordinator()
        coordinator.startSession()

        coordinator.handleTranscript("open WhatsApp", 0.95)
        coordinator.nextResponse()
        coordinator.finishSpeaking()
        val firstCount = coordinator.diagnostics().actionsPrepared

        val duplicate = coordinator.handleTranscript("open WhatsApp", 0.95)
        assertNull(duplicate.action)
        assertEquals(firstCount, coordinator.diagnostics().actionsPrepared)
    }

    @Test
    fun diagnosticsTrackConfidenceAndClarifications() {
        val coordinator = MayraVoiceCoordinator()
        coordinator.startSession()
        coordinator.handleTranscript("reminder medicine", 0.80)

        val diagnostics = coordinator.diagnostics()
        assertEquals(1L, diagnostics.turnsHandled)
        assertEquals(1L, diagnostics.intentsResolved)
        assertEquals(1L, diagnostics.clarificationsAsked)
        assertTrue(diagnostics.averageIntentConfidence in 0.0..1.0)
        assertEquals(1L, diagnostics.runtime.turnsAccepted)
    }
}
