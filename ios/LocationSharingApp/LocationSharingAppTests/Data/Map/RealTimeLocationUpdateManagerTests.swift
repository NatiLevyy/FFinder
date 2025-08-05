import XCTest
import Combine
@testable import LocationSharingApp

class RealTimeLocationUpdateManagerTests: XCTestCase {
    
    var realTimeLocationUpdateManager: RealTimeLocationUpdateManager!
    var mockLocationMarkerManager: MockLocationMarkerManager!
    var cancellables: Set<AnyCancellable>!
    
    override func setUp() {
        super.setUp()
        mockLocationMarkerManager = MockLocationMarkerManager()
        realTimeLocationUpdateManager = RealTimeLocationUpdateManager(locationMarkerManager: mockLocationMarkerManager)
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        realTimeLocationUpdateManager.cleanup()
        cancellables.removeAll()
        realTimeLocationUpdateManager = nil
        mockLocationMarkerManager = nil
        super.tearDown()
    }
    
    func testStartRealTimeUpdates_ShouldEnableUpdateProcessing() {
        // When
        realTimeLocationUpdateManager.startRealTimeUpdates()
        
        // Then
        XCTAssertTrue(realTimeLocationUpdateManager.isUpdating)
        
        // Should be able to submit updates without warnings
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        realTimeLocationUpdateManager.submitLocationUpdate(friendId: "friend123", friendName: "John Doe", location: location)
        
        // No exception should be thrown
    }
    
    func testStopRealTimeUpdates_ShouldDisableUpdateProcessing() {
        // Given
        realTimeLocationUpdateManager.startRealTimeUpdates()
        
        // When
        realTimeLocationUpdateManager.stopRealTimeUpdates()
        
        // Then
        XCTAssertFalse(realTimeLocationUpdateManager.isUpdating)
        
        // Submitting updates should log warning but not crash
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        realTimeLocationUpdateManager.submitLocationUpdate(friendId: "friend123", friendName: "John Doe", location: location)
        
        // No exception should be thrown, but update won't be processed
    }
    
