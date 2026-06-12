package com.orca.agent.memory
import com.orca.agent.data.database.OrcaDatabase
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class EpisodicJournal @Inject constructor(private val db: OrcaDatabase) {
    fun initialize() {}
    suspend fun recordEvent(event: String) {}
}
