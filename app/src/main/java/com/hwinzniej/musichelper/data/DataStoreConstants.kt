package com.hwinzniej.musichelper.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreConstants {
    val SETTINGS_PREFERENCES = "settings"
    val KEY_LANGUAGE = stringPreferencesKey("language")
    val KEY_THEME_MODE = intPreferencesKey("theme_mode")
    val KEY_ENABLE_DYNAMIC_COLOR = booleanPreferencesKey("enable_dynamic_color")
    val KEY_ENABLE_AUTO_CHECK_UPDATE = booleanPreferencesKey("enable_auto_check_update")
    val KEY_USE_ROOT_ACCESS = booleanPreferencesKey("use_root_access")
    val KEY_ENABLE_HAPTIC = booleanPreferencesKey("enable_haptic")
}