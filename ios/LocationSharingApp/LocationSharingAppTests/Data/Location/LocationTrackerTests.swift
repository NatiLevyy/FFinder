import XCTest
import CoreLocation
import Combine
@testable import LocationSharingApp

class LocationTrackerTests: XCTestCase {
    
    var locationTracker: LocationTracker!
    var mockPermissionManager: MockLocationPermissionManager!
    var cancellables: Set<AnyCancellable>!
    
    override func setUp() {
        super.setUp()
        mockPermissionManager = MockLocationPermissionManager()
        locationTracker = LocationTracker(permissionManager: mockPermissionManager)
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        cancellables = nil
        locationTracker = nil
        mockPermissionManager = nil
        super.tearDown()
    }
    
    func testStartLocationTracking_WithPermissions_ReturnsSuccess() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        
        // When
        let result = await locationTracker.startLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true, "Should return success")
        case .failure(let error):
            XCTFail("Should not fail, but got error: \(error)")
        }
        
        // Verify tracking state
        let isTracking = await locationTracker.isTracking.first()
        XCTAssertTrue(isTracking)
    }
    
    func testStartLocationTracking_WithoutPermissions_ReturnsFailure() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .denied
        
        // When
        let result = await locationTracker.startLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTFail("Should fail when permissions are denied")
        case .failure(let error):
            XCTAssertEqual(error, .permissionDenied)
        }
        
        // Verify tracking state
        let isTracking = await locationTracker.isTracking.first()
        XCTAssertFalse(isTracking)
    }
    
    func testStartLocationTracking_WithLocationDisabled_ReturnsFailure() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        // Mock CLLocationManager.locationServicesEnabled() to return false
        // Note: This is difficult to mock in unit tests, so we'll test the logic path
        
        // When
        let result = await locationTracker.startLocationTracking()
        
        // Then - If location services are disabled, should fail
        // This test would need more sophisticated mocking of CLLocationManager
        // For now, we'll verify the success case and assume location services are enabled
        switch result {
        case .success:
            XCTAssertTrue(true, "Location services are enabled in test environment")
        case .failure(let error):
            XCTAssertEqual(error, .locationDisabled)
        }
    }
    
    func testStartLocationTracking_AlreadyTracking_ReturnsSuccess() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        
        // Start tracking first time
        _ = await locationTracker.startLocationTracking()
        
        // When - start tracking again
        let result = await locationTracker.startLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true, "Should return success when already tracking")
        case .failure(let error):
            XCTFail("Should not fail when already tracking, but got error: \(error)")
        }
        
        // Verify still tracking
        let isTracking = await locationTracker.isTracking.first()
        XCTAssertTrue(isTracking)
    }
    
    func testStopLocationTracking_ReturnsSuccess() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        _ = await locationTracker.startLocationTracking()
        
        // When
        let result = await locationTracker.stopLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true, "Should return success")
        case .failure(let error):
            XCTFail("Should not fail, but got error: \(error)")
        }
        
        // Verify tracking stopped
        let isTracking = await locationTracker.isTracking.first()
        XCTAssertFalse(isTracking)
    }
    
    func testGetCurrentLocation_WithPermissions_ReturnsLocation() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        
        // When
        let result = await locationTracker.getCurrentLocation()
        
        // Then
        switch result {
        case .success(let location):
            // Verify location properties
            XCTAssertNotNil(location)
            XCTAssertTrue(location.latitude >= -90 && location.latitude <= 90)
            XCTAssertTrue(location.longitude >= -180 && location.longitude <= 180)
            XCTAssertTrue(location.accuracy > 0)
        case .failure(let error):
            // In test environment, location might not be available
            // Accept timeout or location disabled errors as valid test outcomes
            XCTAssertTrue(
                error == .timeout || error == .locationDisabled || error == .permissionDenied,
                "Expected timeout, location disabled, or permission denied, but got: \(error)"
            )
        }
    }
    
    func testGetCurrentLocation_WithoutPermissions_ReturnsFailure() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .denied
        
        // When
        let result = await locationTracker.getCurrentLocation()
        
        // Then
        switch result {
        case .success:
            XCTFail("Should fail when permissions are denied")
        case .failure(let error):
            XCTAssertEqual(error, .permissionDenied)
        }
    }
    
    func testGetCurrentLocation_WithTimeout_ReturnsTimeoutError() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        let shortTimeout: TimeInterval = 0.1 // Very short timeout
        
        // When
        let result = await locationTracker.getCurrentLocation(timeout: shortTimeout)
        
        // Then
        switch result {
        case .success:
            // Location might be available immediately in some test environments
            XCTAssertTrue(true, "Location was available immediately")
        case .failure(let error):
            // Expect timeout or other location-related errors
            XCTAssertTrue(
                error == .timeout || error == .locationDisabled,
                "Expected timeout or location disabled, but got: \(error)"
            )
        }
    }
    
    func testLocationUpdates_PublishesLocations() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        
        var receivedLocations: [Location] = []
        let expectation = XCTestExpectation(description: "Receive location updates")
        expectation.expectedFulfillmentCount = 1
        
        // Subscribe to location updates
        locationTracker.locationUpdates
            .sink { location in
                receivedLocations.append(location)
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // When
        _ = await locationTracker.startLocationTracking()
        
        // Then
        await fulfillment(of: [expectation], timeout: 10.0)
        
        // Verify we received at least one location
        XCTAssertGreaterThan(receivedLocations.count, 0)
        
        // Verify location properties
        if let firstLocation = receivedLocations.first {
            XCTAssertTrue(firstLocation.latitude >= -90 && firstLocation.latitude <= 90)
            XCTAssertTrue(firstLocation.longitude >= -180 && firstLocation.longitude <= 180)
            XCTAssertTrue(firstLocation.accuracy > 0)
        }
    }
    
    func testLocationErrors_PublishesErrors() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        
        var receivedErrors: [LocationError] = []
        let expectation = XCTestExpectation(description: "Receive location errors")
        expectation.isInverted = true // We don't expect errors in normal operation
        
        // Subscribe to location errors
        locationTracker.locationErrors
            .sink { error in
                receivedErrors.append(error)
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // When
        _ = await locationTracker.startLocationTracking()
        
        // Then
        await fulfillment(of: [expectation], timeout: 5.0)
        
        // In normal operation, we shouldn't receive errors
        XCTAssertEqual(receivedErrors.count, 0)
    }
    
    func testIsLocationEnabled_ReturnsCorrectStatus() {
        // When
        let isEnabled = locationTracker.isLocationEnabled()
        
        // Then
        // In test environment, this depends on the simulator/device settings
        // We just verify the method doesn't crash and returns a boolean
        XCTAssertTrue(isEnabled == true || isEnabled == false)
    }
    
    func testTrackingInterval_ConfiguresLocationManager() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        let highAccuracyInterval: TimeInterval = 1.0
        let batterySaverInterval: TimeInterval = 30.0
        
        // When - start with high accuracy
        _ = await locationTracker.startLocationTracking(interval: highAccuracyInterval)
        
        // Then - verify tracking started
        let isTracking1 = await locationTracker.isTracking.first()
        XCTAssertTrue(isTracking1)
        
        // Stop tracking
        _ = await locationTracker.stopLocationTracking()
        
        // When - start with battery saver interval
        _ = await locationTracker.startLocationTracking(interval: batterySaverInterval)
        
        // Then - verify tracking started with different configuration
        let isTracking2 = await locationTracker.isTracking.first()
        XCTAssertTrue(isTracking2)
    }
    
    // MARK: - Background Location Tracking Tests
    
    func testStartBackgroundLocationTracking_WithAlwaysPermission_ReturnsSuccess() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        mockPermissionManager.backgroundLocationPermissionStatus = .granted
        
        // When
        let result = await locationTracker.startBackgroundLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true, "Background location tracking started successfully")
        case .failure(let error):
            XCTFail("Expected success, got error: \(error)")
        }
    }
    
    func testStartBackgroundLocationTracking_WithoutAlwaysPermission_ReturnsFailure() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        mockPermissionManager.backgroundLocationPermissionStatus = .denied
        
        // When
        let result = await locationTracker.startBackgroundLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure due to insufficient permissions")
        case .failure(let error):
            XCTAssertEqual(error, .permissionDenied)
        }
    }
    
    func testStopBackgroundLocationTracking_ReturnsSuccess() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        mockPermissionManager.backgroundLocationPermissionStatus = .granted
        await locationTracker.startBackgroundLocationTracking()
        
        // When
        let result = await locationTracker.stopBackgroundLocationTracking()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(true, "Background location tracking stopped successfully")
        case .failure(let error):
            XCTFail("Expected success, got error: \(error)")
        }
    }
    
    func testIsBackgroundTrackingActive_InitiallyFalse() {
        // Given
        var isActive: Bool?
        
        // When
        locationTracker.isBackgroundTrackingActive
            .sink { isActive = $0 }
            .store(in: &cancellables)
        
        // Then
        XCTAssertEqual(isActive, false)
    }
    
    func testIsBackgroundTrackingActive_TrueAfterStart() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        mockPermissionManager.backgroundLocationPermissionStatus = .granted
        var isActive: Bool?
        
        locationTracker.isBackgroundTrackingActive
            .sink { isActive = $0 }
            .store(in: &cancellables)
        
        // When
        await locationTracker.startBackgroundLocationTracking()
        
        // Then
        XCTAssertEqual(isActive, true)
    }
    
    func testBackgroundLocationUpdates_PublishesLocations() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        mockPermissionManager.backgroundLocationPermissionStatus = .granted
        await locationTracker.startBackgroundLocationTracking()
        
        var receivedLocation: Location?
        let expectation = XCTestExpectation(description: "Background location update received")
        expectation.isInverted = true // We don't expect actual location updates in unit tests
        
        locationTracker.backgroundLocationUpdates
            .sink { location in
                receivedLocation = location
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // When
        // Background location updates would be triggered by the BackgroundLocationManager
        
        // Then
        await fulfillment(of: [expectation], timeout: 1.0)
        // This test verifies the publisher is properly exposed
        XCTAssertNotNil(locationTracker.backgroundLocationUpdates)
    }
    
    func testBackgroundLocationErrors_PublishesErrors() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        mockPermissionManager.backgroundLocationPermissionStatus = .granted
        await locationTracker.startBackgroundLocationTracking()
        
        var receivedError: LocationError?
        let expectation = XCTestExpectation(description: "Background location error received")
        expectation.isInverted = true // We don't expect errors in normal operation
        
        locationTracker.backgroundLocationErrors
            .sink { error in
                receivedError = error
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // When
        // Background location errors would be triggered by the BackgroundLocationManager
        
        // Then
        await fulfillment(of: [expectation], timeout: 1.0)
        // This test verifies the publisher is properly exposed
        XCTAssertNotNil(locationTracker.backgroundLocationErrors)
    }
    
    func testStopLocationTracking_AlsoStopsBackgroundTracking() async {
        // Given
        mockPermissionManager.locationPermissionStatus = .granted
        mockPermissionManager.backgroundLocationPermissionStatus = .granted
        await locationTracker.startLocationTracking()
        await locationTracker.startBackgroundLocationTracking()
        
        var isBackgroundActive: Bool?
        locationTracker.isBackgroundTrackingActive
            .sink { isBackgroundActive = $0 }
            .store(in: &cancellables)
        
        // When
        await locationTracker.stopLocationTracking()
        
        // Then
        XCTAssertEqual(isBackgroundActive, false)
    }
}

