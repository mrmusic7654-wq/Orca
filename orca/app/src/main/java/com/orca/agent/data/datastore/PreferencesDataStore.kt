package com.orca.agent.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "orca_preferences")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Agent Settings
    val autoPilotEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PILOT_KEY] ?: false
    }
    
    val deepThinkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEEP_THINK_KEY] ?: false
    }
    
    val silentTaskEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SILENT_TASK_KEY] ?: true
    }
    
    val voiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOICE_ENABLED_KEY] ?: true
    }
    
    val threatDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[THREAT_DETECTION_KEY] ?: true
    }
    
    val autoReplyEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_REPLY_KEY] ?: false
    }
    
    // API Keys (encrypted in production)
    val geminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY] ?: ""
    }
    
    // User Profile
    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: ""
    }
    
    val userPreferences: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_PREFERENCES_KEY] ?: "{}"
    }
    
    // Memory Settings
    val maxContextSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[MAX_CONTEXT_SIZE_KEY] ?: 1000000 // 1M tokens
    }
    
    val autoSaveInterval: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SAVE_INTERVAL_KEY] ?: 300000L // 5 minutes
    }
    
    // Setters
    suspend fun setAutoPilot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PILOT_KEY] = enabled
        }
    }
    
    suspend fun setDeepThink(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEEP_THINK_KEY] = enabled
        }
    }
    
    suspend fun setSilentTask(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SILENT_TASK_KEY] = enabled
        }
    }
    
    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setThreatDetection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THREAT_DETECTION_KEY] = enabled
        }
    }
    
    suspend fun setAutoReply(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_REPLY_KEY] = enabled
        }
    }
    
    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = key
        }
    }
    
    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }
    
    suspend fun setUserPreferences(prefs: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PREFERENCES_KEY] = prefs
        }
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
    
    companion object {
        // Preference Keys
        private val AUTO_PILOT_KEY = booleanPreferencesKey("auto_pilot_enabled")
        private val DEEP_THINK_KEY = booleanPreferencesKey("deep_think_mode")
        private val SILENT_TASK_KEY = booleanPreferencesKey("silent_task_enabled")
        private val VOICE_ENABLED_KEY = booleanPreferencesKey("voice_enabled")
        private val THREAT_DETECTION_KEY = booleanPreferencesKey("threat_detection")
        private val AUTO_REPLY_KEY = booleanPreferencesKey("auto_reply_enabled")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_PREFERENCES_KEY = stringPreferencesKey("user_preferences")
        private val MAX_CONTEXT_SIZE_KEY = intPreferencesKey("max_context_size")
        private val AUTO_SAVE_INTERVAL_KEY = longPreferencesKey("auto_save_interval")
    }
}
