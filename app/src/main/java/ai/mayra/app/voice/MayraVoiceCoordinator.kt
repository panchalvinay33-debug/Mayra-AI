package ai.mayra.app.voice

import java.util.ArrayDeque

data class VoiceActionRequest(
    val actionKey: String,
    val payload: Map<String, String>,
    val intentId: String,
    val requiresConfirmation: Boolean,
    val sensitive: Boolean
)

data class VoiceTurnResult(
    val accepted: Boolean,
    val intent: ResolvedIntent?,
    val response: VoiceResponsePlan?,
    val action: VoiceActionRequest?,
    val session: VoiceSessionSnapshot,
    val reason: String? = null
)

data class VoiceCoordinatorDiagnostics(
    val turnsHandled: Long,
    val intentsResolved: Long,
    val clarificationsAsked: Long,
    val actionsPrepared: Long,
    val confirmationsResolved: Long,
    val contextInheritances: Long,
    val unknownIntents: Long,
    val averageIntentConfidence: Double,
    val runtime: VoiceRuntimeDiagnostics
)

class MayraVoiceCoordinator(
    private val runtime: VoiceSessionRuntime = VoiceSessionRuntime(),
    private val resolver: LocalIntentResolver = LocalIntentResolver(),
    private val dialog: VoiceDialogManager = VoiceDialogManager()
) {
    private var context = ConversationContext()
    private var pendingIntent: ResolvedIntent? = null
    private val recentActionFingerprints = ArrayDeque<Pair<String, Long>>()

    private var turnsHandled = 0L
    private var intentsResolved = 0L
    private var clarificationsAsked = 0L
    private var actionsPrepared = 0L
    private var confirmationsResolved = 0L
    private var contextInheritances = 0L
    private var unknownIntents = 0L
    private var confidenceTotal = 0.0

    @Synchronized
    fun startSession(topic: String? = null, locale: String = "hi-IN"): VoiceSessionSnapshot {
        context = ConversationContext(topic = topic, locale = locale)
        pendingIntent = null
        recentActionFingerprints.clear()
        return runtime.start(topic)
    }

    @Synchronized
    fun wake(): VoiceSessionSnapshot = runtime.wake()

    @Synchronized
    fun standby(): VoiceSessionSnapshot = runtime.standby()

    @Synchronized
    fun handleTranscript(
        text: String,
        recognitionConfidence: Double,
        sensitive: Boolean = false
    ): VoiceTurnResult {
        if (!runtime.acceptUserTurn(text, recognitionConfidence, sensitive)) {
            return VoiceTurnResult(
                accepted = false,
                intent = null,
                response = null,
                action = null,
                session = runtime.snapshot(),
                reason = "duplicate_turn_suppressed"
            )
        }
        turnsHandled++

        val normalized = text.trim()
        if (context.pendingConfirmationAction != null) {
            val confirmationIntent = resolver.resolve(normalized, context)
            if (confirmationIntent.type == MayraIntentType.CONFIRM) {
                return resolvePendingConfirmation(true, confirmationIntent)
            }
            if (confirmationIntent.type in setOf(MayraIntentType.REJECT, MayraIntentType.CANCEL)) {
                return resolvePendingConfirmation(false, confirmationIntent)
            }
        }

        val mergedContext = mergeClarificationAnswer(normalized)
        val intent = resolver.resolve(mergedContext.first, mergedContext.second)
        recordIntent(intent)
        val decision = dialog.decide(intent, mergedContext.second)
        runtime.setTopic(inferTopic(intent))
        runtime.enqueue(decision.responsePlan)

        if (decision.clarification != null) {
            clarificationsAsked++
            pendingIntent = intent
            context = mergedContext.second.copy(
                previousIntent = intent,
                pendingQuestion = decision.clarification.question,
                slots = mergedContext.second.slots + intent.entities.mapValues { it.value.value }
            )
            return VoiceTurnResult(
                accepted = true,
                intent = intent,
                response = decision.responsePlan,
                action = null,
                session = runtime.snapshot(),
                reason = "clarification_required"
            )
        }

        pendingIntent = null
        val payload = decision.actionPayload
        context = mergedContext.second.copy(
            topic = inferTopic(intent),
            previousIntent = intent,
            pendingQuestion = null,
            pendingConfirmationAction = decision.responsePlan.confirmation?.actionKey,
            slots = mergedContext.second.slots + payload
        )

        val action = when {
            decision.responsePlan.confirmation != null -> null
            !decision.shouldExecute || decision.actionKey == null -> null
            isDuplicateAction(decision.actionKey, payload) -> null
            else -> VoiceActionRequest(
                actionKey = decision.actionKey,
                payload = payload,
                intentId = intent.id,
                requiresConfirmation = false,
                sensitive = intent.sensitive
            ).also {
                rememberAction(it.actionKey, it.payload)
                actionsPrepared++
            }
        }
        return VoiceTurnResult(
            accepted = true,
            intent = intent,
            response = decision.responsePlan,
            action = action,
            session = runtime.snapshot(),
            reason = if (decision.responsePlan.confirmation != null) "confirmation_required" else null
        )
    }

    @Synchronized
    fun interrupt(): VoiceSessionSnapshot = runtime.interrupt()

    @Synchronized
    fun resume(): VoiceResponsePlan? = runtime.resumeInterrupted()

    @Synchronized
    fun nextResponse(): VoiceResponsePlan? = runtime.nextResponse()

    @Synchronized
    fun finishSpeaking(listenAgain: Boolean = true): VoiceSessionSnapshot = runtime.finishSpeaking(listenAgain)

    @Synchronized
    fun cancel(): VoiceSessionSnapshot {
        pendingIntent = null
        context = context.copy(pendingQuestion = null, pendingConfirmationAction = null)
        runtime.resolveConfirmation(false)
        return runtime.end()
    }

    @Synchronized
    fun session(): VoiceSessionSnapshot = runtime.snapshot()

    @Synchronized
    fun conversationContext(): ConversationContext = context

    @Synchronized
    fun recentTurns(limit: Int = 20, includeSensitive: Boolean = false): List<VoiceTurn> =
        runtime.recentTurns(limit, includeSensitive)

    @Synchronized
    fun diagnostics(): VoiceCoordinatorDiagnostics = VoiceCoordinatorDiagnostics(
        turnsHandled = turnsHandled,
        intentsResolved = intentsResolved,
        clarificationsAsked = clarificationsAsked,
        actionsPrepared = actionsPrepared,
        confirmationsResolved = confirmationsResolved,
        contextInheritances = contextInheritances,
        unknownIntents = unknownIntents,
        averageIntentConfidence = if (intentsResolved == 0L) 0.0 else confidenceTotal / intentsResolved,
        runtime = runtime.diagnostics()
    )

    private fun resolvePendingConfirmation(accepted: Boolean, confirmationIntent: ResolvedIntent): VoiceTurnResult {
        val confirmation = runtime.resolveConfirmation(accepted)
        confirmationsResolved++
        val response = if (accepted && confirmation != null) {
            VoiceResponsePlan("Theek hai, action confirm ho gayi.")
        } else {
            VoiceResponsePlan("Theek hai, maine action cancel kar di.")
        }
        runtime.enqueue(response)

        val action = if (accepted && confirmation != null && !isDuplicateAction(confirmation.actionKey, confirmation.payload)) {
            VoiceActionRequest(
                actionKey = confirmation.actionKey,
                payload = confirmation.payload,
                intentId = confirmationIntent.id,
                requiresConfirmation = true,
                sensitive = confirmation.sensitive
            ).also {
                rememberAction(it.actionKey, it.payload)
                actionsPrepared++
            }
        } else null

        context = context.copy(
            previousIntent = confirmationIntent,
            pendingConfirmationAction = null,
            pendingQuestion = null
        )
        return VoiceTurnResult(
            accepted = true,
            intent = confirmationIntent,
            response = response,
            action = action,
            session = runtime.snapshot(),
            reason = if (accepted) "confirmation_accepted" else "confirmation_rejected"
        )
    }

    private fun mergeClarificationAnswer(text: String): Pair<String, ConversationContext> {
        val previous = pendingIntent ?: return text to context
        if (context.pendingQuestion == null || previous.missingSlots.isEmpty()) return text to context
        val missing = previous.missingSlots.first()
        val slots = context.slots + (missing to text)
        val reconstructed = when (previous.type) {
            MayraIntentType.OPEN_APP -> "open ${slots["app"].orEmpty()}"
            MayraIntentType.CALL_CONTACT -> "call ${slots["contact"].orEmpty()}"
            MayraIntentType.SEND_MESSAGE -> "message ${slots["message"].orEmpty()} to ${slots["contact"].orEmpty()}"
            MayraIntentType.CREATE_REMINDER -> "remind me to ${slots["content"].orEmpty()} ${slots["time"].orEmpty()}"
            MayraIntentType.CREATE_NOTE -> "note ${slots["content"].orEmpty()}"
            MayraIntentType.ADD_TO_LIST -> "add ${slots["content"].orEmpty()} to list ${slots["list"].orEmpty()}"
            MayraIntentType.SEARCH_MEMORY -> "search ${slots["query"].orEmpty()}"
            MayraIntentType.DEVICE_CONTROL -> "${slots["control"].orEmpty()} ${slots["operation"].orEmpty()}"
            else -> text
        }
        return reconstructed to context.copy(slots = slots, pendingQuestion = null)
    }

    private fun recordIntent(intent: ResolvedIntent) {
        intentsResolved++
        confidenceTotal += intent.confidence
        if (intent.inheritedContext) contextInheritances++
        if (intent.type == MayraIntentType.UNKNOWN) unknownIntents++
    }

    private fun inferTopic(intent: ResolvedIntent): String = when (intent.type) {
        MayraIntentType.OPEN_APP, MayraIntentType.DEVICE_CONTROL -> "device"
        MayraIntentType.CALL_CONTACT, MayraIntentType.SEND_MESSAGE -> "communication"
        MayraIntentType.CREATE_REMINDER, MayraIntentType.CREATE_NOTE, MayraIntentType.ADD_TO_LIST -> "personal_memory"
        MayraIntentType.SEARCH_MEMORY -> "knowledge_search"
        else -> context.topic ?: "general"
    }

    private fun actionFingerprint(actionKey: String, payload: Map<String, String>): String =
        actionKey + "|" + payload.toSortedMap().entries.joinToString("|") { "${it.key}=${it.value.trim().lowercase()}" }

    private fun isDuplicateAction(actionKey: String, payload: Map<String, String>): Boolean {
        pruneActionFingerprints()
        val fingerprint = actionFingerprint(actionKey, payload)
        return recentActionFingerprints.any { it.first == fingerprint }
    }

    private fun rememberAction(actionKey: String, payload: Map<String, String>) {
        pruneActionFingerprints()
        if (recentActionFingerprints.size >= MAX_ACTION_FINGERPRINTS) recentActionFingerprints.removeFirst()
        recentActionFingerprints.addLast(actionFingerprint(actionKey, payload) to System.currentTimeMillis())
    }

    private fun pruneActionFingerprints() {
        val cutoff = System.currentTimeMillis() - ACTION_DEDUP_WINDOW_MS
        while (recentActionFingerprints.isNotEmpty() && recentActionFingerprints.first().second < cutoff) {
            recentActionFingerprints.removeFirst()
        }
    }

    companion object {
        const val ACTION_DEDUP_WINDOW_MS = 15_000L
        const val MAX_ACTION_FINGERPRINTS = 30
    }
}
