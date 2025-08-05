import Foundation
import CoreLocation
import Combine

/**
 * iOS implementation of location tracking using CLLocationManager.
 */
class LocationTracker: NSObject {
    
    // MARK: - Properties
    
    private let locationManager = CLLocationManager()
    private let permissionManager: LocationPermissionManager
    private let backgroundLocationManager: BackgroundLocationManager
    
    // MARK: - State
    
    private let _isTracking = CurrentValueSubject<Bool, Never>(false)
    private let _locationUpdates = PassthroughSubject<Location, Never>()
    private let _locationErrors = PassthroughSubject<LocationError, Never>()
    
    private var updateInterval: TimeInterval = Constants.defaultUpdateInterval
    private var locationUpdateTimer: Timer?
    private var currentLocationContinuation: CheckedContinuation<Result<Location, LocationError>, Never>?
    private var timeoutTimer: Timer?
    
    // MARK: - Publishers
    
    /**
     * Publisher indicating if location tracking is currently active.
     */
    var isTracking: AnyPublisher<Bool, Never> {
        _isTracking.eraseToAnyPublisher()
    }
    
    /**
     * Publisher of location updates while tracking is active.
     */
    var locationUpdates: AnyPublisher<Location, Never> {
        _locationUpdates.eraseToAnyPublisher()
    }
    
    /**
     * Publisher of location errors that occur during tracking.
     */
    var locationErrors: AnyPublisher<LocationError, Never> {
        _locationErrors.eraseToAnyPublisher()
    }
    
    // MARK: - Initialization
    
    init(permissionManager: LocationPermissionManager) {
        self.permissionManager = permissionManager
        self.backgroundLocationManager = BackgroundLocationManager(permissionManager: permissionManager)
        super.init()
        setupLocationManager()
    }
    
    // MARK: - Location Tracking
    
    /**
     * Starts location tracking with the specified update interval.
     */
    func startLocationTracking(interval: TimeInterval = Constants.defaultUpdateInterval) async -> Result<Void, LocationError> {
        // Check if already tracking
        if _isTracking.value {
            return .success(())
        }
        
        // Check permissions
        let permissionStatus = permissionManager.checkLocationPermission()
        guard permissionStatus.isGranted else {
            return .failure(.permissionDenied)
        }
        
        // Check if location services are enabled
        guard isLocationEnabled() else {
            return .failure(.locationDisabled)
        }
        
        // Configure location manager for tracking
        updateInterval = interval
        configureLocationManager(for: interval)
        
        // Start location updates
        locationManager.startUpdatingLocation()
        _isTracking.send(true)
        
        // Start timer for regular updates if needed
        startUpdateTimer()
        
        return .success(())
    }
    
    /**
     * Stops location tracking.
     */
    func stopLocationTracking() async -> Result<Void, LocationError> {
        locationManager.stopUpdatingLocation()
        stopUpdateTimer()
        _isTracking.send(false)
        
        // Also stop background tracking if active
        await stopBackgroundLocationTracking()
        
        return .success(())
    }
    
    /**
     * Starts background location tracking for continuous updates when app is backgrounded.
     */
    func startBackgroundLocationTracking() async -> Result<Void, LocationError> {
        return await backgroundLocationManager.startBackgroundLocationTracking()
    }
    
    /**
     * Stops background location tracking.
     */
    func stopBackgroundLocationTracking() async -> Result<Void, LocationError> {
        return await backgroundLocationManager.stopBackgroundLocationTracking()
    }
    
    /**
     * Publisher indicating if background location tracking is currently active.
     */
    var isBackgroundTrackingActive: AnyPublisher<Bool, Never> {
        backgroundLocationManager.isBackgroundTrackingActive
    }
    
    /**
     * Publisher of location updates received in background mode.
     */
    var backgroundLocationUpdates: AnyPublisher<Location, Never> {
        backgroundLocationManager.backgroundLocationUpdates
    }
    
    /**
     * Publisher of location errors that occur during background tracking.
     */
    var backgroundLocationErrors: AnyPublisher<LocationError, Never> {
        backgroundLocationManager.backgroundLocationErrors
    }
    
