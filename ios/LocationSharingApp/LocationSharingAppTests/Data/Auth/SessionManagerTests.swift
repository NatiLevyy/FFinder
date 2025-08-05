import XCTest
import Combine
@testable import LocationSharingApp

@MainActor
class SessionManagerTests: XCTestCase {
    
    var sessionManager: SessionManager!
    var cancellables: Set<AnyCancellable>!
    
    override func setUp() {
        super.setUp()
        sessionManager = SessionManager()
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        sessionManager = nil
        cancellables = nil
        super.tearDown()
    }
    
    // MARK: - Initial State Tests
    
    func testInitialState_ShouldBeUnauthenticatedWithNoUser() {
        // Then
        XCTAssertNil(sessionManager.currentUser)
        XCTAssertFalse(sessionManager.isAuthenticated)
        XCTAssertFalse(sessionManager.isUserAuthenticated())
        XCTAssertNil(sessionManager.getCurrentUserSync())
    }
    
    // MARK: - Set Current User Tests
    
    func testSetCurrentUser_ShouldUpdateUserAndAuthenticationState() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        let expectation = XCTestExpectation(description: "User state updated")
        var receivedUser: User?
        var receivedAuthState: Bool?
        
        // When
        sessionManager.currentUserPublisher
            .sink { user in
                receivedUser = user
                if user != nil {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        sessionManager.isAuthenticatedPublisher
            .sink { isAuthenticated in
                receivedAuthState = isAuthenticated
            }
            .store(in: &cancellables)
        
        sessionManager.setCurrentUser(user)
        
        // Then
        wait(for: [expectation], timeout: 1.0)
        
        XCTAssertEqual(sessionManager.currentUser, user)
        XCTAssertTrue(sessionManager.isAuthenticated)
        XCTAssertTrue(sessionManager.isUserAuthenticated())
        XCTAssertEqual(sessionManager.getCurrentUserSync(), user)
        XCTAssertEqual(receivedUser, user)
        XCTAssertTrue(receivedAuthState == true)
    }
    
    // MARK: - Clear Current User Tests
    
    func testClearCurrentUser_ShouldResetUserAndAuthenticationState() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        sessionManager.setCurrentUser(user)
        
        let expectation = XCTestExpectation(description: "User state cleared")
        var receivedUser: User?
        var receivedAuthState: Bool?
        
        // When
        sessionManager.currentUserPublisher
            .dropFirst() // Skip initial value
            .sink { user in
                receivedUser = user
                if user == nil {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        sessionManager.isAuthenticatedPublisher
            .dropFirst() // Skip initial value
            .sink { isAuthenticated in
                receivedAuthState = isAuthenticated
            }
            .store(in: &cancellables)
        
        sessionManager.clearCurrentUser()
        
        // Then
        wait(for: [expectation], timeout: 1.0)
        
        XCTAssertNil(sessionManager.currentUser)
        XCTAssertFalse(sessionManager.isAuthenticated)
        XCTAssertFalse(sessionManager.isUserAuthenticated())
        XCTAssertNil(sessionManager.getCurrentUserSync())
        XCTAssertNil(receivedUser)
        XCTAssertFalse(receivedAuthState == true)
    }
    
    // MARK: - Update Current User Tests
    
    func testUpdateCurrentUser_WithSameId_ShouldUpdateUser() {
        // Given
        let originalUser = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        sessionManager.setCurrentUser(originalUser)
        
        let updatedUser = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Updated User",
            profileImageUrl: "https://example.com/avatar.jpg",
            createdAt: originalUser.createdAt,
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        let expectation = XCTestExpectation(description: "User updated")
        var receivedUser: User?
        
        // When
        sessionManager.currentUserPublisher
            .dropFirst() // Skip initial value
            .sink { user in
                receivedUser = user
                if user?.displayName == "Updated User" {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        sessionManager.updateCurrentUser(updatedUser)
        
        // Then
        wait(for: [expectation], timeout: 1.0)
        
        XCTAssertEqual(sessionManager.currentUser, updatedUser)
        XCTAssertEqual(sessionManager.currentUser?.displayName, "Updated User")
        XCTAssertEqual(sessionManager.currentUser?.profileImageUrl, "https://example.com/avatar.jpg")
        XCTAssertEqual(receivedUser, updatedUser)
    }
    
    func testUpdateCurrentUser_WithDifferentId_ShouldNotUpdateUser() {
        // Given
        let originalUser = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        sessionManager.setCurrentUser(originalUser)
        
        let differentUser = User(
            id: "user456",
            email: "other@example.com",
            displayName: "Other User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        // When
        sessionManager.updateCurrentUser(differentUser)
        
        // Then
        XCTAssertEqual(sessionManager.currentUser, originalUser)
        XCTAssertEqual(sessionManager.currentUser?.displayName, "Test User")
        XCTAssertNotEqual(sessionManager.currentUser?.id, differentUser.id)
    }
    
    // MARK: - Multiple User Changes Tests
    
    func testMultipleSetCurrentUserCalls_ShouldUpdateStateCorrectly() {
        // Given
        let user1 = User(
            id: "user1",
            email: "user1@example.com",
            displayName: "User 1",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        let user2 = User(
            id: "user2",
            email: "user2@example.com",
            displayName: "User 2",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        let expectation = XCTestExpectation(description: "Multiple user changes")
        expectation.expectedFulfillmentCount = 2
        var userChanges: [User?] = []
        
        // When
        sessionManager.currentUserPublisher
            .sink { user in
                userChanges.append(user)
                if userChanges.count >= 3 { // nil, user1, user2
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        sessionManager.setCurrentUser(user1)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.sessionManager.setCurrentUser(user2)
            expectation.fulfill()
        }
        
        // Then
        wait(for: [expectation], timeout: 1.0)
        
        XCTAssertEqual(sessionManager.currentUser, user2)
        XCTAssertTrue(sessionManager.isUserAuthenticated())
        XCTAssertTrue(userChanges.contains { $0?.id == user1.id })
        XCTAssertTrue(userChanges.contains { $0?.id == user2.id })
    }
    
    // MARK: - Publisher Tests
    
    func testCurrentUserPublisher_ShouldEmitUserChanges() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        let expectation = XCTestExpectation(description: "User publisher emits")
        var emittedUsers: [User?] = []
        
        // When
        sessionManager.currentUserPublisher
            .sink { user in
                emittedUsers.append(user)
                if emittedUsers.count == 2 { // nil, then user
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        sessionManager.setCurrentUser(user)
        
        // Then
        wait(for: [expectation], timeout: 1.0)
        
        XCTAssertEqual(emittedUsers.count, 2)
        XCTAssertNil(emittedUsers[0])
        XCTAssertEqual(emittedUsers[1], user)
    }
    
    func testIsAuthenticatedPublisher_ShouldEmitAuthenticationChanges() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        let expectation = XCTestExpectation(description: "Auth publisher emits")
        var emittedStates: [Bool] = []
        
        // When
        sessionManager.isAuthenticatedPublisher
            .sink { isAuthenticated in
                emittedStates.append(isAuthenticated)
                if emittedStates.count == 2 { // false, then true
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        sessionManager.setCurrentUser(user)
        
        // Then
        wait(for: [expectation], timeout: 1.0)
        
        XCTAssertEqual(emittedStates.count, 2)
        XCTAssertFalse(emittedStates[0])
        XCTAssertTrue(emittedStates[1])
    }
}