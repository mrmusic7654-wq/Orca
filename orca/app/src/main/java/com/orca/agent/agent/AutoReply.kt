package com.orca.agent.agent
import com.orca.agent.brain.IntentPredictor
import com.orca.agent.core.OrcaCore
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class AutoReply @Inject constructor(private val core: OrcaCore, private val ip: IntentPredictor) {}
