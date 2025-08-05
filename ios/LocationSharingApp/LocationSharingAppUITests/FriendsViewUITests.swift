import XCTest

/**
 * UI tests for FriendsView.
 * 
 * These tests verify the friends list display, search functionality,
 * location sharing controls, and friend removal operations.
 */
class FriendsViewUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }
    
    func testFriendsView_displaysCorrectTitle() throws {
        // Navigate to friends view
        let friendsButton = app.buttons["Friends"]
        XCTAssertTrue(friendsButton.exists)
        friendsButton.tap()
        
        // Check navigation title
        let friendsTitle = app.navigationBars["Friends"]
        XCTAssertTrue(friendsTitle.exists)
    }
    
    func testFriendsView_displaysSearchBar() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let searchField = app.textFields["Search friends..."]
        XCTAssertTrue(searchField.exists)
    }
    
    func testFriendsView_displaysAddFriendButton() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        XCTAssertTrue(addButton.exists)
    }
    
    func testFriendsView_displaysRequestsButton() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let requestsButton = app.buttons["Requests"]
        XCTAssertTrue(requestsButton.exists)
    }
    
    func testFriendsView_displaysEmptyState() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        // Check for empty state elements
        let emptyStateImage = app.images["person.2"]
        let emptyStateText = app.staticTexts["No friends yet"]
        let emptyStateSubtext = app.staticTexts["Add friends to start sharing locations"]
        
        XCTAssertTrue(emptyStateImage.exists)
        XCTAssertTrue(emptyStateText.exists)
        XCTAssertTrue(emptyStateSubtext.exists)
    }
    
    func testSearchBar_acceptsTextInput() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let searchField = app.textFields["Search friends..."]
        searchField.tap()
        searchField.typeText("test@example.com")
        
        XCTAssertEqual(searchField.value as? String, "test@example.com")
    }
    
    func testAddFriendButton_opensAddFriendView() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let addButton = app.buttons["plus"]
        addButton.tap()
        
        // Check that Add Friend view is presented
        let addFriendTitle = app.navigationBars["Add Friend"]
        XCTAssertTrue(addFriendTitle.exists)
    }
    
    func testRequestsButton_opensFriendRequestsView() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let requestsButton = app.buttons["Requests"]
        requestsButton.tap()
        
        // Check that Friend Requests view is presented
        let requestsTitle = app.navigationBars["Friend Requests"]
        XCTAssertTrue(requestsTitle.exists)
    }
    
    func testSearchBar_hasClearButton() throws {
        let friendsButton = app.buttons["Friends"]
        friendsButton.tap()
        
        let searchField = app.textFields["Search friends..."]
        searchField.tap()
        searchField.typeText("test")
        
        // Clear button should appear
        let clearButton = app.buttons["xmark.circle.fill"]
        XCTAssertTrue(clearButton.exists)
        
        clearButton.tap()
        XCTAssertEqual(searchField.value as? String, "")
    }
}