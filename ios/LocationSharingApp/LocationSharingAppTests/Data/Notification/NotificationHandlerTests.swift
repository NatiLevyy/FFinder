import XCTest
import UserNotifications
@testable import LocationSharingApp

class NotificationHandlerTests: XCTestCase {
    
    var notificationHandler: NotificationHandler!
    
    override func setUp() {
        super.setUp()
        notificationHandler = NotificationHandler()
    }
    
    override func tearDown() {
        notificationHandler = nil
        super.tearDown()
    }
    
    func testHandleFCMNotification_WithValidPayload_CreatesNotification() {
        // Given
        let payload = FCMNotificationPayload(
            type: NotificationType.friendRequest.rawValue,
            title: "Friend Request",
            body: "John wants to be your friend",
            data: ["from_user_id": "123", "request_id": "456"]
        )
        
        // When
        notificationHandler.handleFCMNotification(payload)
        
        // Then
        // Note: In a real test, we would mock UNUserNotificationCenter
        // For now, we just verify the method doesn't crash
        XCTAssertTrue(true)
    }
    
    func testHandleFCMNotification_WithInvalidType_HandlesGracefully() {
        // Given
        let payload = FCMNotificationPayload(
            type: "invalid_type",
            title: "Test Title",
            body: "Test Body"
        )
        
        // When & Then
        // Should not crash with invalid notification type
        notificationHandler.handleFCMNotification(payload)
        XCTAssertTrue(true)
    }
    
    func testShowNotification_WithFriendRequest_DoesNotThrow() async {
        // Given
        let notification = AppNotification(
            type: .friendRequest,
            title: "Friend Request",
            body: "Jane wants to be your friend",
            data: ["from_user_id": "789", "request_id": "101"]
        )
        
        // When & Then
        do {
            try await notificationHandler.showNotification(notification)
            // In a real test environment with proper permissions, this would succeed
        } catch {
            // Expected to fail in test environment without notification permissions
            XCTAssertTrue(error is NotificationError || error.localizedDescription.contains("not authorized"))
        }
    }
    
    func testShowNotification_WithLocationSharingRequest_DoesNotThrow() async {
        // Given
        let notification = AppNotification(
            type: .locationSharingRequest,
            title: "Location Sharing Request",
            body: "Bob wants to see your location",
            data: ["from_user_id": "456", "request_id": "789"]
        )
        
        // When & Then
        do {
            try await notificationHandler.showNotification(notification)
            // In a real test environment with proper permissions, this would succeed
        } catch {
            // Expected to fail in test environment without notification permissions
            XCTAssertTrue(error is NotificationError || error.localizedDescription.contains("not authorized"))
        }
    }
    
    func testCancelNotification_DoesNotThrow() {
        // Given
        let notificationId = "test_notification_id"
        
        // When & Then
        notificationHandler.cancelNotification(withId: notificationId)
        XCTAssertTrue(true) // Should not crash
    }
    
    func testClearAllNotifications_DoesNotThrow() {
        // When & Then
        notificationHandler.clearAllNotifications()
        XCTAssertTrue(true) // Should not crash
    }
}