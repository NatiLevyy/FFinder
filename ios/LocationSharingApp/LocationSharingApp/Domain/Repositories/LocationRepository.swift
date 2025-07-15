import Foundation
import Combine

protocol LocationRepository {
    func updateLocation(_ location: Location) async -> Result<Void, Error>
    func getCurrentLocation() async -> Location?
    func getLocationUpdates() -> AnyPublisher<Location, Never>
    func getFriendLocations(friendIds: [String]) async -> Result<[String: Location], Error>
    func startLocationSharing() async -> Result<Void, Error>
    func stopLocationSharing() async -> Result<Void, Error>
}