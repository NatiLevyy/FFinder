import XCTest
@testable import LocationSharingApp

class LocationErrorTests: XCTestCase {
    
    func testLocalizedDescription_ReturnsCorrectMessages() {
        // Given & When & Then
        XCTAssertEqual(LocationError.permissionDenied.localizedDescription, "Location permission denied")
        XCTAssertEqual(LocationError.locationDisabled.localizedDescription, "Location services are disabled")
        XCTAssertEqual(LocationError.gpsNotAvailable.localizedDescription, "GPS provider is not available")
        XCTAssertEqual(LocationError.networkNotAvailable.localizedDescription, "Network location provider is not available")
        XCTAssertEqual(LocationError.timeout.localizedDescription, "Location request timed out")
        XCTAssertEqual(LocationError.backgroundPermissionDenied.localizedDescription, "Background location permission denied")
        XCTAssertEqual(LocationError.settingsResolutionRequired.localizedDescription, "Location settings need to be resolved")
    }
    
    func testInaccurateLocationDescription() {
        // Given
        let accuracy = 150.5
        let error = LocationError.inaccurateLocation(accuracy: accuracy)
        
        // When & Then
        XCTAssertEqual(error.localizedDescription, "Location accuracy too low: \(accuracy)m")
    }
    
    func testOutdatedLocationDescription() {
        // Given
        let age: TimeInterval = 60.0
        let error = LocationError.outdatedLocation(age: age)
        
        // When & Then
        XCTAssertEqual(error.localizedDescription, "Location data is too old: \(age)s")
    }
    
    func testUnknownErrorDescription() {
        // Given
        let underlyingError = NSError(domain: "TestDomain", code: 123, userInfo: [NSLocalizedDescriptionKey: "Test error"])
        let error = LocationError.unknown(underlyingError)
        
        // When & Then
        XCTAssertEqual(error.localizedDescription, "Unknown location error: Test error")
    }
    
    func testErrorCode_ReturnsUniqueValues() {
        // Given
        let errors: [LocationError] = [
            .permissionDenied,
            .locationDisabled,
            .gpsNotAvailable,
            .networkNotAvailable,
            .timeout,
            .inaccurateLocation(accuracy: 100.0),
            .outdatedLocation(age: 30.0),
            .backgroundPermissionDenied,
            .settingsResolutionRequired,
            .unknown(NSError(domain: "Test", code: 0))
        ]
        
        // When
        let errorCodes = errors.map { $0.errorCode }
        let uniqueErrorCodes = Set(errorCodes)
        
        // Then
        XCTAssertEqual(errorCodes.count, uniqueErrorCodes.count, "All error codes should be unique")
    }
    
    func testRecoverySuggestion_ReturnsHelpfulMessages() {
        // Given & When & Then
        XCTAssertEqual(LocationError.permissionDenied.recoverySuggestion, "Please grant location permission in Settings")
        XCTAssertEqual(LocationError.locationDisabled.recoverySuggestion, "Please enable Location Services in Settings")
        XCTAssertEqual(LocationError.gpsNotAvailable.recoverySuggestion, "Please ensure GPS is enabled and try again")
        XCTAssertEqual(LocationError.networkNotAvailable.recoverySuggestion, "Please check your internet connection")
        XCTAssertEqual(LocationError.timeout.recoverySuggestion, "Please try again or move to an area with better signal")
        XCTAssertEqual(LocationError.inaccurateLocation(accuracy: 100.0).recoverySuggestion, "Please move to an area with better GPS signal")
        XCTAssertEqual(LocationError.outdatedLocation(age: 30.0).recoverySuggestion, "Please wait for a fresh location update")
        XCTAssertEqual(LocationError.backgroundPermissionDenied.recoverySuggestion, "Please grant 'Always' location permission in Settings")
        XCTAssertEqual(LocationError.settingsResolutionRequired.recoverySuggestion, "Please enable high accuracy location mode")
        XCTAssertEqual(LocationError.unknown(NSError(domain: "Test", code: 0)).recoverySuggestion, "Please try again or contact support")
    }
    
    func testEquality_SameErrorTypes() {
        // Given & When & Then
        XCTAssertEqual(LocationError.permissionDenied, LocationError.permissionDenied)
        XCTAssertEqual(LocationError.locationDisabled, LocationError.locationDisabled)
        XCTAssertEqual(LocationError.timeout, LocationError.timeout)
    }
    
    func testEquality_InaccurateLocationWithSameAccuracy() {
        // Given
        let error1 = LocationError.inaccurateLocation(accuracy: 100.0)
        let error2 = LocationError.inaccurateLocation(accuracy: 100.0)
        let error3 = LocationError.inaccurateLocation(accuracy: 150.0)
        
        // When & Then
        XCTAssertEqual(error1, error2)
        XCTAssertNotEqual(error1, error3)
    }
    
    func testEquality_OutdatedLocationWithSameAge() {
        // Given
        let error1 = LocationError.outdatedLocation(age: 30.0)
        let error2 = LocationError.outdatedLocation(age: 30.0)
        let error3 = LocationError.outdatedLocation(age: 60.0)
        
        // When & Then
        XCTAssertEqual(error1, error2)
        XCTAssertNotEqual(error1, error3)
    }
    
    func testEquality_UnknownErrorWithSameDescription() {
        // Given
        let underlyingError1 = NSError(domain: "Test", code: 123, userInfo: [NSLocalizedDescriptionKey: "Same error"])
        let underlyingError2 = NSError(domain: "Test", code: 456, userInfo: [NSLocalizedDescriptionKey: "Same error"])
        let underlyingError3 = NSError(domain: "Test", code: 123, userInfo: [NSLocalizedDescriptionKey: "Different error"])
        
        let error1 = LocationError.unknown(underlyingError1)
        let error2 = LocationError.unknown(underlyingError2)
        let error3 = LocationError.unknown(underlyingError3)
        
        // When & Then
        XCTAssertEqual(error1, error2) // Same description
        XCTAssertNotEqual(error1, error3) // Different description
    }
    
    func testEquality_DifferentErrorTypes() {
        // Given & When & Then
        XCTAssertNotEqual(LocationError.permissionDenied, LocationError.locationDisabled)
        XCTAssertNotEqual(LocationError.timeout, LocationError.gpsNotAvailable)
        XCTAssertNotEqual(LocationError.inaccurateLocation(accuracy: 100.0), LocationError.permissionDenied)
    }
}