import Foundation

/**
 * Struct representing a notification in the app
 */
struct AppNotification: Identifiable, Codable {
    let id: String
    let type: NotificationType
    let title: String
    let body: String
    let data: [String: String]
    let timestamp: Date
    let isRead: Bool
    
    init(
        id: String = UUID().uuidString,
        type: NotificationType,
        title: String,
        body: String,
        data: [String: String] = [:],
        timestamp: Date = Date(),
        isRead: Bool = false
    ) {
        self.id = id
        self.type = type
        self.title = title
        self.body = body
        self.data = data
        self.timestamp = timestamp
        self.isRead = isRead
    }
}

/**
 * Struct for FCM notification payload
 */
struct FCMNotificationPayload: Codable {
    let type: String
    let title: String
    let body: String
    let data: [String: String]
    
    init(type: String, title: String, body: String, data: [String: String] = [:]) {
        self.type = type
        self.title = title
        self.body = body
        self.data = data
    }
}