import Foundation
import UserNotifications

/**
 * Handles notification display and management for iOS
 */
class NotificationHandler: NSObject {
    
    private let notificationCenter = UNUserNotificationCenter.current()
    
    override init() {
        super.init()
        setupNotificationCategories()
    }
    
    /**
     * Handle FCM notification payload
     */
    func handleFCMNotification(_ payload: FCMNotificationPayload) {
        guard let notificationType = NotificationType(rawValue: payload.type) else {
            print("Unknown notification type: \(payload.type)")
            return
        }
        
        let notification = AppNotification(
            type: notificationType,
            title: payload.title,
            body: payload.body,
            data: payload.data
        )
        
        Task {
            try await showNotification(notification)
        }
    }
    
    /**
     * Show a local notification
     */
    func showNotification(_ notification: AppNotification) async throws {
        let content = UNMutableNotificationContent()
        content.title = notification.title
        content.body = notification.body
        content.sound = .default
        content.userInfo = notification.data
        
        // Set category for action buttons
        content.categoryIdentifier = getCategoryIdentifier(for: notification.type)
        
        // Create trigger (immediate delivery)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.1, repeats: false)
        
        // Create request
        let request = UNNotificationRequest(
            identifier: notification.id,
            content: content,
            trigger: trigger
        )
        
        try await notificationCenter.add(request)
    }
    
    /**
     * Cancel a notification by ID
     */
    func cancelNotification(withId notificationId: String) {
        notificationCenter.removePendingNotificationRequests(withIdentifiers: [notificationId])
        notificationCenter.removeDeliveredNotifications(withIdentifiers: [notificationId])
    }
    
    /**
     * Clear all notifications
     */
    func clearAllNotifications() {
        notificationCenter.removeAllPendingNotificationRequests()
        notificationCenter.removeAllDeliveredNotifications()
    }
    
    private func setupNotificationCategories() {
        // Friend Request Category
        let acceptFriendAction = UNNotificationAction(
            identifier: "ACCEPT_FRIEND_REQUEST",
            title: "Accept",
            options: [.foreground]
        )
        let declineFriendAction = UNNotificationAction(
            identifier: "DECLINE_FRIEND_REQUEST",
            title: "Decline",
            options: [.destructive]
        )
        let friendRequestCategory = UNNotificationCategory(
            identifier: "FRIEND_REQUEST_CATEGORY",
            actions: [acceptFriendAction, declineFriendAction],
            intentIdentifiers: [],
            options: []
        )
        
        // Location Sharing Request Category
        let allowLocationAction = UNNotificationAction(
            identifier: "ALLOW_LOCATION_SHARING",
            title: "Allow",
            options: [.foreground]
        )
        let denyLocationAction = UNNotificationAction(
            identifier: "DENY_LOCATION_SHARING",
            title: "Deny",
            options: [.destructive]
        )
        let locationSharingCategory = UNNotificationCategory(
            identifier: "LOCATION_SHARING_CATEGORY",
            actions: [allowLocationAction, denyLocationAction],
            intentIdentifiers: [],
            options: []
        )
        
        // General Category (no actions)
        let generalCategory = UNNotificationCategory(
            identifier: "GENERAL_CATEGORY",
            actions: [],
            intentIdentifiers: [],
            options: []
        )
        
        notificationCenter.setNotificationCategories([
            friendRequestCategory,
            locationSharingCategory,
            generalCategory
        ])
    }
    
    private func getCategoryIdentifier(for type: NotificationType) -> String {
        switch type {
        case .friendRequest:
            return "FRIEND_REQUEST_CATEGORY"
        case .locationSharingRequest:
            return "LOCATION_SHARING_CATEGORY"
        default:
            return "GENERAL_CATEGORY"
        }
    }
}

// MARK: - UNUserNotificationCenterDelegate
extension NotificationHandler: UNUserNotificationCenterDelegate {
    
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        
        switch response.actionIdentifier {
        case "ACCEPT_FRIEND_REQUEST":
            handleAcceptFriendRequest(userInfo: userInfo)
        case "DECLINE_FRIEND_REQUEST":
            handleDeclineFriendRequest(userInfo: userInfo)
        case "ALLOW_LOCATION_SHARING":
            handleAllowLocationSharing(userInfo: userInfo)
        case "DENY_LOCATION_SHARING":
            handleDenyLocationSharing(userInfo: userInfo)
        default:
            break
        }
        
        completionHandler()
    }
    
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show notification even when app is in foreground
        completionHandler([.banner, .sound, .badge])
    }
    
    private func handleAcceptFriendRequest(userInfo: [AnyHashable: Any]) {
        guard let requestId = userInfo["request_id"] as? String,
              let fromUserId = userInfo["from_user_id"] as? String else {
            print("Missing required data for friend request acceptance")
            return
        }
        
        print("Accepting friend request: \(requestId) from user: \(fromUserId)")
        // TODO: Implement friend request acceptance logic
        // This will be connected to the FriendsRepository when available
    }
    
    private func handleDeclineFriendRequest(userInfo: [AnyHashable: Any]) {
        guard let requestId = userInfo["request_id"] as? String,
              let fromUserId = userInfo["from_user_id"] as? String else {
            print("Missing required data for friend request decline")
            return
        }
        
        print("Declining friend request: \(requestId) from user: \(fromUserId)")
        // TODO: Implement friend request decline logic
        // This will be connected to the FriendsRepository when available
    }
    
    private func handleAllowLocationSharing(userInfo: [AnyHashable: Any]) {
        guard let requestId = userInfo["request_id"] as? String,
              let fromUserId = userInfo["from_user_id"] as? String else {
            print("Missing required data for location sharing permission")
            return
        }
        
        print("Allowing location sharing: \(requestId) from user: \(fromUserId)")
        // TODO: Implement location sharing permission logic
        // This will be connected to the LocationSharingManager when available
    }
    
    private func handleDenyLocationSharing(userInfo: [AnyHashable: Any]) {
        guard let requestId = userInfo["request_id"] as? String,
              let fromUserId = userInfo["from_user_id"] as? String else {
            print("Missing required data for location sharing denial")
            return
        }
        
        print("Denying location sharing: \(requestId) from user: \(fromUserId)")
        // TODO: Implement location sharing denial logic
        // This will be connected to the LocationSharingManager when available
    }
}