import Foundation
import CoreLocation
import Combine
import UIKit

/**
 * Manager for handling background location updates on iOS.
 * 
 * This manager coordinates background app refresh and significant location changes
 * to provide continuous location updates even when the app is backgrounded.
 */
class BackgroundLocationManager: NSObject {
    
    // MARK: - Properties
    
    private let locationManager = CLLocationManager()
    private let permissionManager: LocationPermissionManager
    
    // MARK: - State
    
    private let _isBackgroundTrackingActive = CurrentValueSubject<Bool, Never>(false)
    private let _backgroundLocationUpdates = PassthroughSubject<Location, Never>()
    private let _backgroundLocationErrors = PassthroughSubject<LocationError, Never>()
    
    private var backgroundTask: UIBackgroundTaskIdentifier = .invalid
    private var backgroundUpdateTimer: Timer?
    private var lastBackgroundUpdate: Date = Date()
    
    // MARK: - Publishers
    
    /**
     * Publisher indicating if background location tracking is currently active.
     */
    var isBackgroundTrackingActive: AnyPublisher<Bool, Never> {
        _isBackgroundTrackingActive.eraseToAnyPublisher()
    }
    
    /**
     * Publisher of location updates received in background mode.
     */
    var backgroundLocationUpdates: AnyPublisher<Location, Never> {
        _backgroundLocationUpdates.eraseToAnyPublisher()
    }
    
    /**
     * Publisher of location errors that occur during background tracking.
     */
    var backgroundLocationErrors: AnyPublisher<LocationError, Never> {
        _backgroundLocationErrors.eraseToAnyPublisher()
    }
    
    // MARK: - Initialization
    
    init(permissionManager: LocationPermissionManager) {
        self.permissionManager = permissionManager
        super.init()
        setupLocationManager()
        setupAppStateObservers()
    }
    
    // MARK: - Background Location Tracking
    
    /**
     * Starts background location tracking with optimized settings for battery life.
     */
    func startBackgroundLocationTracking() async -> Result<Void, LocationError> {
        // Check permissions - need "always" permission for background
        let permissionStatus = permissionManager.checkLocationPermission()
        guard permissionStatus == .authorizedAlways else {
            return .failure(.permissionDenied)
        }
        
        // Check if location services are enabled
        guard CLLocationManager.locationServicesEnabled() else {
            return .failure(.locationDisabled)
        }
        
        // Configure for background tracking
        configureForBackgroundTracking()
        
        // Start significant location changes for battery efficiency
        locationManager.startMonitoringSignificantLocationChanges()
        
        // Also start standard location updates with reduced accuracy
        locationManager.startUpdatingLocation()
        
        _isBackgroundTrackingActive.send(true)
        
        return .success(())
    }
    
    /**
     * Stops background location tracking.
     */
    func stopBackgroundLocationTracking() async -> Result<Void, LocationError> {
        locationManager.stopMonitoringSignificantLocationChanges()
        locationManager.stopUpdatingLocation()
        
        stopBackgroundTask()
        stopBackgroundUpdateTimer()
        
        _isBackgroundTrackingActive.send(false)
        
        return .success(())
    }
    
    /**
     * Requests background app refresh permission if not already granted.
     */
    func requestBackgroundAppRefresh() async -> Bool {
        guard UIApplication.shared.backgroundRefreshStatus == .available else {
            return false
        }
        
        // Background app refresh is controlled by system settings
        // We can only check if it's available, not request it programmatically
        return UIApplication.shared.backgroundRefreshStatus == .available
    }
    
    // MARK: - Private Methods
    
    private func setupLocationManager() {
        locationManager.delegate = self
        
        // Configure for background efficiency
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.distanceFilter = 100.0 // 100 meters
        
        // Allow deferred location updates for battery optimization
        if CLLocationManager.deferredLocationUpdatesAvailable() {
            locationManager.allowDeferredLocationUpdates(
                untilTraveled: 500.0, // 500 meters
                timeout: 300.0 // 5 minutes
            )
        }
    }
    
