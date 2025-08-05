import Foundation
import Combine

/**
 * ViewModel for managing notification settings in iOS
 */
@MainActor
class NotificationSettingsViewModel: ObservableObject {
    
    @Published var hasPermission = false
    @Published var friendRequestEnabled = true
    @Published var friendRequestAcceptedEnabled = true
    @Published var locationSharingRequestEnabled = true
    @Published var locationSharingGrantedEnabled = true
    @Published var locationSharingRevokedEnabled = true
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    
    private let notificationService: NotificationService
    
    init(notificationService: NotificationService = NotificationServiceImpl()) {
        self.notificationService = notificationService
    }
    
    func loadSettings() async {
        isLoading = true
        
        do {
            // Check permission status
            hasPermission = await notificationService.hasNotificationPermission()
            
            // Load notification type preferences
            friendRequestEnabled = notificationService.isNotificationTypeEnabled(.friendRequest)
            friendRequestAcceptedEnabled = notificationService.isNotificationTypeEnabled(.friendRequestAccepted)
            locationSharingRequestEnabled = notificationService.isNotificationTypeEnabled(.locationSharingRequest)
            locationSharingGrantedEnabled = notificationService.isNotificationTypeEnabled(.locationSharingGranted)
            locationSharingRevokedEnabled = notificationService.isNotificationTypeEnabled(.locationSharingRevoked)
            
        } catch {
            errorMessage = "Failed to load notification settings: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func requestPermission() async {
        do {
            let granted = try await notificationService.requestNotificationPermission()
            hasPermission = granted
            
            if granted {
                successMessage = "Notification permission granted!"
            } else {
                errorMessage = "Notification permission denied. Some features may not work properly."
            }
        } catch {
            errorMessage = "Failed to request notification permission: \(error.localizedDescription)"
        }
    }
    
    func sendTestNotification() async {
        guard hasPermission else {
            errorMessage = "Notification permission is required to send test notifications"
            return
        }
        
        isLoading = true
        
        do {
            let testNotification = AppNotification(
                type: .friendRequest,
                title: "Test Notification",
                body: "This is a test notification to verify your settings are working correctly.",
                data: ["test": "true"]
            )
            
            try await notificationService.showNotification(testNotification)
            successMessage = "Test notification sent successfully!"
            
        } catch {
            errorMessage = "Failed to send test notification: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    func clearError() {
        errorMessage = nil
    }
    
    func clearSuccess() {
        successMessage = nil
    }
    
    // MARK: - Notification Type Setters
    
    func setFriendRequestEnabled(_ enabled: Bool) {
        friendRequestEnabled = enabled
        notificationService.setNotificationTypeEnabled(.friendRequest, enabled: enabled)
    }
    
    func setFriendRequestAcceptedEnabled(_ enabled: Bool) {
        friendRequestAcceptedEnabled = enabled
        notificationService.setNotificationTypeEnabled(.friendRequestAccepted, enabled: enabled)
    }
    
    func setLocationSharingRequestEnabled(_ enabled: Bool) {
        locationSharingRequestEnabled = enabled
        notificationService.setNotificationTypeEnabled(.locationSharingRequest, enabled: enabled)
    }
    
    func setLocationSharingGrantedEnabled(_ enabled: Bool) {
        locationSharingGrantedEnabled = enabled
        notificationService.setNotificationTypeEnabled(.locationSharingGranted, enabled: enabled)
    }
    
    func setLocationSharingRevokedEnabled(_ enabled: Bool) {
        locationSharingRevokedEnabled = enabled
        notificationService.setNotificationTypeEnabled(.locationSharingRevoked, enabled: enabled)
    }
}

// MARK: - Binding Extensions

extension NotificationSettingsViewModel {
    var friendRequestEnabledBinding: Binding<Bool> {
        Binding(
            get: { self.friendRequestEnabled },
            set: { self.setFriendRequestEnabled($0) }
        )
    }
    
    var friendRequestAcceptedEnabledBinding: Binding<Bool> {
        Binding(
            get: { self.friendRequestAcceptedEnabled },
            set: { self.setFriendRequestAcceptedEnabled($0) }
        )
    }
    
    var locationSharingRequestEnabledBinding: Binding<Bool> {
        Binding(
            get: { self.locationSharingRequestEnabled },
            set: { self.setLocationSharingRequestEnabled($0) }
        )
    }
    
    var locationSharingGrantedEnabledBinding: Binding<Bool> {
        Binding(
            get: { self.locationSharingGrantedEnabled },
            set: { self.setLocationSharingGrantedEnabled($0) }
        )
    }
    
    var locationSharingRevokedEnabledBinding: Binding<Bool> {
        Binding(
            get: { self.locationSharingRevokedEnabled },
            set: { self.setLocationSharingRevokedEnabled($0) }
        )
    }
}