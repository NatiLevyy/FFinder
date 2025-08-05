import Foundation
import CoreLocation
@testable import LocationSharingApp

/**
 * Mock implementation of Core Location services for iOS integration testing.
 * Provides controlled location updates without requiring actual GPS.
 */
class MockLocationManager: NSObject {
    
    static let shared = MockLocationManager()
    
    private var currentLatitude: Double = 37.7749
    private var currentLongitude: Double = -122.4194
    private var currentAccuracy: Double = 10.0
    private var shouldFailLocationRequest = false
    private var locationError: Error?
    private var locationUpdateTimer: Timer?
    
    weak var delegate: CLLocationManagerDelegate?
    
    private override init() {
        super.init()
    }
    
    /**
     * Sets the mock location that will be returned by location requests.
     */
    func setMockLocation(latitude: Double, longitude: Double, accuracy: Double = 10.0) {
        currentLatitude = latitude
        currentLongitude = longitude
        currentAccuracy = accuracy
    }
    
    /**
     * Configures the mock to simulate location request failures.
     */
    func simulateLocationFailure(error: Error) {
        shouldFailLocationRequest = true
        locationError = error
    }
    
    /**
     * Configures the mock to simulate successful location requests.
     */
    func simulateSuccessfulLocation() {
        shouldFailLocationRequest = false
        locationError = nil
    }
    
    /**
     * Simulates a location update by calling the delegate.
     */
    func simulateLocationUpdate(latitude: Double, longitude: Double, accuracy: Double = 10.0) {
        setMockLocation(latitude: latitude, longitude: longitude, accuracy: accuracy)
        
        let location = CLLocation(
            coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            altitude: 0,
            horizontalAccuracy: accuracy,
            verticalAccuracy: -1,
            timestamp: Date()
        )
        
        delegate?.locationManager?(CLLocationManager(), didUpdateLocations: [location])
    }
    
    /**
     * Simulates multiple location updates over time.
     */
    func simulateLocationUpdates(locations: [(Double, Double, Double)], interval: TimeInterval = 1.0) {
        var index = 0
        locationUpdateTimer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] timer in
            guard let self = self, index < locations.count else {
                timer.invalidate()
                return
            }
            
            let (lat, lng, accuracy) = locations[index]
            self.simulateLocationUpdate(latitude: lat, longitude: lng, accuracy: accuracy)
            index += 1
        }
    }
    
    // MARK: - Mock CLLocationManager Methods
    
    var authorizationStatus: CLAuthorizationStatus = .notDetermined
    
    func requestWhenInUseAuthorization() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            if self.shouldFailLocationRequest {
                self.authorizationStatus = .denied
                self.delegate?.locationManager?(CLLocationManager(), didFailWithError: self.locationError ?? CLError(.denied))
            } else {
                self.authorizationStatus = .authorizedWhenInUse
                self.delegate?.locationManager?(CLLocationManager(), didChangeAuthorization: .authorizedWhenInUse)
            }
        }
    }
    
    func requestAlwaysAuthorization() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            if self.shouldFailLocationRequest {
                self.authorizationStatus = .denied
                self.delegate?.locationManager?(CLLocationManager(), didFailWithError: self.locationError ?? CLError(.denied))
            } else {
                self.authorizationStatus = .authorizedAlways
                self.delegate?.locationManager?(CLLocationManager(), didChangeAuthorization: .authorizedAlways)
            }
        }
    }
    
    func startUpdatingLocation() {
        guard !shouldFailLocationRequest else {
            delegate?.locationManager?(CLLocationManager(), didFailWithError: locationError ?? CLError(.locationUnknown))
            return
        }
        
        // Immediately provide first location update
        simulateLocationUpdate(latitude: currentLatitude, longitude: currentLongitude, accuracy: currentAccuracy)
    }
    
    func stopUpdatingLocation() {
        locationUpdateTimer?.invalidate()
        locationUpdateTimer = nil
    }
    
    func requestLocation() {
        guard !shouldFailLocationRequest else {
            delegate?.locationManager?(CLLocationManager(), didFailWithError: locationError ?? CLError(.locationUnknown))
            return
        }
        
        simulateLocationUpdate(latitude: currentLatitude, longitude: currentLongitude, accuracy: currentAccuracy)
    }
    
    var location: CLLocation? {
        guard !shouldFailLocationRequest else { return nil }
        
        return CLLocation(
            coordinate: CLLocationCoordinate2D(latitude: currentLatitude, longitude: currentLongitude),
            altitude: 0,
            horizontalAccuracy: currentAccuracy,
            verticalAccuracy: -1,
            timestamp: Date()
        )
    }
    
    func stopLocationUpdates() {
        stopUpdatingLocation()
    }
}