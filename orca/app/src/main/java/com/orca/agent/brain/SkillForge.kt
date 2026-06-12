package com.orca.agent.brain
import com.orca.agent.core.AgentAction
import com.orca.agent.memory.ProceduralGraph
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SkillForge @Inject constructor(private val pg: ProceduralGraph) {
    private val skills = mutableMapOf<String, Skill>()
    fun storeSkill(name: String, actions: List<AgentAction>) { skills[name] = Skill(name, actions) }
    fun getSkill(name: String) = skills[name]?.actions
}
data class Skill(val name: String, val actions: List<AgentAction>)
