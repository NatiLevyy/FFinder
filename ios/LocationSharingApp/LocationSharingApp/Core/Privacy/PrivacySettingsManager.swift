import Foundation

/**
 * Manages privacy settings and data retention policies.
 */
class PrivacySettingsManager {
    
    // MARK: - Constants
    
    private struct Constants {
        static let privacySettingsKey = "privacy_settings"
        static let defaultRetentionDays = 90
    }
    
    // MARK: - Singleton
    
    static let shared = PrivacySettingsManager()
    
    // MARK: - Private Properties
    
    private let secureStorage = SecureStorage.shared
    
    // MARK: - Initialization
    
    private init() {}
    
    // MARK: - Public Methods
    
    /**
     * Gets the current privacy settings.
     * 
     * - Returns: PrivacySettings object with current settings
     */
    func getPrivacySettings() async -> PrivacySettings {
        do {
            if let settingsJson = try await secureStorage.retrieve(key: Constants.privacySettingsKey),
               let settingsData = settingsJson.data(using: .utf8) {
                let settings = try JSONDecoder().decode(PrivacySettings.self, from: settingsData)
                return settings
            } else {
                return getDefaultPrivacySettings()
            }
        } catch {
            return getDefaultPrivacySettings()
        }
    }
    
    /**
     * Updates privacy settings.
     * 
     * - Parameter settings: The new privacy settings
     * - Returns: Result indicating success or failure
     */
    func updatePrivacySettings(_ settings: PrivacySettings) async -> Result<Void, Error> {
        do {
            let updatedSettings = PrivacySettings(
                locationDataRetentionDays: settings.locationDataRetentionDays,
                shareLocationWithFriends: settings.shareLocationWithFriends,
                allowFriendRequests: settings.allowFriendRequests,
                showOnlineStatus: settings.showOnlineStatus,
                dataProcessingConsent: settings.dataProcessingConsent,
                marketingConsent: settings.marketingConsent,
                lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
            )
            
            let settingsData = try JSONEncoder().encode(updatedSettings)
            let settingsJson = String(data: settingsData, encoding: .utf8) ?? ""
            
            try await secureStorage.store(key: Constants.privacySettingsKey, value: settingsJson)
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    /**
     * Updates location data retention period.
     * 
     * - Parameter days: Number of days to retain location data
     * - Returns: Result indicating success or failure
     */
    func updateLocationDataRetention(days: Int) async -> Result<Void, Error> {
        guard days >= 1 && days <= 365 else {
            return .failure(PrivacySettingsError.invalidRetentionPeriod)
        }
        
        let currentSettings = await getPrivacySettings()
        let updatedSettings = PrivacySettings(
            locationDataRetentionDays: days,
            shareLocationWithFriends: currentSettings.shareLocationWithFriends,
            allowFriendRequests: currentSettings.allowFriendRequests,
            showOnlineStatus: currentSettings.showOnlineStatus,
            dataProcessingConsent: currentSettings.dataProcessingConsent,
            marketingConsent: currentSettings.marketingConsent,
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        return await updatePrivacySettings(updatedSettings)
    }
    
    /**
     * Updates location sharing preference.
     * 
     * - Parameter enabled: Whether to share location with friends
     * - Returns: Result indicating success or failure
     */
    func updateLocationSharingPreference(enabled: Bool) async -> Result<Void, Error> {
        let currentSettings = await getPrivacySettings()
        let updatedSettings = PrivacySettings(
            locationDataRetentionDays: currentSettings.locationDataRetentionDays,
            shareLocationWithFriends: enabled,
            allowFriendRequests: currentSettings.allowFriendRequests,
            showOnlineStatus: currentSettings.showOnlineStatus,
            dataProcessingConsent: currentSettings.dataProcessingConsent,
            marketingConsent: currentSettings.marketingConsent,
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        return await updatePrivacySettings(updatedSettings)
    }
    
    /**
     * Updates friend request preference.
     * 
     * - Parameter enabled: Whether to allow friend requests
     * - Returns: Result indicating success or failure
     */
    func updateFriendRequestPreference(enabled: Bool) async -> Result<Void, Error> {
        let currentSettings = await getPrivacySettings()
        let updatedSettings = PrivacySettings(
            locationDataRetentionDays: currentSettings.locationDataRetentionDays,
            shareLocationWithFriends: currentSettings.shareLocationWithFriends,
            allowFriendRequests: enabled,
            showOnlineStatus: currentSettings.showOnlineStatus,
            dataProcessingConsent: currentSettings.dataProcessingConsent,
            marketingConsent: currentSettings.marketingConsent,
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        return await updatePrivacySettings(updatedSettings)
    }
    
    /**
     * Updates online status visibility preference.
     * 
     * - Parameter enabled: Whether to show online status to friends
     * - Returns: Result indicating success or failure
     */
    func updateOnlineStatusVisibility(enabled: Bool) async -> Result<Void, Error> {
        let currentSettings = await getPrivacySettings()
        let updatedSettings = PrivacySettings(
            locationDataRetentionDays: currentSettings.locationDataRetentionDays,
            shareLocationWithFriends: currentSettings.shareLocationWithFriends,
            allowFriendRequests: currentSettings.allowFriendRequests,
            showOnlineStatus: enabled,
            dataProcessingConsent: currentSettings.dataProcessingConsent,
            marketingConsent: currentSettings.marketingConsent,
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        return await updatePrivacySettings(updatedSettings)
    }
    
    /**
     * Updates data processing consent.
     * 
     * - Parameter consent: Whether user consents to data processing
     * - Returns: Result indicating success or failure
     */
    func updateDataProcessingConsent(consent: Bool) async -> Result<Void, Error> {
        let currentSettings = await getPrivacySettings()
        let updatedSettings = PrivacySettings(
            locationDataRetentionDays: currentSettings.locationDataRetentionDays,
            shareLocationWithFriends: currentSettings.shareLocationWithFriends,
            allowFriendRequests: currentSettings.allowFriendRequests,
            showOnlineStatus: currentSettings.showOnlineStatus,
            dataProcessingConsent: consent,
            marketingConsent: currentSettings.marketingConsent,
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        return await updatePrivacySettings(updatedSettings)
    }
    
    /**
     * Updates marketing consent.
     * 
     * - Parameter consent: Whether user consents to marketing communications
     * - Returns: Result indicating success or failure
     */
    func updateMarketingConsent(consent: Bool) async -> Result<Void, Error> {
        let currentSettings = await getPrivacySettings()
        let updatedSettings = PrivacySettings(
            locationDataRetentionDays: currentSettings.locationDataRetentionDays,
            shareLocationWithFriends: currentSettings.shareLocationWithFriends,
            allowFriendRequests: currentSettings.allowFriendRequests,
            showOnlineStatus: currentSettings.showOnlineStatus,
            dataProcessingConsent: currentSettings.dataProcessingConsent,
            marketingConsent: consent,
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        return await updatePrivacySettings(updatedSettings)
    }
    
    /**
     * Resets privacy settings to defaults.
     * 
     * - Returns: Result indicating success or failure
     */
    func resetToDefaults() async -> Result<Void, Error> {
        let defaultSettings = getDefaultPrivacySettings()
        return await updatePrivacySettings(defaultSettings)
    }
    
    /**
     * Gets privacy policy compliance information.
     * 
     * - Returns: PrivacyComplianceInfo object
     */
    func getPrivacyComplianceInfo() async -> PrivacyComplianceInfo {
        let settings = await getPrivacySettings()
        
        return PrivacyComplianceInfo(
            dataProcessingConsentGiven: settings.dataProcessingConsent,
            dataProcessingConsentDate: settings.dataProcessingConsent ? settings.lastUpdated : nil,
            marketingConsentGiven: settings.marketingConsent,
            marketingConsentDate: settings.marketingConsent ? settings.lastUpdated : nil,
            locationDataRetentionDays: settings.locationDataRetentionDays,
            privacyPolicyVersion: "1.0",
            privacyPolicyAcceptedDate: settings.lastUpdated,
            gdprCompliant: true,
            ccpaCompliant: true
        )
    }
    
    /**
     * Checks if location data should be retained based on settings.
     * 
     * - Parameter dataTimestamp: Timestamp of the location data
     * - Returns: True if data should be retained, false if it should be deleted
     */
    func shouldRetainLocationData(dataTimestamp: Int64) async -> Bool {
        let settings = await getPrivacySettings()
        let retentionPeriodMs = Int64(settings.locationDataRetentionDays * 24 * 60 * 60 * 1000)
        let cutoffTime = Int64(Date().timeIntervalSince1970 * 1000) - retentionPeriodMs
        
        return dataTimestamp >= cutoffTime
    }
    
    /**
     * Gets data retention summary.
     * 
     * - Returns: DataRetentionSummary object
     */
    func getDataRetentionSummary() async -> DataRetentionSummary {
        let settings = await getPrivacySettings()
        let retentionPeriodMs = Int64(settings.locationDataRetentionDays * 24 * 60 * 60 * 1000)
        let cutoffTime = Int64(Date().timeIntervalSince1970 * 1000) - retentionPeriodMs
        let nextCleanupDate = Int64(Date().timeIntervalSince1970 * 1000) + (24 * 60 * 60 * 1000) // Next day
        
        return DataRetentionSummary(
            locationDataRetentionDays: settings.locationDataRetentionDays,
            locationDataCutoffDate: cutoffTime,
            automaticCleanupEnabled: true,
            nextCleanupDate: nextCleanupDate
        )
    }
    
    // MARK: - Private Methods
    
    private func getDefaultPrivacySettings() -> PrivacySettings {
        return PrivacySettings(
            locationDataRetentionDays: Constants.defaultRetentionDays,
            shareLocationWithFriends: true,
            allowFriendRequests: true,
            showOnlineStatus: true,
            dataProcessingConsent: false, // Must be explicitly granted
            marketingConsent: false, // Must be explicitly granted
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
    }
}

// MARK: - Data Structures

struct PrivacySettings: Codable {
    let locationDataRetentionDays: Int
    let shareLocationWithFriends: Bool
    let allowFriendRequests: Bool
    let showOnlineStatus: Bool
    let dataProcessingConsent: Bool
    let marketingConsent: Bool
    let lastUpdated: Int64
}

struct PrivacyComplianceInfo {
    let dataProcessingConsentGiven: Bool
    let dataProcessingConsentDate: Int64?
    let marketingConsentGiven: Bool
    let marketingConsentDate: Int64?
    let locationDataRetentionDays: Int
    let privacyPolicyVersion: String
    let privacyPolicyAcceptedDate: Int64
    let gdprCompliant: Bool
    let ccpaCompliant: Bool
}

struct DataRetentionSummary {
    let locationDataRetentionDays: Int
    let locationDataCutoffDate: Int64
    let automaticCleanupEnabled: Bool
    let nextCleanupDate: Int64
}

// MARK: - Error Types

enum PrivacySettingsError: Error, LocalizedError {
    case invalidRetentionPeriod
    case settingsUpdateFailed
    
    var errorDescription: String? {
        switch self {
        case .invalidRetentionPeriod:
            return "Retention period must be between 1 and 365 days"
        case .settingsUpdateFailed:
            return "Failed to update privacy settings"
        }
    }
}