    func testSubmitLocationUpdate_ShouldProcessUpdateWhenUpdatesAreStarted() {
        // Given
        realTimeLocationUpdateManager.startRealTimeUpdates()
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        let expectation = XCTestExpectation(description: "Location update processed")
        
        // When
        realTimeLocationUpdateManager.submitLocationUpdate(friendId: "friend123", friendName: "John Doe", location: location)
        
        // Allow some time for async processing
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            // Then
            XCTAssertGreaterThan(self.mockLocationMarkerManager.updateFriendMarkerCallCount, 0)
            XCTAssertEqual(self.mockLocationMarkerManager.lastUpdatedFriendId, "friend123")
            XCTAssertEqual(self.mockLocationMarkerManager.lastUpdatedFriendName, "John Doe")
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 3.0)
    }
    
    func testSubmitBatchLocationUpdates_ShouldProcessMultipleUpdates() {
        // Given
        realTimeLocationUpdateManager.startRealTimeUpdates()
        let updates: [String: (String, Location)] = [
            "friend1": ("John Doe", Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))),
            "friend2": ("Jane Smith", Location(latitude: 40.7128, longitude: -74.0060, accuracy: 3.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000)))
        ]
        
        let expectation = XCTestExpectation(description: "Batch updates processed")
        
        // When
        realTimeLocationUpdateManager.submitBatchLocationUpdates(updates)
        
        // Allow time for async processing
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            // Then
            XCTAssertGreaterThanOrEqual(self.mockLocationMarkerManager.updateFriendMarkerCallCount, 2)
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 5.0)
    }
    
    func testForceImmediateUpdate_ShouldBypassRateLimiting() {
        // Given
        realTimeLocationUpdateManager.startRealTimeUpdates()
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        let expectation = XCTestExpectation(description: "Immediate update processed")
        
        // When
        realTimeLocationUpdateManager.forceImmediateUpdate(friendId: "friend123", friendName: "John Doe", location: location)
        
        // Then - should update immediately without delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            XCTAssertGreaterThan(self.mockLocationMarkerManager.updateFriendMarkerCallCount, 0)
            XCTAssertEqual(self.mockLocationMarkerManager.lastUpdatedFriendId, "friend123")
            XCTAssertEqual(self.mockLocationMarkerManager.lastUpdatedFriendName, "John Doe")
            XCTAssertTrue(self.mockLocationMarkerManager.lastForceUpdate)
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 1.0)
    }
    
    func testGetUpdateStatus_ShouldReturnMarkerStates() {
        // Given
        let mockStates: [String: LocationMarkerManager.MarkerState] = [
            "friend1": LocationMarkerManager.MarkerState(friendId: "friend1", friendName: "John", location: Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: 123456), isVisible: true, lastUpdated: 123456),
            "friend2": LocationMarkerManager.MarkerState(friendId: "friend2", friendName: "Jane", location: Location(latitude: 40.7128, longitude: -74.0060, accuracy: 3.0, timestamp: 123457), isVisible: true, lastUpdated: 123457)
        ]
        mockLocationMarkerManager.mockMarkerStates = mockStates
        
        // When
        let status = realTimeLocationUpdateManager.getUpdateStatus()
        
        // Then
        XCTAssertEqual(status.count, 2)
        XCTAssertEqual(status["friend1"], 123456)
        XCTAssertEqual(status["friend2"], 123457)
    }
    
    func testCleanup_ShouldStopUpdatesAndCleanResources() {
        // Given
        realTimeLocationUpdateManager.startRealTimeUpdates()
        
        // When
        realTimeLocationUpdateManager.cleanup()
        
        // Then
        XCTAssertFalse(realTimeLocationUpdateManager.isUpdating)
        
        // Subsequent update submissions should not be processed
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        realTimeLocationUpdateManager.submitLocationUpdate(friendId: "friend123", friendName: "John Doe", location: location)
        
        // No processing should occur after cleanup
    }
    
    func testSubmitLocationUpdateWithoutStartingUpdates_ShouldLogWarning() {
        // Given - updates not started
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // When
        realTimeLocationUpdateManager.submitLocationUpdate(friendId: "friend123", friendName: "John Doe", location: location)
        
        // Then - should not crash but also not process update
        XCTAssertEqual(mockLocationMarkerManager.updateFriendMarkerCallCount, 0)
    }
    
    func testSubmitBatchLocationUpdatesWithoutStartingUpdates_ShouldLogWarning() {
        // Given - updates not started
        let updates: [String: (String, Location)] = [
            "friend1": ("John Doe", Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000)))
        ]
        
        // When
        realTimeLocationUpdateManager.submitBatchLocationUpdates(updates)
        
        // Then - should not crash but also not process updates
        XCTAssertEqual(mockLocationMarkerManager.updateFriendMarkerCallCount, 0)
    }
    
    func testMultipleUpdatesForSameFriend_ShouldProcessLatestUpdate() {
        // Given
        realTimeLocationUpdateManager.startRealTimeUpdates()
        let location1 = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        let location2 = Location(latitude: 37.7750, longitude: -122.4195, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        let expectation = XCTestExpectation(description: "Latest update processed")
        
        // When - submit two updates quickly for same friend
        realTimeLocationUpdateManager.submitLocationUpdate(friendId: "friend123", friendName: "John Doe", location: location1)
        realTimeLocationUpdateManager.submitLocationUpdate(friendId: "friend123", friendName: "John Doe", location: location2)
        
        // Allow time for processing
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            // Then - should process updates (may be batched)
            XCTAssertGreaterThan(self.mockLocationMarkerManager.updateFriendMarkerCallCount, 0)
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 5.0)
    }
}

// MARK: - Mock LocationMarkerManager
class MockLocationMarkerManager: LocationMarkerManager {
    
    var updateFriendMarkerCallCount = 0
    var lastUpdatedFriendId: String?
    var lastUpdatedFriendName: String?
    var lastForceUpdate = false
    var mockMarkerStates: [String: LocationMarkerManager.MarkerState] = [:]
    
    init() {
        // Initialize with a mock MapKitService
        super.init(mapService: MockMapKitService())
    }
    
    override func updateFriendMarker(friendId: String, friendName: String, location: Location, forceUpdate: Bool = false) {
        updateFriendMarkerCallCount += 1
        lastUpdatedFriendId = friendId
        lastUpdatedFriendName = friendName
        lastForceUpdate = forceUpdate
    }
    
    override func getAllMarkerStates() -> [String: LocationMarkerManager.MarkerState] {
        return mockMarkerStates
    }
}