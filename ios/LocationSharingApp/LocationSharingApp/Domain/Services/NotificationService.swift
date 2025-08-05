import Foundation
import UserNotifications

/**
 * Protocol for managing notifications
 */
protocol NotificationService {
    
    /**
     * Initialize the notification service and request permissions
     */
    func initialize() async throws
    
    /**
     * Get the FCM token for this device
     */
    func getFCMToken() async throws -> String
    
    /**
     * Check if notification permissions are granted
     */
    func hasNotificationPermission() async -> Bool
    
    /**
     * Request notification permissions from the user
     */
    func requestNotificationPermission() async throws -> Bool
    
    /**
     * Show a local notification
     */
    func showNotification(_ notification: AppNotification) async throws
    
    /**
     * Cancel a notification by ID
     */
    func cancelNotification(withId notificationId: String)
    
    /**
     * Clear all notifications
     */
    func clearAllNotifications()
    
    /**
     * Check if notifications are enabled for a specific type
     */
    func isNotificationTypeEnabled(_ type: NotificationType) -> Bool
    
    /**
     * Enable/disable notifications for a specific type
     */
    func setNotificationTypeEnabled(_ type: NotificationType, enabled: Bool)
    
    /**
     * Subscribe to a topic for receiving notifications
     */
    func subscribeToTopic(_ topic: String) async throws
    
    /**
     * Unsubscribe from a topic
     */
    func unsubscribeFromTopic(_ topic: String) async throws
}