    private func configureForBackgroundTracking() {
        // Optimize settings for background use
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.distanceFilter = 50.0 // 50 meters for background
        
        // Enable background location updates
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
    }
    
    private func setupAppStateObservers() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appWillEnterForeground),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
    }
    
    @objc private func appDidEnterBackground() {
        guard _isBackgroundTrackingActive.value else { return }
        
        startBackgroundTask()
        startBackgroundUpdateTimer()
    }
    
    @objc private func appWillEnterForeground() {
        stopBackgroundTask()
        stopBackgroundUpdateTimer()
    }
    
    private func startBackgroundTask() {
        backgroundTask = UIApplication.shared.beginBackgroundTask(withName: "LocationTracking") { [weak self] in
            self?.stopBackgroundTask()
        }
    }
    
    private func stopBackgroundTask() {
        guard backgroundTask != .invalid else { return }
        
        UIApplication.shared.endBackgroundTask(backgroundTask)
        backgroundTask = .invalid
    }
    
    private func startBackgroundUpdateTimer() {
        stopBackgroundUpdateTimer()
        
        // Timer to periodically check location in background
        backgroundUpdateTimer = Timer.scheduledTimer(withTimeInterval: 60.0, repeats: true) { [weak self] _ in
            self?.performBackgroundLocationUpdate()
        }
    }
    
    private func stopBackgroundUpdateTimer() {
        backgroundUpdateTimer?.invalidate()
        backgroundUpdateTimer = nil
    }
    
    private func performBackgroundLocationUpdate() {
        guard UIApplication.shared.applicationState == .background else { return }
        
        // Request a one-time location update
        locationManager.requestLocation()
        lastBackgroundUpdate = Date()
    }
    
    private func handleBackgroundLocationUpdate(_ clLocation: CLLocation) {
        let location = Location(from: clLocation)
        
        // Validate location quality (more lenient for background)
        guard location.accuracy <= Constants.maxBackgroundAccuracy else {
            _backgroundLocationErrors.send(.inaccurateLocation(accuracy: location.accuracy))
            return
        }
        
        // Check if location is reasonably fresh
        let age = Date().timeIntervalSince(Date(timeIntervalSince1970: location.timestamp / 1000))
        guard age <= Constants.maxLocationAge else {
            _backgroundLocationErrors.send(.outdatedLocation(age: Int64(age * 1000)))
            return
        }
        
        // Send background location update
        _backgroundLocationUpdates.send(location)
        
        // TODO: Send to repository/backend for sharing with friends
        // This would be implemented when location synchronization is added
    }
    
    private func handleBackgroundLocationError(_ error: Error) {
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
        
        _backgroundLocationErrors.send(locationError)
    }
    
    // MARK: - Constants
    
    private struct Constants {
        static let maxBackgroundAccuracy: Float = 200.0 // 200 meters
        static let maxLocationAge: TimeInterval = 300.0 // 5 minutes
        static let backgroundUpdateInterval: TimeInterval = 60.0 // 1 minute
    }
}

// MARK: - CLLocationManagerDelegate

extension BackgroundLocationManager: CLLocationManagerDelegate {
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        // Handle background location update
        if UIApplication.shared.applicationState == .background {
            handleBackgroundLocationUpdate(location)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        handleBackgroundLocationError(error)
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let permissionStatus = permissionManager.checkLocationPermission()
        
        // Stop background tracking if permission is revoked
        if permissionStatus != .authorizedAlways && _isBackgroundTrackingActive.value {
            Task {
                await stopBackgroundLocationTracking()
            }
            _backgroundLocationErrors.send(.permissionDenied)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFinishDeferredUpdatesWithError error: Error?) {
        if let error = error {
            handleBackgroundLocationError(error)
        }
    }
}