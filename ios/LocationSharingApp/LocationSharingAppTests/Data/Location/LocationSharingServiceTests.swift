import XCTest
import Combine
import FirebaseAuth
import FirebaseDatabase
@testable import LocationSharingApp

class LocationSharingServiceTests: XCTestCase {
    
    var locationSharingService: LocationSharingService!
    var mockFirebaseAuth: MockFirebaseAuth!
    var mockFirebaseDatabase: MockFirebaseDatabase!
    var mockLocationValidator: MockLocationValidator!
    var cancellables: Set<AnyCancellable>!
    
    let testUserId = "test-user-id"
    let testFriendId = "test-friend-id"
    let testLocation = Location(
        latitude: 37.7749,
        longitude: -122.4194,
        accuracy: 5.0,
        timestamp: Int64(Date().timeIntervalSince1970 * 1000),
        altitude: 100.0
    )
    
    override func setUp() {
        super.setUp()
        mockFirebaseAuth = MockFirebaseAuth()
        mockFirebaseDatabase = MockFirebaseDatabase()
        mockLocationValidator = MockLocationValidator()
        cancellables = Set<AnyCancellable>()
        
        locationSharingService = LocationSharingService(
            firebaseAuth: mockFirebaseAuth,
            firebaseDatabase: mockFirebaseDatabase,
            locationValidator: mockLocationValidator
        )
    }
    
    override func tearDown() {
        cancellables.removeAll()
        locationSharingService = nil
        mockFirebaseAuth = nil
        mockFirebaseDatabase = nil
        mockLocationValidator = nil
        super.tearDown()
    }
    
    func testBroadcastLocationUpdate_WithValidLocationAndSharingEnabled_ReturnsSuccess() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockLocationValidator.validationResult = ValidationResult(isValid: true, errors: [])
        mockFirebaseDatabase.shouldSucceed = true
        mockFirebaseDatabase.mockLocationData = ["isSharing": true]
        
