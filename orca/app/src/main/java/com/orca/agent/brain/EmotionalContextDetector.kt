package com.orca.agent.brain
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class EmotionalContextDetector @Inject constructor() {
    fun detect(): String = "neutral"
}
