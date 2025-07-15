package com.locationsharing.app.domain.service

import com.locationsharing.app.data.models.Location

interface MapService {
    fun initializeMap()
    fun updateUserLocation(location: Location)
    fun updateFriendLocations(friendLocations: Map<String, Location>)
    fun addLocationMarker(friendId: String, location: Location, friendName: String)
    fun removeLocationMarker(friendId: String)
    fun clearAllMarkers()
}