package com.orca.agent.data.datastore
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
private val Context.dataStore by preferencesDataStore(name = "orca_prefs")
@Singleton
class PreferencesDataStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    val autoPilot: Flow<Boolean> = ctx.dataStore.data.map { it[booleanPreferencesKey("auto_pilot")] ?: false }
}
