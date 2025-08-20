package com.locationsharing.app.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationsharing.app.data.settings.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val preferences = dataStore.data.first()
            _uiState.value = SettingsUiState(
                privacyVisibility = preferences[AppPreferences.PRIVACY_VISIBILITY] ?: "everyone",
                backgroundSharingEnabled = preferences[AppPreferences.BACKGROUND_SHARING_ENABLED] ?: true,
                mapStyle = preferences[AppPreferences.MAP_STYLE] ?: "system",
                units = preferences[AppPreferences.UNITS] ?: "km",
                a11yLargeLabels = preferences[AppPreferences.A11Y_LARGE_LABELS] ?: false,
                // TODO: Get phone number and link status from a different source
                isPhoneNumberLinked = false,
                phoneNumber = ""
            )
        }
    }

    fun setPrivacy(privacy: String) {
        viewModelScope.launch {
            dataStore.edit {
                it[AppPreferences.PRIVACY_VISIBILITY] = privacy
            }
            _uiState.value = _uiState.value.copy(privacyVisibility = privacy)
        }
    }

    fun setBackgroundSharing(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit {
                it[AppPreferences.BACKGROUND_SHARING_ENABLED] = enabled
            }
            _uiState.value = _uiState.value.copy(backgroundSharingEnabled = enabled)
        }
    }

    fun setMapStyle(style: String) {
        viewModelScope.launch {
            dataStore.edit {
                it[AppPreferences.MAP_STYLE] = style
            }
            _uiState.value = _uiState.value.copy(mapStyle = style)
        }
    }

    fun setUnits(units: String) {
        viewModelScope.launch {
            dataStore.edit {
                it[AppPreferences.UNITS] = units
            }
            _uiState.value = _uiState.value.copy(units = units)
        }
    }

    fun setA11yLargeLabels(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit {
                it[AppPreferences.A11Y_LARGE_LABELS] = enabled
            }
            _uiState.value = _uiState.value.copy(a11yLargeLabels = enabled)
        }
    }

    fun enableContactDiscovery() {
        // TODO: Implement phone linking flow
    }
}