import XCTest
@testable import LocationSharingApp

class PermissionStatusTests: XCTestCase {
    
    func testIsGranted_OnlyGrantedReturnsTrue() {
        // Given & When & Then
        XCTAssertTrue(PermissionStatus.granted.isGranted)
        XCTAssertFalse(PermissionStatus.denied.isGranted)
        XCTAssertFalse(PermissionStatus.permanentlyDenied.isGranted)
        XCTAssertFalse(PermissionStatus.pending.isGranted)
        XCTAssertFalse(PermissionStatus.unknown.isGranted)
    }
    
    func testIsDenied_DeniedAndPermanentlyDeniedReturnTrue() {
        // Given & When & Then
        XCTAssertFalse(PermissionStatus.granted.isDenied)
        XCTAssertTrue(PermissionStatus.denied.isDenied)
        XCTAssertTrue(PermissionStatus.permanentlyDenied.isDenied)
        XCTAssertFalse(PermissionStatus.pending.isDenied)
        XCTAssertFalse(PermissionStatus.unknown.isDenied)
    }
    
    func testIsPermanentlyDenied_OnlyPermanentlyDeniedReturnsTrue() {
        // Given & When & Then
        XCTAssertFalse(PermissionStatus.granted.isPermanentlyDenied)
        XCTAssertFalse(PermissionStatus.denied.isPermanentlyDenied)
        XCTAssertTrue(PermissionStatus.permanentlyDenied.isPermanentlyDenied)
        XCTAssertFalse(PermissionStatus.pending.isPermanentlyDenied)
        XCTAssertFalse(PermissionStatus.unknown.isPermanentlyDenied)
    }
    
    func testCanRequest_DeniedAndUnknownReturnTrue() {
        // Given & When & Then
        XCTAssertFalse(PermissionStatus.granted.canRequest)
        XCTAssertTrue(PermissionStatus.denied.canRequest)
        XCTAssertFalse(PermissionStatus.permanentlyDenied.canRequest)
        XCTAssertFalse(PermissionStatus.pending.canRequest)
        XCTAssertTrue(PermissionStatus.unknown.canRequest)
    }
    
    func testDescription_ReturnsCorrectStrings() {
        // Given & When & Then
        XCTAssertEqual(PermissionStatus.granted.description, "Granted")
        XCTAssertEqual(PermissionStatus.denied.description, "Denied")
        XCTAssertEqual(PermissionStatus.permanentlyDenied.description, "Permanently Denied")
        XCTAssertEqual(PermissionStatus.pending.description, "Pending")
        XCTAssertEqual(PermissionStatus.unknown.description, "Unknown")
    }
    
    func testAllCases_ContainsAllStatuses() {
        // Given
        let allCases = PermissionStatus.allCases
        
        // When & Then
        XCTAssertEqual(allCases.count, 5)
        XCTAssertTrue(allCases.contains(.granted))
        XCTAssertTrue(allCases.contains(.denied))
        XCTAssertTrue(allCases.contains(.permanentlyDenied))
        XCTAssertTrue(allCases.contains(.pending))
        XCTAssertTrue(allCases.contains(.unknown))
    }
    
    func testEquality() {
        // Given & When & Then
        XCTAssertEqual(PermissionStatus.granted, PermissionStatus.granted)
        XCTAssertNotEqual(PermissionStatus.granted, PermissionStatus.denied)
        XCTAssertNotEqual(PermissionStatus.denied, PermissionStatus.permanentlyDenied)
    }
}