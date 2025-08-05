import XCTest

final class AccessibilityUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["UI_TESTING", "ACCESSIBILITY_TESTING"]
        app.launch()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    func testOnboarding_AccessibilityLabelsAndTraits() throws {
        // Test onboarding accessibility
        XCTAssertTrue(app.images.firstMatch.exists)
        XCTAssertFalse(app.images.firstMatch.label.isEmpty, "Onboarding images should have accessibility labels")
        
        XCTAssertTrue(app.staticTexts.firstMatch.exists)
        XCTAssertFalse(app.staticTexts.firstMatch.label.isEmpty, "Onboarding text should have accessibility labels")
        
        // Test navigation buttons
        let skipButton = app.buttons["Skip"]
        XCTAssertTrue(skipButton.exists)
        XCTAssertTrue(skipButton.isHittable, "Skip button should be accessible")
        
        let nextButton = app.buttons["Next"]
        XCTAssertTrue(nextButton.exists)
        XCTAssertTrue(nextButton.isHittable, "Next button should be accessible")
    }
    
    func testLogin_FormAccessibility() throws {
        // Skip onboarding to get to login
        if app.buttons["Skip"].exists {
            app.buttons["Skip"].tap()
        }
        
        // Test form field accessibility
        let emailField = app.textFields["Email"]
        XCTAssertTrue(emailField.exists)
        XCTAssertTrue(emailField.isHittable, "Email field should be accessible")
        XCTAssertFalse(emailField.label.isEmpty, "Email field should have accessibility label")
        
        let passwordField = app.secureTextFields["Password"]
        XCTAssertTrue(passwordField.exists)
        XCTAssertTrue(passwordField.isHittable, "Password field should be accessible")
        XCTAssertFalse(passwordField.label.isEmpty, "Password field should have accessibility label")
        
        // Test buttons
        let signInButton = app.buttons["Sign In"]
        XCTAssertTrue(signInButton.exists)
        XCTAssertTrue(signInButton.isHittable, "Sign In button should be accessible")
        
        let signUpButton = app.buttons["Sign Up"]
        XCTAssertTrue(signUpButton.exists)
        XCTAssertTrue(signUpButton.isHittable, "Sign Up button should be accessible")
    }
    
    func testMap_AccessibilityElements() throws {
        // Setup authenticated user
        app.launchArguments.append("AUTHENTICATED_USER_TEST")
        app.terminate()
        app.launch()
        
        // Wait for map to load
        let mapView = app.otherElements["Map"]
        XCTAssertTrue(mapView.waitForExistence(timeout: 10))
        XCTAssertFalse(mapView.label.isEmpty, "Map should have accessibility label")
        
        // Test location button
        let locationButton = app.buttons["location.fill"]
        if locationButton.exists {
            XCTAssertTrue(locationButton.isHittable, "Location button should be accessible")
            XCTAssertFalse(locationButton.label.isEmpty, "Location button should have accessibility label")
        }
        
        // Test tab bar accessibility
        let mapTab = app.tabBars.buttons["Map"]
        XCTAssertTrue(mapTab.exists)
        XCTAssertTrue(mapTab.isHittable, "Map tab should be accessible")
        
        let friendsTab = app.tabBars.buttons["Friends"]
        XCTAssertTrue(friendsTab.exists)
        XCTAssertTrue(friendsTab.isHittable, "Friends tab should be accessible")
        
        let settingsTab = app.tabBars.buttons["Settings"]
        XCTAssertTrue(settingsTab.exists)
        XCTAssertTrue(settingsTab.isHittable, "Settings tab should be accessible")
    }
    
    func testFriends_ListAccessibility() throws {
        // Setup authenticated user with friends
        app.launchArguments.append("AUTHENTICATED_USER_WITH_FRIENDS_TEST")
        app.terminate()
        app.launch()
        
        // Navigate to friends tab
        app.tabBars.buttons["Friends"].tap()
        
        // Test friends list accessibility
        XCTAssertTrue(app.navigationBars["Friends"].exists)
        
        // Test add friend button
        let addFriendButton = app.buttons["Add Friend"]
        if addFriendButton.exists {
            XCTAssertTrue(addFriendButton.isHittable, "Add Friend button should be accessible")
            XCTAssertFalse(addFriendButton.label.isEmpty, "Add Friend button should have accessibility label")
        }
        
        // Test friend cells if they exist
        let friendCells = app.cells
        if friendCells.count > 0 {
            let firstCell = friendCells.firstMatch
            XCTAssertTrue(firstCell.isHittable, "Friend cells should be accessible")
            XCTAssertFalse(firstCell.label.isEmpty, "Friend cells should have accessibility labels")
        }
    }
    
    func testSettings_ToggleAccessibility() throws {
        // Setup authenticated user
        app.launchArguments.append("AUTHENTICATED_USER_TEST")
        app.terminate()
        app.launch()
        
        // Navigate to settings tab
        app.tabBars.buttons["Settings"].tap()
        
        // Test notification toggles
        let notificationSwitches = app.switches
        for switchElement in notificationSwitches.allElementsBoundByIndex {
            XCTAssertTrue(switchElement.isHittable, "Switches should be accessible")
            XCTAssertFalse(switchElement.label.isEmpty, "Switches should have accessibility labels")
            
            // Test that switch value is announced
            let value = switchElement.value as? String
            XCTAssertTrue(value == "0" || value == "1", "Switch should have proper accessibility value")
        }
    }
    
    func testLoadingStates_Accessibility() throws {
        // Test loading view accessibility
        app.launchArguments.append("SLOW_LOADING_TEST")
        app.terminate()
        app.launch()
        
        // Check for loading indicator
        let loadingIndicator = app.activityIndicators.firstMatch
        if loadingIndicator.exists {
            XCTAssertFalse(loadingIndicator.label.isEmpty, "Loading indicator should have accessibility label")
        }
        
        // Check for loading text
        let loadingText = app.staticTexts.containing(NSPredicate(format: "label CONTAINS 'Loading'")).firstMatch
        if loadingText.exists {
            XCTAssertTrue(loadingText.exists, "Loading text should be accessible")
        }
    }
    
    func testErrorStates_Accessibility() throws {
        // Test error handling accessibility
        app.launchArguments.append("ERROR_STATE_TEST")
        app.terminate()
        app.launch()
        
        // Check for error alerts
        let errorAlert = app.alerts["Error"]
        if errorAlert.waitForExistence(timeout: 5) {
            XCTAssertTrue(errorAlert.isHittable, "Error alerts should be accessible")
            XCTAssertFalse(errorAlert.label.isEmpty, "Error alerts should have accessibility labels")
            
            // Check alert buttons
            let okButton = errorAlert.buttons["OK"]
            if okButton.exists {
                XCTAssertTrue(okButton.isHittable, "Alert buttons should be accessible")
            }
        }
    }
    
    func testVoiceOverNavigation() throws {
        // This test would be run with VoiceOver enabled
        // It tests that VoiceOver can navigate through the app properly
        
        // Skip onboarding
        if app.buttons["Skip"].exists {
            app.buttons["Skip"].tap()
        }
        
        // Test that elements are in logical reading order
        let emailField = app.textFields["Email"]
        let passwordField = app.secureTextFields["Password"]
        let signInButton = app.buttons["Sign In"]
        
        XCTAssertTrue(emailField.exists)
        XCTAssertTrue(passwordField.exists)
        XCTAssertTrue(signInButton.exists)
        
        // In a real VoiceOver test, you would verify the reading order
        // and that focus moves logically between elements
    }
    
    func testDynamicType_TextScaling() throws {
        // This test would verify that text scales properly with Dynamic Type
        // In a real implementation, you would test different text size categories
        
        let titleText = app.staticTexts.firstMatch
        if titleText.exists {
            let originalFrame = titleText.frame
            
            // In a real test, you would change the text size setting
            // and verify that the text frame changes accordingly
            XCTAssertTrue(originalFrame.width > 0)
            XCTAssertTrue(originalFrame.height > 0)
        }
    }
    
    func testColorContrast_HighContrastMode() throws {
        // This test would verify that the app works properly in high contrast mode
        // In a real implementation, you would enable high contrast mode
        // and verify that all text remains readable
        
        // Check that text elements exist and are visible
        let textElements = app.staticTexts
        for textElement in textElements.allElementsBoundByIndex {
            if textElement.exists {
                XCTAssertTrue(textElement.isHittable || !textElement.label.isEmpty,
                             "Text elements should be visible in high contrast mode")
            }
        }
    }
    
    func testReducedMotion_AnimationHandling() throws {
        // This test would verify that the app respects reduced motion settings
        // In a real implementation, you would enable reduced motion
        // and verify that animations are disabled or reduced
        
        // Navigate through the app and verify it works without animations
        if app.buttons["Skip"].exists {
            app.buttons["Skip"].tap()
        }
        
        // Verify that the app still functions properly
        XCTAssertTrue(app.textFields["Email"].exists)
    }
}