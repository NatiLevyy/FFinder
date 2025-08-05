import XCTest
@testable import LocationSharingApp

/**
 * Unit tests for FriendsViewModel.
 * 
 * These tests verify the friends list management, search functionality,
 * location sharing controls, and friend removal operations.
 */
class FriendsViewModelTests: XCTestCase {
    
    var viewModel: FriendsViewModel!
    var mockFriendsRepository: MockFriendsRepository!
    
    override func setUp() {
        super.setUp()
        mockFriendsRepository = MockFriendsRepository()
        viewModel = FriendsViewModel(friendsRepository: mockFriendsRepository)
    }
    
    override func tearDown() {
        viewModel = nil
        mockFriendsRepository = nil
        super.tearDown()
    }
    
    @MainActor
    func testLoadFriends_Success_UpdatesUIState() async {
        // Given
        let mockFriends = [
            createMockFriend(id: "1", displayName: "John Doe", email: "john@example.com"),
            createMockFriend(id: "2", displayName: "Jane Smith", email: "jane@example.com")
        ]
        mockFriendsRepository.getFriendsResult = .success(mockFriends)
        
        // When
        viewModel.loadFriends()
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertEqual(viewModel.friends.count, 2)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertEqual(viewModel.friends.first?.user.displayName, "John Doe")
    }
    
    @MainActor
    func testLoadFriends_Failure_UpdatesErrorState() async {
        // Given
        let error = NSError(domain: "TestError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Network error"])
        mockFriendsRepository.getFriendsResult = .failure(error)
        
