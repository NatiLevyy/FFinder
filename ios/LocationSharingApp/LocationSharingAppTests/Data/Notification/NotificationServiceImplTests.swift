import XCTest
import UserNotifications
@testable import LocationSharingApp

class NotificationServiceImplTests: XCTestCase {
    
    var notificationService: NotificationServiceImpl!
    
    override func setUp() {
        super.setUp()
        notificationService = NotificationServiceImpl()
    }
    
    override func tearDown() {
        notificationService = nil
        super.tearDown()
    }
    
    func testGetFCMToken_ReturnsValidToken() async throws {
        // When
        let token = try await notificationService.getFCMToken()
        
        // Then
        XCTAssertFalse(token.isEmpty)
        XCTAssertTrue(token.contains("mock_fcm_token"))
    }
    
    func testIsNotificationTypeEnabled_DefaultsToTrue() {
        // Given
        let type = NotificationType.friendRequest
        
        // When
        let isEnabled = notificationService.isNotificationTypeEnabled(type)
        
        // Then
        XCTAssertTrue(isEnabled)
    }
    
    func testSetNotificationTypeEnabled_UpdatesPreference() {
        // Given
        let type = NotificationType.locationSharingRequest
        let enabled = false
        
        // When
        notificationService.setNotificationTypeEnabled(type, enabled: enabled)
        
        // Then
        let result = notificationService.isNotificationTypeEnabled(type)
        XCTAssertEqual(result, enabled)
    }
    
    func testGetStoredFCMToken_ReturnsNilInitially() {
        // When
        let token = notificationService.getStoredFCMToken()
        
        // Then
        XCTAssertNil(token)
    }
    
    func testGetStoredFCMToken_ReturnsTokenAfterRetrieval() async throws {
        // Given
        let expectedToken = try await notificationService.getFCMToken()
        
        // When
        let storedToken = notificationService.getStoredFCMToken()
        
        // Then
        XCTAssertEqual(storedToken, expectedToken)
    }
    
    func testShowNotification_WithDisabledType_DoesNotThrow() async {
        // Given
        let notification = AppNotification(
            type: .friendRequest,
            title: "Test Title",
            body: "Test Body"
        )
        notificationService.setNotificationTypeEnabled(.friendRequest, enabled: false)
        
        // When & Then
        do {
            try await notificationService.showNotification(notification)
            // Should not throw an error, just skip showing the notification
        } catch {
            XCTFail("Should not throw error when notification type is disabled")
        }
    }
    
    func testAllNotificationTypes_HaveDefaultPreferences() {
        // Given & When & Then
        for type in NotificationType.allCases {
            let isEnabled = notificationService.isNotificationTypeEnabled(type)
            XCTAssertTrue(isEnabled, "Notification type \(type.rawValue) should be enabled by default")
        }
    }
}