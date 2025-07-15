import Foundation
import Combine
import CoreLocation

protocol LocationService {
    func startLocationUpdates(interval: TimeInterval)
    func stopLocationUpdates()
    func getCurrentLocation() async -> Location?
    func getLocationUpdates() -> AnyPublisher<Location, Never>
    func requestPermissions() async -> PermissionStatus
    func hasLocationPermission() -> Bool
}

enum PermissionStatus {
    case granted
    case denied
    case shouldShowRationale
    case notRequested
}