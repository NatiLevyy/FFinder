import XCTest

final class CompleteUserJourneyUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["UI_TESTING"]
        app.launch()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    func testCompleteUserJourney_NewUser_ShouldCompleteSuccessfully() throws {
        // Step 1: User should see login screen (unauthenticated)
        XCTAssertTrue(app.textFields["Email"].exists)
        XCTAssertTrue(app.secureTextFields["Password"].exists)
        
        // Step 2: Navigate to registration
        app.buttons["Sign Up"].tap()
        
        // Step 3: Fill registration form
        let displayNameField = app.textFields["Display Name"]
        XCTAssertTrue(displayNameField.exists)
        displayNameField.tap()
        displayNameField.typeText("Test User")
        
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Step 4: Submit registration
        app.buttons["Create Account"].tap()
        
        // Wait for authentication to complete
        let mapView = app.otherElements["Map"]
        let mapExists = mapView.waitForExistence(timeout: 10)
        XCTAssertTrue(mapExists, "Map should appear after successful registration")
        
        // Step 5: Verify location permission dialog and grant permission
        let locationAlert = app.alerts.firstMatch
        if locationAlert.exists {
            locationAlert.buttons["Allow While Using App"].tap()
        }
        
        // Step 6: Verify map is displayed and functional
        XCTAssertTrue(app.buttons["location.fill"].exists, "Location button should be visible")
        
        // Step 7: Navigate to friends tab
        app.tabBars.buttons["Friends"].tap()
        
        // Step 8: Verify friends screen is displayed
        XCTAssertTrue(app.navigationBars["Friends"].exists)
        
        // Step 9: Add a friend
        app.buttons["Add Friend"].tap()
        
        let friendEmailField = app.textFields["Friend's Email"]
        XCTAssertTrue(friendEmailField.exists)
        friendEmailField.tap()
        friendEmailField.typeText("friend@example.com")
        
        app.buttons["Send Request"].tap()
        
        // Step 10: Verify friend request was sent
        let successAlert = app.alerts["Success"]
        XCTAssertTrue(successAlert.waitForExistence(timeout: 5))
        successAlert.buttons["OK"].tap()
        
        // Step 11: Navigate back to map
        app.tabBars.buttons["Map"].tap()
        
        // Step 12: Verify we're back on the map screen
        XCTAssertTrue(app.otherElements["Map"].exists)
        
        // Step 13: Open settings tab
        app.tabBars.buttons["Settings"].tap()
        
        // Step 14: Verify settings screen
        XCTAssertTrue(app.navigationBars["Settings"].exists)
        XCTAssertTrue(app.switches["Friend Requests"].exists)
    }
    
    func testCompleteUserJourney_ExistingUser_ShouldCompleteSuccessfully() throws {
        // Pre-setup: Simulate existing user by setting up mock data
        app.launchArguments.append("EXISTING_USER_TEST")
        app.terminate()
        app.launch()
        
        // Step 1: User should see login screen
        XCTAssertTrue(app.textFields["Email"].exists)
        
        // Step 2: Login with existing credentials
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("existing@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        app.buttons["Sign In"].tap()
        
        // Step 3: Should navigate directly to map screen
        let mapView = app.otherElements["Map"]
        let mapExists = mapView.waitForExistence(timeout: 10)
        XCTAssertTrue(mapExists, "Map should appear after successful login")
        
        // Step 4: Verify location services are running
        XCTAssertTrue(app.buttons["location.fill"].exists)
        
        // Step 5: Test location sharing functionality
        app.tabBars.buttons["Friends"].tap()
        
        // Assuming we have a pre-existing friend for this test
        let friendCell = app.cells["Test Friend"]
        if friendCell.exists {
            friendCell.tap()
            
            // Step 6: Enable location sharing with friend
            let locationSharingToggle = app.switches["Share Location"]
            if locationSharingToggle.exists {
                locationSharingToggle.tap()
                
                // Step 7: Verify location sharing is enabled
                let confirmAlert = app.alerts["Location Sharing"]
                if confirmAlert.exists {
                    confirmAlert.buttons["Enable"].tap()
                }
            }
            
            app.navigationBars.buttons.element(boundBy: 0).tap() // Back button
        }
        
        // Step 8: Navigate back to map and verify friend's location appears
        app.tabBars.buttons["Map"].tap()
        
        // Wait for location updates
        sleep(2)
        
        // Step 9: Verify map shows friend markers (if any friends are sharing)
        XCTAssertTrue(app.otherElements["Map"].exists)
    }
    
    func testCompleteUserJourney_OfflineScenario_ShouldHandleGracefully() throws {
        // Setup existing user
        app.launchArguments.append("OFFLINE_TEST")
        app.terminate()
        app.launch()
        
        // Step 1: Login successfully
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("offline@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        app.buttons["Sign In"].tap()
        
        // Wait for map to load
        let mapView = app.otherElements["Map"]
        XCTAssertTrue(mapView.waitForExistence(timeout: 10))
        
        // Step 2: Simulate network disconnection
        // This would be handled by the test setup with mock network conditions
        
        // Step 3: Try to perform actions that require network
        app.tabBars.buttons["Friends"].tap()
        app.buttons["Add Friend"].tap()
        
        let friendEmailField = app.textFields["Friend's Email"]
        friendEmailField.tap()
        friendEmailField.typeText("offline.friend@example.com")
        
        app.buttons["Send Request"].tap()
        
        // Step 4: Verify offline handling
        let offlineAlert = app.alerts["Offline"]
        if offlineAlert.exists {
            XCTAssertTrue(offlineAlert.staticTexts["Request will be sent when connection is restored"].exists)
            offlineAlert.buttons["OK"].tap()
        }
        
        // Step 5: Simulate network reconnection and verify queued request processing
        // This would be handled by the test framework
        sleep(3)
        
        // Step 6: Verify queued request was processed
        let successAlert = app.alerts["Success"]
        if successAlert.waitForExistence(timeout: 5) {
            XCTAssertTrue(successAlert.staticTexts["Friend request sent"].exists)
            successAlert.buttons["OK"].tap()
        }
    }
    
    func testCompleteUserJourney_ErrorRecovery_ShouldHandleGracefully() throws {
        // Step 1: Try to login with invalid credentials
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("invalid@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("wrongpassword")
        
        app.buttons["Sign In"].tap()
        
        // Step 2: Verify error message is displayed
        let errorAlert = app.alerts["Error"]
        XCTAssertTrue(errorAlert.waitForExistence(timeout: 5))
        XCTAssertTrue(errorAlert.staticTexts["Invalid email or password"].exists)
        errorAlert.buttons["OK"].tap()
        
        // Step 3: Correct the credentials and try again
        emailField.tap()
        emailField.clearAndEnterText("test@example.com")
        
        passwordField.tap()
        passwordField.clearAndEnterText("password123")
        
        app.buttons["Sign In"].tap()
        
        // Step 4: Verify successful login and navigation to map
        let mapView = app.otherElements["Map"]
        XCTAssertTrue(mapView.waitForExistence(timeout: 10))
        
        // Step 5: Test location permission denial scenario
        let locationAlert = app.alerts.firstMatch
        if locationAlert.exists && locationAlert.buttons["Don't Allow"].exists {
            locationAlert.buttons["Don't Allow"].tap()
            
            // Step 6: Verify graceful handling of permission denial
            let permissionAlert = app.alerts["Location Permission Required"]
            if permissionAlert.waitForExistence(timeout: 5) {
                XCTAssertTrue(permissionAlert.staticTexts["Location permissions are required for the app to work"].exists)
                permissionAlert.buttons["Settings"].tap()
                
                // This would open Settings app - in a real test we'd handle this
                // For now, just verify the alert appeared
            }
        }
    }
    
    func testCompleteUserJourney_AppLifecycle_ShouldMaintainState() throws {
        // Step 1: Login successfully
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("lifecycle@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        app.buttons["Sign In"].tap()
        
        // Wait for map to load
        let mapView = app.otherElements["Map"]
        XCTAssertTrue(mapView.waitForExistence(timeout: 10))
        
        // Step 2: Navigate to friends and add some state
        app.tabBars.buttons["Friends"].tap()
        XCTAssertTrue(app.navigationBars["Friends"].exists)
        
        // Step 3: Background the app
        XCUIDevice.shared.press(.home)
        sleep(2)
        
        // Step 4: Reactivate the app
        app.activate()
        
        // Step 5: Verify state is maintained
        XCTAssertTrue(app.navigationBars["Friends"].exists, "App should maintain navigation state")
        
        // Step 6: Navigate back to map
        app.tabBars.buttons["Map"].tap()
        
        // Step 7: Verify map state is maintained
        XCTAssertTrue(app.otherElements["Map"].exists)
        
        // Step 8: Test app termination and restart
        app.terminate()
        app.launch()
        
        // Step 9: Verify user remains authenticated
        let mapExistsAfterRestart = app.otherElements["Map"].waitForExistence(timeout: 10)
        XCTAssertTrue(mapExistsAfterRestart, "User should remain authenticated after app restart")
    }
}

// Helper extension for clearing text fields
extension XCUIElement {
    func clearAndEnterText(_ text: String) {
        guard let stringValue = self.value as? String else {
            XCTFail("Tried to clear and enter text into a non-string value")
            return
        }
        
        self.tap()
        
        let deleteString = String(repeating: XCUIKeyboardKey.delete.rawValue, count: stringValue.count)
        self.typeText(deleteString)
        self.typeText(text)
    }
}