package com.locationsharing.app.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AppPreferences {
    val PRIVACY_VISIBILITY = stringPreferencesKey("privacy_visibility")
    val BACKGROUND_SHARING_ENABLED = booleanPreferencesKey("background_sharing_enabled")
    val MAP_STYLE = stringPreferencesKey("map_style")
    val UNITS = stringPreferencesKey("units")
    val A11Y_LARGE_LABELS = booleanPreferencesKey("a11y_large_labels")
}