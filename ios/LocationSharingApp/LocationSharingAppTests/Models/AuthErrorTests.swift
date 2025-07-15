import XCTest
@testable import LocationSharingApp

class AuthErrorTests: XCTestCase {
    
    func testInvalidCredentialsError() {
        // Given
        let error = AuthError.invalidCredentials
        
        // Then
        XCTAssertEqual(error.errorDescription, "Invalid email or password")
    }
    
    func testUserNotFoundError() {
        // Given
        let error = AuthError.userNotFound
        
        // Then
        XCTAssertEqual(error.errorDescription, "User account not found")
    }
    
    func testUserDisabledError() {
        // Given
        let error = AuthError.userDisabled
        
        // Then
        XCTAssertEqual(error.errorDescription, "User account has been disabled")
    }
    
    func testEmailAlreadyInUseError() {
        // Given
        let error = AuthError.emailAlreadyInUse
        
        // Then
        XCTAssertEqual(error.errorDescription, "Email address is already in use")
    }
    
    func testInvalidEmailError() {
        // Given
        let error = AuthError.invalidEmail
        
        // Then
        XCTAssertEqual(error.errorDescription, "Invalid email address format")
    }
    
    func testWeakPasswordError() {
        // Given
        let error = AuthError.weakPassword
        
        // Then
        XCTAssertEqual(error.errorDescription, "Password is too weak")
    }
    
    func testTokenExpiredError() {
        // Given
        let error = AuthError.tokenExpired
        
        // Then
        XCTAssertEqual(error.errorDescription, "Authentication token has expired")
    }
    
    func testInvalidTokenError() {
        // Given
        let error = AuthError.invalidToken
        
        // Then
        XCTAssertEqual(error.errorDescription, "Invalid authentication token")
    }
    
    func testNetworkError() {
        // Given
        let error = AuthError.networkError
        
        // Then
        XCTAssertEqual(error.errorDescription, "Network connection error")
    }
    
    func testTooManyRequestsError() {
        // Given
        let error = AuthError.tooManyRequests
        
        // Then
        XCTAssertEqual(error.errorDescription, "Too many authentication attempts. Please try again later")
    }
    
    func testUnknownError() {
        // Given
        let customMessage = "Custom error message"
        let error = AuthError.unknown(customMessage)
        
        // Then
        XCTAssertEqual(error.errorDescription, customMessage)
    }
    
    func testAuthErrorIsError() {
        // Given
        let error = AuthError.invalidCredentials
        
        // Then
        XCTAssertTrue(error is Error)
        XCTAssertTrue(error is LocalizedError)
    }
    
    func testAuthErrorEquality() {
        // Given
        let error1 = AuthError.invalidCredentials
        let error2 = AuthError.invalidCredentials
        let error3 = AuthError.userNotFound
        
        // Then
        XCTAssertTrue(type(of: error1) == type(of: error2))
        XCTAssertFalse(type(of: error1) == type(of: error3))
    }
}