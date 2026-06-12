package com.orca.agent.brain
import com.orca.agent.data.network.GeminiApi
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ContextWindowManager @Inject constructor(private val api: GeminiApi) {
    fun addEntry(type: String, content: String) {}
    fun buildContext(): String = ""
}
