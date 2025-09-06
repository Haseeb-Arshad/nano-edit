package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val OFFLINE = booleanPreferencesKey("offline")
        val QUALITY = intPreferencesKey("quality") // 50..100
        val WATERMARK = booleanPreferencesKey("watermark")
        val AUTO_SCENELIFT = booleanPreferencesKey("auto_scenelift")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val offlineFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.OFFLINE] ?: false }
    val qualityFlow: Flow<Int> = context.dataStore.data.map { it[Keys.QUALITY] ?: 90 }
    val watermarkFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.WATERMARK] ?: false }
    val autoSceneLiftFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SCENELIFT] ?: false }
    val darkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_THEME] ?: false }

    suspend fun setOffline(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE] = enabled }
    }

    suspend fun setQuality(value: Int) {
        context.dataStore.edit { it[Keys.QUALITY] = value.coerceIn(50, 100) }
    }

    suspend fun setWatermark(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WATERMARK] = enabled }
    }

    suspend fun setAutoSceneLift(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SCENELIFT] = enabled }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }
}

