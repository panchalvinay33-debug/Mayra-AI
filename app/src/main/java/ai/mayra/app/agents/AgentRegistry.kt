package ai.mayra.app.agents

import java.util.concurrent.ConcurrentHashMap

class AgentRegistry {
    private val agents = ConcurrentHashMap<String, MayraAgent>()

    fun register(agent: MayraAgent) {
        require(agent.id.isNotBlank()) { "Agent id cannot be blank" }
        check(agents.putIfAbsent(agent.id, agent) == null) {
            "Agent already registered: ${agent.id}"
        }
    }

    fun unregister(agentId: String): MayraAgent? = agents.remove(agentId)

    fun find(agentId: String): MayraAgent? = agents[agentId]

    fun contains(agentId: String): Boolean = agents.containsKey(agentId)

    fun all(): List<MayraAgent> = agents.values.sortedBy { it.id }
}
