import XCTest
import CoreLocation
@testable import LocationSharingApp

class LocationTests: XCTestCase {
    
    func testHasAcceptableAccuracy_WithinThreshold_ReturnsTrue() {
        // Given
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 50.0,
            timestamp: Date()
        )
        
        // When & Then
        XCTAssertTrue(location.hasAcceptableAccuracy)
    }
    
    func testHasAcceptableAccuracy_AboveThreshold_ReturnsFalse() {
        // Given
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 150.0,
            timestamp: Date()
        )
        
        // When & Then
        XCTAssertFalse(location.hasAcceptableAccuracy)
    }
    
    func testHasAcceptableAccuracy_NegativeAccuracy_ReturnsFalse() {
        // Given
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: -1.0,
            timestamp: Date()
        )
        
        // When & Then
        XCTAssertFalse(location.hasAcceptableAccuracy)
    }
    
    func testIsHighAccuracy_HighAccuracyLocation_ReturnsTrue() {
        // Given
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Date()
        )
        
        // When & Then
        XCTAssertTrue(location.isHighAccuracy)
    }
    
    func testIsHighAccuracy_LowAccuracyLocation_ReturnsFalse() {
        // Given
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 50.0,
            timestamp: Date()
        )
        
        // When & Then
        XCTAssertFalse(location.isHighAccuracy)
    }
    
    func testAge_ReturnsCorrectAge() {
        // Given
        let pastDate = Date().addingTimeInterval(-10.0) // 10 seconds ago
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 10.0,
            timestamp: pastDate
        )
        
        // When
        let age = location.age
        
        // Then
        XCTAssertGreaterThanOrEqual(age, 10.0)
        XCTAssertLessThan(age, 11.0) // Allow some tolerance
    }
    
    func testIsFresh_RecentLocation_ReturnsTrue() {
        // Given
        let recentDate = Date().addingTimeInterval(-5.0) // 5 seconds ago
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 10.0,
            timestamp: recentDate
        )
        
        // When & Then
        XCTAssertTrue(location.isFresh)
    }
    
    func testIsFresh_OldLocation_ReturnsFalse() {
        // Given
        let oldDate = Date().addingTimeInterval(-60.0) // 1 minute ago
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 10.0,
            timestamp: oldDate
        )
        
        // When & Then
        XCTAssertFalse(location.isFresh)
    }
    
    func testInitFromCLLocation() {
        // Given
        let coordinate = CLLocationCoordinate2D(latitude: 37.7749, longitude: -122.4194)
        let clLocation = CLLocation(
            coordinate: coordinate,
            altitude: 100.0,
            horizontalAccuracy: 5.0,
            verticalAccuracy: 10.0,
            course: 45.0,
            speed: 2.5,
            timestamp: Date()
        )
        
        // When
        let location = Location(from: clLocation)
        
        // Then
        XCTAssertEqual(location.latitude, 37.7749, accuracy: 0.0001)
        XCTAssertEqual(location.longitude, -122.4194, accuracy: 0.0001)
        XCTAssertEqual(location.accuracy, 5.0, accuracy: 0.001)
        XCTAssertEqual(location.altitude, 100.0)
        XCTAssertEqual(location.speed, 2.5)
        XCTAssertEqual(location.bearing, 45.0)
        XCTAssertEqual(location.timestamp, clLocation.timestamp)
    }
    
    func testInitFromCLLocation_WithNegativeValues() {
        // Given
        let coordinate = CLLocationCoordinate2D(latitude: 37.7749, longitude: -122.4194)
        let clLocation = CLLocation(
            coordinate: coordinate,
            altitude: 100.0,
            horizontalAccuracy: 5.0,
            verticalAccuracy: 10.0,
            course: -1.0, // Invalid course
            speed: -1.0, // Invalid speed
            timestamp: Date()
        )
        
        // When
        let location = Location(from: clLocation)
        
        // Then
        XCTAssertNil(location.speed)
        XCTAssertNil(location.bearing)
    }
    
    func testToCLLocation() {
        // Given
        let timestamp = Date()
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: timestamp,
            altitude: 100.0,
            speed: 2.5,
            bearing: 45.0
        )
        
        // When
        let clLocation = location.toCLLocation()
        
        // Then
        XCTAssertEqual(clLocation.coordinate.latitude, 37.7749, accuracy: 0.0001)
        XCTAssertEqual(clLocation.coordinate.longitude, -122.4194, accuracy: 0.0001)
        XCTAssertEqual(clLocation.horizontalAccuracy, 5.0, accuracy: 0.001)
        XCTAssertEqual(clLocation.altitude, 100.0, accuracy: 0.001)
        XCTAssertEqual(clLocation.speed, 2.5, accuracy: 0.001)
        XCTAssertEqual(clLocation.course, 45.0, accuracy: 0.001)
        XCTAssertEqual(clLocation.timestamp, timestamp)
    }
    
    func testToCLLocation_WithNilValues() {
        // Given
        let timestamp = Date()
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: timestamp
        )
        
        // When
        let clLocation = location.toCLLocation()
        
        // Then
        XCTAssertEqual(clLocation.coordinate.latitude, 37.7749, accuracy: 0.0001)
        XCTAssertEqual(clLocation.coordinate.longitude, -122.4194, accuracy: 0.0001)
        XCTAssertEqual(clLocation.horizontalAccuracy, 5.0, accuracy: 0.001)
        XCTAssertEqual(clLocation.altitude, 0.0, accuracy: 0.001)
        XCTAssertEqual(clLocation.speed, -1.0, accuracy: 0.001)
        XCTAssertEqual(clLocation.course, -1.0, accuracy: 0.001)
    }
    
    func testEquality() {
        // Given
        let timestamp = Date()
        let location1 = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: timestamp
        )
        let location2 = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: timestamp
        )
        let location3 = Location(
            latitude: 37.7750, // Different latitude
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: timestamp
        )
        
        // When & Then
        XCTAssertEqual(location1, location2)
        XCTAssertNotEqual(location1, location3)
    }
}