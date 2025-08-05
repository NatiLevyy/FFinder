import XCTest

/**
 * UI tests for AddFriendView.
 * 
 * These tests verify the user search functionality and friend request sending.
 */
class AddFriendViewUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }
    
    func testAddFriendView_displaysCorrectTitle() throws {
        // Navigate to add friend view
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let addFriendTitle = app.navigationBars["Add Friend"]
        XCTAssertTrue(addFriendTitle.exists)
    }
    
    func testAddFriendView_displaysEmailField() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let emailField = app.textFields["Enter email address"]
        XCTAssertTrue(emailField.exists)
    }
    
    func testAddFriendView_displaysSearchButton() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let searchButton = app.buttons["Search User"]
        XCTAssertTrue(searchButton.exists)
    }
    
    func testAddFriendView_displaysSendRequestButton() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let sendRequestButton = app.buttons["Send Friend Request"]
        XCTAssertTrue(sendRequestButton.exists)
        XCTAssertFalse(sendRequestButton.isEnabled) // Should be disabled initially
    }
    
    func testAddFriendView_hasCancelButton() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let cancelButton = app.buttons["Cancel"]
        XCTAssertTrue(cancelButton.exists)
    }
    
    func testEmailField_acceptsTextInput() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let emailField = app.textFields["Enter email address"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        XCTAssertEqual(emailField.value as? String, "test@example.com")
    }
    
    func testSearchButton_enabledWithEmailInput() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let emailField = app.textFields["Enter email address"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let searchButton = app.buttons["Search User"]
        XCTAssertTrue(searchButton.isEnabled)
    }
    
    func testCancelButton_dismissesView() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let cancelButton = app.buttons["Cancel"]
        cancelButton.tap()
        
        // Should return to friends view
        let friendsTitle = app.navigationBars["Friends"]
        XCTAssertTrue(friendsTitle.exists)
    }
    
    func testEmailField_hasCorrectKeyboardType() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        let emailField = app.textFields["Enter email address"]
        emailField.tap()
        
        // Check that email keyboard is displayed (contains @ symbol)
        let atSymbolKey = app.keys["@"]
        XCTAssertTrue(atSymbolKey.exists)
    }
}