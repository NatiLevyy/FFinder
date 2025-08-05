import XCTest
@testable import LocationSharingApp

class FriendRequestServiceImplTests: XCTestCase {
    
    var friendRequestService: FriendRequestServiceImpl!
    var mockFriendsRepository: MockFriendsRepository!
    var mockSessionManager: MockSessionManager!
    
    let currentUser = User(
        id: "current_user",
        email: "current@example.com",
        displayName: "Current User",
        profileImageUrl: nil,
        createdAt: 1640995200000,
        lastActiveAt: 1640995200000
    )
    
    let targetUser = User(
        id: "target_user",
        email: "target@example.com",
        displayName: "Target User",
        profileImageUrl: nil,
        createdAt: 1640995200000,
        lastActiveAt: 1640995200000
    )
    
    override func setUp() {
        super.setUp()
        mockFriendsRepository = MockFriendsRepository()
        mockSessionManager = MockSessionManager()
        mockSessionManager.currentUser = currentUser
        
        friendRequestService = FriendRequestServiceImpl(
            friendsRepository: mockFriendsRepository,
            sessionManager: mockSessionManager
        )
    }
    
    override func tearDown() {
        friendRequestService = nil
        mockFriendsRepository = nil
        mockSessionManager = nil
        super.tearDown()
    }
    
    func testSearchUserByEmailWithValidEmailAndUserExists() async {
        // Given
        mockFriendsRepository.searchUserResult = .success(targetUser)
        
        // When
        let result = await friendRequestService.searchUser(byEmail: "target@example.com")
        
        // Then
        switch result {
        case .success(let user):
            XCTAssertEqual(user?.id, targetUser.id)
            XCTAssertEqual(user?.email, targetUser.email)
        case .failure:
            XCTFail("Expected success")
        }
    }
    
    func testSearchUserByEmailWithInvalidEmail() async {
        // When
        let result = await friendRequestService.searchUser(byEmail: "invalid-email")
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure(let error):
            XCTAssertTrue(error is FriendRequestError)
            if case FriendRequestError.invalidEmail = error {
                // Expected error type
            } else {
                XCTFail("Expected invalidEmail error")
            }
        }
    }
    
    func testSearchUserByEmailWithUserNotFound() async {
        // Given
        mockFriendsRepository.searchUserResult = .success(nil)
        
        // When
        let result = await friendRequestService.searchUser(byEmail: "nonexistent@example.com")
        
        // Then
        switch result {
        case .success(let user):
            XCTAssertNil(user)
        case .failure:
            XCTFail("Expected success with nil user")
        }
    }
    
    func testSendFriendRequestWithValidEmailAndUserExists() async {
        // Given
        mockFriendsRepository.searchUserResult = .success(targetUser)
        mockFriendsRepository.sendFriendRequestResult = .success(())
        
        // When
        let result = await friendRequestService.sendFriendRequest(email: "target@example.com")
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFriendsRepository.sendFriendRequestCalled)
        case .failure:
            XCTFail("Expected success")
        }
    }
    
    func testSendFriendRequestWithInvalidEmail() async {
        // When
        let result = await friendRequestService.sendFriendRequest(email: "invalid-email")
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure(let error):
            XCTAssertTrue(error is FriendRequestError)
            if case FriendRequestError.invalidEmail = error {
                // Expected error type
            } else {
                XCTFail("Expected invalidEmail error")
            }
            XCTAssertFalse(mockFriendsRepository.sendFriendRequestCalled)
        }
    }
    
    func testSendFriendRequestToSelf() async {
        // When
        let result = await friendRequestService.sendFriendRequest(email: "current@example.com")
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure(let error):
            XCTAssertTrue(error is FriendRequestError)
            if case FriendRequestError.cannotRequestSelf = error {
                // Expected error type
            } else {
                XCTFail("Expected cannotRequestSelf error")
            }
            XCTAssertFalse(mockFriendsRepository.sendFriendRequestCalled)
        }
    }
    
    func testSendFriendRequestWithUserNotFound() async {
        // Given
        mockFriendsRepository.searchUserResult = .success(nil)
        
        // When
        let result = await friendRequestService.sendFriendRequest(email: "nonexistent@example.com")
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure(let error):
            XCTAssertTrue(error is FriendRequestError)
            if case FriendRequestError.userNotFound = error {
                // Expected error type
            } else {
                XCTFail("Expected userNotFound error")
            }
            XCTAssertFalse(mockFriendsRepository.sendFriendRequestCalled)
        }
    }
    
    func testGetPendingFriendRequests() async {
        // Given
        let friendRequests = [
            FriendRequest(
                id: "request1",
                fromUserId: "user1",
                toUserId: "current_user",
                status: .pending,
                createdAt: 1640995200000
            )
        ]
        mockFriendsRepository.getFriendRequestsResult = .success(friendRequests)
        
        // When
        let result = await friendRequestService.getPendingFriendRequests()
        
        // Then
        switch result {
        case .success(let requests):
            XCTAssertEqual(requests.count, 1)
            XCTAssertEqual(requests[0].id, "request1")
        case .failure:
            XCTFail("Expected success")
        }
    }
    
    func testAcceptFriendRequestWithValidId() async {
        // Given
        mockFriendsRepository.acceptFriendRequestResult = .success(())
        
        // When
        let result = await friendRequestService.acceptFriendRequest(requestId: "request123")
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFriendsRepository.acceptFriendRequestCalled)
        case .failure:
            XCTFail("Expected success")
        }
    }
    
    func testAcceptFriendRequestWithEmptyId() async {
        // When
        let result = await friendRequestService.acceptFriendRequest(requestId: "")
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure:
            XCTAssertFalse(mockFriendsRepository.acceptFriendRequestCalled)
        }
    }
    
    func testRejectFriendRequestWithValidId() async {
        // Given
        mockFriendsRepository.rejectFriendRequestResult = .success(())
        
        // When
        let result = await friendRequestService.rejectFriendRequest(requestId: "request123")
        
        // Then
        switch result {
        case .success:
            XCTAssertTrue(mockFriendsRepository.rejectFriendRequestCalled)
        case .failure:
            XCTFail("Expected success")
        }
    }
    
    func testRejectFriendRequestWithEmptyId() async {
        // When
        let result = await friendRequestService.rejectFriendRequest(requestId: "")
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure:
            XCTAssertFalse(mockFriendsRepository.rejectFriendRequestCalled)
        }
    }
    
    func testValidateFriendRequestWithValidEmail() async {
        // Given
        mockFriendsRepository.searchUserResult = .success(targetUser)
        
        // When
        let result = await friendRequestService.validateFriendRequest(email: "target@example.com")
        
        // Then
        switch result {
        case .success(let validation):
            XCTAssertTrue(validation.isValid)
            XCTAssertNil(validation.reason)
        case .failure:
            XCTFail("Expected success")
        }
    }
    
    func testValidateFriendRequestWithInvalidEmail() async {
        // When
        let result = await friendRequestService.validateFriendRequest(email: "invalid-email")
        
        // Then
        switch result {
        case .success(let validation):
            XCTAssertFalse(validation.isValid)
            XCTAssertEqual(validation.reason, "Invalid email")
        case .failure:
            XCTFail("Expected success with invalid validation")
        }
    }
    
    func testValidateFriendRequestWithSelfEmail() async {
        // When
        let result = await friendRequestService.validateFriendRequest(email: "current@example.com")
        
        // Then
        switch result {
        case .success(let validation):
            XCTAssertFalse(validation.isValid)
            XCTAssertEqual(validation.reason, "Cannot request self")
        case .failure:
            XCTFail("Expected success with invalid validation")
        }
    }
    
    func testValidateFriendRequestWithUserNotFound() async {
        // Given
        mockFriendsRepository.searchUserResult = .success(nil)
        
        // When
        let result = await friendRequestService.validateFriendRequest(email: "nonexistent@example.com")
        
        // Then
        switch result {
        case .success(let validation):
            XCTAssertFalse(validation.isValid)
            XCTAssertEqual(validation.reason, "User not found")
        case .failure:
            XCTFail("Expected success with invalid validation")
        }
    }
}

