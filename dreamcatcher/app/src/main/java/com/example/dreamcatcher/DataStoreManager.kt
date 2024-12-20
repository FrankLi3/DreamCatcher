package com.example.dreamcatcher

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")
private val SETTINGS_KEY = stringPreferencesKey("home_screen_settings")


class DataStoreManager(private val context: Context) {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val SETTINGS_KEY = stringPreferencesKey("home_screen_settings")
    private val LOGIN_STATE_KEY = booleanPreferencesKey("is_logged_in")
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE_KEY = intPreferencesKey("reminder_minute")

    private val gson = Gson()

    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    suspend fun setDarkModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isEnabled
        }
    }

    val homeScreenSettings: Flow<Map<String, Boolean>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[SETTINGS_KEY]
            if (json != null) {
                try {
                    // Use TypeToken for type-safe deserialization
                    val type = object : TypeToken<Map<String, Boolean>>() {}.type
                    gson.fromJson<Map<String, Boolean>>(json, type)
                } catch (e: Exception) {
                    // Handle deserialization error and fallback to default settings
                    defaultSettings()
                }
            } else {
                // Fallback to default settings if no JSON is found
                defaultSettings()
            }
        }

    // Default home screen settings
    private fun defaultSettings(): Map<String, Boolean> {
        return mapOf(
            "Show Today's Dream" to true,
            "Show Log Dream" to true,
            "Show Dream Calendar" to true,
            "Show Nearby Therapists" to true,
            "Show Trend Analysis" to true
        )
    }

    suspend fun saveHomeScreenSettings(settings: Map<String, Boolean>) {
        context.dataStore.edit { preferences ->
            preferences[SETTINGS_KEY] = gson.toJson(settings) // Serialize Map to JSON
        }
    }
    val loginState: Flow<Pair<Boolean, String?>> = context.dataStore.data
        .map { preferences ->
            val isLoggedIn = preferences[LOGIN_STATE_KEY] ?: false
            val userId = preferences[USER_ID_KEY]
            isLoggedIn to userId
        }

    suspend fun setLoginState(isLoggedIn: Boolean, userId: String?) {
        context.dataStore.edit { preferences ->
            preferences[LOGIN_STATE_KEY] = isLoggedIn
            if (userId != null) {
                preferences[USER_ID_KEY] = userId
            } else {
                preferences.remove(USER_ID_KEY)
            }
        }
    }
    suspend fun saveReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_HOUR_KEY] = hour
            preferences[REMINDER_MINUTE_KEY] = minute
        }
    }

    val reminderTime: Flow<Pair<Int, Int>> = context.dataStore.data
        .map { preferences ->
            val hour = preferences[REMINDER_HOUR_KEY] ?: 9
            val minute = preferences[REMINDER_MINUTE_KEY] ?: 0
            hour to minute
        }}
