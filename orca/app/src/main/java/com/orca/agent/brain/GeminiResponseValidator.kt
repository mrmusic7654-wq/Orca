package com.orca.agent.brain
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class GeminiResponseValidator @Inject constructor() {
    fun validate(response: String): Boolean = true
}
