package ai.mayra.app.voice

import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.max

enum class VoiceSessionState {
    IDLE,
    STANDBY,
    LISTENING,
    PROCESSING,
    SPEAKING,
    INTERRUPTED,
    WAITING_FOR_CONFIRMATION,
    ERROR
}

enum class VoiceTurnRole { USER, ASSISTANT, SYSTEM }

enum class VoiceOutputMode { SPEAK, SCREEN_ONLY, SILENT, ASK, CONFIRM }

data class VoiceTurn(
    val id: String = UUID.randomUUID().toString(),
    val role: VoiceTurnRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val recognitionConfidence: Double? = null,
    val intentId: String? = null,
    val sensitive: Boolean = false
) {
    init {
        require(text.isNotBlank())
        recognitionConfidence?.let { require(it in 0.0..1.0) }
    }
}

data class PendingVoiceConfirmation(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val actionKey: String,
    val payload: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DEFAULT_CONFIRMATION_TTL_MS,
    val sensitive: Boolean = false
) {
    fun expired(now: Long = System.currentTimeMillis()): Boolean = now >= expiresAt

    companion object {
        const val DEFAULT_CONFIRMATION_TTL_MS = 2 * 60 * 1000L
    }
}

data class VoiceResponsePlan(
    val spokenText: String,
    val screenText: String = spokenText,
    val mode: VoiceOutputMode = VoiceOutputMode.SPEAK,
    val suggestions: List<String> = emptyList(),
    val confirmation: PendingVoiceConfirmation? = null,
    val shouldEndSession: Boolean = false,
    val sensitive: Boolean = false
) {
    init {
        require(spokenText.isNotBlank() || screenText.isNotBlank())
        require(suggestions.size <= 5)
    }
}

data class VoiceSessionSnapshot(
    val sessionId: String,
    val state: VoiceSessionState,
    val activeTopic: String?,
    val turnCount: Int,
    val queuedResponses: Int,
    val pendingConfirmation: PendingVoiceConfirmation?,
    val interruptedResponse: VoiceResponsePlan?,
    val startedAt: Long,
    val lastActivityAt: Long
)

data class VoiceRuntimeDiagnostics(
    val sessionsStarted: Long,
    val turnsAccepted: Long,
    val lowConfidenceTurns: Long,
    val interruptions: Long,
    val confirmationsRequested: Long,
    val confirmationsAccepted: Long,
    val confirmationsRejected: Long,
    val duplicateTurnsSuppressed: Long,
    val averageRecognitionConfidence: Double
)