        // When
        let result = await locationSharingService.broadcastLocationUpdate(testLocation)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockLocationValidator.validateLocationCalled)
            XCTAssertTrue(mockFirebaseDatabase.setValueCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testBroadcastLocationUpdate_WithUnauthenticatedUser_ReturnsFailure() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let result = await locationSharingService.broadcastLocationUpdate(testLocation)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            if case LocationSharingError.userNotAuthenticated = error {
                // Expected error
            } else {
                XCTFail("Expected userNotAuthenticated error but got: \(error)")
            }
        }
    }
    
    func testBroadcastLocationUpdate_WithInvalidLocation_ReturnsFailure() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockLocationValidator.validationResult = ValidationResult(
            isValid: false,
            errors: ["Invalid latitude", "Invalid longitude"]
        )
        
        // When
        let result = await locationSharingService.broadcastLocationUpdate(testLocation)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            if case LocationSharingError.invalidLocationData(let errors) = error {
                XCTAssertEqual(errors, ["Invalid latitude", "Invalid longitude"])
            } else {
                XCTFail("Expected invalidLocationData error but got: \(error)")
            }
        }
    }
    
    func testBroadcastLocationUpdate_WithSharingDisabled_ReturnsFailure() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockLocationValidator.validationResult = ValidationResult(isValid: true, errors: [])
        mockFirebaseDatabase.shouldSucceed = true
        mockFirebaseDatabase.mockLocationData = ["isSharing": false]
        
        // When
        let result = await locationSharingService.broadcastLocationUpdate(testLocation)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            if case LocationSharingError.locationSharingDisabled = error {
                // Expected error
            } else {
                XCTFail("Expected locationSharingDisabled error but got: \(error)")
            }
        }
    }
    
    func testSubscribeToFriendLocationUpdates_WithValidData_EmitsLocationUpdates() {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockLocationValidator.validationResult = ValidationResult(isValid: true, errors: [])
        mockFirebaseDatabase.shouldSucceed = true
        mockFirebaseDatabase.mockLocationData = [
            "latitude": testLocation.latitude,
            "longitude": testLocation.longitude,
            "accuracy": testLocation.accuracy,
            "timestamp": testLocation.timestamp,
            "altitude": testLocation.altitude as Any
        ]
        
        let expectation = XCTestExpectation(description: "Location update received")
        
        // When
        locationSharingService.subscribeToFriendLocationUpdates(friendId: testFriendId)
            .sink(
                receiveCompletion: { completion in
                    if case .failure(let error) = completion {
                        XCTFail("Unexpected error: \(error)")
                    }
                },
                receiveValue: { location in
                    XCTAssertEqual(location.latitude, self.testLocation.latitude)
                    XCTAssertEqual(location.longitude, self.testLocation.longitude)
                    expectation.fulfill()
                }
            )
            .store(in: &cancellables)
        
        // Simulate Firebase data change
        mockFirebaseDatabase.simulateDataChange()
        
        // Then
        wait(for: [expectation], timeout: 1.0)
        XCTAssertTrue(mockLocationValidator.validateLocationCalled)
    }
    
    func testSubscribeToFriendLocationUpdates_WithInvalidData_DoesNotEmit() {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockLocationValidator.validationResult = ValidationResult(
            isValid: false,
            errors: ["Invalid location"]
        )
        mockFirebaseDatabase.shouldSucceed = true
        mockFirebaseDatabase.mockLocationData = [
            "latitude": testLocation.latitude,
            "longitude": testLocation.longitude,
            "accuracy": testLocation.accuracy,
            "timestamp": testLocation.timestamp,
            "altitude": testLocation.altitude as Any
        ]
        
        let expectation = XCTestExpectation(description: "No location update should be received")
        expectation.isInverted = true
        
        // When
        locationSharingService.subscribeToFriendLocationUpdates(friendId: testFriendId)
            .sink(
                receiveCompletion: { _ in },
                receiveValue: { _ in
                    expectation.fulfill()
                }
            )
            .store(in: &cancellables)
        
        // Simulate Firebase data change
        mockFirebaseDatabase.simulateDataChange()
        
        // Then
        wait(for: [expectation], timeout: 0.5)
        XCTAssertTrue(mockLocationValidator.validateLocationCalled)
    }
    
    func testSubscribeToFriendLocationUpdates_WithUnauthenticatedUser_FailsImmediately() {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        let expectation = XCTestExpectation(description: "Publisher fails immediately")
        
        // When
        locationSharingService.subscribeToFriendLocationUpdates(friendId: testFriendId)
            .sink(
                receiveCompletion: { completion in
                    if case .failure(let error) = completion {
                        if case LocationSharingError.userNotAuthenticated = error {
                            expectation.fulfill()
                        } else {
                            XCTFail("Expected userNotAuthenticated error but got: \(error)")
                        }
                    }
                },
                receiveValue: { _ in
                    XCTFail("Should not receive any values")
                }
            )
            .store(in: &cancellables)
        
        // Then
        wait(for: [expectation], timeout: 1.0)
    }
    
    func testEnableLocationSharing_WithAuthenticatedUser_ReturnsSuccess() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = true
        
        // When
        let result = await locationSharingService.enableLocationSharing()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFirebaseDatabase.setValueCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testEnableLocationSharing_WithUnauthenticatedUser_ReturnsFailure() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let result = await locationSharingService.enableLocationSharing()
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            if case LocationSharingError.userNotAuthenticated = error {
                // Expected error
            } else {
                XCTFail("Expected userNotAuthenticated error but got: \(error)")
            }
        }
    }
    
    func testDisableLocationSharing_WithAuthenticatedUser_ReturnsSuccess() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = true
        
        // When
        let result = await locationSharingService.disableLocationSharing()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFirebaseDatabase.setValueCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testSubscribeToMultipleFriendsLocationUpdates_WithValidData_EmitsUpdatesForAllFriends() {
        // Given
        let friendIds = [testFriendId, "friend2"]
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockLocationValidator.validationResult = ValidationResult(isValid: true, errors: [])
        mockFirebaseDatabase.shouldSucceed = true
        mockFirebaseDatabase.mockLocationData = [
            "latitude": testLocation.latitude,
            "longitude": testLocation.longitude,
            "accuracy": testLocation.accuracy,
            "timestamp": testLocation.timestamp,
            "altitude": testLocation.altitude as Any
        ]
        
        let expectation = XCTestExpectation(description: "Multiple friend location updates received")
        
        // When
        locationSharingService.subscribeToMultipleFriendsLocationUpdates(friendIds: friendIds)
            .sink(
                receiveCompletion: { completion in
                    if case .failure(let error) = completion {
                        XCTFail("Unexpected error: \(error)")
                    }
                },
                receiveValue: { locations in
                    if locations.count >= 1 {
                        expectation.fulfill()
                    }
                }
            )
            .store(in: &cancellables)
        
        // Simulate Firebase data change
        mockFirebaseDatabase.simulateDataChange()
        
        // Then
        wait(for: [expectation], timeout: 1.0)
    }
}

// MARK: - Mock LocationValidator

class MockLocationValidator: LocationValidator {
    
    var validateLocationCalled = false
    var validationResult = ValidationResult(isValid: true, errors: [])
    
    override func validateLocation(_ location: Location) -> ValidationResult {
        validateLocationCalled = true
        return validationResult
    }
}