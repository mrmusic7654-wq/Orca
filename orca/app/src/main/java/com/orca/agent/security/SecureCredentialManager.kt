package com.orca.agent.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialManager @Inject constructor(
    private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private fun getEncryptedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            "orca_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun storeApiKey(key: String) {
        getEncryptedPrefs().edit().putString("gemini_api_key", key).apply()
    }

    fun getApiKey(): String? {
        return getEncryptedPrefs().getString("gemini_api_key", null)
    }

    fun storeUserCredential(service: String, username: String, password: String) {
        val prefs = getEncryptedPrefs()
        prefs.edit()
            .putString("${service}_username", username)
            .putString("${service}_password", password)
            .apply()
    }

    fun getUserCredential(service: String): Pair<String, String>? {
        val prefs = getEncryptedPrefs()
        val username = prefs.getString("${service}_username", null) ?: return null
        val password = prefs.getString("${service}_password", null) ?: return null
        return Pair(username, password)
    }

    fun wipeAllCredentials() {
        getEncryptedPrefs().edit().clear().apply()
    }
}
