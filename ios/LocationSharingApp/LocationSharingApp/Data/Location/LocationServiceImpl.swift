import Foundation
import CoreLocation
import Combine
import UIKit

/**
 * iOS implementation of LocationService using LocationTracker.
 */
class LocationServiceImpl: LocationService {
    
    // MARK: - Properties
    
    private let locationTracker: LocationTracker
    private let permissionManager: LocationPermissionManager
    
    // MARK: - Publishers
    
    var locationUpdates: AnyPublisher<Location, Never> {
        locationTracker.locationUpdates
    }
    
    var locationErrors: AnyPublisher<LocationError, Never> {
        locationTracker.locationErrors
    }
    
    var isTracking: AnyPublisher<Bool, Never> {
        locationTracker.isTracking
    }
    
    // MARK: - Initialization
    
    init(locationTracker: LocationTracker, permissionManager: LocationPermissionManager) {
        self.locationTracker = locationTracker
        self.permissionManager = permissionManager
    }
    
    // MARK: - Location Tracking
    
    func startLocationTracking(interval: TimeInterval) async -> Result<Void, LocationError> {
        return await locationTracker.startLocationTracking(interval: interval)
    }
    
    func stopLocationTracking() async -> Result<Void, LocationError> {
        return await locationTracker.stopLocationTracking()
    }
    
    func getCurrentLocation(timeout: TimeInterval) async -> Result<Location, LocationError> {
        return await locationTracker.getCurrentLocation(timeout: timeout)
    }
    
    // MARK: - Permissions
    
    func checkLocationPermission() async -> PermissionStatus {
        return permissionManager.checkLocationPermission()
    }
    
    func checkBackgroundLocationPermission() async -> PermissionStatus {
        return permissionManager.checkBackgroundLocationPermission()
    }
    
    func requestLocationPermission() async -> Result<PermissionStatus, LocationError> {
        let result = await permissionManager.requestLocationPermissions(requireBackground: false)
        return result.mapError { _ in LocationError.permissionDenied }
    }
    
    func requestBackgroundLocationPermission() async -> Result<PermissionStatus, LocationError> {
        let result = await permissionManager.requestLocationPermissions(requireBackground: true)
        return result.mapError { _ in LocationError.backgroundPermissionDenied }
    }
    
    // MARK: - Location Services
    
    func isLocationEnabled() async -> Bool {
        return locationTracker.isLocationEnabled()
    }
    
    func promptEnableLocation() async -> Result<Bool, LocationError> {
        return await withCheckedContinuation { continuation in
            DispatchQueue.main.async {
                guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
                    continuation.resume(returning: .failure(.unknown(NSError(domain: "LocationService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Cannot open settings"]))))
                    return
                }
                
                if UIApplication.shared.canOpenURL(settingsUrl) {
                    UIApplication.shared.open(settingsUrl) { success in
                        continuation.resume(returning: .success(success))
                    }
                } else {
                    continuation.resume(returning: .failure(.unknown(NSError(domain: "LocationService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Cannot open settings URL"]))))
                }
            }
        }
    }
}