package com.elendheim.recorder.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** All user-facing toggles, kept small and flat. */
data class AppSettings(
    val highContrast: Boolean = false,
    val showPitch: Boolean = false,
    val monitoring: Boolean = false
)

/**
 * Simple SharedPreferences-backed settings, exposed as a StateFlow so both the
 * UI (theme, screens) and the recording service can read the same values. One
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
        monitoring = prefs.getBoolean(KEY_MONITORING, false)
    )

    fun setHighContrast(value: Boolean) = update(KEY_HIGH_CONTRAST, value) { it.copy(highContrast = value) }
    fun setShowPitch(value: Boolean) = update(KEY_SHOW_PITCH, value) { it.copy(showPitch = value) }
    fun setMonitoring(value: Boolean) = update(KEY_MONITORING, value) { it.copy(monitoring = value) }

    private inline fun update(key: String, value: Boolean, transform: (AppSettings) -> AppSettings) {
        prefs.edit().putBoolean(key, value).apply()
        _settings.value = transform(_settings.value)
    }

    companion object {
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_SHOW_PITCH = "show_pitch"
        private const val KEY_MONITORING = "monitoring"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
