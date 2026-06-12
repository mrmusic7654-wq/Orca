package com.orca.agent.security

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialManager @Inject constructor(
    private val context: Context
) {
    private fun getPrefs(): SharedPreferences {
        return context.getSharedPreferences("orca_secure_prefs", Context.MODE_PRIVATE)
    }

    fun storeApiKey(key: String) {
        getPrefs().edit().putString("gemini_api_key", key).apply()
    }

    fun getApiKey(): String? {
        return getPrefs().getString("gemini_api_key", null)
    }

    fun storeUserCredential(service: String, username: String, password: String) {
        getPrefs().edit()
            .putString("${service}_username", username)
            .putString("${service}_password", password)
            .apply()
    }

    fun getUserCredential(service: String): Pair<String, String>? {
        val prefs = getPrefs()
        val username = prefs.getString("${service}_username", null) ?: return null
        val password = prefs.getString("${service}_password", null) ?: return null
        return Pair(username, password)
    }

    fun wipeAllCredentials() {
        getPrefs().edit().clear().apply()
    }
}
