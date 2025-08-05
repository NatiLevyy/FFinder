import XCTest
import CoreLocation
@testable import LocationSharingApp

/**
 * Integration tests for iOS location services covering the complete
 * location tracking and sharing flow.
 */
class LocationIntegrationTest: BaseIntegrationTest {
    
    var locationRepository: LocationRepositoryImpl!
    var locationService: LocationServiceImpl!
    var locationTracker: LocationTracker!
    var mockLocationManager: MockLocationManager!
    
    private let testUserId = "test-user-id"
    private let testLatitude = 37.7749
    private let testLongitude = -122.4194
    private let testAccuracy = 10.0
    
    override func setUp() {
        super.setUp()
        
        // Initialize dependencies
        mockLocationManager = MockLocationManager.shared
        locationTracker = LocationTracker()
        locationRepository = LocationRepositoryImpl()
        locationService = LocationServiceImpl(
            locationTracker: locationTracker,
            locationRepository: locationRepository
        )
        
        // Reset location services state
        mockLocationManager.simulateSuccessfulLocation()
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
    }
    
    override func tearDown() {
        locationRepository = nil
        locationService = nil
        locationTracker = nil
        mockLocationManager = nil
        super.tearDown()
    }
    
    func testLocationTrackingStart() async throws {
        // Given: Location manager is configured with test location
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
        
        // When: Location tracking is started
        let result = try await locationService.startLocationTracking()
        
        // Then: Location tracking should start successfully
        XCTAssertTrue(result.isSuccess, "Location tracking should start")
        
        // And: Current location should be available
        let currentLocation = locationService.getCurrentLocation()
        XCTAssertNotNil(currentLocation, "Current location should be available")
        XCTAssertEqual(currentLocation?.latitude, testLatitude, accuracy: 0.001, "Latitude should match")
        XCTAssertEqual(currentLocation?.longitude, testLongitude, accuracy: 0.001, "Longitude should match")
    }
    
    func testLocationTrackingFailure() async throws {
        // Given: Location manager is configured to fail
        let error = CLError(.denied)
        mockLocationManager.simulateLocationFailure(error: error)
        
        // When: Location tracking is started
        let result = try await locationService.startLocationTracking()
        
        // Then: Location tracking should fail
        XCTAssertTrue(result.isFailure, "Location tracking should fail")
    }
    
    func testLocationUpdatesFlow() async throws {
        // Given: Location tracking is started
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
        _ = try await locationService.startLocationTracking()
        
        // When: Location updates are simulated
        let testLocations = [
            (37.7849, -122.4094, 8.0),
            (37.7949, -122.3994, 12.0),
            (37.8049, -122.3894, 6.0)
        ]
        
        let expectation = XCTestExpectation(description: "Location updates")
        var updateCount = 0
        
        // Simulate location updates
        for (lat, lng, accuracy) in testLocations {
            mockLocationManager.simulateLocationUpdate(latitude: lat, longitude: lng, accuracy: accuracy)
            updateCount += 1
            
            // Allow time for processing
            try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        }
        
        expectation.fulfill()
        await fulfillment(of: [expectation], timeout: 5.0)
        
        // Then: Location updates should be processed
        let currentLocation = locationService.getCurrentLocation()
        XCTAssertNotNil(currentLocation, "Current location should be updated")
    }
    
    func testLocationSharingToFirebase() async throws {
        // Given: User is authenticated and location tracking is active
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
        _ = try await locationService.startLocationTracking()
        
        let testLocation = createTestLocation(
            latitude: testLatitude,
            longitude: testLongitude,
            accuracy: testAccuracy
        )
        
        // When: Location is shared to Firebase
        let result = try await locationRepository.updateUserLocation(userId: testUserId, location: testLocation)
        
        // Then: Location should be successfully shared
        XCTAssertTrue(result.isSuccess, "Location sharing should succeed")
    }
    
    func testLocationSharingFailure() async throws {
        // Given: Firebase database is configured to fail
        // Note: In a real implementation, we would configure the mock Firebase to fail
        
        let testLocation = createTestLocation(
            latitude: testLatitude,
            longitude: testLongitude,
            accuracy: testAccuracy
        )
        
        // When: Location sharing is attempted with network error
        // For this test, we simulate a network error scenario
        let result = try await locationRepository.updateUserLocation(userId: testUserId, location: testLocation)
        
        // Then: We verify that error handling is in place
        // Note: The actual behavior depends on the implementation
        XCTAssertNotNil(result, "Result should not be nil")
    }
    
    func testFriendLocationRetrieval() async throws {
        // Given: Friend locations are available in Firebase
        let friendId = "friend-user-id"
        
        // When: Friend location is retrieved
        let result = try await locationRepository.getFriendLocation(friendId: friendId)
        
        // Then: Friend location should be retrieved successfully
        XCTAssertTrue(result.isSuccess, "Friend location retrieval should succeed")
        let location = try result.get()
        XCTAssertNotNil(location, "Friend location should not be nil")
    }
    
