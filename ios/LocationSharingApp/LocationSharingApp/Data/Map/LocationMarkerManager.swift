import Foundation
import CoreLocation
import os.log

/**
 * Manager for handling location markers on the map.
 * 
 * This class provides a higher-level interface for managing friend location
 * markers, including batch updates, marker filtering, and marker state management.
 */
class LocationMarkerManager {
    
    private let mapService: MapKitService
    private var markerStates: [String: MarkerState] = [:]
    
    private static let logger = Logger(subsystem: "com.locationsharing.app", category: "LocationMarkerManager")
    private static let markerUpdateThresholdMs: Int64 = 30000 // 30 seconds
    
    /**
     * Represents the state of a location marker.
     */
    struct MarkerState {
        let friendId: String
        let friendName: String
        let location: Location
        let isVisible: Bool
        let lastUpdated: Int64
        
        init(friendId: String, friendName: String, location: Location, isVisible: Bool = true, lastUpdated: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
            self.friendId = friendId
            self.friendName = friendName
            self.location = location
            self.isVisible = isVisible
            self.lastUpdated = lastUpdated
        }
    }
    
    /**
     * Initializes the LocationMarkerManager with a MapService.
     * 
     * - Parameter mapService: The MapKitService instance to use for marker operations
     */
    init(mapService: MapKitService) {
        self.mapService = mapService
    }
    
    /**
     * Updates a friend's location marker.
     * 
     * - Parameters:
     *   - friendId: Unique identifier for the friend
     *   - friendName: Display name for the friend
     *   - location: The friend's current location
     *   - forceUpdate: Whether to force update even if location hasn't changed significantly
     */
    func updateFriendMarker(friendId: String, friendName: String, location: Location, forceUpdate: Bool = false) {
        do {
            let currentState = markerStates[friendId]
            
            // Check if update is necessary
            if !forceUpdate, let currentState = currentState, !shouldUpdateMarker(currentState: currentState, newLocation: location) {
                Self.logger.debug("Skipping marker update for \(friendId) - no significant change")
                return
            }
            
            // Update marker on map
            mapService.addLocationMarker(friendId: friendId, location: location, friendName: friendName)
            
            // Update internal state
            markerStates[friendId] = MarkerState(
                friendId: friendId,
                friendName: friendName,
                location: location,
                isVisible: true,
                lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
            )
            
            Self.logger.debug("Updated marker for friend: \(friendName)")
        } catch {
            Self.logger.error("Error updating friend marker \(friendId): \(error.localizedDescription)")
        }
    }
    
    /**
     * Updates multiple friend markers in batch.
     * 
     * - Parameter friendLocations: Dictionary of friend IDs to (friendName, location) pairs
     */
    func updateFriendMarkers(_ friendLocations: [String: (String, Location)]) {
        do {
            for (friendId, friendData) in friendLocations {
                let (friendName, location) = friendData
                updateFriendMarker(friendId: friendId, friendName: friendName, location: location)
            }
            
            Self.logger.debug("Batch updated \(friendLocations.count) friend markers")
        } catch {
            Self.logger.error("Error in batch marker update: \(error.localizedDescription)")
        }
    }
    
    /**
     * Removes a friend's marker from the map.
     * 
     * - Parameter friendId: Unique identifier for the friend
     */
    func removeFriendMarker(friendId: String) {
        do {
            mapService.removeLocationMarker(friendId: friendId)
            markerStates.removeValue(forKey: friendId)
            
            Self.logger.debug("Removed marker for friend: \(friendId)")
        } catch {
            Self.logger.error("Error removing friend marker \(friendId): \(error.localizedDescription)")
        }
    }
    
