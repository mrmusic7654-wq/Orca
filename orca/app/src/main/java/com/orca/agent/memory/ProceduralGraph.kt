package com.orca.agent.memory
import com.orca.agent.brain.Skill
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ProceduralGraph @Inject constructor() {
    private val skills = mutableMapOf<String, Skill>()
    fun initialize() {}
    fun storeSkill(skill: Skill) { skills[skill.name] = skill }
    fun getSkill(name: String) = skills[name]
}