class VoiceSessionRuntime(
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private var sessionId: String = UUID.randomUUID().toString()
    private var state: VoiceSessionState = VoiceSessionState.IDLE
    private var activeTopic: String? = null
    private var startedAt: Long = now()
    private var lastActivityAt: Long = startedAt
    private val turns = ArrayDeque<VoiceTurn>()
    private val responseQueue = ArrayDeque<VoiceResponsePlan>()
    private var pendingConfirmation: PendingVoiceConfirmation? = null
    private var interruptedResponse: VoiceResponsePlan? = null
    private var lastUserFingerprint: String? = null
    private var lastUserFingerprintAt: Long = 0L

    private var sessionsStarted = 0L
    private var turnsAccepted = 0L
    private var lowConfidenceTurns = 0L
    private var interruptions = 0L
    private var confirmationsRequested = 0L
    private var confirmationsAccepted = 0L
    private var confirmationsRejected = 0L
    private var duplicateTurnsSuppressed = 0L
    private var confidenceTotal = 0.0
    private var confidenceSamples = 0L

    @Synchronized
    fun start(topic: String? = null, standby: Boolean = false): VoiceSessionSnapshot {
        sessionId = UUID.randomUUID().toString()
        state = if (standby) VoiceSessionState.STANDBY else VoiceSessionState.LISTENING
        activeTopic = topic?.trim()?.takeIf { it.isNotEmpty() }
        startedAt = now()
        lastActivityAt = startedAt
        turns.clear()
        responseQueue.clear()
        pendingConfirmation = null
        interruptedResponse = null
        lastUserFingerprint = null
        sessionsStarted++
        return snapshot()
    }

    @Synchronized
    fun wake(): VoiceSessionSnapshot {
        if (state == VoiceSessionState.IDLE) return start()
        state = VoiceSessionState.LISTENING
        touch()
        return snapshot()
    }

    @Synchronized
    fun standby(): VoiceSessionSnapshot {
        require(state != VoiceSessionState.PROCESSING) { "Cannot enter standby while processing" }
        state = VoiceSessionState.STANDBY
        touch()
        return snapshot()
    }

    @Synchronized
    fun beginListening(): VoiceSessionSnapshot {
        require(state !in setOf(VoiceSessionState.IDLE, VoiceSessionState.ERROR)) { "Session is not active" }
        expireConfirmationIfNeeded()
        state = VoiceSessionState.LISTENING
        touch()
        return snapshot()
    }

    @Synchronized
    fun acceptUserTurn(text: String, confidence: Double, sensitive: Boolean = false): Boolean {
        require(confidence in 0.0..1.0)
        require(state in setOf(VoiceSessionState.LISTENING, VoiceSessionState.WAITING_FOR_CONFIRMATION, VoiceSessionState.INTERRUPTED)) {
            "Voice runtime is not accepting user input in state $state"
        }
        val normalized = normalize(text)
        require(normalized.isNotBlank())
        val fingerprint = normalized.lowercase()
        val current = now()
        if (fingerprint == lastUserFingerprint && current - lastUserFingerprintAt <= DUPLICATE_WINDOW_MS) {
            duplicateTurnsSuppressed++
            return false
        }
        lastUserFingerprint = fingerprint
        lastUserFingerprintAt = current
        addTurn(VoiceTurn(role = VoiceTurnRole.USER, text = normalized, recognitionConfidence = confidence, sensitive = sensitive))
        turnsAccepted++
        confidenceTotal += confidence
        confidenceSamples++
        if (confidence < LOW_CONFIDENCE_THRESHOLD) lowConfidenceTurns++
        state = VoiceSessionState.PROCESSING
        touch()
        return true
    }

    @Synchronized
    fun setTopic(topic: String?) {
        activeTopic = topic?.trim()?.take(MAX_TOPIC_LENGTH)?.takeIf { it.isNotEmpty() }
        touch()
    }

    @Synchronized
    fun enqueue(plan: VoiceResponsePlan): VoiceSessionSnapshot {
        if (responseQueue.size >= MAX_RESPONSE_QUEUE) responseQueue.removeFirst()
        responseQueue.addLast(plan)
        plan.confirmation?.let {
            pendingConfirmation = it
            confirmationsRequested++
        }
        if (state == VoiceSessionState.PROCESSING || state == VoiceSessionState.LISTENING) {
            state = if (plan.confirmation != null) VoiceSessionState.WAITING_FOR_CONFIRMATION else VoiceSessionState.SPEAKING
        }
        touch()
        return snapshot()
    }

    @Synchronized
    fun nextResponse(): VoiceResponsePlan? {
        expireConfirmationIfNeeded()
        val plan = responseQueue.pollFirst() ?: return null
        addTurn(
            VoiceTurn(
                role = VoiceTurnRole.ASSISTANT,
                text = plan.screenText.ifBlank { plan.spokenText },
                sensitive = plan.sensitive
            )
        )
        state = when {
            plan.confirmation != null -> VoiceSessionState.WAITING_FOR_CONFIRMATION
            plan.shouldEndSession -> VoiceSessionState.IDLE
            else -> VoiceSessionState.SPEAKING
        }
        touch()
        return plan
    }

    @Synchronized
    fun finishSpeaking(listenAgain: Boolean = true): VoiceSessionSnapshot {
        if (state == VoiceSessionState.IDLE) return snapshot()
        state = when {
            pendingConfirmation != null -> VoiceSessionState.WAITING_FOR_CONFIRMATION
            listenAgain -> VoiceSessionState.LISTENING
            else -> VoiceSessionState.STANDBY
        }
        touch()
        return snapshot()
    }

    @Synchronized
    fun interrupt(): VoiceSessionSnapshot {
        if (state == VoiceSessionState.SPEAKING) {
            interruptedResponse = responseQueue.peekFirst()
            interruptions++
        }
        state = VoiceSessionState.INTERRUPTED
        touch()
        return snapshot()
    }

    @Synchronized
    fun resumeInterrupted(): VoiceResponsePlan? {
        val plan = interruptedResponse ?: return null
        interruptedResponse = null
        state = VoiceSessionState.SPEAKING
        touch()
        return plan
    }

    @Synchronized
    fun resolveConfirmation(accepted: Boolean): PendingVoiceConfirmation? {
        expireConfirmationIfNeeded()
        val confirmation = pendingConfirmation ?: return null
        pendingConfirmation = null
        if (accepted) confirmationsAccepted++ else confirmationsRejected++
        state = VoiceSessionState.PROCESSING
        touch()
        return confirmation
    }

    @Synchronized
    fun fail(message: String): VoiceSessionSnapshot {
        addTurn(VoiceTurn(role = VoiceTurnRole.SYSTEM, text = normalize(message)))
        state = VoiceSessionState.ERROR
        touch()
        return snapshot()
    }

    @Synchronized
    fun end(): VoiceSessionSnapshot {
        pendingConfirmation = null
        interruptedResponse = null
        responseQueue.clear()
        state = VoiceSessionState.IDLE
        touch()
        return snapshot()
    }

    @Synchronized
    fun recentTurns(limit: Int = 20, includeSensitive: Boolean = false): List<VoiceTurn> =
        turns.asSequence()
            .filter { includeSensitive || !it.sensitive }
            .toList()
            .takeLast(limit.coerceIn(1, MAX_TURNS))

    @Synchronized
    fun snapshot(): VoiceSessionSnapshot {
        expireConfirmationIfNeeded()
        return VoiceSessionSnapshot(
            sessionId = sessionId,
            state = state,
            activeTopic = activeTopic,
            turnCount = turns.size,
            queuedResponses = responseQueue.size,
            pendingConfirmation = pendingConfirmation,
            interruptedResponse = interruptedResponse,
            startedAt = startedAt,
            lastActivityAt = lastActivityAt
        )
    }

    @Synchronized
    fun diagnostics(): VoiceRuntimeDiagnostics = VoiceRuntimeDiagnostics(
        sessionsStarted = sessionsStarted,
        turnsAccepted = turnsAccepted,
        lowConfidenceTurns = lowConfidenceTurns,
        interruptions = interruptions,
        confirmationsRequested = confirmationsRequested,
        confirmationsAccepted = confirmationsAccepted,
        confirmationsRejected = confirmationsRejected,
        duplicateTurnsSuppressed = duplicateTurnsSuppressed,
        averageRecognitionConfidence = if (confidenceSamples == 0L) 0.0 else confidenceTotal / confidenceSamples
    )

    private fun addTurn(turn: VoiceTurn) {
        if (turns.size >= MAX_TURNS) turns.removeFirst()
        turns.addLast(turn)
    }

    private fun expireConfirmationIfNeeded() {
        val pending = pendingConfirmation ?: return
        if (pending.expired(now())) {
            pendingConfirmation = null
            if (state == VoiceSessionState.WAITING_FOR_CONFIRMATION) state = VoiceSessionState.LISTENING
        }
    }

    private fun touch() {
        lastActivityAt = max(lastActivityAt, now())
    }

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(MAX_TEXT_LENGTH)

    companion object {
        const val LOW_CONFIDENCE_THRESHOLD = 0.55
        const val DUPLICATE_WINDOW_MS = 2_000L
        const val MAX_TURNS = 80
        const val MAX_RESPONSE_QUEUE = 12
        const val MAX_TOPIC_LENGTH = 120
        const val MAX_TEXT_LENGTH = 4_000
    }
}
