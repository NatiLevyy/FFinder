import XCTest
import Combine
import FirebaseAuth
import FirebaseDatabase
@testable import LocationSharingApp

class LocationRepositoryImplTests: XCTestCase {
    
    var locationRepository: LocationRepositoryImpl!
    var mockFirebaseAuth: MockFirebaseAuth!
    var mockFirebaseDatabase: MockFirebaseDatabase!
    var mockLocationCache: MockLocationCache!
    var cancellables: Set<AnyCancellable>!
    
    let testUserId = "test-user-id"
    let testFriendId = "test-friend-id"
    let testLocation = Location(
        latitude: 37.7749,
        longitude: -122.4194,
        accuracy: 5.0,
        timestamp: 1640995200000,
        altitude: 100.0
    )
    
    override func setUp() {
        super.setUp()
        mockFirebaseAuth = MockFirebaseAuth()
        mockFirebaseDatabase = MockFirebaseDatabase()
        mockLocationCache = MockLocationCache()
        cancellables = Set<AnyCancellable>()
        
        locationRepository = LocationRepositoryImpl(
            firebaseAuth: mockFirebaseAuth,
            firebaseDatabase: mockFirebaseDatabase,
            locationCache: mockLocationCache
        )
    }
    
    override func tearDown() {
        cancellables.removeAll()
        locationRepository = nil
        mockFirebaseAuth = nil
        mockFirebaseDatabase = nil
        mockLocationCache = nil
        super.tearDown()
    }
    
    func testUpdateLocation_WithAuthenticatedUser_ReturnsSuccess() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = true
        
