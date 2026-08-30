package com.fanstaf.selah.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the verse is presented in the overlay. */
enum class DisplayMode { READ, RECALL }

/** How the next verse is chosen from the active set. */
enum class SelectionStrategy { SEQUENTIAL, RANDOM, SINGLE }

/** Immutable snapshot of all user settings. */
data class Settings(
    val enabled: Boolean = false,
    val durationSeconds: Int = 6,
    val mode: DisplayMode = DisplayMode.RECALL,
    val selection: SelectionStrategy = SelectionStrategy.SEQUENTIAL,
    /** Minimum minutes between shows. 0 = every unlock. */
    val minIntervalMinutes: Int = 30,
    val singleVerseId: Long = -1L,
    val sequentialCursor: Int = 0,
    val lastShownAt: Long = 0L,
    val fontScale: Float = 1.0f,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "selah_settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val DURATION = intPreferencesKey("duration_seconds")
        val MODE = stringPreferencesKey("mode")
        val SELECTION = stringPreferencesKey("selection")
        val MIN_INTERVAL = intPreferencesKey("min_interval_minutes")
        val SINGLE_ID = longPreferencesKey("single_verse_id")
        val CURSOR = intPreferencesKey("sequential_cursor")
        val LAST_SHOWN = longPreferencesKey("last_shown_at")
        val FONT_SCALE = floatPreferencesKey("font_scale")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            enabled = p[Keys.ENABLED] ?: false,
            durationSeconds = p[Keys.DURATION] ?: 6,
            mode = (p[Keys.MODE]?.let { runCatching { DisplayMode.valueOf(it) }.getOrNull() }) ?: DisplayMode.RECALL,
            selection = (p[Keys.SELECTION]?.let { runCatching { SelectionStrategy.valueOf(it) }.getOrNull() }) ?: SelectionStrategy.SEQUENTIAL,
            minIntervalMinutes = p[Keys.MIN_INTERVAL] ?: 30,
            singleVerseId = p[Keys.SINGLE_ID] ?: -1L,
            sequentialCursor = p[Keys.CURSOR] ?: 0,
            lastShownAt = p[Keys.LAST_SHOWN] ?: 0L,
            fontScale = p[Keys.FONT_SCALE] ?: 1.0f,
        )
    }

    suspend fun setEnabled(v: Boolean) = context.dataStore.edit { it[Keys.ENABLED] = v }
    suspend fun setDuration(v: Int) = context.dataStore.edit { it[Keys.DURATION] = v }
    suspend fun setMode(v: DisplayMode) = context.dataStore.edit { it[Keys.MODE] = v.name }
    suspend fun setSelection(v: SelectionStrategy) = context.dataStore.edit { it[Keys.SELECTION] = v.name }
    suspend fun setMinInterval(v: Int) = context.dataStore.edit { it[Keys.MIN_INTERVAL] = v }
    suspend fun setSingleVerseId(v: Long) = context.dataStore.edit { it[Keys.SINGLE_ID] = v }
    suspend fun setFontScale(v: Float) = context.dataStore.edit { it[Keys.FONT_SCALE] = v }

    suspend fun setCursor(v: Int) = context.dataStore.edit { it[Keys.CURSOR] = v }
    suspend fun setLastShownAt(v: Long) = context.dataStore.edit { it[Keys.LAST_SHOWN] = v }
}
