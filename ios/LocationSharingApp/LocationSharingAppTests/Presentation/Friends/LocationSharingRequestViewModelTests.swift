import XCTest
import Combine
@testable import LocationSharingApp

/**
 * Unit tests for LocationSharingRequestViewModel.
 * 
 * These tests verify the location sharing request handling logic,
 * error handling, and UI state management.
 */
class LocationSharingRequestViewModelTests: XCTestCase {
    
    var viewModel: LocationSharingRequestViewModel!
    var mockLocationSharingManager: MockLocationSharingManager!
    var cancellables: Set<AnyCancellable>!
    
    override func setUp() {
        super.setUp()
        mockLocationSharingManager = MockLocationSharingManager()
        viewModel = LocationSharingRequestViewModel(locationSharingManager: mockLocationSharingManager)
        cancellables = Set<AnyCancellable>()
    }
    
    override func tearDown() {
        viewModel = nil
        mockLocationSharingManager = nil
        cancellables = nil
        super.tearDown()
    }
    
    @MainActor
    func testAcceptLocationSharingRequestSuccess() async {
        // Given
        let friendId = "friend123"
        mockLocationSharingManager.grantLocationSharingPermissionResult = .success(())
        
        // When
        viewModel.acceptLocationSharingRequest(friendId: friendId)
        
        // Wait for async operation to complete
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertTrue(viewModel.requestProcessed)
        XCTAssertTrue(viewModel.requestAccepted)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertTrue(mockLocationSharingManager.grantLocationSharingPermissionCalled)
        XCTAssertEqual(mockLocationSharingManager.lastFriendId, friendId)
    }
    
    @MainActor
    func testAcceptLocationSharingRequestFailure() async {
        // Given
        let friendId = "friend123"
        let error = LocationSharingError.permissionUpdateFailed("Permission grant failed")
        mockLocationSharingManager.grantLocationSharingPermissionResult = .failure(error)
        
        // When
        viewModel.acceptLocationSharingRequest(friendId: friendId)
        
        // Wait for async operation to complete
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertFalse(viewModel.requestProcessed)
        XCTAssertFalse(viewModel.requestAccepted)
        XCTAssertNotNil(viewModel.errorMessage)
        XCTAssertEqual(viewModel.errorMessage, error.localizedDescription)
    }
    
    @MainActor
    func testDenyLocationSharingRequestSuccess() async {
        // Given
        let friendId = "friend123"
        mockLocationSharingManager.denyLocationSharingPermissionResult = .success(())
        
        // When
        viewModel.denyLocationSharingRequest(friendId: friendId)
        
        // Wait for async operation to complete
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertTrue(viewModel.requestProcessed)
        XCTAssertFalse(viewModel.requestAccepted)
        XCTAssertNil(viewModel.errorMessage)
        XCTAssertTrue(mockLocationSharingManager.denyLocationSharingPermissionCalled)
        XCTAssertEqual(mockLocationSharingManager.lastFriendId, friendId)
    }
    
    @MainActor
    func testDenyLocationSharingRequestFailure() async {
        // Given
        let friendId = "friend123"
        let error = LocationSharingError.permissionUpdateFailed("Permission denial failed")
        mockLocationSharingManager.denyLocationSharingPermissionResult = .failure(error)
        
        // When
        viewModel.denyLocationSharingRequest(friendId: friendId)
        
        // Wait for async operation to complete
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertFalse(viewModel.requestProcessed)
        XCTAssertFalse(viewModel.requestAccepted)
        XCTAssertNotNil(viewModel.errorMessage)
        XCTAssertEqual(viewModel.errorMessage, error.localizedDescription)
    }
    
    @MainActor
    func testClearError() {
        // Given
        viewModel.errorMessage = "Test error"
        
        // When
        viewModel.clearError()
        
        // Then
        XCTAssertNil(viewModel.errorMessage)
    }
    
    @MainActor
    func testLoadingStateDuringRequest() async {
        // Given
        let friendId = "friend123"
        mockLocationSharingManager.grantLocationSharingPermissionResult = .success(())
        
        // When
        viewModel.acceptLocationSharingRequest(friendId: friendId)
        
        // Then - check loading state immediately
        XCTAssertTrue(viewModel.isLoading)
        
        // Wait for completion
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // Then - check loading state after completion
        XCTAssertFalse(viewModel.isLoading)
    }
}

/**
 * Mock LocationSharingManager for testing.
 */
class MockLocationSharingManager: LocationSharingManager {
    var grantLocationSharingPermissionResult: Result<Void, LocationSharingError> = .success(())
    var denyLocationSharingPermissionResult: Result<Void, LocationSharingError> = .success(())
    var grantLocationSharingPermissionCalled = false
    var denyLocationSharingPermissionCalled = false
    var lastFriendId: String?
    
    override func grantLocationSharingPermission(friendId: String) async -> Result<Void, LocationSharingError> {
        grantLocationSharingPermissionCalled = true
        lastFriendId = friendId
        return grantLocationSharingPermissionResult
    }
    
    override func denyLocationSharingPermission(friendId: String) async -> Result<Void, LocationSharingError> {
        denyLocationSharingPermissionCalled = true
        lastFriendId = friendId
        return denyLocationSharingPermissionResult
    }
}