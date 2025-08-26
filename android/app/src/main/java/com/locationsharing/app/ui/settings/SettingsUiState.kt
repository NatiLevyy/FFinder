package com.locationsharing.app.ui.settings

data class SettingsUiState(
    val privacyVisibility: String = "everyone",
    val backgroundSharingEnabled: Boolean = true,
    val mapStyle: String = "system",
    val units: String = "km",
    val a11yLargeLabels: Boolean = false,
    val isPhoneNumberLinked: Boolean = false,
    val phoneNumber: String = ""
)