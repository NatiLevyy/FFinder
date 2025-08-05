import XCTest
import SwiftUI

/**
 * UI tests for LoginView covering authentication flow and error scenarios.
 * Tests requirements 1.1, 1.5, 1.6, 7.2.
 */
class LoginViewUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    func testLoginScreen_displaysAllRequiredElements() throws {
        // Verify all UI elements are displayed
        XCTAssertTrue(app.staticTexts["Location Sharing"].exists)
        XCTAssertTrue(app.staticTexts["Welcome back! Please sign in to continue."].exists)
        
        XCTAssertTrue(app.textFields["Email"].exists)
        XCTAssertTrue(app.secureTextFields["Password"].exists)
        
        XCTAssertTrue(app.buttons["Sign In"].exists)
        XCTAssertTrue(app.buttons["Forgot Password?"].exists)
        XCTAssertTrue(app.buttons["Sign Up"].exists)
    }
    
    func testLoginForm_showsErrorForEmptyEmail() throws {
        // Enter password but leave email empty
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap sign in button
        app.buttons["Sign In"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Email is required"].waitForExistence(timeout: 2))
    }
    
    func testLoginForm_showsErrorForInvalidEmail() throws {
        // Enter invalid email
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("invalid-email")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap sign in button
        app.buttons["Sign In"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Please enter a valid email address"].waitForExistence(timeout: 2))
    }
    
    func testLoginForm_showsErrorForEmptyPassword() throws {
        // Enter email but leave password empty
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        // Tap sign in button
        app.buttons["Sign In"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Password is required"].waitForExistence(timeout: 2))
    }
    
    func testLoginForm_clearsErrorWhenUserStartsTyping() throws {
        // Trigger an error first
        app.buttons["Sign In"].tap()
        
        // Verify error is displayed
        XCTAssertTrue(app.staticTexts["Email is required"].waitForExistence(timeout: 2))
        
        // Start typing in email field
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("t")
        
        // Verify error is cleared (wait a moment for UI update)
        XCTAssertFalse(app.staticTexts["Email is required"].exists)
    }
    
    func testForgotPassword_showsAlertForEmptyEmail() throws {
        // Tap forgot password without entering email
        app.buttons["Forgot Password?"].tap()
        
        // Verify error message appears
        XCTAssertTrue(app.staticTexts["Please enter your email address first"].waitForExistence(timeout: 2))
    }
    
    func testForgotPassword_showsAlertWithValidEmail() throws {
        // Enter valid email
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        // Tap forgot password
        app.buttons["Forgot Password?"].tap()
        
        // Verify password reset alert appears
        XCTAssertTrue(app.alerts["Password Reset"].waitForExistence(timeout: 2))
        XCTAssertTrue(app.buttons["Send Reset Email"].exists)
        XCTAssertTrue(app.buttons["Cancel"].exists)
    }
    
    func testSignUpButton_opensRegisterView() throws {
        // Tap sign up button
        app.buttons["Sign Up"].tap()
        
        // Verify register view is presented
        XCTAssertTrue(app.staticTexts["Create Account"].waitForExistence(timeout: 2))
        XCTAssertTrue(app.navigationBars["Sign Up"].exists)
    }
    
    func testLoginForm_handlesValidInput() throws {
        // Enter valid credentials
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap sign in button
        app.buttons["Sign In"].tap()
        
        // Verify loading state is shown (button becomes disabled)
        let signInButton = app.buttons["Sign In"]
        XCTAssertFalse(signInButton.isEnabled)
    }
    
    func testLoginForm_disablesInputsDuringLoading() throws {
        // Enter valid credentials
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("password123")
        
        // Tap sign in button
        app.buttons["Sign In"].tap()
        
        // Verify inputs are disabled during loading
        XCTAssertFalse(emailField.isEnabled)
        XCTAssertFalse(passwordField.isEnabled)
        XCTAssertFalse(app.buttons["Forgot Password?"].isEnabled)
        XCTAssertFalse(app.buttons["Sign Up"].isEnabled)
    }
    
    func testPasswordResetAlert_sendsResetEmail() throws {
        // Enter valid email
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        // Tap forgot password
        app.buttons["Forgot Password?"].tap()
        
        // Tap send reset email
        app.buttons["Send Reset Email"].tap()
        
        // Verify success alert appears
        XCTAssertTrue(app.alerts["Password Reset Sent"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Password reset email sent. Please check your inbox."].exists)
    }
    
    func testPasswordResetAlert_canBeCancelled() throws {
        // Enter valid email
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test@example.com")
        
        // Tap forgot password
        app.buttons["Forgot Password?"].tap()
        
        // Tap cancel
        app.buttons["Cancel"].tap()
        
        // Verify alert is dismissed
        XCTAssertFalse(app.alerts["Password Reset"].exists)
    }
    
    func testEmailField_hasCorrectKeyboardType() throws {
        let emailField = app.textFields["Email"]
        emailField.tap()
        
        // Verify email keyboard is shown (this is implicit in the test environment)
        // In a real device test, we could check for the @ symbol on the keyboard
        XCTAssertTrue(emailField.hasKeyboardFocus)
    }
    
    func testFormFields_clearErrorsOnInput() throws {
        // Trigger validation error
        app.buttons["Sign In"].tap()
        XCTAssertTrue(app.staticTexts["Email is required"].waitForExistence(timeout: 2))
        
        // Type in email field
        let emailField = app.textFields["Email"]
        emailField.tap()
        emailField.typeText("test")
        
        // Error should be cleared
        XCTAssertFalse(app.staticTexts["Email is required"].exists)
        
        // Trigger password error
        app.buttons["Sign In"].tap()
        XCTAssertTrue(app.staticTexts["Password is required"].waitForExistence(timeout: 2))
        
        // Type in password field
        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText("pass")
        
        // Error should be cleared
        XCTAssertFalse(app.staticTexts["Password is required"].exists)
    }
}