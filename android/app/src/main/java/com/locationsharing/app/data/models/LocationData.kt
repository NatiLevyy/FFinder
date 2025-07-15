package com.locationsharing.app.data.models

sealed class LocationData {
    data class Success(val location: Location) : LocationData()
    data class Error(val error: LocationError) : LocationData()
    object Loading : LocationData()
}

sealed class LocationError : Exception() {
    object PermissionDenied : LocationError()
    object LocationDisabled : LocationError()
    object NetworkUnavailable : LocationError()
    object BackgroundPermissionDenied : LocationError()
    data class UnknownError(val cause: Throwable) : LocationError()
}

enum class LocationPermissionStatus {
    GRANTED,
    DENIED,
    BACKGROUND_DENIED,
    NOT_REQUESTED
}