package com.orca.agent.agent
import com.orca.agent.memory.MemoryCortex
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class DigitalTwin @Inject constructor(private val mc: MemoryCortex) { fun loadProfile() {}; fun getProfile() = mapOf<String,String>() }
