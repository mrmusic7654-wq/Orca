package com.orca.agent.brain
import com.orca.agent.memory.MemoryCortex
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class OfflineFallbackManager @Inject constructor(private val mc: MemoryCortex) {
    fun getCachedWorkflow(task: String): Any? = null
}
