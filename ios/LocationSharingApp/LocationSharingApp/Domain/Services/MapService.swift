import Foundation
import MapKit

protocol MapService {
    func initializeMap()
    func updateUserLocation(_ location: Location)
    func updateFriendLocations(_ friendLocations: [String: Location])
    func addLocationMarker(friendId: String, location: Location, friendName: String)
    func removeLocationMarker(friendId: String)
    func clearAllMarkers()
}