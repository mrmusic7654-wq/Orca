package com.orca.agent.memory
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class ContextHygieneManager @Inject constructor(private val ctx: Context) {
    fun check() {}
}
