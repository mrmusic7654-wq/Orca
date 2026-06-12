package com.orca.agent.agent
import android.content.Context
import com.orca.agent.core.OrcaCore
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SilentTask @Inject constructor(private val ctx: Context, private val core: OrcaCore) { fun startBackgroundOptimization() {} }
