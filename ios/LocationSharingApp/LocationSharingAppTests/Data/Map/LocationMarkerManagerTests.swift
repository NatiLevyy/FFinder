import XCTest
@testable import LocationSharingApp

class LocationMarkerManagerTests: XCTestCase {
    
    var locationMarkerManager: LocationMarkerManager!
    var mockMapService: MockMapKitService!
    
    override func setUp() {
        super.setUp()
        mockMapService = MockMapKitService()
        locationMarkerManager = LocationMarkerManager(mapService: mockMapService)
    }
    
    override func tearDown() {
        locationMarkerManager = nil
        mockMapService = nil
        super.tearDown()
    }
    
    func testUpdateFriendMarker_ShouldAddMarkerAndUpdateInternalState() {
        // Given
        let friendId = "friend123"
        let friendName = "John Doe"
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        // When
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location)
        
        // Then
        XCTAssertEqual(mockMapService.addLocationMarkerCallCount, 1)
        XCTAssertEqual(mockMapService.lastAddedFriendId, friendId)
        XCTAssertEqual(mockMapService.lastAddedFriendName, friendName)
        
        let markerStates = locationMarkerManager.getAllMarkerStates()
        XCTAssertTrue(markerStates.keys.contains(friendId))
        XCTAssertEqual(markerStates[friendId]?.friendName, friendName)
        XCTAssertTrue(markerStates[friendId]?.isVisible == true)
    }
    
    func testUpdateFriendMarker_ShouldSkipUpdateWhenLocationHasntChangedSignificantly() {
        // Given
        let friendId = "friend123"
        let friendName = "John Doe"
        let location1 = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        let location2 = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000)) // Same location
        
        // Add initial marker
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location1)
        
        // When - update with same location shortly after
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location2)
        
        // Then - should only call map service once (for initial marker)
        XCTAssertEqual(mockMapService.addLocationMarkerCallCount, 1)
    }
    
    func testUpdateFriendMarker_ShouldForceUpdateWhenForceUpdateIsTrue() {
        // Given
        let friendId = "friend123"
        let friendName = "John Doe"
        let location1 = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        let location2 = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000)) // Same location
        
        // Add initial marker
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location1)
        
        // When - force update with same location
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location2, forceUpdate: true)
        
        // Then - should call map service twice
        XCTAssertEqual(mockMapService.addLocationMarkerCallCount, 2)
    }
    
    func testUpdateFriendMarkers_ShouldUpdateMultipleMarkersInBatch() {
        // Given
        let friendLocations: [String: (String, Location)] = [
            "friend1": ("John Doe", Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))),
            "friend2": ("Jane Smith", Location(latitude: 40.7128, longitude: -74.0060, accuracy: 3.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000)))
        ]
        
        // When
        locationMarkerManager.updateFriendMarkers(friendLocations)
        
        // Then
        XCTAssertEqual(mockMapService.addLocationMarkerCallCount, 2)
        
        let markerStates = locationMarkerManager.getAllMarkerStates()
        XCTAssertEqual(markerStates.count, 2)
        XCTAssertTrue(markerStates.keys.contains("friend1"))
        XCTAssertTrue(markerStates.keys.contains("friend2"))
    }
    
    func testRemoveFriendMarker_ShouldRemoveMarkerAndInternalState() {
        // Given
        let friendId = "friend123"
        let friendName = "John Doe"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // Add marker first
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location)
        
        // When
        locationMarkerManager.removeFriendMarker(friendId: friendId)
        
        // Then
        XCTAssertEqual(mockMapService.removeLocationMarkerCallCount, 1)
        XCTAssertEqual(mockMapService.lastRemovedFriendId, friendId)
        
        let markerStates = locationMarkerManager.getAllMarkerStates()
        XCTAssertFalse(markerStates.keys.contains(friendId))
    }
    
    func testSetMarkerVisibility_ShouldHideMarkerWhenVisibleIsFalse() {
        // Given
        let friendId = "friend123"
        let friendName = "John Doe"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // Add visible marker first
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location)
        
        // When
        locationMarkerManager.setMarkerVisibility(friendId: friendId, visible: false)
        
        // Then
        XCTAssertEqual(mockMapService.removeLocationMarkerCallCount, 1)
        
        let markerStates = locationMarkerManager.getAllMarkerStates()
        XCTAssertEqual(markerStates[friendId]?.isVisible, false)
    }
    
    func testSetMarkerVisibility_ShouldShowMarkerWhenVisibleIsTrue() {
        // Given
        let friendId = "friend123"
        let friendName = "John Doe"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // Add marker and hide it
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location)
        locationMarkerManager.setMarkerVisibility(friendId: friendId, visible: false)
        
        // When
        locationMarkerManager.setMarkerVisibility(friendId: friendId, visible: true)
        
        // Then
        XCTAssertGreaterThanOrEqual(mockMapService.addLocationMarkerCallCount, 2)
        
        let markerStates = locationMarkerManager.getAllMarkerStates()
        XCTAssertEqual(markerStates[friendId]?.isVisible, true)
    }
    
    func testGetVisibleMarkerStates_ShouldReturnOnlyVisibleMarkers() {
        // Given
        let friend1Id = "friend1"
        let friend2Id = "friend2"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // Add two markers, hide one
        locationMarkerManager.updateFriendMarker(friendId: friend1Id, friendName: "John", location: location)
        locationMarkerManager.updateFriendMarker(friendId: friend2Id, friendName: "Jane", location: location)
        locationMarkerManager.setMarkerVisibility(friendId: friend2Id, visible: false)
        
        // When
        let visibleMarkers = locationMarkerManager.getVisibleMarkerStates()
        
        // Then
        XCTAssertEqual(visibleMarkers.count, 1)
        XCTAssertTrue(visibleMarkers.keys.contains(friend1Id))
        XCTAssertFalse(visibleMarkers.keys.contains(friend2Id))
    }
    
    func testClearAllMarkers_ShouldRemoveAllMarkersAndClearState() {
        // Given
        let friendLocations: [String: (String, Location)] = [
            "friend1": ("John Doe", Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))),
            "friend2": ("Jane Smith", Location(latitude: 40.7128, longitude: -74.0060, accuracy: 3.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000)))
        ]
        locationMarkerManager.updateFriendMarkers(friendLocations)
        
        // When
        locationMarkerManager.clearAllMarkers()
        
        // Then
        XCTAssertEqual(mockMapService.clearAllMarkersCallCount, 1)
        
        let markerStates = locationMarkerManager.getAllMarkerStates()
        XCTAssertTrue(markerStates.isEmpty)
    }
    
    func testRemoveStaleMarkers_ShouldRemoveMarkersOlderThanThreshold() {
        // Given
        let friendId = "friend123"
        let friendName = "John Doe"
        let oldTimestamp = Int64(Date().timeIntervalSince1970 * 1000) - 600000 // 10 minutes ago
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: oldTimestamp)
        
        locationMarkerManager.updateFriendMarker(friendId: friendId, friendName: friendName, location: location)
        
        // When - remove markers older than 5 minutes
        locationMarkerManager.removeStaleMarkers(maxAgeMs: 300000) // 5 minutes
        
        // Then
        XCTAssertEqual(mockMapService.removeLocationMarkerCallCount, 1)
        
        let markerStates = locationMarkerManager.getAllMarkerStates()
        XCTAssertFalse(markerStates.keys.contains(friendId))
    }
}

// MARK: - Mock MapKitService
class MockMapKitService: MapKitService {
    
    var addLocationMarkerCallCount = 0
    var removeLocationMarkerCallCount = 0
    var clearAllMarkersCallCount = 0
    var updateUserLocationCallCount = 0
    var updateFriendLocationsCallCount = 0
    
    var lastAddedFriendId: String?
    var lastAddedFriendName: String?
    var lastRemovedFriendId: String?
    
    override func addLocationMarker(friendId: String, location: Location, friendName: String) {
        addLocationMarkerCallCount += 1
        lastAddedFriendId = friendId
        lastAddedFriendName = friendName
    }
    
    override func removeLocationMarker(friendId: String) {
        removeLocationMarkerCallCount += 1
        lastRemovedFriendId = friendId
    }
    
    override func clearAllMarkers() {
        clearAllMarkersCallCount += 1
    }
    
    override func updateUserLocation(_ location: Location) {
        updateUserLocationCallCount += 1
    }
    
    override func updateFriendLocations(_ friendLocations: [String: Location]) {
        updateFriendLocationsCallCount += 1
    }
}