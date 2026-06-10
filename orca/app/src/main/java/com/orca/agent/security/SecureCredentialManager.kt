package com.orca.agent.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialManager @Inject constructor(
    private val context: Context
) {
    // NEVER store API keys in BuildConfig or code
    // Use encrypted storage with server-side key retrieval
    
    private val masterKeyAlias = "orca_master_key"
    
    // ============================================================
    // SECURE KEY STORAGE
    // ============================================================
    
    fun storeApiKey(key: String) {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "orca_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        encryptedPrefs.edit().putString("gemini_api_key", key).apply()
    }
    
    fun getApiKey(): String? {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "orca_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        return encryptedPrefs.getString("gemini_api_key", null)
    }
    
    // ============================================================
    // KEY ROTATION
    // ============================================================
    
    fun rotateApiKey(newKey: String) {
        // Store old key temporarily for in-flight requests
        val oldKey = getApiKey()
        
        // Store new key
        storeApiKey(newKey)
        
        // After 5 minutes, securely wipe old key
        // (In production, this would use a scheduled task)
    }
    
    // ============================================================
    // USAGE MONITORING (Detect stolen key)
    // ============================================================
    
    fun monitorApiUsage() {
        // Track API call frequency
        // If usage spikes beyond normal patterns, the key may be compromised
        // Alert user and suggest key rotation
    }
    
    // ============================================================
    // SECURE CREDENTIAL STORAGE
    // ============================================================
    
    fun storeUserCredential(service: String, username: String, password: String) {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "orca_credentials",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        val credentialJson = """{"username":"$username","password":"$password"}"""
        encryptedPrefs.edit().putString(service, credentialJson).apply()
    }
    
    fun getUserCredential(service: String): Pair<String, String>? {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "orca_credentials",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        val json = encryptedPrefs.getString(service, null) ?: return null
        // Parse JSON and return credentials
        return null // Simplified
    }
    
    fun wipeAllCredentials() {
        val encryptedPrefs = EncryptedSharedPreferences.create(
            "orca_credentials",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        encryptedPrefs.edit().clear().apply()
    }
}