// MARK: - Mock Classes

class MockFriendsRepository: FriendsRepository {
    var searchUserResult: Result<User?, Error> = .success(nil)
    var sendFriendRequestResult: Result<Void, Error> = .success(())
    var getFriendRequestsResult: Result<[FriendRequest], Error> = .success([])
    var acceptFriendRequestResult: Result<Void, Error> = .success(())
    var rejectFriendRequestResult: Result<Void, Error> = .success(())
    
    var sendFriendRequestCalled = false
    var acceptFriendRequestCalled = false
    var rejectFriendRequestCalled = false
    
    func getFriends() async -> Result<[Friend], Error> {
        return .success([])
    }
    
    func sendFriendRequest(email: String) async -> Result<Void, Error> {
        sendFriendRequestCalled = true
        return sendFriendRequestResult
    }
    
    func acceptFriendRequest(requestId: String) async -> Result<Void, Error> {
        acceptFriendRequestCalled = true
        return acceptFriendRequestResult
    }
    
    func rejectFriendRequest(requestId: String) async -> Result<Void, Error> {
        rejectFriendRequestCalled = true
        return rejectFriendRequestResult
    }
    
    func removeFriend(friendId: String) async -> Result<Void, Error> {
        return .success(())
    }
    
    func updateLocationSharingPermission(friendId: String, enabled: Bool) async -> Result<Void, Error> {
        return .success(())
    }
    
    func getFriendRequests() async -> Result<[FriendRequest], Error> {
        return getFriendRequestsResult
    }
    
    func searchUser(byEmail email: String) async -> Result<User?, Error> {
        return searchUserResult
    }
}

class MockSessionManager: SessionManager {
    var currentUser: User?
    
    func getCurrentUserSync() -> User? {
        return currentUser
    }
    
    func setCurrentUser(_ user: User) {
        currentUser = user
    }
    
    func clearCurrentUser() {
        currentUser = nil
    }
}