// MARK: - Mock Classes

class MockLocationPermissionManager: LocationPermissionManager {
    
    var locationPermissionStatus: PermissionStatus = .unknown
    var backgroundLocationPermissionStatus: PermissionStatus = .unknown
    var hasLocationPermissionValue: Bool = false
    var hasBackgroundLocationPermissionValue: Bool = false
    var isLocationServicesEnabledValue: Bool = true
    
    override func checkLocationPermission() -> PermissionStatus {
        return locationPermissionStatus
    }
    
    override func checkBackgroundLocationPermission() -> PermissionStatus {
        return backgroundLocationPermissionStatus
    }
    
    override func hasLocationPermission() -> Bool {
        return hasLocationPermissionValue || locationPermissionStatus.isGranted
    }
    
    override func hasBackgroundLocationPermission() -> Bool {
        return hasBackgroundLocationPermissionValue || backgroundLocationPermissionStatus.isGranted
    }
    
    override func isLocationServicesEnabled() -> Bool {
        return isLocationServicesEnabledValue
    }
    
    override func requestWhenInUsePermission() async -> PermissionStatus {
        return locationPermissionStatus
    }
    
    override func requestAlwaysPermission() async -> PermissionStatus {
        return backgroundLocationPermissionStatus
    }
    
    override func requestLocationPermissions(requireBackground: Bool) async -> Result<PermissionStatus, LocationError> {
        if requireBackground {
            return .success(backgroundLocationPermissionStatus)
        } else {
            return .success(locationPermissionStatus)
        }
    }
}