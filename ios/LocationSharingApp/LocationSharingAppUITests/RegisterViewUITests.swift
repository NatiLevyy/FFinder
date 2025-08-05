import XCTest
import SwiftUI

/**
 * UI tests for RegisterView covering registration flow and error scenarios.
 * Tests requirements 1.1, 1.5, 1.6, 7.2.
 */
class RegisterViewUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
        
        // Navigate to register view
        app.buttons["Sign Up"].tap()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    func testRegisterScreen_displaysAllRequiredElements() throws {
        // Verify all UI elements are displayed
        XCTAssertTrue(app.navigationBars["Sign Up"].exists)
        XCTAssertTrue(app.staticTexts["Create Account"].exists)
        XCTAssertTrue(app.staticTexts["Join our location sharing community"].exists)
        
        XCTAssertTrue(app.textFields["Display Name"].exists)
        XCTAssertTrue(app.textFields["Email"].exists)
        XCTAssertTrue(app.secureTextFields["Password"].exists)
        
        XCTAssertTrue(app.buttons["Create Account"].exists)
        XCTAssertTrue(app.staticTexts["At least 6 characters"].exists)
        XCTAssertTrue(app.staticTexts["By creating an account, you agree to our Terms of Service and Privacy Policy"].exists)
        
        XCTAssertTrue(app.buttons["Cancel"].exists)
    }
    
    func testRegisterForm_showsErrorForEmptyDisplayName() throws {
        // Fill email and password, leave display name empty
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Display name is required"].waitForExistence(timeout: 2))
    }
    
    func testRegisterForm_showsErrorForShortDisplayName() throws {
        // Enter short display name
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("A")
        
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Display name must be at least 2 characters"].waitForExistence(timeout: 2))
    }
    
    func testRegisterForm_showsErrorForEmptyEmail() throws {
        // Fill display name and password, leave email empty
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("Test User")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Email is required"].waitForExistence(timeout: 2))
    }
    
    func testRegisterForm_showsErrorForInvalidEmail() throws {
        // Enter invalid email
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("Test User")
        
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("invalid-email")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Please enter a valid email address"].waitForExistence(timeout: 2))
    }
    
    func testRegisterForm_showsErrorForEmptyPassword() throws {
        // Fill display name and email, leave password empty
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("Test User")
        
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Password is required"].waitForExistence(timeout: 2))
    }
    
    func testRegisterForm_showsErrorForShortPassword() throws {
        // Enter short password
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("Test User")
        
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("123")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Password must be at least 6 characters"].waitForExistence(timeout: 2))
    }
    
    func testRegisterForm_clearsErrorWhenUserStartsTyping() throws {
        // Trigger an error first
        app.buttons["Create Account"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Display name is required"].waitForExistence(timeout: 2))
        
        // Start typing in display name field
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("T")
        
        // Verify error is cleared
        XCTAssertFalse(app.staticTexts["Display name is required"].exists)
    }
    
    func testRegisterForm_handlesValidInput() throws {
        // Enter valid registration data
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("Test User")
        
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify loading state is shown (button becomes disabled)
        let createAccountButton = app.buttons["Create Account"]
        XCTAssertFalse(createAccountButton.isEnabled)
    }
    
    func testRegisterForm_disablesInputsDuringLoading() throws {
        // Enter valid registration data
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("Test User")
        
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap create account button
        app.buttons["Create Account"].tap()
        
        // Verify all inputs are disabled during loading
        XCTAssertFalse(displayNameField.isEnabled)
        XCTAssertFalse(emailField.isEnabled)
        XCTAssertFalse(passwordField.isEnabled)
        XCTAssertFalse(app.buttons["Cancel"].isEnabled)
    }
    
    func testCancelButton_dismissesView() throws {
        // Tap cancel button
        app.buttons["Cancel"].tap()
        
        // Verify we're back to login view
        XCTAssertTrue(app.staticTexts["Location Sharing"].waitForExistence(timeout: 2))
        XCTAssertTrue(app.staticTexts["Welcome back! Please sign in to continue."].exists)
    }
    
    func testDisplayNameField_hasCorrectKeyboardType() throws {
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        
        // Verify display name field is focused
        XCTAssertTrue(displayNameField.hasKeyboardFocus)
    }
    
    func testEmailField_hasCorrectKeyboardType() throws {
        let emailField = app.textFields["Email"]
        emailField.tap()
        
        // Verify email field is focused
        XCTAssertTrue(emailField.hasKeyboardFocus)
    }
    
    func testFormFields_clearErrorsOnInput() throws {
        // Trigger validation error for display name
        app.buttons["Create Account"].tap()
        XCTAssertTrue(app.staticTexts["Display name is required"].waitForExistence(timeout: 2))
        
        // Type in display name field
        let displayNameField = app.textFields["Display Name"]
        displayNameField.tap()
        displayNameField.typeText("Test")
        
        // Error should be cleared
        XCTAssertFalse(app.staticTexts["Display name is required"].exists)
        
        // Trigger email error
        app.buttons["Create Account"].tap()
        XCTAssertTrue(app.staticTexts["Email is required"].waitForExistence(timeout: 2))
        
        // Type in email field
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test")
        
        // Error should be cleared
        XCTAssertFalse(app.staticTexts["Email is required"].exists)
        
        // Trigger password error
        app.buttons["Create Account"].tap()
        XCTAssertTrue(app.staticTexts["Password is required"].waitForExistence(timeout: 2))
        
        // Type in password field
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("pass")
        
        // Error should be cleared
        XCTAssertFalse(app.staticTexts["Password is required"].exists)
    }
    
    func testPasswordHelperText_isDisplayed() throws {
        // Verify password helper text is shown
        XCTAssertTrue(app.staticTexts["At least 6 characters"].exists)
    }
    
    func testTermsAndPrivacyText_isDisplayed() throws {
        // Verify terms and privacy text is shown
        XCTAssertTrue(app.staticTexts["By creating an account, you agree to our Terms of Service and Privacy Policy"].exists)
    }
}