        // When
        viewModel.loadFriends()
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertTrue(viewModel.friends.isEmpty)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.errorMessage, "Network error")
    }
    
    @MainActor
    func testSearchFriends_FiltersByDisplayName() async {
        // Given
        let mockFriends = [
            createMockFriend(id: "1", displayName: "John Doe", email: "john@example.com"),
            createMockFriend(id: "2", displayName: "Jane Smith", email: "jane@example.com")
        ]
        mockFriendsRepository.getFriendsResult = .success(mockFriends)
        
        // Load friends first
        viewModel.loadFriends()
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // When
        viewModel.searchFriends(query: "John")
        
        // Then
        XCTAssertEqual(viewModel.friends.count, 1)
        XCTAssertEqual(viewModel.friends.first?.user.displayName, "John Doe")
    }
    
    @MainActor
    func testSearchFriends_FiltersByEmail() async {
        // Given
        let mockFriends = [
            createMockFriend(id: "1", displayName: "John Doe", email: "john@example.com"),
            createMockFriend(id: "2", displayName: "Jane Smith", email: "jane@example.com")
        ]
        mockFriendsRepository.getFriendsResult = .success(mockFriends)
        
        // Load friends first
        viewModel.loadFriends()
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // When
        viewModel.searchFriends(query: "jane@example.com")
        
        // Then
        XCTAssertEqual(viewModel.friends.count, 1)
        XCTAssertEqual(viewModel.friends.first?.user.email, "jane@example.com")
    }
    
    @MainActor
    func testSearchFriends_EmptyQuery_ReturnsAllFriends() async {
        // Given
        let mockFriends = [
            createMockFriend(id: "1", displayName: "John Doe", email: "john@example.com"),
            createMockFriend(id: "2", displayName: "Jane Smith", email: "jane@example.com")
        ]
        mockFriendsRepository.getFriendsResult = .success(mockFriends)
        
        // Load friends first
        viewModel.loadFriends()
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // When
        viewModel.searchFriends(query: "")
        
        // Then
        XCTAssertEqual(viewModel.friends.count, 2)
    }
    
    @MainActor
    func testUpdateLocationSharing_Success_ShowsSuccessMessage() async {
        // Given
        let friendId = "friend123"
        let enabled = true
        mockFriendsRepository.updateLocationSharingResult = .success(())
        mockFriendsRepository.getFriendsResult = .success([])
        
        // When
        viewModel.updateLocationSharing(friendId: friendId, enabled: enabled)
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Then
        XCTAssertTrue(viewModel.showSuccessMessage)
        XCTAssertEqual(viewModel.successMessage, "Location sharing updated")
        XCTAssertTrue(mockFriendsRepository.updateLocationSharingCalled)
    }
    
    @MainActor
    func testUpdateLocationSharing_Failure_ShowsError() async {
        // Given
        let friendId = "friend123"
        let enabled = true
        let error = NSError(domain: "TestError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Update failed"])
        mockFriendsRepository.updateLocationSharingResult = .failure(error)
        
        // When
        viewModel.updateLocationSharing(friendId: friendId, enabled: enabled)
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Then
        XCTAssertEqual(viewModel.errorMessage, "Update failed")
        XCTAssertFalse(viewModel.showSuccessMessage)
    }
    
    @MainActor
    func testRemoveFriend_Success_ShowsSuccessMessage() async {
        // Given
        let friendId = "friend123"
        mockFriendsRepository.removeFriendResult = .success(())
        mockFriendsRepository.getFriendsResult = .success([])
        
        // When
        viewModel.removeFriend(friendId: friendId)
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Then
        XCTAssertTrue(viewModel.showSuccessMessage)
        XCTAssertEqual(viewModel.successMessage, "Friend removed")
        XCTAssertTrue(mockFriendsRepository.removeFriendCalled)
    }
    
    @MainActor
    func testRemoveFriend_Failure_ShowsError() async {
        // Given
        let friendId = "friend123"
        let error = NSError(domain: "TestError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Remove failed"])
        mockFriendsRepository.removeFriendResult = .failure(error)
        
        // When
        viewModel.removeFriend(friendId: friendId)
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Then
        XCTAssertEqual(viewModel.errorMessage, "Remove failed")
        XCTAssertFalse(viewModel.showSuccessMessage)
    }
    
    @MainActor
    func testClearError_ResetsErrorState() {
        // Given
        viewModel.errorMessage = "Test error"
        
        // When
        viewModel.clearError()
        
        // Then
        XCTAssertNil(viewModel.errorMessage)
    }
    
    @MainActor
    func testRequestLocationSharing_Success_ShowsSuccessMessage() async {
        // Given
        let friendId = "friend123"
        mockFriendsRepository.getFriendsResult = .success([])
        
        // When
        viewModel.requestLocationSharing(friendId: friendId)
        
        // Wait for async operation
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Then
        XCTAssertTrue(viewModel.showSuccessMessage)
        XCTAssertEqual(viewModel.successMessage, "Location sharing request sent")
    }
    
    @MainActor
    func testClearSuccessMessage_ResetsSuccessState() {
        // Given
        viewModel.showSuccessMessage = true
        viewModel.successMessage = "Test success"
        
        // When
        viewModel.clearSuccessMessage()
        
        // Then
        XCTAssertFalse(viewModel.showSuccessMessage)
        XCTAssertEqual(viewModel.successMessage, "")
    }
    
    // MARK: - Helper Methods
    
    private func createMockFriend(id: String, displayName: String, email: String) -> Friend {
        return Friend(
            id: id,
            user: User(
                id: id,
                email: email,
                displayName: displayName,
                profileImageUrl: nil,
                createdAt: Int64(Date().timeIntervalSince1970 * 1000),
                lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
            ),
            friendshipStatus: .accepted,
            locationSharingEnabled: false,
            lastKnownLocation: nil,
            locationSharingPermission: .none
        )
    }
}

// MARK: - Mock Friends Repository

class MockFriendsRepository: FriendsRepository {
    var getFriendsResult: Result<[Friend], Error> = .success([])
    var updateLocationSharingResult: Result<Void, Error> = .success(())
    var removeFriendResult: Result<Void, Error> = .success(())
    
    var updateLocationSharingCalled = false
    var removeFriendCalled = false
    
    func getFriends() async -> Result<[Friend], Error> {
        return getFriendsResult
    }
    
    func sendFriendRequest(email: String) async -> Result<Void, Error> {
        return .success(())
    }
    
    func acceptFriendRequest(requestId: String) async -> Result<Void, Error> {
        return .success(())
    }
    
    func rejectFriendRequest(requestId: String) async -> Result<Void, Error> {
        return .success(())
    }
    
    func removeFriend(friendId: String) async -> Result<Void, Error> {
        removeFriendCalled = true
        return removeFriendResult
    }
    
    func updateLocationSharingPermission(friendId: String, enabled: Bool) async -> Result<Void, Error> {
        updateLocationSharingCalled = true
        return updateLocationSharingResult
    }
    
    func getFriendRequests() async -> Result<[FriendRequest], Error> {
        return .success([])
    }
    
    func searchUser(byEmail email: String) async -> Result<User?, Error> {
        return .success(nil)
    }
}