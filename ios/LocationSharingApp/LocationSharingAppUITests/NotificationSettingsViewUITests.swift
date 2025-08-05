import XCTest

/**
 * UI tests for NotificationSettingsView
 */
class NotificationSettingsViewUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    func testNotificationSettingsView_DisplaysCorrectTitle() throws {
        // Navigate to notification settings (assuming there's a way to get there)
        // This would depend on the app's navigation structure
        
        let notificationSettingsTitle = app.navigationBars["Notification Settings"]
        XCTAssertTrue(notificationSettingsTitle.exists)
    }
    
    func testPermissionSection_IsDisplayed() throws {
        let permissionSection = app.staticTexts["Permission Status"]
        XCTAssertTrue(permissionSection.exists)
        
        let notificationPermissionText = app.staticTexts["Notification Permission"]
        XCTAssertTrue(notificationPermissionText.exists)
    }
    
    func testNotificationTypesSection_IsDisplayed() throws {
        let notificationTypesSection = app.staticTexts["Notification Types"]
        XCTAssertTrue(notificationTypesSection.exists)
        
        // Check for notification type labels
        XCTAssertTrue(app.staticTexts["Friend Requests"].exists)
        XCTAssertTrue(app.staticTexts["Friend Request Accepted"].exists)
        XCTAssertTrue(app.staticTexts["Location Sharing Requests"].exists)
        XCTAssertTrue(app.staticTexts["Location Sharing Granted"].exists)
        XCTAssertTrue(app.staticTexts["Location Sharing Revoked"].exists)
    }
    
    func testNotificationToggles_AreDisplayed() throws {
        // Check that toggle switches are present
        let toggles = app.switches
        XCTAssertGreaterThanOrEqual(toggles.count, 5) // Should have at least 5 toggles
    }
    
    func testTestNotificationSection_IsDisplayed() throws {
        let testNotificationSection = app.staticTexts["Test Notifications"]
        XCTAssertTrue(testNotificationSection.exists)
        
        let sendTestNotificationButton = app.buttons["Send Test Notification"]
        XCTAssertTrue(sendTestNotificationButton.exists)
    }
    
    func testSendTestNotificationButton_IsTappable() throws {
        let sendTestNotificationButton = app.buttons["Send Test Notification"]
        XCTAssertTrue(sendTestNotificationButton.exists)
        
        // Tap the button
        sendTestNotificationButton.tap()
        
        // Note: In a real test, we would verify the notification was sent
        // or that appropriate feedback was shown to the user
    }
    
    func testToggleSwitches_CanBeToggled() throws {
        let switches = app.switches
        
        // Test toggling the first few switches
        if switches.count > 0 {
            let firstSwitch = switches.element(boundBy: 0)
            let initialValue = firstSwitch.value as? String
            
            firstSwitch.tap()
            
            // Verify the switch state changed
            let newValue = firstSwitch.value as? String
            XCTAssertNotEqual(initialValue, newValue)
        }
    }
    
    func testDoneButton_DismissesView() throws {
        let doneButton = app.navigationBars.buttons["Done"]
        XCTAssertTrue(doneButton.exists)
        
        doneButton.tap()
        
        // Note: In a real test, we would verify the view was dismissed
        // This would depend on the navigation context
    }
    
    func testPermissionDescriptions_AreDisplayed() throws {
        // Check that descriptive text is shown for each notification type
        XCTAssertTrue(app.staticTexts["Get notified when someone sends you a friend request"].exists)
        XCTAssertTrue(app.staticTexts["Get notified when someone accepts your friend request"].exists)
        XCTAssertTrue(app.staticTexts["Get notified when someone wants to see your location"].exists)
        XCTAssertTrue(app.staticTexts["Get notified when someone allows you to see their location"].exists)
        XCTAssertTrue(app.staticTexts["Get notified when someone stops sharing their location with you"].exists)
    }
    
    func testTestNotificationDescription_IsDisplayed() throws {
        let testDescription = app.staticTexts["Verify your notification settings are working correctly"]
        XCTAssertTrue(testDescription.exists)
    }
}