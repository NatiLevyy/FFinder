import XCTest
@testable import LocationSharingApp

class LocationValidatorTests: XCTestCase {
    
    var locationValidator: LocationValidator!
    
    override func setUp() {
        super.setUp()
        locationValidator = LocationValidator()
    }
    
    override func tearDown() {
        locationValidator = nil
        super.tearDown()
    }
    
    func testValidateLocation_WithValidLocation_ReturnsValid() {
        // Given
        let validLocation = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        
        // When
        let result = locationValidator.validateLocation(validLocation)
        
        // Then
        XCTAssertTrue(result.isValid)
        XCTAssertTrue(result.errors.isEmpty)
    }
    
    func testValidateLocation_WithInvalidLatitude_ReturnsInvalid() {
        // Given
        let invalidLocation = Location(
            latitude: 95.0, // Invalid: > 90
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        
        // When
        let result = locationValidator.validateLocation(invalidLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid latitude") })
    }
    
    func testValidateLocation_WithInvalidLongitude_ReturnsInvalid() {
        // Given
        let invalidLocation = Location(
            latitude: 37.7749,
            longitude: -185.0, // Invalid: < -180
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        
        // When
        let result = locationValidator.validateLocation(invalidLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid longitude") })
    }
    
    func testValidateLocation_WithInvalidAccuracy_ReturnsInvalid() {
        // Given
        let invalidLocation = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 1500.0, // Invalid: > 1000
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        
        // When
        let result = locationValidator.validateLocation(invalidLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid accuracy") })
    }
    
    func testValidateLocation_WithFutureTimestamp_ReturnsInvalid() {
        // Given
        let futureTimestamp = Int64(Date().timeIntervalSince1970 * 1000) + (60 * 60 * 1000) // 1 hour in future
        let invalidLocation = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: futureTimestamp,
            altitude: 100.0
        )
        
        // When
        let result = locationValidator.validateLocation(invalidLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid timestamp") })
    }
    
    func testValidateLocation_WithOldTimestamp_ReturnsInvalid() {
        // Given
        let oldTimestamp = Int64(Date().timeIntervalSince1970 * 1000) - (2 * 60 * 60 * 1000) // 2 hours ago
        let invalidLocation = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: oldTimestamp,
            altitude: 100.0
        )
        
        // When
        let result = locationValidator.validateLocation(invalidLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid timestamp") })
    }
    
    func testValidateLocation_WithInvalidAltitude_ReturnsInvalid() {
        // Given
        let invalidLocation = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 15000.0 // Invalid: > 10000
        )
        
        // When
        let result = locationValidator.validateLocation(invalidLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid altitude") })
    }
    
    func testValidateLocation_WithNullIslandCoordinates_ReturnsInvalid() {
        // Given
        let nullIslandLocation = Location(
            latitude: 0.0,
            longitude: 0.0,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 0.0
        )
        
        // When
        let result = locationValidator.validateLocation(nullIslandLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("null island") })
    }
    
    func testValidateLocation_WithoutAltitude_ReturnsValid() {
        // Given
        let locationWithoutAltitude = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: nil
        )
        
        // When
        let result = locationValidator.validateLocation(locationWithoutAltitude)
        
        // Then
        XCTAssertTrue(result.isValid)
        XCTAssertTrue(result.errors.isEmpty)
    }
    
    func testValidateLocation_WithMultipleInvalidFields_ReturnsMultipleErrors() {
        // Given
        let invalidLocation = Location(
            latitude: 95.0, // Invalid
            longitude: -185.0, // Invalid
            accuracy: 1500.0, // Invalid
            timestamp: Int64(Date().timeIntervalSince1970 * 1000) + (60 * 60 * 1000), // Invalid
            altitude: 15000.0 // Invalid
        )
        
        // When
        let result = locationValidator.validateLocation(invalidLocation)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertEqual(result.errors.count, 5)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid latitude") })
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid longitude") })
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid accuracy") })
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid timestamp") })
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid altitude") })
    }
    
    func testValidateLocations_WithMixedValidAndInvalidLocations_ReturnsBatchResult() {
        // Given
        let validLocation = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        let invalidLocation = Location(
            latitude: 95.0, // Invalid
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        let locations = [validLocation, invalidLocation]
        
        // When
        let result = locationValidator.validateLocations(locations)
        
        // Then
        XCTAssertEqual(result.totalCount, 2)
        XCTAssertEqual(result.validCount, 1)
        XCTAssertEqual(result.invalidCount, 1)
        XCTAssertEqual(result.validationRate, 0.5)
        XCTAssertEqual(result.validLocations.count, 1)
        XCTAssertEqual(result.invalidLocations.count, 1)
        XCTAssertEqual(result.validLocations[0].latitude, validLocation.latitude)
        XCTAssertEqual(result.invalidLocations[0].location.latitude, invalidLocation.latitude)
    }
    
    func testValidateCoordinates_WithValidCoordinates_ReturnsValid() {
        // Given
        let latitude = 37.7749
        let longitude = -122.4194
        
        // When
        let result = locationValidator.validateCoordinates(latitude: latitude, longitude: longitude)
        
        // Then
        XCTAssertTrue(result.isValid)
        XCTAssertTrue(result.errors.isEmpty)
    }
    
    func testValidateCoordinates_WithNullIslandCoordinates_ReturnsInvalid() {
        // Given
        let latitude = 0.0
        let longitude = 0.0
        
        // When
        let result = locationValidator.validateCoordinates(latitude: latitude, longitude: longitude)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertTrue(result.errors.contains { $0.contains("null island") })
    }
    
    func testValidateCoordinates_WithOutOfRangeCoordinates_ReturnsInvalid() {
        // Given
        let latitude = 95.0 // Invalid
        let longitude = -185.0 // Invalid
        
        // When
        let result = locationValidator.validateCoordinates(latitude: latitude, longitude: longitude)
        
        // Then
        XCTAssertFalse(result.isValid)
        XCTAssertEqual(result.errors.count, 2)
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid latitude") })
        XCTAssertTrue(result.errors.contains { $0.contains("Invalid longitude") })
    }
}