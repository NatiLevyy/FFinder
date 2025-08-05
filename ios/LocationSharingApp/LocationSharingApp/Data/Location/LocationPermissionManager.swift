import Foundation
import CoreLocation
import Combine

/**
 * Manages location permissions for the iOS application.
 */
class LocationPermissionManager: NSObject {
    
    // MARK: - Properties
    
    private let locationManager = CLLocationManager()
    private var permissionSubject = PassthroughSubject<PermissionStatus, Never>()
    private var currentPermissionContinuation: CheckedContinuation<PermissionStatus, Never>?
    
    // MARK: - Publishers
    
    /**
     * Publisher that emits permission status changes.
     */
    var permissionStatusPublisher: AnyPublisher<PermissionStatus, Never> {
        permissionSubject.eraseToAnyPublisher()
    }
    
    // MARK: - Initialization
    
    override init() {
        super.init()
        locationManager.delegate = self
    }
    
    // MARK: - Permission Checking
    
    /**
     * Checks the current status of location permission.
     */
    func checkLocationPermission() -> PermissionStatus {
        guard CLLocationManager.locationServicesEnabled() else {
            return .denied
        }
        
        let authorizationStatus = locationManager.authorizationStatus
        return mapAuthorizationStatus(authorizationStatus)
    }
    
    /**
     * Checks the current status of background location permission.
     */
    func checkBackgroundLocationPermission() -> PermissionStatus {
        guard CLLocationManager.locationServicesEnabled() else {
            return .denied
        }
        
        let authorizationStatus = locationManager.authorizationStatus
        
        switch authorizationStatus {
        case .authorizedAlways:
            return .granted
        case .authorizedWhenInUse:
            return .denied // Has foreground but not background
        case .denied, .restricted:
            return .permanentlyDenied
        case .notDetermined:
            return .unknown
        @unknown default:
            return .unknown
        }
    }
    
    /**
     * Checks if basic location permissions are granted.
     */
    func hasLocationPermission() -> Bool {
        return checkLocationPermission().isGranted
    }
    
    /**
     * Checks if background location permission is granted.
     */
    func hasBackgroundLocationPermission() -> Bool {
        return checkBackgroundLocationPermission().isGranted
    }
    
    // MARK: - Permission Requesting
    
    /**
     * Requests when-in-use location permission.
     */
    func requestWhenInUsePermission() async -> PermissionStatus {
        let currentStatus = checkLocationPermission()
        
        // If already granted or permanently denied, return current status
        if currentStatus.isGranted || currentStatus.isPermanentlyDenied {
            return currentStatus
        }
        
        return await withCheckedContinuation { continuation in
            currentPermissionContinuation = continuation
            locationManager.requestWhenInUseAuthorization()
        }
    }
    
    /**
     * Requests always (background) location permission.
     * Should only be called after when-in-use permission is granted.
     */
    func requestAlwaysPermission() async -> PermissionStatus {
        let currentStatus = checkBackgroundLocationPermission()
        
        // If already granted or permanently denied, return current status
        if currentStatus.isGranted || currentStatus.isPermanentlyDenied {
            return currentStatus
        }
        
        // Must have when-in-use permission first
        let whenInUseStatus = checkLocationPermission()
        guard whenInUseStatus.isGranted else {
            return .denied
        }
        
        return await withCheckedContinuation { continuation in
            currentPermissionContinuation = continuation
            locationManager.requestAlwaysAuthorization()
        }
    }
    
    /**
     * Requests the appropriate location permissions based on requirements.
     */
    func requestLocationPermissions(requireBackground: Bool = false) async -> Result<PermissionStatus, LocationError> {
        // First request when-in-use permission
        let whenInUseStatus = await requestWhenInUsePermission()
        
        guard whenInUseStatus.isGranted else {
            return .failure(.permissionDenied)
        }
        
        // If background permission is required, request it
        if requireBackground {
            let alwaysStatus = await requestAlwaysPermission()
            return .success(alwaysStatus)
        }
        
        return .success(whenInUseStatus)
    }
    
    // MARK: - Utility Methods
    
    /**
     * Checks if location services are enabled system-wide.
     */
    func isLocationServicesEnabled() -> Bool {
        return CLLocationManager.locationServicesEnabled()
    }
    
    /**
     * Gets a user-friendly description of the current permission status.
     */
    func getPermissionStatusDescription() -> String {
        let status = checkLocationPermission()
        let backgroundStatus = checkBackgroundLocationPermission()
        
        switch (status, backgroundStatus) {
        case (.granted, .granted):
            return "Location access granted (Always)"
        case (.granted, .denied):
            return "Location access granted (While Using App)"
        case (.denied, _):
            return "Location access denied"
        case (.permanentlyDenied, _):
            return "Location access permanently denied"
        case (.unknown, _):
            return "Location permission not determined"
        case (.pending, _):
            return "Location permission request pending"
        }
    }
    
    // MARK: - Private Methods
    
    private func mapAuthorizationStatus(_ status: CLAuthorizationStatus) -> PermissionStatus {
        switch status {
        case .authorizedWhenInUse, .authorizedAlways:
            return .granted
        case .denied, .restricted:
            return .permanentlyDenied
        case .notDetermined:
            return .unknown
        @unknown default:
            return .unknown
        }
    }
    
    private func handleAuthorizationStatusChange(_ status: CLAuthorizationStatus) {
        let permissionStatus = mapAuthorizationStatus(status)
        
        // Resume any pending continuation
        currentPermissionContinuation?.resume(returning: permissionStatus)
        currentPermissionContinuation = nil
        
        // Emit status change
        permissionSubject.send(permissionStatus)
    }
}

// MARK: - CLLocationManagerDelegate

extension LocationPermissionManager: CLLocationManagerDelegate {
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        handleAuthorizationStatusChange(manager.authorizationStatus)
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // Handle location manager errors if needed
        print("LocationPermissionManager error: \(error.localizedDescription)")
    }
}