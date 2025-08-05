import XCTest
@testable import LocationSharingApp

@MainActor
class NotificationSettingsViewModelTests: XCTestCase {
    
    var viewModel: NotificationSettingsViewModel!
    var mockNotificationService: MockNotificationService!
    
    override func setUp() {
        super.setUp()
        mockNotificationService = MockNotificationService()
        viewModel = NotificationSettingsViewModel(notificationService: mockNotificationService)
    }
    
    override func tearDown() {
        viewModel = nil
        mockNotificationService = nil
        super.tearDown()
    }
    
    func testLoadSettings_UpdatesPermissionStatus() async {
        // Given
        mockNotificationService.hasPermissionResult = true
        
        // When
        await viewModel.loadSettings()
        
        // Then
        XCTAssertTrue(viewModel.hasPermission)
        XCTAssertFalse(viewModel.isLoading)
    }
    
    func testLoadSettings_LoadsNotificationPreferences() async {
        // Given
        mockNotificationService.notificationTypeStates[.friendRequest] = false
        mockNotificationService.notificationTypeStates[.locationSharingRequest] = true
        
        // When
        await viewModel.loadSettings()
        
        // Then
        XCTAssertFalse(viewModel.friendRequestEnabled)
        XCTAssertTrue(viewModel.locationSharingRequestEnabled)
    }
    
    func testRequestPermission_WithGrantedPermission_UpdatesState() async {
        // Given
        mockNotificationService.requestPermissionResult = true
        
        // When
        await viewModel.requestPermission()
        
        // Then
        XCTAssertTrue(viewModel.hasPermission)
        XCTAssertEqual(viewModel.successMessage, "Notification permission granted!")
        XCTAssertNil(viewModel.errorMessage)
    }
    
    func testRequestPermission_WithDeniedPermission_ShowsError() async {
        // Given
        mockNotificationService.requestPermissionResult = false
        
        // When
        await viewModel.requestPermission()
        
        // Then
        XCTAssertFalse(viewModel.hasPermission)
        XCTAssertNotNil(viewModel.errorMessage)
        XCTAssertNil(viewModel.successMessage)
    }
    
    func testSendTestNotification_WithoutPermission_ShowsError() async {
        // Given
        viewModel.hasPermission = false
        
        // When
        await viewModel.sendTestNotification()
        
        // Then
        XCTAssertEqual(viewModel.errorMessage, "Notification permission is required to send test notifications")
        XCTAssertFalse(mockNotificationService.showNotificationCalled)
    }
    
    func testSendTestNotification_WithPermission_SendsNotification() async {
        // Given
        viewModel.hasPermission = true
        mockNotificationService.showNotificationShouldSucceed = true
        
        // When
        await viewModel.sendTestNotification()
        
        // Then
        XCTAssertTrue(mockNotificationService.showNotificationCalled)
        XCTAssertEqual(viewModel.successMessage, "Test notification sent successfully!")
        XCTAssertNil(viewModel.errorMessage)
    }
    
    func testSetFriendRequestEnabled_UpdatesPreference() {
        // When
        viewModel.setFriendRequestEnabled(false)
        
        // Then
        XCTAssertFalse(viewModel.friendRequestEnabled)
        XCTAssertEqual(mockNotificationService.notificationTypeStates[.friendRequest], false)
    }
    
    func testSetLocationSharingRequestEnabled_UpdatesPreference() {
        // When
        viewModel.setLocationSharingRequestEnabled(false)
        
        // Then
        XCTAssertFalse(viewModel.locationSharingRequestEnabled)
        XCTAssertEqual(mockNotificationService.notificationTypeStates[.locationSharingRequest], false)
    }
    
    func testClearError_RemovesErrorMessage() {
        // Given
        viewModel.errorMessage = "Test error"
        
        // When
        viewModel.clearError()
        
        // Then
        XCTAssertNil(viewModel.errorMessage)
    }
    
    func testClearSuccess_RemovesSuccessMessage() {
        // Given
        viewModel.successMessage = "Test success"
        
        // When
        viewModel.clearSuccess()
        
        // Then
        XCTAssertNil(viewModel.successMessage)
    }
}

// MARK: - Mock Notification Service

class MockNotificationService: NotificationService {
    var hasPermissionResult = false
    var requestPermissionResult = false
    var showNotificationShouldSucceed = true
    var showNotificationCalled = false
    var notificationTypeStates: [NotificationType: Bool] = [:]
    
    func initialize() async throws {
        // Mock implementation
    }
    
    func getFCMToken() async throws -> String {
        return "mock_token"
    }
    
    func hasNotificationPermission() async -> Bool {
        return hasPermissionResult
    }
    
    func requestNotificationPermission() async throws -> Bool {
        return requestPermissionResult
    }
    
    func showNotification(_ notification: AppNotification) async throws {
        showNotificationCalled = true
        if !showNotificationShouldSucceed {
            throw NotificationError.notificationCreationFailed
        }
    }
    
    func cancelNotification(withId notificationId: String) {
        // Mock implementation
    }
    
    func clearAllNotifications() {
        // Mock implementation
    }
    
    func isNotificationTypeEnabled(_ type: NotificationType) -> Bool {
        return notificationTypeStates[type] ?? true
    }
    
    func setNotificationTypeEnabled(_ type: NotificationType, enabled: Bool) {
        notificationTypeStates[type] = enabled
    }
    
    func subscribeToTopic(_ topic: String) async throws {
        // Mock implementation
    }
    
    func unsubscribeFromTopic(_ topic: String) async throws {
        // Mock implementation
    }
}