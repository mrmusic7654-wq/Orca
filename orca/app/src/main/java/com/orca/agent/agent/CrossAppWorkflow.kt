package com.orca.agent.agent
import com.orca.agent.core.OrcaCore
import com.orca.agent.execution.AppNavigator
import com.orca.agent.execution.GestureEngine
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class CrossAppWorkflow @Inject constructor(private val nav: AppNavigator, private val ge: GestureEngine, private val core: OrcaCore) {}
