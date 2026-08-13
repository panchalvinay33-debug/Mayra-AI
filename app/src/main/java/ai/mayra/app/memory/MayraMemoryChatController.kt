package ai.mayra.app.memory

import java.time.Clock
import java.time.Instant

data class PendingMemoryApproval(
    val proposalId: String,
    val key: String,
    val newValue: String,
    val previousValue: String? = null
)

sealed interface MayraMemoryChatResult {
    data object NotHandled : MayraMemoryChatResult
    data class Reply(val text: String) : MayraMemoryChatResult
    data class NeedsApproval(val pending: PendingMemoryApproval) : MayraMemoryChatResult
}

/** Deterministic, model-independent chat commands for owner-controlled personal memory. */
class MayraMemoryChatController(
    private val manager: MayraPersonalMemoryManager,
    private val clock: Clock = Clock.systemUTC(),
    private val preferenceLearner: MayraPreferenceLearner = MayraPreferenceLearner(clock)
) {
    fun handle(message: String): MayraMemoryChatResult {
        val text = message.trim()
        if (text.isEmpty()) return MayraMemoryChatResult.NotHandled

        CONFIRM.matchEntire(text)?.groupValues?.get(1)?.let { proposalId ->
            return approve(proposalId)
        }
        CANCEL.matchEntire(text)?.groupValues?.get(1)?.let { proposalId ->
            return cancel(proposalId)
        }
        REMEMBER.matchEntire(text)?.let { match ->
            val key = match.groupValues[1].trim()
            val value = match.groupValues[2].trim()
            val candidate = MayraMemoryCandidate(
                key = key,
                value = value,
                category = inferCategory(key),
                provenance = MayraMemoryProvenance("chat", "owner-command", Instant.now(clock))
            )
            return proposalResult(manager.propose(candidate))
        }
        FORGET.matchEntire(text)?.groupValues?.get(1)?.trim()?.let { query ->
            val matches = manager.retrieve(query, limit = 10)
            return when (matches.size) {
                0 -> MayraMemoryChatResult.Reply("I could not find an approved memory matching: $query")
                1 -> {
                    manager.delete(matches.single().id)
                    MayraMemoryChatResult.Reply("Forgot: ${matches.single().key}.")
                }
                else -> MayraMemoryChatResult.Reply(
                    "I found multiple memories. Please be more specific: " + matches.joinToString { it.key }
                )
            }
        }
        if (LIST.matches(text)) {
            val memories = manager.activeMemories()
            return MayraMemoryChatResult.Reply(
                if (memories.isEmpty()) "I do not have any approved personal memories yet."
                else memories.take(10).joinToString(prefix = "Approved memories: ", separator = "; ") { "${it.key} = ${it.value}" }
            )
        }

        // Safe self-learning: only explicit standalone preferences are recognized, and even those
        // become ordinary pending proposals. Nothing is written to trusted memory until approval.
        preferenceLearner.observe(text)?.let { preference ->
            return proposalResult(manager.propose(preferenceLearner.toCandidate(preference)))
        }

        return MayraMemoryChatResult.NotHandled
    }

    fun approve(proposalId: String): MayraMemoryChatResult.Reply = when (val result = manager.approve(proposalId)) {
        is MayraMemoryApprovalResult.Saved -> MayraMemoryChatResult.Reply(
            "Saved: ${result.memory.key} = ${result.memory.value}. You can review or delete it in Memory Center."
        )
        is MayraMemoryApprovalResult.Rejected -> MayraMemoryChatResult.Reply(result.reason)
    }

    fun cancel(proposalId: String): MayraMemoryChatResult.Reply = MayraMemoryChatResult.Reply(
        if (manager.reject(proposalId)) "Memory proposal cancelled." else "That memory proposal is missing or already handled."
    )

    fun restoreLatestPending(): PendingMemoryApproval? {
        val proposal = manager.pendingProposals().firstOrNull() ?: return null
        val previous = proposal.conflictingMemoryId?.let { id ->
            manager.activeMemories().firstOrNull { it.id == id }?.value
        }
        return PendingMemoryApproval(proposal.id, proposal.candidate.key, proposal.candidate.value, previous)
    }

    private fun proposalResult(result: MayraMemoryProposalResult): MayraMemoryChatResult = when (result) {
        is MayraMemoryProposalResult.ApprovalRequired -> MayraMemoryChatResult.NeedsApproval(
            PendingMemoryApproval(
                proposalId = result.proposalId,
                key = result.candidate.key,
                newValue = result.candidate.value,
                previousValue = result.conflictingMemory?.value
            )
        )
        is MayraMemoryProposalResult.Rejected -> MayraMemoryChatResult.Reply(result.reason)
    }

    private fun inferCategory(key: String): MayraMemoryCategory = when {
        key.contains("prefer", true) || key.contains("favorite", true) -> MayraMemoryCategory.PREFERENCE
        key.contains("project", true) || key.contains("business", true) -> MayraMemoryCategory.PROJECT
        key.contains("routine", true) || key.contains("daily", true) -> MayraMemoryCategory.ROUTINE
        key.contains("wife", true) || key.contains("son", true) || key.contains("daughter", true) || key.contains("relation", true) -> MayraMemoryCategory.RELATIONSHIP
        key.contains("name", true) || key.contains("profile", true) -> MayraMemoryCategory.PROFILE
        else -> MayraMemoryCategory.OTHER
    }

    private companion object {
        val REMEMBER = Regex("(?is)^(?:mayra[, ]+)?(?:remember|yaad rakh(?:o|na)?|याद रखो)\\s+(.+?)\\s*(?:=|:| is | hai | है )\\s*(.+)$")
        val CONFIRM = Regex("(?i)^confirm memory\\s+([a-f0-9]{64})$")
        val CANCEL = Regex("(?i)^cancel memory\\s+([a-f0-9]{64})$")
        val FORGET = Regex("(?is)^(?:mayra[, ]+)?(?:forget|bhool jao|भूल जाओ)\\s+(.+)$")
        val LIST = Regex("(?i)^(?:mayra[, ]+)?(?:what do you remember|show my memories|tumhe kya yaad hai|tumne kya seekha|क्या याद है|तुमने क्या सीखा)\\??$")
    }
}
