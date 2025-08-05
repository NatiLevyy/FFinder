import XCTest
import Combine
@testable import LocationSharingApp

/**
 * Unit tests for LocationSharingManager.
 * 
 * Tests the location sharing permission system including request, grant, deny,
 * and revoke operations, as well as status tracking functionality.
 */
class LocationSharingManagerTests: XCTestCase {
    
    var locationSharingManager: LocationSharingManager!
    var mockFriendsRepository: MockFriendsRepository!
    var cancellables: Set<AnyCancellable>!
    
    let testUser = User(
        id: "user123",
        email: "test@example.com",
        displayName: "Test User",
        profileImageUrl: nil,
        createdAt: Int64(Date().timeIntervalSince1970 * 1000),
        lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
    )
    
    lazy var testFriend = Friend(
        id: "friend123",
        user: testUser,
        friendshipStatus: .accepted,
        locationSharingEnabled: false,
        lastKnownLocation: nil,
        locationSharingPermission: .none
    )
    
    override func setUp() {
        super.setUp()
        mockFriendsRepository = MockFriendsRepository()
        locationSharingManager = LocationSharingManager(friendsRepository: mockFriendsRepository)
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        cancellables = nil
        locationSharingManager = nil
        mockFriendsRepository = nil
        super.tearDown()
    }
    