        // When
        let result = await locationRepository.updateLocation(testLocation)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFirebaseDatabase.setValueCalled)
            XCTAssertTrue(mockLocationCache.cacheUserLocationCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testUpdateLocation_WithUnauthenticatedUser_ReturnsFailure() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let result = await locationRepository.updateLocation(testLocation)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            XCTAssertTrue(error is LocationRepositoryError)
            if case LocationRepositoryError.userNotAuthenticated = error {
                // Expected error
            } else {
                XCTFail("Expected userNotAuthenticated error")
            }
        }
    }
    
    func testUpdateLocation_WithFirebaseError_ReturnsFailure() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = false
        mockFirebaseDatabase.errorToThrow = NSError(domain: "Firebase", code: 1, userInfo: nil)
        
        // When
        let result = await locationRepository.updateLocation(testLocation)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure:
            XCTAssertTrue(mockFirebaseDatabase.setValueCalled)
        }
    }
    
    func testGetCurrentLocation_WithFirebaseData_ReturnsLocation() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = true
        mockFirebaseDatabase.mockLocationData = [
            "latitude": testLocation.latitude,
            "longitude": testLocation.longitude,
            "accuracy": testLocation.accuracy,
            "timestamp": testLocation.timestamp,
            "altitude": testLocation.altitude as Any
        ]
        
        // When
        let result = await locationRepository.getCurrentLocation()
        
        // Then
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.latitude, testLocation.latitude)
        XCTAssertEqual(result?.longitude, testLocation.longitude)
        XCTAssertEqual(result?.accuracy, testLocation.accuracy)
        XCTAssertEqual(result?.timestamp, testLocation.timestamp)
        XCTAssertEqual(result?.altitude, testLocation.altitude)
    }
    
    func testGetCurrentLocation_WithFirebaseError_ReturnsCachedLocation() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = false
        mockFirebaseDatabase.errorToThrow = NSError(domain: "Firebase", code: 1, userInfo: nil)
        mockLocationCache.cachedUserLocation = testLocation
        
        // When
        let result = await locationRepository.getCurrentLocation()
        
        // Then
        XCTAssertNotNil(result)
        XCTAssertEqual(result, testLocation)
        XCTAssertTrue(mockLocationCache.getCachedUserLocationCalled)
    }
    
    func testGetCurrentLocation_WithUnauthenticatedUser_ReturnsNil() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let result = await locationRepository.getCurrentLocation()
        
        // Then
        XCTAssertNil(result)
    }
    
    func testGetFriendLocations_WithFirebaseData_ReturnsLocations() async {
        // Given
        let friendIds = [testFriendId]
        mockFirebaseDatabase.shouldSucceed = true
        mockFirebaseDatabase.mockLocationData = [
            "latitude": testLocation.latitude,
            "longitude": testLocation.longitude,
            "accuracy": testLocation.accuracy,
            "timestamp": testLocation.timestamp,
            "altitude": testLocation.altitude as Any
        ]
        
        // When
        let result = await locationRepository.getFriendLocations(friendIds: friendIds)
        
        // Then
        switch result {
        case .success(let locations):
            XCTAssertEqual(locations.count, 1)
            XCTAssertNotNil(locations[testFriendId])
            XCTAssertTrue(mockLocationCache.cacheFriendLocationCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testGetFriendLocations_WithFirebaseError_ReturnsCachedLocations() async {
        // Given
        let friendIds = [testFriendId]
        mockFirebaseDatabase.shouldSucceed = false
        mockFirebaseDatabase.errorToThrow = NSError(domain: "Firebase", code: 1, userInfo: nil)
        mockLocationCache.cachedFriendLocations[testFriendId] = testLocation
        
        // When
        let result = await locationRepository.getFriendLocations(friendIds: friendIds)
        
        // Then
        switch result {
        case .success(let locations):
            XCTAssertEqual(locations.count, 1)
            XCTAssertEqual(locations[testFriendId], testLocation)
            XCTAssertTrue(mockLocationCache.getCachedFriendLocationCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testStartLocationSharing_WithAuthenticatedUser_ReturnsSuccess() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = true
        
        // When
        let result = await locationRepository.startLocationSharing()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFirebaseDatabase.setValueCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testStartLocationSharing_WithUnauthenticatedUser_ReturnsFailure() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let result = await locationRepository.startLocationSharing()
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            XCTAssertTrue(error is LocationRepositoryError)
        }
    }
    
    func testStopLocationSharing_WithAuthenticatedUser_ReturnsSuccess() async {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
        mockFirebaseDatabase.shouldSucceed = true
        
        // When
        let result = await locationRepository.stopLocationSharing()
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFirebaseDatabase.setValueCalled)
        case .failure(let error):
            XCTFail("Expected success but got error: \(error)")
        }
    }
    
    func testStopLocationSharing_WithUnauthenticatedUser_ReturnsFailure() async {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        // When
        let result = await locationRepository.stopLocationSharing()
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            XCTAssertTrue(error is LocationRepositoryError)
        }
    }
    
    func testGetLocationUpdates_WithAuthenticatedUser_PublishesUpdates() {
        // Given
        let mockUser = MockFirebaseUser(uid: testUserId)
        mockFirebaseAuth.currentUser = mockUser
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
        locationRepository.getLocationUpdates()
            .sink { location in
                XCTAssertEqual(location.latitude, self.testLocation.latitude)
                XCTAssertEqual(location.longitude, self.testLocation.longitude)
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // Simulate Firebase data change
        mockFirebaseDatabase.simulateDataChange()
        
        // Then
        wait(for: [expectation], timeout: 1.0)
    }
    
    func testGetLocationUpdates_WithUnauthenticatedUser_CompletesImmediately() {
        // Given
        mockFirebaseAuth.currentUser = nil
        
        let expectation = XCTestExpectation(description: "Publisher completes")
        
        // When
        locationRepository.getLocationUpdates()
            .sink(
                receiveCompletion: { completion in
                    expectation.fulfill()
                },
                receiveValue: { _ in
                    XCTFail("Should not receive any values")
                }
            )
            .store(in: &cancellables)
        
        // Then
        wait(for: [expectation], timeout: 1.0)
    }
}