package com.pixellab.suite.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixellab.suite.core.designsystem.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pixel_lab_settings")

data class UserSettingsState(
    val themeMode: ThemeMode = ThemeMode.OBSIDIAN_DARK,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val thermalAwareMode: Boolean = true,
    val strictOfflineProfile: Boolean = false,
    val batteryLowThreshold: Int = 20,
    val batteryHighThreshold: Int = 85,
    val batteryTempThresholdC: Int = 42,
    val dashboardCardOrder: List<String> = DEFAULT_CARD_ORDER,
    val hiddenDashboardCards: Set<String> = emptySet(),
    val favoriteModules: Set<String> = setOf("device", "sensors", "localai", "productivity")
) {
    companion object {
        val DEFAULT_CARD_ORDER = listOf(
            "IDENTITY_VITALS",
            "BATTERY_THERMAL",
            "MEMORY_STORAGE",
            "MODULES_GRID",
            "LOCAL_AI_AUTOPILOT",
            "RECENT_ACTIVITY"
        )
    }
}

class PixelLabPreferences(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val THERMAL_AWARE = booleanPreferencesKey("thermal_aware")
        val STRICT_OFFLINE = booleanPreferencesKey("strict_offline")
        val BATTERY_LOW_ALERT = intPreferencesKey("battery_low_alert")
        val BATTERY_HIGH_ALERT = intPreferencesKey("battery_high_alert")
        val BATTERY_TEMP_ALERT = intPreferencesKey("battery_temp_alert")
        val CARD_ORDER = stringPreferencesKey("dashboard_card_order")
        val HIDDEN_CARDS = stringPreferencesKey("hidden_dashboard_cards")
        val FAVORITE_MODULES = stringPreferencesKey("favorite_modules")
    }

    val settingsFlow: Flow<UserSettingsState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.OBSIDIAN_DARK.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.OBSIDIAN_DARK)
        val orderStr = prefs[Keys.CARD_ORDER]
        val orderList = if (orderStr.isNullOrBlank()) {
            UserSettingsState.DEFAULT_CARD_ORDER
        } else {
            orderStr.split(",").filter { it.isNotBlank() }
        }
        val hiddenSet = prefs[Keys.HIDDEN_CARDS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: emptySet()
        val favSet = prefs[Keys.FAVORITE_MODULES]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: setOf("device", "sensors", "localai", "productivity")

        UserSettingsState(
            themeMode = themeMode,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            thermalAwareMode = prefs[Keys.THERMAL_AWARE] ?: true,
            strictOfflineProfile = prefs[Keys.STRICT_OFFLINE] ?: false,
            batteryLowThreshold = prefs[Keys.BATTERY_LOW_ALERT] ?: 20,
            batteryHighThreshold = prefs[Keys.BATTERY_HIGH_ALERT] ?: 85,
            batteryTempThresholdC = prefs[Keys.BATTERY_TEMP_ALERT] ?: 42,
            dashboardCardOrder = orderList,
            hiddenDashboardCards = hiddenSet,
            favoriteModules = favSet
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setThermalAwareMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.THERMAL_AWARE] = enabled }
    }

    suspend fun setStrictOfflineProfile(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STRICT_OFFLINE] = enabled }
    }

    suspend fun updateBatteryThresholds(low: Int, high: Int, tempC: Int) {
        context.dataStore.edit {
            it[Keys.BATTERY_LOW_ALERT] = low.coerceIn(5, 50)
            it[Keys.BATTERY_HIGH_ALERT] = high.coerceIn(60, 100)
            it[Keys.BATTERY_TEMP_ALERT] = tempC.coerceIn(35, 55)
        }
    }

    suspend fun moveDashboardCard(cardId: String, direction: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.CARD_ORDER]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: UserSettingsState.DEFAULT_CARD_ORDER.toMutableList()
            val idx = current.indexOf(cardId)
            if (idx != -1) {
                val targetIdx = (idx + direction).coerceIn(0, current.lastIndex)
                if (targetIdx != idx) {
                    val item = current.removeAt(idx)
                    current.add(targetIdx, item)
                    prefs[Keys.CARD_ORDER] = current.joinToString(",")
                }
            }
        }
    }

    suspend fun toggleDashboardCardVisibility(cardId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_CARDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet() ?: mutableSetOf()
            if (!current.add(cardId)) {
                current.remove(cardId)
            }
            prefs[Keys.HIDDEN_CARDS] = current.joinToString(",")
        }
    }

    suspend fun toggleFavoriteModule(moduleId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_MODULES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet() ?: mutableSetOf("device", "sensors", "localai", "productivity")
            if (!current.add(moduleId)) {
                current.remove(moduleId)
            }
            prefs[Keys.FAVORITE_MODULES] = current.joinToString(",")
        }
    }
}
