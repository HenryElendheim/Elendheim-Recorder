package com.elendheim.recorder.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** All user-facing toggles and preferences, kept small and flat. */
data class AppSettings(
    val highContrast: Boolean = false,
    val showPitch: Boolean = false,
    val pianoRoll: Boolean = false,
    val monitoring: Boolean = false,
    val namePrefix: String = "Session",
    val keepScreenOn: Boolean = false,
    val confirmDelete: Boolean = true,
    val nameCounter: Int = 1           // the next number a new recording will get
)

/**
 * Simple SharedPreferences-backed settings, exposed as a StateFlow so the UI
 * (theme, screens) and the recording service read the same values. One
 * process-wide instance keeps everyone in sync.
 */
class SettingsStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val current: AppSettings get() = _settings.value

    private fun load() = AppSettings(
        highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false),
        showPitch = prefs.getBoolean(KEY_SHOW_PITCH, false),
        pianoRoll = prefs.getBoolean(KEY_PIANO_ROLL, false),
        monitoring = prefs.getBoolean(KEY_MONITORING, false),
        namePrefix = prefs.getString(KEY_NAME_PREFIX, "Session") ?: "Session",
        keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false),
        confirmDelete = prefs.getBoolean(KEY_CONFIRM_DELETE, true),
        nameCounter = prefs.getInt(KEY_NAME_COUNTER, 1)
    )

    fun setHighContrast(value: Boolean) = updateBool(KEY_HIGH_CONTRAST, value) { it.copy(highContrast = value) }
    fun setShowPitch(value: Boolean) = updateBool(KEY_SHOW_PITCH, value) { it.copy(showPitch = value) }
    fun setPianoRoll(value: Boolean) = updateBool(KEY_PIANO_ROLL, value) { it.copy(pianoRoll = value) }
    fun setMonitoring(value: Boolean) = updateBool(KEY_MONITORING, value) { it.copy(monitoring = value) }
    fun setKeepScreenOn(value: Boolean) = updateBool(KEY_KEEP_SCREEN_ON, value) { it.copy(keepScreenOn = value) }
    fun setConfirmDelete(value: Boolean) = updateBool(KEY_CONFIRM_DELETE, value) { it.copy(confirmDelete = value) }

    fun setNamePrefix(value: String) {
        prefs.edit().putString(KEY_NAME_PREFIX, value).apply()
        _settings.value = _settings.value.copy(namePrefix = value)
    }

    fun resetNumbering() {
        prefs.edit().putInt(KEY_NAME_COUNTER, 1).apply()
        _settings.value = _settings.value.copy(nameCounter = 1)
    }

    /**
     * Build the name for a new recording from the prefix and the running
     * counter, then advance the counter. Called once when a take is finalised.
     */
    @Synchronized
    fun nextRecordingName(): String {
        val prefix = _settings.value.namePrefix.trim().ifEmpty { "Recording" }
        val n = prefs.getInt(KEY_NAME_COUNTER, 1)
        prefs.edit().putInt(KEY_NAME_COUNTER, n + 1).apply()
        _settings.value = _settings.value.copy(nameCounter = n + 1)
        return "$prefix $n"
    }

    private inline fun updateBool(key: String, value: Boolean, transform: (AppSettings) -> AppSettings) {
        prefs.edit().putBoolean(key, value).apply()
        _settings.value = transform(_settings.value)
    }

    companion object {
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_SHOW_PITCH = "show_pitch"
        private const val KEY_PIANO_ROLL = "piano_roll"
        private const val KEY_MONITORING = "monitoring"
        private const val KEY_NAME_PREFIX = "name_prefix"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_CONFIRM_DELETE = "confirm_delete"
        private const val KEY_NAME_COUNTER = "name_counter"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