    /**
     * Shows or hides a friend's marker.
     * 
     * - Parameters:
     *   - friendId: Unique identifier for the friend
     *   - visible: Whether the marker should be visible
     */
    func setMarkerVisibility(friendId: String, visible: Bool) {
        do {
            guard let currentState = markerStates[friendId] else { return }
            
            if visible && !currentState.isVisible {
                // Show marker
                mapService.addLocationMarker(friendId: friendId, location: currentState.location, friendName: currentState.friendName)
                markerStates[friendId] = MarkerState(
                    friendId: currentState.friendId,
                    friendName: currentState.friendName,
                    location: currentState.location,
                    isVisible: true,
                    lastUpdated: currentState.lastUpdated
                )
            } else if !visible && currentState.isVisible {
                // Hide marker
                mapService.removeLocationMarker(friendId: friendId)
                markerStates[friendId] = MarkerState(
                    friendId: currentState.friendId,
                    friendName: currentState.friendName,
                    location: currentState.location,
                    isVisible: false,
                    lastUpdated: currentState.lastUpdated
                )
            }
            
            Self.logger.debug("Set marker visibility for \(friendId): \(visible)")
        } catch {
            Self.logger.error("Error setting marker visibility \(friendId): \(error.localizedDescription)")
        }
    }
    
    /**
     * Gets all currently tracked friend markers.
     * 
     * - Returns: Dictionary of friend IDs to their marker states
     */
    func getAllMarkerStates() -> [String: MarkerState] {
        return markerStates
    }
    
    /**
     * Gets visible friend markers only.
     * 
     * - Returns: Dictionary of friend IDs to their marker states (visible only)
     */
    func getVisibleMarkerStates() -> [String: MarkerState] {
        return markerStates.filter { $0.value.isVisible }
    }
    
    /**
     * Clears all friend markers from the map.
     */
    func clearAllMarkers() {
        do {
            mapService.clearAllMarkers()
            markerStates.removeAll()
            
            Self.logger.debug("Cleared all friend markers")
        } catch {
            Self.logger.error("Error clearing all markers: \(error.localizedDescription)")
        }
    }
    
    /**
     * Removes stale markers that haven't been updated recently.
     * 
     * - Parameter maxAgeMs: Maximum age in milliseconds before a marker is considered stale
     */
    func removeStaleMarkers(maxAgeMs: Int64 = Self.markerUpdateThresholdMs * 10) {
        do {
            let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
            let staleMarkers = markerStates.filter { 
                currentTime - $0.value.lastUpdated > maxAgeMs 
            }
            
            for (friendId, _) in staleMarkers {
                removeFriendMarker(friendId: friendId)
            }
            
            if !staleMarkers.isEmpty {
                Self.logger.debug("Removed \(staleMarkers.count) stale markers")
            }
        } catch {
            Self.logger.error("Error removing stale markers: \(error.localizedDescription)")
        }
    }
    
    /**
     * Determines if a marker should be updated based on location change and time.
     * 
     * - Parameters:
     *   - currentState: Current marker state
     *   - newLocation: New location data
     * - Returns: True if marker should be updated
     */
    private func shouldUpdateMarker(currentState: MarkerState, newLocation: Location) -> Bool {
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let timeDiff = currentTime - currentState.lastUpdated
        
        // Always update if enough time has passed
        if timeDiff > Self.markerUpdateThresholdMs {
            return true
        }
        
        // Update if location has changed significantly
        let distanceThreshold: Double = 10.0 // 10 meters
        let distance = calculateDistance(location1: currentState.location, location2: newLocation)
        
        return distance > distanceThreshold
    }
    
    /**
     * Calculates the distance between two locations in meters.
     * 
     * - Parameters:
     *   - location1: First location
     *   - location2: Second location
     * - Returns: Distance in meters
     */
    private func calculateDistance(location1: Location, location2: Location) -> Double {
        let earthRadius: Double = 6371000.0 // Earth radius in meters
        
        let lat1Rad = location1.latitude * .pi / 180
        let lat2Rad = location2.latitude * .pi / 180
        let deltaLatRad = (location2.latitude - location1.latitude) * .pi / 180
        let deltaLonRad = (location2.longitude - location1.longitude) * .pi / 180
        
        let a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
}