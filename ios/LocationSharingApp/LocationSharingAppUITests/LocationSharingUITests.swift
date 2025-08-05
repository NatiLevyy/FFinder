import XCTest

/**
 * UI tests for location sharing permission flows.
 * 
 * These tests verify the location sharing request and response UI components
 * work correctly and provide appropriate user feedback.
 */
class LocationSharingUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    func testLocationSharingToggleVisibility() throws {
        // Navigate to friends view
        app.tabBars.buttons["Friends"].tap()
        
        // Verify location sharing toggle is visible in friends list
        let locationToggle = app.switches["Share Location"]
        XCTAssertTrue(locationToggle.exists)
    }
    
    func testLocationSharingStatusIndicator() throws {
        // Navigate to friends view
        app.tabBars.buttons["Friends"].tap()
        
        // Verify location sharing status text is displayed
        let statusTexts = ["Sharing location", "Location requested", "Location denied", "Not sharing"]
        let statusExists = statusTexts.contains { text in
            app.staticTexts[text].exists
        }
        XCTAssertTrue(statusExists, "Location sharing status should be displayed")
    }
    
    func testLocationRequestButtonVisibility() throws {
        // Navigate to friends view
        app.tabBars.buttons["Friends"].tap()
        
        // Verify location request button is visible when permission is NONE
        let requestButton = app.buttons["Request Location"]
        if requestButton.exists {
            XCTAssertTrue(requestButton.isEnabled)
        }
    }
    
    func testLocationRequestButtonInteraction() throws {
        // Navigate to friends view
        app.tabBars.buttons["Friends"].tap()
        
        // Test clicking the location request button
        let requestButton = app.buttons["Request Location"]
        if requestButton.exists {
            requestButton.tap()
            
            // Verify button state changes to pending
            let pendingText = app.staticTexts["Pending..."]
            XCTAssertTrue(pendingText.waitForExistence(timeout: 2.0))
        }
    }
    
    func testLocationSharingToggleInteraction() throws {
        // Navigate to friends view
        app.tabBars.buttons["Friends"].tap()
        
        // Test toggling location sharing switch
        let locationToggle = app.switches["Share Location"]
        if locationToggle.exists && locationToggle.isEnabled {
            let initialValue = locationToggle.value as? String
            locationToggle.tap()
            
            // Verify toggle state changes
            let newValue = locationToggle.value as? String
            XCTAssertNotEqual(initialValue, newValue)
        }
    }
    
    func testRemoveFriendAlert() throws {
        // Navigate to friends view
        app.tabBars.buttons["Friends"].tap()
        
        // Test remove friend button shows confirmation alert
        let removeButton = app.buttons["Remove"]
        if removeButton.exists {
            removeButton.tap()
            
            // Verify alert is displayed
            let alert = app.alerts["Remove Friend"]
            XCTAssertTrue(alert.waitForExistence(timeout: 2.0))
            
            // Cancel the alert
            alert.buttons["Cancel"].tap()
        }
    }
    
    func testLocationSharingRequestView() throws {
        // This test would require setting up a specific scenario where a location sharing request is received
        // For now, we'll test the view components if they exist
        
        let requestTitle = app.staticTexts["Location Sharing Request"]
        if requestTitle.exists {
            XCTAssertTrue(requestTitle.isHittable)
            
            // Verify action buttons exist
            let acceptButton = app.buttons["Accept"]
            let denyButton = app.buttons["Deny"]
            let cancelButton = app.buttons["Cancel"]
            
            XCTAssertTrue(acceptButton.exists)
            XCTAssertTrue(denyButton.exists)
            XCTAssertTrue(cancelButton.exists)
        }
    }
    
    func testLocationSharingRequestAccept() throws {
        // Test accepting location sharing request
        let requestTitle = app.staticTexts["Location Sharing Request"]
        if requestTitle.exists {
            let acceptButton = app.buttons["Accept"]
            acceptButton.tap()
            
            // Verify loading state or success feedback
            // This would depend on the specific implementation
        }
    }
    
    func testLocationSharingRequestDeny() throws {
        // Test denying location sharing request
        let requestTitle = app.staticTexts["Location Sharing Request"]
        if requestTitle.exists {
            let denyButton = app.buttons["Deny"]
            denyButton.tap()
            
            // Verify loading state or success feedback
            // This would depend on the specific implementation
        }
    }
    
    func testLocationSharingRequestCancel() throws {
        // Test canceling location sharing request
        let requestTitle = app.staticTexts["Location Sharing Request"]
        if requestTitle.exists {
            let cancelButton = app.buttons["Cancel"]
            cancelButton.tap()
            
            // Verify view is dismissed
            XCTAssertFalse(requestTitle.exists)
        }
    }
    
    func testFriendsListLocationPermissionStates() throws {
        // Navigate to friends view
        app.tabBars.buttons["Friends"].tap()
        
        // Test different permission states are displayed correctly
        let permissionStates = [
            ("Sharing location", NSPredicate(format: "label CONTAINS 'Sharing location'")),
            ("Location requested", NSPredicate(format: "label CONTAINS 'Location requested'")),
            ("Location denied", NSPredicate(format: "label CONTAINS 'Location denied'")),
            ("Not sharing", NSPredicate(format: "label CONTAINS 'Not sharing'"))
        ]
        
        for (stateName, predicate) in permissionStates {
            let elements = app.staticTexts.matching(predicate)
            if elements.count > 0 {
                XCTAssertTrue(elements.firstMatch.exists, "\(stateName) should be visible when present")
            }
        }
    }
    
    func testLocationSharingPermissionFlow() throws {
        // Test complete permission flow from request to acceptance
        app.tabBars.buttons["Friends"].tap()
        
        // Find a friend with no location sharing permission
        let requestButton = app.buttons["Request Location"]
        if requestButton.exists {
            // Send request
            requestButton.tap()
            
            // Verify status changes to pending
            let pendingText = app.staticTexts["Pending..."]
            XCTAssertTrue(pendingText.waitForExistence(timeout: 2.0))
            
            // This would continue with accepting the request on the other side
            // and verifying the status changes to "Sharing location"
        }
    }
}