package com.orca.agent.agent
import com.orca.agent.brain.ConsciousMind
import com.orca.agent.memory.MemoryCortex
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class GoalManager @Inject constructor(private val cm: ConsciousMind, private val mc: MemoryCortex) { fun initialize() {} }