    func testRequestLocationSharingPermission_Success_UpdatesPermissionToRequested() async {
        // Given
        let friendId = "friend123"
        mockFriendsRepository.updateLocationSharingPermissionResult = .success(())
        
        // When
        let result = await locationSharingManager.requestLocationSharingPermission(friendId: friendId)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFriendsRepository.updateLocationSharingPermissionCalled)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedFriendId, friendId)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedPermission, .requested)
            
            // Verify local status is updated
            let status = locationSharingManager.getCurrentLocationSharingStatus()
            XCTAssertEqual(status[friendId], .requested)
            
        case .failure(let error):
            XCTFail("Expected success but got failure: \(error)")
        }
    }
    
    func testRequestLocationSharingPermission_Failure_ReturnsError() async {
        // Given
        let friendId = "friend123"
        let expectedError = NSError(domain: "TestError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Network error"])
        mockFriendsRepository.updateLocationSharingPermissionResult = .failure(expectedError)
        
        // When
        let result = await locationSharingManager.requestLocationSharingPermission(friendId: friendId)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            XCTAssertTrue(error is LocationSharingError)
            if case .requestFailed(let message) = error {
                XCTAssertTrue(message.contains("Failed to request location sharing permission"))
            } else {
                XCTFail("Expected requestFailed error")
            }
        }
    }
    
    func testGrantLocationSharingPermission_Success_UpdatesPermissionToGranted() async {
        // Given
        let friendId = "friend123"
        mockFriendsRepository.updateLocationSharingPermissionResult = .success(())
        
        // When
        let result = await locationSharingManager.grantLocationSharingPermission(friendId: friendId)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFriendsRepository.updateLocationSharingPermissionCalled)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedFriendId, friendId)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedPermission, .granted)
            
            // Verify local status is updated
            let status = locationSharingManager.getCurrentLocationSharingStatus()
            XCTAssertEqual(status[friendId], .granted)
            
        case .failure(let error):
            XCTFail("Expected success but got failure: \(error)")
        }
    }
    
    func testGrantLocationSharingPermission_Failure_ReturnsError() async {
        // Given
        let friendId = "friend123"
        let expectedError = NSError(domain: "TestError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Database error"])
        mockFriendsRepository.updateLocationSharingPermissionResult = .failure(expectedError)
        
        // When
        let result = await locationSharingManager.grantLocationSharingPermission(friendId: friendId)
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure but got success")
        case .failure(let error):
            if case .permissionUpdateFailed(let message) = error {
                XCTAssertTrue(message.contains("Failed to grant location sharing permission"))
            } else {
                XCTFail("Expected permissionUpdateFailed error")
            }
        }
    }
    
    func testDenyLocationSharingPermission_Success_UpdatesPermissionToDenied() async {
        // Given
        let friendId = "friend123"
        mockFriendsRepository.updateLocationSharingPermissionResult = .success(())
        
        // When
        let result = await locationSharingManager.denyLocationSharingPermission(friendId: friendId)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFriendsRepository.updateLocationSharingPermissionCalled)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedFriendId, friendId)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedPermission, .denied)
            
            // Verify local status is updated
            let status = locationSharingManager.getCurrentLocationSharingStatus()
            XCTAssertEqual(status[friendId], .denied)
            
        case .failure(let error):
            XCTFail("Expected success but got failure: \(error)")
        }
    }
    
    func testRevokeLocationSharingPermission_Success_UpdatesPermissionToNone() async {
        // Given
        let friendId = "friend123"
        mockFriendsRepository.updateLocationSharingPermissionResult = .success(())
        
        // When
        let result = await locationSharingManager.revokeLocationSharingPermission(friendId: friendId)
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFriendsRepository.updateLocationSharingPermissionCalled)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedFriendId, friendId)
            XCTAssertEqual(mockFriendsRepository.lastUpdatedPermission, .none)
            
            // Verify local status is updated
            let status = locationSharingManager.getCurrentLocationSharingStatus()
            XCTAssertEqual(status[friendId], .none)
            
        case .failure(let error):
            XCTFail("Expected success but got failure: \(error)")
        }
    }
    
    func testGetLocationSharingPermission_FriendExists_ReturnsPermission() async {
        // Given
        let friendId = "friend123"
        let friendWithPermission = Friend(
            id: friendId,
            user: testUser,
            friendshipStatus: .accepted,
            locationSharingEnabled: true,
            lastKnownLocation: nil,
            locationSharingPermission: .granted
        )
        mockFriendsRepository.getFriendResult = friendWithPermission
        
        // When
        let permission = await locationSharingManager.getLocationSharingPermission(friendId: friendId)
        
        // Then
        XCTAssertEqual(permission, .granted)
        XCTAssertTrue(mockFriendsRepository.getFriendCalled)
        XCTAssertEqual(mockFriendsRepository.lastQueriedFriendId, friendId)
    }
    
    func testGetLocationSharingPermission_FriendNotFound_ReturnsNone() async {
        // Given
        let friendId = "nonexistent"
        mockFriendsRepository.getFriendResult = nil
        
        // When
        let permission = await locationSharingManager.getLocationSharingPermission(friendId: friendId)
        
        // Then
        XCTAssertEqual(permission, .none)
    }
    
    func testIsLocationSharingActive_PermissionGranted_ReturnsTrue() async {
        // Given
        let friendId = "friend123"
        let friendWithGrantedPermission = Friend(
            id: friendId,
            user: testUser,
            friendshipStatus: .accepted,
            locationSharingEnabled: true,
            lastKnownLocation: nil,
            locationSharingPermission: .granted
        )
        mockFriendsRepository.getFriendResult = friendWithGrantedPermission
        
        // When
        let isActive = await locationSharingManager.isLocationSharingActive(friendId: friendId)
        
        // Then
        XCTAssertTrue(isActive)
    }
    
    func testIsLocationSharingActive_PermissionNotGranted_ReturnsFalse() async {
        // Given
        let friendId = "friend123"
        let friendWithDeniedPermission = Friend(
            id: friendId,
            user: testUser,
            friendshipStatus: .accepted,
            locationSharingEnabled: false,
            lastKnownLocation: nil,
            locationSharingPermission: .denied
        )
        mockFriendsRepository.getFriendResult = friendWithDeniedPermission
        
        // When
        let isActive = await locationSharingManager.isLocationSharingActive(friendId: friendId)
        
        // Then
        XCTAssertFalse(isActive)
    }
    
    func testGetAuthorizedFriends_ReturnsOnlyGrantedPermissions() async {
        // Given
        let friend1 = Friend(id: "friend1", user: testUser, friendshipStatus: .accepted, locationSharingEnabled: true, locationSharingPermission: .granted)
        let friend2 = Friend(id: "friend2", user: testUser, friendshipStatus: .accepted, locationSharingEnabled: false, locationSharingPermission: .denied)
        let friend3 = Friend(id: "friend3", user: testUser, friendshipStatus: .accepted, locationSharingEnabled: true, locationSharingPermission: .granted)
        let friends = [friend1, friend2, friend3]
        
        mockFriendsRepository.getFriendsResult = .success(friends)
        
        // When
        let authorizedFriends = await locationSharingManager.getAuthorizedFriends()
        
        // Then
        XCTAssertEqual(authorizedFriends.count, 2)
        XCTAssertTrue(authorizedFriends.contains("friend1"))
        XCTAssertTrue(authorizedFriends.contains("friend3"))
        XCTAssertFalse(authorizedFriends.contains("friend2"))
    }
    
    func testGetAuthorizedFriends_RepositoryFailure_ReturnsEmptyList() async {
        // Given
        let error = NSError(domain: "TestError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Network error"])
        mockFriendsRepository.getFriendsResult = .failure(error)
        
        // When
        let authorizedFriends = await locationSharingManager.getAuthorizedFriends()
        
        // Then
        XCTAssertTrue(authorizedFriends.isEmpty())
    }
    
    func testInitializeLocationSharingStatus_PopulatesStatusMap() async {
        // Given
        let friend1 = Friend(id: "friend1", user: testUser, friendshipStatus: .accepted, locationSharingEnabled: true, locationSharingPermission: .granted)
        let friend2 = Friend(id: "friend2", user: testUser, friendshipStatus: .accepted, locationSharingEnabled: false, locationSharingPermission: .denied)
        let friends = [friend1, friend2]
        
        mockFriendsRepository.getFriendsResult = .success(friends)
        
        // When
        await locationSharingManager.initializeLocationSharingStatus()
        
        // Then
        let status = locationSharingManager.getCurrentLocationSharingStatus()
        XCTAssertEqual(status["friend1"], .granted)
        XCTAssertEqual(status["friend2"], .denied)
    }
    
    func testInitializeLocationSharingStatus_RepositoryFailure_HandlesGracefully() async {
        // Given
        let error = NSError(domain: "TestError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Network error"])
        mockFriendsRepository.getFriendsResult = .failure(error)
        
        // When
        await locationSharingManager.initializeLocationSharingStatus()
        
        // Then
        let status = locationSharingManager.getCurrentLocationSharingStatus()
        XCTAssertTrue(status.isEmpty)
    }
    
    func testGetFriendsWithLocationSharingStatus_ReturnsPublisher() {
        // Given
        let friends = [testFriend]
        mockFriendsRepository.getFriendsPublisherResult = Just(friends).setFailureType(to: Error.self).eraseToAnyPublisher()
        
        // When
        let publisher = locationSharingManager.getFriendsWithLocationSharingStatus()
        
        // Then
        let expectation = XCTestExpectation(description: "Publisher emits friends")
        
        publisher
            .sink(
                receiveCompletion: { completion in
                    if case .failure(let error) = completion {
                        XCTFail("Expected success but got error: \(error)")
                    }
                },
                receiveValue: { receivedFriends in
                    XCTAssertEqual(receivedFriends.count, 1)
                    XCTAssertEqual(receivedFriends.first?.id, "friend123")
                    expectation.fulfill()
                }
            )
            .store(in: &cancellables)
        
        wait(for: [expectation], timeout: 1.0)
    }
}