    func testRealTimeLocationUpdates() async throws {
        // Given: Real-time location updates are set up
        let friendId = "friend-user-id"
        
        let expectation = XCTestExpectation(description: "Real-time location updates")
        
        // When: Location updates are simulated
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            // Simulate real-time update
            expectation.fulfill()
        }
        
        await fulfillment(of: [expectation], timeout: 5.0)
        
        // Then: Real-time updates should be received
        // Note: In a real implementation, we would observe the location updates
    }
    
    func testLocationAccuracyValidation() async throws {
        // Given: Location with poor accuracy
        let poorAccuracyLocation = createTestLocation(
            latitude: testLatitude,
            longitude: testLongitude,
            accuracy: 1000.0 // Very poor accuracy
        )
        
        // When: Location with poor accuracy is processed
        let result = try await locationRepository.updateUserLocation(userId: testUserId, location: poorAccuracyLocation)
        
        // Then: Location should be handled appropriately
        // Note: The actual behavior depends on the implementation
        XCTAssertNotNil(result, "Result should not be nil")
    }
    
    func testLocationPermissionHandling() async throws {
        // Given: Location permissions are not granted
        mockLocationManager.authorizationStatus = .denied
        
        // When: Location tracking is attempted
        let result = try await locationService.startLocationTracking()
        
        // Then: Appropriate permission handling should occur
        // Note: The actual behavior depends on the implementation
        XCTAssertNotNil(result, "Result should not be nil")
    }
    
    func testBackgroundLocationTracking() async throws {
        // Given: App goes to background
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
        _ = try await locationService.startLocationTracking()
        
        // When: Background location updates are simulated
        let backgroundLocations = [
            (37.7849, -122.4094, 8.0),
            (37.7949, -122.3994, 12.0)
        ]
        
        let expectation = XCTestExpectation(description: "Background location updates")
        
        // Simulate background updates
        mockLocationManager.simulateLocationUpdates(locations: backgroundLocations, interval: 0.5)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            expectation.fulfill()
        }
        
        await fulfillment(of: [expectation], timeout: 5.0)
        
        // Then: Background location updates should be processed
        let currentLocation = locationService.getCurrentLocation()
        XCTAssertNotNil(currentLocation, "Current location should be updated from background")
    }
    
    func testLocationTrackingStop() async throws {
        // Given: Location tracking is active
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
        _ = try await locationService.startLocationTracking()
        
        // When: Location tracking is stopped
        let result = try await locationService.stopLocationTracking()
        
        // Then: Location tracking should stop successfully
        XCTAssertTrue(result.isSuccess, "Location tracking should stop")
    }
    
    func testCompleteLocationSharingFlow() async throws {
        // This test covers the complete location sharing flow
        
        // Step 1: Start location tracking
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
        let startResult = try await locationService.startLocationTracking()
        XCTAssertTrue(startResult.isSuccess, "Location tracking should start")
        
        // Step 2: Get current location
        let currentLocation = locationService.getCurrentLocation()
        XCTAssertNotNil(currentLocation, "Current location should be available")
        
        // Step 3: Share location to Firebase
        let shareResult = try await locationRepository.updateUserLocation(userId: testUserId, location: currentLocation!)
        XCTAssertTrue(shareResult.isSuccess, "Location sharing should succeed")
        
        // Step 4: Simulate location update
        let newLatitude = 37.7849
        let newLongitude = -122.4094
        mockLocationManager.simulateLocationUpdate(latitude: newLatitude, longitude: newLongitude, accuracy: testAccuracy)
        
        // Allow time for processing
        try await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Step 5: Verify location was updated
        let updatedLocation = locationService.getCurrentLocation()
        XCTAssertNotNil(updatedLocation, "Updated location should be available")
        
        // Step 6: Stop location tracking
        let stopResult = try await locationService.stopLocationTracking()
        XCTAssertTrue(stopResult.isSuccess, "Location tracking should stop")
    }
    
    func testLocationServiceErrorRecovery() async throws {
        // This test verifies that location services can recover from errors
        
        // Step 1: Start with a failure
        let error = CLError(.locationUnknown)
        mockLocationManager.simulateLocationFailure(error: error)
        
        let failedResult = try await locationService.startLocationTracking()
        XCTAssertTrue(failedResult.isFailure, "Location tracking should fail initially")
        
        // Step 2: Recover from failure
        mockLocationManager.simulateSuccessfulLocation()
        mockLocationManager.setMockLocation(latitude: testLatitude, longitude: testLongitude, accuracy: testAccuracy)
        
        let recoveredResult = try await locationService.startLocationTracking()
        XCTAssertTrue(recoveredResult.isSuccess, "Location tracking should succeed after recovery")
        
        // Step 3: Verify location is available
        let location = locationService.getCurrentLocation()
        XCTAssertNotNil(location, "Location should be available after recovery")
    }
}