import XCTest
import MapKit
@testable import LocationSharingApp

class MapKitServiceTests: XCTestCase {
    
    var mapKitService: MapKitService!
    var mockMapView: MockMKMapView!
    
    override func setUp() {
        super.setUp()
        mapKitService = MapKitService()
        mockMapView = MockMKMapView()
    }
    
    override func tearDown() {
        mapKitService = nil
        mockMapView = nil
        super.tearDown()
    }
    
    func testInitializeMap_ShouldCompleteWithoutErrors() {
        // When
        mapKitService.initializeMap()
        
        // Then - no exception should be thrown
        // This is a placeholder method, so we just verify it doesn't crash
    }
    
    func testSetMapView_ShouldConfigureMapSettings() {
        // When
        mapKitService.setMapView(mockMapView)
        
        // Then
        XCTAssertEqual(mockMapView.mapType, .standard)
        XCTAssertTrue(mockMapView.isZoomEnabled)
        XCTAssertTrue(mockMapView.isScrollEnabled)
        XCTAssertTrue(mockMapView.isRotateEnabled)
        XCTAssertTrue(mockMapView.isPitchEnabled)
        XCTAssertFalse(mockMapView.showsUserLocation)
        XCTAssertEqual(mockMapView.userTrackingMode, .none)
    }
    
    func testUpdateUserLocation_ShouldAddUserLocationAnnotationAndSetRegion() {
        // Given
        mapKitService.setMapView(mockMapView)
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        // When
        mapKitService.updateUserLocation(location)
        
        // Then
        XCTAssertEqual(mockMapView.addAnnotationCallCount, 1)
        XCTAssertEqual(mockMapView.setRegionCallCount, 1)
        
        let addedAnnotation = mockMapView.addedAnnotations.first as? MKPointAnnotation
        XCTAssertNotNil(addedAnnotation)
        XCTAssertEqual(addedAnnotation?.coordinate.latitude, location.latitude, accuracy: 0.0001)
        XCTAssertEqual(addedAnnotation?.coordinate.longitude, location.longitude, accuracy: 0.0001)
        XCTAssertEqual(addedAnnotation?.title, "Your Location")
    }
    
    func testUpdateUserLocation_WithoutInitializedMapView_ShouldNotCrash() {
        // Given - no map view set
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        // When
        mapKitService.updateUserLocation(location)
        
        // Then - should not crash and no map interactions should occur
        XCTAssertEqual(mockMapView.addAnnotationCallCount, 0)
    }
    
    func testAddLocationMarker_ShouldAddFriendAnnotationWithCorrectDetails() {
        // Given
        mapKitService.setMapView(mockMapView)
        let friendId = "friend123"
        let friendName = "John Doe"
        let location = Location(
            latitude: 40.7128,
            longitude: -74.0060,
            accuracy: 3.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        // When
        mapKitService.addLocationMarker(friendId: friendId, location: location, friendName: friendName)
        
        // Then
        XCTAssertEqual(mockMapView.addAnnotationCallCount, 1)
        
        let addedAnnotation = mockMapView.addedAnnotations.first as? MKPointAnnotation
        XCTAssertNotNil(addedAnnotation)
        XCTAssertEqual(addedAnnotation?.coordinate.latitude, location.latitude, accuracy: 0.0001)
        XCTAssertEqual(addedAnnotation?.coordinate.longitude, location.longitude, accuracy: 0.0001)
        XCTAssertEqual(addedAnnotation?.title, friendName)
        XCTAssertTrue(addedAnnotation?.subtitle?.contains("Last updated:") == true)
    }
    
    func testAddLocationMarker_ShouldRemoveExistingMarkerForSameFriend() {
        // Given
        mapKitService.setMapView(mockMapView)
        let friendId = "friend123"
        let friendName = "John Doe"
        let location1 = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        let location2 = Location(latitude: 40.7128, longitude: -74.0060, accuracy: 3.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // When - add marker twice for same friend
        mapKitService.addLocationMarker(friendId: friendId, location: location1, friendName: friendName)
        mapKitService.addLocationMarker(friendId: friendId, location: location2, friendName: friendName)
        
        // Then - should remove previous annotation and add new one
        XCTAssertEqual(mockMapView.removeAnnotationCallCount, 1)
        XCTAssertEqual(mockMapView.addAnnotationCallCount, 2)
    }
    
    func testRemoveLocationMarker_ShouldRemoveAnnotationForSpecifiedFriend() {
        // Given
        mapKitService.setMapView(mockMapView)
        let friendId = "friend123"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // Add marker first
        mapKitService.addLocationMarker(friendId: friendId, location: location, friendName: "John Doe")
        
        // When
        mapKitService.removeLocationMarker(friendId: friendId)
        
        // Then
        XCTAssertEqual(mockMapView.removeAnnotationCallCount, 1)
    }
    
    func testUpdateFriendLocations_ShouldAddAnnotationsForAllFriends() {
        // Given
        mapKitService.setMapView(mockMapView)
        let friendLocations: [String: Location] = [
            "friend1": Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000)),
            "friend2": Location(latitude: 40.7128, longitude: -74.0060, accuracy: 3.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        ]
        
        // When
        mapKitService.updateFriendLocations(friendLocations)
        
        // Then
        XCTAssertEqual(mockMapView.addAnnotationCallCount, 2)
    }
    
    func testClearAllMarkers_ShouldRemoveAllAnnotations() {
        // Given
        mapKitService.setMapView(mockMapView)
        let friendId = "friend123"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000))
        
        // Add user location and friend marker
        mapKitService.updateUserLocation(location)
        mapKitService.addLocationMarker(friendId: friendId, location: location, friendName: "John Doe")
        
        // When
        mapKitService.clearAllMarkers()
        
        // Then - should remove both user and friend annotations
        XCTAssertEqual(mockMapView.removeAnnotationsCallCount, 1)
        XCTAssertGreaterThanOrEqual(mockMapView.removeAnnotationCallCount, 1)
    }
    
    func testGetMapView_ShouldReturnCurrentMapViewInstance() {
        // Given
        mapKitService.setMapView(mockMapView)
        
        // When
        let result = mapKitService.getMapView()
        
        // Then
        XCTAssertTrue(result === mockMapView)
    }
    
    func testGetMapView_ShouldReturnNilWhenNoMapViewIsSet() {
        // When
        let result = mapKitService.getMapView()
        
        // Then
        XCTAssertNil(result)
    }
}

// MARK: - Mock MKMapView
class MockMKMapView: MKMapView {
    
    var addAnnotationCallCount = 0
    var removeAnnotationCallCount = 0
    var removeAnnotationsCallCount = 0
    var setRegionCallCount = 0
    var addedAnnotations: [MKAnnotation] = []
    var removedAnnotations: [MKAnnotation] = []
    
    override func addAnnotation(_ annotation: MKAnnotation) {
        addAnnotationCallCount += 1
        addedAnnotations.append(annotation)
    }
    
    override func removeAnnotation(_ annotation: MKAnnotation) {
        removeAnnotationCallCount += 1
        removedAnnotations.append(annotation)
    }
    
    override func removeAnnotations(_ annotations: [MKAnnotation]) {
        removeAnnotationsCallCount += 1
        removedAnnotations.append(contentsOf: annotations)
    }
    
    override func setRegion(_ region: MKCoordinateRegion, animated: Bool) {
        setRegionCallCount += 1
        super.setRegion(region, animated: animated)
    }
}