    /**
     * Gets the current location as a one-time request.
     */
    func getCurrentLocation(timeout: TimeInterval = Constants.defaultTimeout) async -> Result<Location, LocationError> {
        // Check permissions
        let permissionStatus = permissionManager.checkLocationPermission()
        guard permissionStatus.isGranted else {
            return .failure(.permissionDenied)
        }
        
        // Check if location services are enabled
        guard isLocationEnabled() else {
            return .failure(.locationDisabled)
        }
        
        return await withCheckedContinuation { continuation in
            currentLocationContinuation = continuation
            
            // Set up timeout
            timeoutTimer = Timer.scheduledTimer(withTimeInterval: timeout, repeats: false) { [weak self] _ in
                self?.handleLocationTimeout()
            }
            
            // Request one-time location
            locationManager.requestLocation()
        }
    }
    
    // MARK: - Location Services
    
    /**
     * Checks if location services are enabled on the device.
     */
    func isLocationEnabled() -> Bool {
        return CLLocationManager.locationServicesEnabled()
    }
    
    // MARK: - Private Methods
    
    private func setupLocationManager() {
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 5.0 // 5 meters
    }
    
    private func configureLocationManager(for interval: TimeInterval) {
        // Configure accuracy based on update interval
        switch interval {
        case 0..<2.0:
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
            locationManager.distanceFilter = 1.0
        case 2.0..<10.0:
            locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
            locationManager.distanceFilter = 5.0
        case 10.0..<30.0:
            locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
            locationManager.distanceFilter = 10.0
        default:
            locationManager.desiredAccuracy = kCLLocationAccuracyKilometer
            locationManager.distanceFilter = 50.0
        }
    }
    
    private func startUpdateTimer() {
        stopUpdateTimer()
        
        // Only use timer for intervals > 1 second to avoid over-requesting
        guard updateInterval > 1.0 else { return }
        
        locationUpdateTimer = Timer.scheduledTimer(withTimeInterval: updateInterval, repeats: true) { [weak self] _ in
            guard let self = self, self._isTracking.value else { return }
            // The location manager will automatically provide updates based on distance filter
            // This timer is just to ensure we get regular updates even if the user isn't moving much
        }
    }
    
    private func stopUpdateTimer() {
        locationUpdateTimer?.invalidate()
        locationUpdateTimer = nil
    }
    
    private func handleLocationUpdate(_ clLocation: CLLocation) {
        let location = Location(from: clLocation)
        
        // Validate location quality
        if !location.hasAcceptableAccuracy {
            _locationErrors.send(.inaccurateLocation(accuracy: location.accuracy))
            return
        }
        
        if !location.isFresh {
            _locationErrors.send(.outdatedLocation(age: location.age))
            return
        }
        
        // Send location update
        _locationUpdates.send(location)
        
        // Handle one-time location request
        if let continuation = currentLocationContinuation {
            timeoutTimer?.invalidate()
            timeoutTimer = nil
            currentLocationContinuation = nil
            continuation.resume(returning: .success(location))
        }
    }
    
    private func handleLocationError(_ error: Error) {
        let locationError: LocationError
        
        if let clError = error as? CLError {
            switch clError.code {
            case .denied:
                locationError = .permissionDenied
            case .locationUnknown:
                locationError = .locationDisabled
            case .network:
                locationError = .networkNotAvailable
            default:
                locationError = .unknown(error)
            }
        } else {
            locationError = .unknown(error)
        }
        
        _locationErrors.send(locationError)
        
        // Handle one-time location request
        if let continuation = currentLocationContinuation {
            timeoutTimer?.invalidate()
            timeoutTimer = nil
            currentLocationContinuation = nil
            continuation.resume(returning: .failure(locationError))
        }
    }
    
    private func handleLocationTimeout() {
        timeoutTimer?.invalidate()
        timeoutTimer = nil
        
        if let continuation = currentLocationContinuation {
            currentLocationContinuation = nil
            continuation.resume(returning: .failure(.timeout))
        }
    }
    
    // MARK: - Constants
    
    private struct Constants {
        static let defaultUpdateInterval: TimeInterval = 5.0 // 5 seconds
        static let defaultTimeout: TimeInterval = 10.0 // 10 seconds
        static let highAccuracyInterval: TimeInterval = 1.0 // 1 second
        static let batterySaverInterval: TimeInterval = 30.0 // 30 seconds
    }
}

// MARK: - CLLocationManagerDelegate

extension LocationTracker: CLLocationManagerDelegate {
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        handleLocationUpdate(location)
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        handleLocationError(error)
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let permissionStatus = permissionManager.checkLocationPermission()
        
        if !permissionStatus.isGranted && _isTracking.value {
            // Permission was revoked while tracking
            Task {
                await stopLocationTracking()
            }
            _locationErrors.send(.permissionDenied)
        }
    }
}