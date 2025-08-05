import Foundation
import UserNotifications

/**
 * Implementation of NotificationService for iOS using UserNotifications framework
 * Note: Firebase Messaging integration would be added here when Firebase SDK is configured
 */
class NotificationServiceImpl: NotificationService {
    
    private let notificationCenter = UNUserNotificationCenter.current()
    private let notificationHandler = NotificationHandler()
    private let userDefaults = UserDefaults.standard
    
    private struct Keys {
        static let fcmToken = "fcm_token"
        static let notificationPrefix = "notification_enabled_"
    }
    
    init() {
        notificationCenter.delegate = notificationHandler
        initializeDefaultPreferences()
    }
    
    func initialize() async throws {
        // Request notification permissions
        let granted = try await requestNotificationPermission()
        if !granted {
            throw NotificationError.permissionDenied
        }
        
        // TODO: Initialize Firebase Messaging when SDK is configured
        // let token = try await getFCMToken()
        // print("FCM initialized with token: \(token)")
        
        print("Notification service initialized successfully")
    }
    
    func getFCMToken() async throws -> String {
        // TODO: Implement Firebase Messaging token retrieval
        // For now, return a mock token
        let mockToken = "mock_fcm_token_\(UUID().uuidString)"
        userDefaults.set(mockToken, forKey: Keys.fcmToken)
        return mockToken
    }
    
    func hasNotificationPermission() async -> Bool {
        let settings = await notificationCenter.notificationSettings()
        return settings.authorizationStatus == .authorized
    }
    
    func requestNotificationPermission() async throws -> Bool {
        let options: UNAuthorizationOptions = [.alert, .sound, .badge]
        let granted = try await notificationCenter.requestAuthorization(options: options)
        return granted
    }
    
    func showNotification(_ notification: AppNotification) async throws {
        guard isNotificationTypeEnabled(notification.type) else {
            print("Notification type \(notification.type.rawValue) is disabled")
            return
        }
        
        guard await hasNotificationPermission() else {
            throw NotificationError.permissionDenied
        }
        
        try await notificationHandler.showNotification(notification)
    }
    
    func cancelNotification(withId notificationId: String) {
        notificationHandler.cancelNotification(withId: notificationId)
    }
    
    func clearAllNotifications() {
        notificationHandler.clearAllNotifications()
    }
    
    func isNotificationTypeEnabled(_ type: NotificationType) -> Bool {
        let key = Keys.notificationPrefix + type.rawValue
        return userDefaults.bool(forKey: key)
    }
    
    func setNotificationTypeEnabled(_ type: NotificationType, enabled: Bool) {
        let key = Keys.notificationPrefix + type.rawValue
        userDefaults.set(enabled, forKey: key)
        print("Notification type \(type.rawValue) set to: \(enabled)")
    }
    
    func subscribeToTopic(_ topic: String) async throws {
        // TODO: Implement Firebase Messaging topic subscription
        print("Subscribed to topic: \(topic)")
    }
    
    func unsubscribeFromTopic(_ topic: String) async throws {
        // TODO: Implement Firebase Messaging topic unsubscription
        print("Unsubscribed from topic: \(topic)")
    }
    
    private func initializeDefaultPreferences() {
        // Set default notification preferences if not already set
        for type in NotificationType.allCases {
            let key = Keys.notificationPrefix + type.rawValue
            if userDefaults.object(forKey: key) == nil {
                userDefaults.set(true, forKey: key)
            }
        }
    }
    
    /**
     * Get the stored FCM token
     */
    func getStoredFCMToken() -> String? {
        return userDefaults.string(forKey: Keys.fcmToken)
    }
}

/**
 * Errors that can occur in the notification service
 */
enum NotificationError: Error, LocalizedError {
    case permissionDenied
    case tokenRetrievalFailed
    case notificationCreationFailed
    
    var errorDescription: String? {
        switch self {
        case .permissionDenied:
            return "Notification permission denied"
        case .tokenRetrievalFailed:
            return "Failed to retrieve FCM token"
        case .notificationCreationFailed:
            return "Failed to create notification"
        }
    }
}