// MARK: - Mock Friends Repository

class MockFriendsRepository: FriendsRepository {
    
    // Mock results
    var getFriendsResult: Result<[Friend], Error> = .success([])
    var getFriendResult: Friend?
    var getFriendsPublisherResult: AnyPublisher<[Friend], Error> = Just([]).setFailureType(to: Error.self).eraseToAnyPublisher()
    var updateLocationSharingPermissionResult: Result<Void, Error> = .success(())
    
    // Call tracking
    var getFriendsCalled = false
    var getFriendCalled = false
    var updateLocationSharingPermissionCalled = false
    var lastQueriedFriendId: String?
    var lastUpdatedFriendId: String?
    var lastUpdatedPermission: LocationSharingPermission?
    
    func getFriends() async -> Result<[Friend], Error> {
        getFriendsCalled = true
        return getFriendsResult
    }
    
    func getFriend(friendId: String) async throws -> Friend? {
        getFriendCalled = true
        lastQueriedFriendId = friendId
        return getFriendResult
    }
    
    func getFriendsPublisher() -> AnyPublisher<[Friend], Error> {
        return getFriendsPublisherResult
    }
    
    func updateLocationSharingPermission(friendId: String, permission: LocationSharingPermission) async -> Result<Void, Error> {
        updateLocationSharingPermissionCalled = true
        lastUpdatedFriendId = friendId
        lastUpdatedPermission = permission
        return updateLocationSharingPermissionResult
    }
    
    // Other required methods (not used in these tests)
    func sendFriendRequest(email: String) async -> Result<Void, Error> { .success(()) }
    func acceptFriendRequest(requestId: String) async -> Result<Void, Error> { .success(()) }
    func rejectFriendRequest(requestId: String) async -> Result<Void, Error> { .success(()) }
    func removeFriend(friendId: String) async -> Result<Void, Error> { .success(()) }
    func updateLocationSharingPermission(friendId: String, enabled: Bool) async -> Result<Void, Error> { .success(()) }
    func getFriendRequests() async -> Result<[FriendRequest], Error> { .success([]) }
    func searchUser(byEmail email: String) async -> Result<User?, Error> { .success(nil) }
}