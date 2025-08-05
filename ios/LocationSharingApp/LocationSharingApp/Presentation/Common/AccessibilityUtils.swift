import SwiftUI
import UIKit

struct AccessibilityUtils {
    
    /**
     * Checks if VoiceOver is enabled.
     */
    static var isVoiceOverEnabled: Bool {
        return UIAccessibility.isVoiceOverRunning
    }
    
    /**
     * Checks if any accessibility features are enabled.
     */
    static var isAccessibilityEnabled: Bool {
        return UIAccessibility.isVoiceOverRunning ||
               UIAccessibility.isSwitchControlRunning ||
               UIAccessibility.isAssistiveTouchRunning
    }
    
    /**
     * Posts an accessibility announcement.
     */
    static func announce(_ message: String) {
        if isVoiceOverEnabled {
            UIAccessibility.post(notification: .announcement, argument: message)
        }
    }
    
    /**
     * Posts a screen changed notification.
     */
    static func screenChanged(focusOn element: Any? = nil) {
        UIAccessibility.post(notification: .screenChanged, argument: element)
    }
    
    /**
     * Posts a layout changed notification.
     */
    static func layoutChanged(focusOn element: Any? = nil) {
        UIAccessibility.post(notification: .layoutChanged, argument: element)
    }
}

// MARK: - View Modifiers for Accessibility

extension View {
    
    /**
     * Sets up accessibility for clickable elements.
     */
    func accessibleButton(
        label: String,
        hint: String? = nil,
        traits: AccessibilityTraits = .isButton
    ) -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityHint(hint ?? "")
            .accessibilityAddTraits(traits)
    }
    
    /**
     * Sets up accessibility for toggle elements.
     */
    func accessibleToggle(
        label: String,
        isOn: Bool,
        hint: String? = nil
    ) -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityValue(isOn ? "On" : "Off")
            .accessibilityHint(hint ?? "Double tap to toggle")
            .accessibilityAddTraits(.isButton)
    }
    
    /**
     * Sets up accessibility for list items.
     */
    func accessibleListItem(
        label: String,
        position: Int,
        totalItems: Int,
        hint: String? = nil
    ) -> some View {
        self
            .accessibilityLabel("\(label), item \(position + 1) of \(totalItems)")
            .accessibilityHint(hint ?? "")
            .accessibilityAddTraits(.isButton)
    }
    
    /**
     * Sets up accessibility for progress indicators.
     */
    func accessibleProgress(
        label: String,
        value: Double,
        range: ClosedRange<Double> = 0...1
    ) -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityValue("\(Int(value * 100))%")
            .accessibilityAddTraits(.updatesFrequently)
    }
    
    /**
     * Sets up accessibility for map elements.
     */
    func accessibleMapElement(
        label: String,
        hint: String? = nil
    ) -> some View {
        self
            .accessibilityLabel(label)
            .accessibilityHint(hint ?? "")
            .accessibilityAddTraits(.isButton)
    }
    
    /**
     * Sets up accessibility for form fields.
     */
    func accessibleFormField(
        label: String,
        hint: String? = nil,
        isRequired: Bool = false,
        hasError: Bool = false,
        errorMessage: String? = nil
    ) -> some View {
        var accessibilityLabel = label
        if isRequired {
            accessibilityLabel += ", required"
        }
        if hasError, let error = errorMessage {
            accessibilityLabel += ", error: \(error)"
        }
        
        return self
            .accessibilityLabel(accessibilityLabel)
            .accessibilityHint(hint ?? "")
    }
    
    /**
     * Groups accessibility elements.
     */
    func accessibilityGroup(
        label: String? = nil,
        hint: String? = nil
    ) -> some View {
        self
            .accessibilityElement(children: .combine)
            .accessibilityLabel(label ?? "")
            .accessibilityHint(hint ?? "")
    }
    
    /**
     * Announces changes for dynamic content.
     */
    func announceChanges(_ message: String, when condition: Bool) -> some View {
        self
            .onChange(of: condition) { _ in
                if AccessibilityUtils.isVoiceOverEnabled {
                    AccessibilityUtils.announce(message)
                }
            }
    }
}

// MARK: - Accessibility Constants

struct AccessibilityIdentifiers {
    // Authentication
    static let loginEmailField = "login_email_field"
    static let loginPasswordField = "login_password_field"
    static let loginButton = "login_button"
    static let registerButton = "register_button"
    
    // Map
    static let mapView = "map_view"
    static let userLocationButton = "user_location_button"
    static let friendMarker = "friend_marker"
    
    // Friends
    static let friendsList = "friends_list"
    static let addFriendButton = "add_friend_button"
    static let friendCell = "friend_cell"
    static let locationSharingToggle = "location_sharing_toggle"
    
    // Settings
    static let notificationToggle = "notification_toggle"
    static let privacySettings = "privacy_settings"
    static let logoutButton = "logout_button"
    
    // Onboarding
    static let onboardingPage = "onboarding_page"
    static let skipButton = "skip_button"
    static let nextButton = "next_button"
    static let getStartedButton = "get_started_button"
}

struct AccessibilityLabels {
    // Map
    static let userLocation = "Your current location"
    static let friendLocation = "Friend's location"
    static let mapCenterButton = "Center map on your location"
    
    // Friends
    static let friendOnline = "Friend is online"
    static let friendOffline = "Friend is offline"
    static let locationSharingEnabled = "Location sharing is enabled"
    static let locationSharingDisabled = "Location sharing is disabled"
    
    // Loading
    static let loadingIndicator = "Loading"
    static let loadingMessage = "Please wait while we load your data"
    
    // Errors
    static let errorAlert = "Error occurred"
    static let networkError = "Network connection error"
    static let locationError = "Location access error"
}