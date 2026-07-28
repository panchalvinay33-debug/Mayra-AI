package ai.mayra.app.memory

import java.time.Clock
import java.time.Instant

sealed interface MayraMemoryChatResult {
    data object NotHandled : MayraMemoryChatResult
    data class Reply(val text: String) : MayraMemoryChatResult
}

/** Deterministic, model-independent chat commands for owner-controlled personal memory. */
class MayraMemoryChatController(
    private val manager: MayraPersonalMemoryManager,
    private val clock: Clock = Clock.systemUTC()
) {
    fun handle(message: String): MayraMemoryChatResult {
        val text = message.trim()
        if (text.isEmpty()) return MayraMemoryChatResult.NotHandled

        CONFIRM.matchEntire(text)?.groupValues?.get(1)?.let { proposalId ->
            return when (val result = manager.approve(proposalId)) {
                is MayraMemoryApprovalResult.Saved -> MayraMemoryChatResult.Reply(
                    "Saved: ${result.memory.key} = ${result.memory.value}. You can review or delete it in Memory Center."
                )
                is MayraMemoryApprovalResult.Rejected -> MayraMemoryChatResult.Reply(result.reason)
            }
        }
        CANCEL.matchEntire(text)?.groupValues?.get(1)?.let { proposalId ->
            return MayraMemoryChatResult.Reply(
                if (manager.reject(proposalId)) "Memory proposal cancelled." else "That memory proposal is missing or already handled."
            )
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
            return when (val result = manager.propose(candidate)) {
                is MayraMemoryProposalResult.ApprovalRequired -> MayraMemoryChatResult.Reply(
                    "Please confirm before I save this memory: ${result.candidate.key} = ${result.candidate.value}. " +
                        "Reply: confirm memory ${result.proposalId} — or cancel memory ${result.proposalId}."
                )
                is MayraMemoryProposalResult.Rejected -> MayraMemoryChatResult.Reply(result.reason)
            }
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
        return MayraMemoryChatResult.NotHandled
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
        val LIST = Regex("(?i)^(?:mayra[, ]+)?(?:what do you remember|show my memories|tumhe kya yaad hai|क्या याद है)\\??$")
    }
}
