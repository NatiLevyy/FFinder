import Foundation
import Combine

/**
 * Protocol for location tracking and management services.
 */
protocol LocationService {
    
    // MARK: - Location Tracking
    
    /**
     * Starts location tracking with the specified update interval.
     * 
     * - Parameter interval: The interval between location updates in seconds
     * - Returns: Result indicating success or failure
     */
    func startLocationTracking(interval: TimeInterval) async -> Result<Void, LocationError>
    
    /**
     * Stops location tracking.
     * 
     * - Returns: Result indicating success or failure
     */
    func stopLocationTracking() async -> Result<Void, LocationError>
    
    /**
     * Gets the current location as a one-time request.
     * 
     * - Parameter timeout: Timeout for the location request in seconds
     * - Returns: Result containing the current location or error
     */
    func getCurrentLocation(timeout: TimeInterval) async -> Result<Location, LocationError>
    
    // MARK: - Publishers
    
    /**
     * Publisher of location updates while tracking is active.
     */
    var locationUpdates: AnyPublisher<Location, Never> { get }
    
    /**
     * Publisher of location errors that occur during tracking.
     */
    var locationErrors: AnyPublisher<LocationError, Never> { get }
    
    /**
     * Publisher indicating if location tracking is currently active.
     */
    var isTracking: AnyPublisher<Bool, Never> { get }
    
    // MARK: - Permissions
    
    /**
     * Checks the current location permission status.
     * 
     * - Returns: Current permission status
     */
    func checkLocationPermission() async -> PermissionStatus
    
    /**
     * Checks the current background location permission status.
     * 
     * - Returns: Current background permission status
     */
    func checkBackgroundLocationPermission() async -> PermissionStatus
    
    /**
     * Requests location permissions from the user.
     * 
     * - Returns: Result containing the permission status
     */
    func requestLocationPermission() async -> Result<PermissionStatus, LocationError>
    
    /**
     * Requests background location permission from the user.
     * 
     * - Returns: Result containing the permission status
     */
    func requestBackgroundLocationPermission() async -> Result<PermissionStatus, LocationError>
    
    // MARK: - Location Services
    
    /**
     * Checks if location services are enabled on the device.
     * 
     * - Returns: True if location services are enabled
     */
    func isLocationEnabled() async -> Bool
    
    /**
     * Prompts the user to enable location services if disabled.
     * 
     * - Returns: Result indicating if location services were enabled
     */
    func promptEnableLocation() async -> Result<Bool, LocationError>
}

// MARK: - Constants

extension LocationService {
    static var defaultUpdateInterval: TimeInterval { 5.0 } // 5 seconds
    static var defaultTimeout: TimeInterval { 10.0 } // 10 seconds
    static var highAccuracyInterval: TimeInterval { 1.0 } // 1 second
    static var batterySaverInterval: TimeInterval { 30.0 } // 30 seconds
}