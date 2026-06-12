package com.orca.agent.brain
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SocialContextAwareness @Inject constructor(private val ctx: Context) {
    fun detect(): String = "alone"
}
