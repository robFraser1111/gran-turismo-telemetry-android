package com.robfraser.granturismotelemetry.models

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class LayoutPreset(val label: String) {
    Driving("Driving"),
    Endurance("Endurance"),
    Minimal("Minimal");

    companion object {
        fun fromRaw(raw: String?): LayoutPreset =
            entries.firstOrNull { it.label == raw || it.name == raw } ?: Driving
    }
}

/**
 * Persisted app settings. [isPro] is a simple flag (SharedPreferences) with a
 * debug toggle — there is no Play Billing purchase flow in this build.
 */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("gt7", Context.MODE_PRIVATE)

    var ps5IP by mutableStateOf(prefs.getString(KEY_IP, "192.168.1.42") ?: "192.168.1.42")
        private set

    /** Default true so the UI works without a console. */
    var useSimulator by mutableStateOf(
        if (prefs.contains(KEY_SIM)) prefs.getBoolean(KEY_SIM, true) else true,
    )
        private set

    /**
     * Paid flag. Free: tire temps + fuel + gear/speed/RPM.
     * Paid: custom layouts, live track map, lap delta, session history.
     */
    var isPro by mutableStateOf(prefs.getBoolean(KEY_PRO, false))
        private set

    var preset by mutableStateOf(LayoutPreset.fromRaw(prefs.getString(KEY_PRESET, null)))
        private set

    fun updatePs5IP(value: String) {
        ps5IP = value
        prefs.edit().putString(KEY_IP, value).apply()
    }

    fun updateUseSimulator(value: Boolean) {
        useSimulator = value
        prefs.edit().putBoolean(KEY_SIM, value).apply()
    }

    fun updateIsPro(value: Boolean) {
        isPro = value
        prefs.edit().putBoolean(KEY_PRO, value).apply()
    }

    fun updatePreset(value: LayoutPreset) {
        preset = value
        prefs.edit().putString(KEY_PRESET, value.label).apply()
    }

    private companion object {
        const val KEY_PRO = "gt7.isPro"
        const val KEY_IP = "gt7.ps5IP"
        const val KEY_SIM = "gt7.useSimulator"
        const val KEY_PRESET = "gt7.preset"
    }
}
