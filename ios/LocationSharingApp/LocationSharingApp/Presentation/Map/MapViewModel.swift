import Foundation
import SwiftUI
import CoreLocation
import MapKit

@MainActor
class MapViewModel: ObservableObject {
    @Published var friendLocations: [FriendLocation] = []
    @Published var currentLocation: Location?
    @Published var errorMessage: String?
    
    private let locationRepository = DIContainer.shared.locationRepository
    private let friendsRepository = DIContainer.shared.friendsRepository
    private let mapKitService = DIContainer.shared.mapKitService
    private let locationMarkerManager = DIContainer.shared.locationMarkerManager
    private let realTimeLocationUpdateManager = DIContainer.shared.realTimeLocationUpdateManager
    private let errorHandler = DIContainer.shared.errorHandler
    
    struct FriendLocation: Identifiable {
        let id: String
        let name: String
        let latitude: Double
        let longitude: Double
        let lastUpdated: String
    }
    
    func initializeMap() async {
        do {
            // Initialize map services
            await mapKitService.initialize()
            await locationMarkerManager.initialize()
            
            // Start real-time location updates
            await realTimeLocationUpdateManager.startUpdates()
            
            // Start observing location updates
            await observeLocationUpdates()
            
            // Start observing friend locations
            await observeFriendLocations()
            
        } catch {
            errorMessage = errorHandler.handleError(error)
        }
    }
    
    private func observeLocationUpdates() async {
        do {
            for await location in locationRepository.getCurrentLocationFlow() {
                currentLocation = location
                
                // Update map with current location
                if let location = location {
                    await mapKitService.updateUserLocation(location)
                }
            }
        } catch {
            errorMessage = errorHandler.handleError(error)
        }
    }
    
    private func observeFriendLocations() async {
        do {
            // Get friends list
            let friends = try await friendsRepository.getFriends()
            
            // Convert to friend locations
            var locations: [FriendLocation] = []
            
            for friend in friends {
                if let location = friend.lastKnownLocation {
                    locations.append(
                        FriendLocation(
                            id: friend.id,
                            name: friend.user.displayName,
                            latitude: location.latitude,
                            longitude: location.longitude,
                            lastUpdated: formatTimestamp(location.timestamp)
                        )
                    )
                }
            }
            
            friendLocations = locations
            
            // Update map markers
            await updateMapMarkers(locations)
            
        } catch {
            errorMessage = errorHandler.handleError(error)
        }
    }
    
    private func updateMapMarkers(_ locations: [FriendLocation]) async {
        do {
            // Clear existing markers
            await locationMarkerManager.clearAllMarkers()
            
            // Add markers for each friend
            for friendLocation in locations {
                let location = Location(
                    latitude: friendLocation.latitude,
                    longitude: friendLocation.longitude,
                    accuracy: 0.0,
                    timestamp: Int64(Date().timeIntervalSince1970)
                )
                await locationMarkerManager.addLocationMarker(
                    friendId: friendLocation.id,
                    location: location,
                    friendName: friendLocation.name
                )
            }
        } catch {
            errorMessage = errorHandler.handleError(error)
        }
    }
    
    func centerOnUserLocation() async {
        do {
            if let location = await locationRepository.getCurrentLocation() {
                currentLocation = location
            }
        } catch {
            errorMessage = errorHandler.handleError(error)
        }
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    private func formatTimestamp(_ timestamp: Int64) -> String {
        let now = Int64(Date().timeIntervalSince1970)
        let diff = now - timestamp
        
        switch diff {
        case 0..<60:
            return "Just now"
        case 60..<3600:
            return "\(diff / 60) minutes ago"
        case 3600..<86400:
            return "\(diff / 3600) hours ago"
        default:
            return "\(diff / 86400) days ago"
        }
    }
    
    deinit {
        Task {
            await realTimeLocationUpdateManager.stopUpdates()
            await locationMarkerManager.cleanup()
        }
    }
}