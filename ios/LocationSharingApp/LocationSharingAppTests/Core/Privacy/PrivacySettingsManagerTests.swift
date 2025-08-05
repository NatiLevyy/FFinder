import XCTest
@testable import LocationSharingApp

class PrivacySettingsManagerTests: XCTestCase {
    
    var privacySettingsManager: PrivacySettingsManager!
    var secureStorage: SecureStorage!
    
    override func setUp() {
        super.setUp()
        secureStorage = SecureStorage.shared
        privacySettingsManager = PrivacySettingsManager.shared
    }
    
    override func tearDown() {
        Task {
            try await secureStorage.clear()
        }
        super.tearDown()
    }
    
    func testGetPrivacySettingsReturnsDefaultSettingsWhenNoneExist() async {
        // When
        let settings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        XCTAssertEqual(90, settings.locationDataRetentionDays)
        XCTAssertTrue(settings.shareLocationWithFriends)
        XCTAssertTrue(settings.allowFriendRequests)
        XCTAssertTrue(settings.showOnlineStatus)
        XCTAssertFalse(settings.dataProcessingConsent)
        XCTAssertFalse(settings.marketingConsent)
        XCTAssertTrue(settings.lastUpdated > 0)
    }
    
    func testUpdatePrivacySettingsStoresAndRetrievesSettingsCorrectly() async {
        // Given
        let newSettings = PrivacySettings(
            locationDataRetentionDays: 30,
            shareLocationWithFriends: false,
            allowFriendRequests: false,
            showOnlineStatus: false,
            dataProcessingConsent: true,
            marketingConsent: true,
            lastUpdated: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        // When
        let updateResult = await privacySettingsManager.updatePrivacySettings(newSettings)
        let retrievedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch updateResult {
        case .success:
            XCTAssertEqual(30, retrievedSettings.locationDataRetentionDays)
            XCTAssertFalse(retrievedSettings.shareLocationWithFriends)
            XCTAssertFalse(retrievedSettings.allowFriendRequests)
            XCTAssertFalse(retrievedSettings.showOnlineStatus)
            XCTAssertTrue(retrievedSettings.dataProcessingConsent)
            XCTAssertTrue(retrievedSettings.marketingConsent)
        case .failure(let error):
            XCTFail("Update should have succeeded: \(error)")
        }
    }
    
    func testUpdateLocationDataRetentionUpdatesOnlyRetentionPeriod() async {
        // Given
        let originalSettings = await privacySettingsManager.getPrivacySettings()
        
        // When
        let updateResult = await privacySettingsManager.updateLocationDataRetention(days: 60)
        let updatedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch updateResult {
        case .success:
            XCTAssertEqual(60, updatedSettings.locationDataRetentionDays)
            XCTAssertEqual(originalSettings.shareLocationWithFriends, updatedSettings.shareLocationWithFriends)
            XCTAssertEqual(originalSettings.allowFriendRequests, updatedSettings.allowFriendRequests)
            XCTAssertEqual(originalSettings.showOnlineStatus, updatedSettings.showOnlineStatus)
            XCTAssertEqual(originalSettings.dataProcessingConsent, updatedSettings.dataProcessingConsent)
            XCTAssertEqual(originalSettings.marketingConsent, updatedSettings.marketingConsent)
        case .failure(let error):
            XCTFail("Update should have succeeded: \(error)")
        }
    }
    
    func testUpdateLocationDataRetentionRejectsInvalidValues() async {
        // When & Then
        let result1 = await privacySettingsManager.updateLocationDataRetention(days: 0)
        switch result1 {
        case .success:
            XCTFail("Should have failed for invalid value")
        case .failure:
            break // Expected
        }
        
        let result2 = await privacySettingsManager.updateLocationDataRetention(days: 366)
        switch result2 {
        case .success:
            XCTFail("Should have failed for invalid value")
        case .failure:
            break // Expected
        }
        
        let result3 = await privacySettingsManager.updateLocationDataRetention(days: -1)
        switch result3 {
        case .success:
            XCTFail("Should have failed for invalid value")
        case .failure:
            break // Expected
        }
    }
    
    func testUpdateLocationSharingPreferenceUpdatesOnlyLocationSharingSetting() async {
        // Given
        let originalSettings = await privacySettingsManager.getPrivacySettings()
        
        // When
        let updateResult = await privacySettingsManager.updateLocationSharingPreference(enabled: false)
        let updatedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch updateResult {
        case .success:
            XCTAssertFalse(updatedSettings.shareLocationWithFriends)
            XCTAssertEqual(originalSettings.locationDataRetentionDays, updatedSettings.locationDataRetentionDays)
            XCTAssertEqual(originalSettings.allowFriendRequests, updatedSettings.allowFriendRequests)
            XCTAssertEqual(originalSettings.showOnlineStatus, updatedSettings.showOnlineStatus)
            XCTAssertEqual(originalSettings.dataProcessingConsent, updatedSettings.dataProcessingConsent)
            XCTAssertEqual(originalSettings.marketingConsent, updatedSettings.marketingConsent)
        case .failure(let error):
            XCTFail("Update should have succeeded: \(error)")
        }
    }
    
    func testUpdateFriendRequestPreferenceUpdatesOnlyFriendRequestSetting() async {
        // Given
        let originalSettings = await privacySettingsManager.getPrivacySettings()
        
        // When
        let updateResult = await privacySettingsManager.updateFriendRequestPreference(enabled: false)
        let updatedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch updateResult {
        case .success:
            XCTAssertFalse(updatedSettings.allowFriendRequests)
            XCTAssertEqual(originalSettings.locationDataRetentionDays, updatedSettings.locationDataRetentionDays)
            XCTAssertEqual(originalSettings.shareLocationWithFriends, updatedSettings.shareLocationWithFriends)
            XCTAssertEqual(originalSettings.showOnlineStatus, updatedSettings.showOnlineStatus)
            XCTAssertEqual(originalSettings.dataProcessingConsent, updatedSettings.dataProcessingConsent)
            XCTAssertEqual(originalSettings.marketingConsent, updatedSettings.marketingConsent)
        case .failure(let error):
            XCTFail("Update should have succeeded: \(error)")
        }
    }
    
    func testUpdateOnlineStatusVisibilityUpdatesOnlyOnlineStatusSetting() async {
        // Given
        let originalSettings = await privacySettingsManager.getPrivacySettings()
        
        // When
        let updateResult = await privacySettingsManager.updateOnlineStatusVisibility(enabled: false)
        let updatedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch updateResult {
        case .success:
            XCTAssertFalse(updatedSettings.showOnlineStatus)
            XCTAssertEqual(originalSettings.locationDataRetentionDays, updatedSettings.locationDataRetentionDays)
            XCTAssertEqual(originalSettings.shareLocationWithFriends, updatedSettings.shareLocationWithFriends)
            XCTAssertEqual(originalSettings.allowFriendRequests, updatedSettings.allowFriendRequests)
            XCTAssertEqual(originalSettings.dataProcessingConsent, updatedSettings.dataProcessingConsent)
            XCTAssertEqual(originalSettings.marketingConsent, updatedSettings.marketingConsent)
        case .failure(let error):
            XCTFail("Update should have succeeded: \(error)")
        }
    }
    
    func testUpdateDataProcessingConsentUpdatesOnlyDataProcessingConsent() async {
        // Given
        let originalSettings = await privacySettingsManager.getPrivacySettings()
        
        // When
        let updateResult = await privacySettingsManager.updateDataProcessingConsent(consent: true)
        let updatedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch updateResult {
        case .success:
            XCTAssertTrue(updatedSettings.dataProcessingConsent)
            XCTAssertEqual(originalSettings.locationDataRetentionDays, updatedSettings.locationDataRetentionDays)
            XCTAssertEqual(originalSettings.shareLocationWithFriends, updatedSettings.shareLocationWithFriends)
            XCTAssertEqual(originalSettings.allowFriendRequests, updatedSettings.allowFriendRequests)
            XCTAssertEqual(originalSettings.showOnlineStatus, updatedSettings.showOnlineStatus)
            XCTAssertEqual(originalSettings.marketingConsent, updatedSettings.marketingConsent)
        case .failure(let error):
            XCTFail("Update should have succeeded: \(error)")
        }
    }
    
    func testUpdateMarketingConsentUpdatesOnlyMarketingConsent() async {
        // Given
        let originalSettings = await privacySettingsManager.getPrivacySettings()
        
        // When
        let updateResult = await privacySettingsManager.updateMarketingConsent(consent: true)
        let updatedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch updateResult {
        case .success:
            XCTAssertTrue(updatedSettings.marketingConsent)
            XCTAssertEqual(originalSettings.locationDataRetentionDays, updatedSettings.locationDataRetentionDays)
            XCTAssertEqual(originalSettings.shareLocationWithFriends, updatedSettings.shareLocationWithFriends)
            XCTAssertEqual(originalSettings.allowFriendRequests, updatedSettings.allowFriendRequests)
            XCTAssertEqual(originalSettings.showOnlineStatus, updatedSettings.showOnlineStatus)
            XCTAssertEqual(originalSettings.dataProcessingConsent, updatedSettings.dataProcessingConsent)
        case .failure(let error):
            XCTFail("Update should have succeeded: \(error)")
        }
    }
    
    func testResetToDefaultsRestoresDefaultSettings() async {
        // Given - modify settings first
        _ = await privacySettingsManager.updateLocationDataRetention(days: 30)
        _ = await privacySettingsManager.updateLocationSharingPreference(enabled: false)
        _ = await privacySettingsManager.updateDataProcessingConsent(consent: true)
        
        // When
        let resetResult = await privacySettingsManager.resetToDefaults()
        let settings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        switch resetResult {
        case .success:
            XCTAssertEqual(90, settings.locationDataRetentionDays)
            XCTAssertTrue(settings.shareLocationWithFriends)
            XCTAssertTrue(settings.allowFriendRequests)
            XCTAssertTrue(settings.showOnlineStatus)
            XCTAssertFalse(settings.dataProcessingConsent)
            XCTAssertFalse(settings.marketingConsent)
        case .failure(let error):
            XCTFail("Reset should have succeeded: \(error)")
        }
    }
    
    func testGetPrivacyComplianceInfoReturnsCorrectComplianceInformation() async {
        // Given
        _ = await privacySettingsManager.updateDataProcessingConsent(consent: true)
        _ = await privacySettingsManager.updateMarketingConsent(consent: false)
        _ = await privacySettingsManager.updateLocationDataRetention(days: 60)
        
        // When
        let complianceInfo = await privacySettingsManager.getPrivacyComplianceInfo()
        
        // Then
        XCTAssertTrue(complianceInfo.dataProcessingConsentGiven)
        XCTAssertNotNil(complianceInfo.dataProcessingConsentDate)
        XCTAssertFalse(complianceInfo.marketingConsentGiven)
        XCTAssertNil(complianceInfo.marketingConsentDate)
        XCTAssertEqual(60, complianceInfo.locationDataRetentionDays)
        XCTAssertEqual("1.0", complianceInfo.privacyPolicyVersion)
        XCTAssertTrue(complianceInfo.gdprCompliant)
        XCTAssertTrue(complianceInfo.ccpaCompliant)
    }
    
    func testShouldRetainLocationDataReturnsCorrectRetentionDecision() async {
        // Given
        _ = await privacySettingsManager.updateLocationDataRetention(days: 30)
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let twentyDaysAgo = currentTime - (20 * 24 * 60 * 60 * 1000)
        let fortyDaysAgo = currentTime - (40 * 24 * 60 * 60 * 1000)
        
        // When
        let shouldRetainRecent = await privacySettingsManager.shouldRetainLocationData(dataTimestamp: twentyDaysAgo)
        let shouldRetainOld = await privacySettingsManager.shouldRetainLocationData(dataTimestamp: fortyDaysAgo)
        
        // Then
        XCTAssertTrue(shouldRetainRecent)
        XCTAssertFalse(shouldRetainOld)
    }
    
    func testGetDataRetentionSummaryReturnsCorrectSummary() async {
        // Given
        _ = await privacySettingsManager.updateLocationDataRetention(days: 45)
        
        // When
        let summary = await privacySettingsManager.getDataRetentionSummary()
        
        // Then
        XCTAssertEqual(45, summary.locationDataRetentionDays)
        XCTAssertTrue(summary.automaticCleanupEnabled)
        XCTAssertTrue(summary.locationDataCutoffDate > 0)
        XCTAssertTrue(summary.nextCleanupDate > Int64(Date().timeIntervalSince1970 * 1000))
    }
    
    func testLastUpdatedTimestampIsUpdatedOnSettingsChanges() async {
        // Given
        let originalSettings = await privacySettingsManager.getPrivacySettings()
        let originalTimestamp = originalSettings.lastUpdated
        
        // Wait a bit to ensure timestamp difference
        try? await Task.sleep(nanoseconds: 100_000_000) // 0.1 seconds
        
        // When
        _ = await privacySettingsManager.updateLocationDataRetention(days: 60)
        let updatedSettings = await privacySettingsManager.getPrivacySettings()
        
        // Then
        XCTAssertTrue(updatedSettings.lastUpdated > originalTimestamp)
    }
}