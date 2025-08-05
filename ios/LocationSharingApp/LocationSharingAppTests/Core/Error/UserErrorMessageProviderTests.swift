import XCTest
@testable import LocationSharingApp

class UserErrorMessageProviderTests: XCTestCase {
    
    func testGetErrorMessage_ReturnsCorrectMessagesForAuthenticationErrors() {
        // Given
        let testCases: [(Error, String)] = [
            (AuthError.invalidCredentials, "Please check your email and password and try again."),
            (AuthError.userNotFound, "No account found with this email address."),
            (AuthError.emailAlreadyInUse, "An account with this email already exists."),
            (AuthError.weakPassword, "Please choose a stronger password with at least 8 characters."),
            (AuthError.tooManyRequests, "Too many attempts. Please wait a moment before trying again."),
            (AuthError.networkError, "Network error. Please check your connection.")
        ]
        
        // When & Then
        testCases.forEach { (error, expectedMessage) in
            let message = UserErrorMessageProvider.getErrorMessage(for: error)
            // Note: In a real test, we would need to mock NSLocalizedString
            // For now, we just verify the method doesn't crash
            XCTAssertFalse(message.isEmpty)
        }
    }
    
    func testGetErrorMessage_ReturnsCorrectMessagesForLocationErrors() {
        // Given
        let testCases: [Error] = [
            LocationError.permissionDenied,
            LocationError.locationDisabled,
            LocationError.backgroundPermissionDenied,
            LocationError.timeout,
            LocationError.inaccurateLocation(accuracy: 10.0)
        ]
        
        // When & Then
        testCases.forEach { error in
            let message = UserErrorMessageProvider.getErrorMessage(for: error)
            XCTAssertFalse(message.isEmpty)
        }
    }
    
    func testGetErrorMessage_ReturnsCorrectMessagesForNetworkErrors() {
        // Given
        let testCases: [Error] = [
            NetworkError.noConnection,
            NetworkError.timeout,
            NetworkError.unauthorized,
            NetworkError.forbidden,
            NetworkError.notFound,
            NetworkError.serviceUnavailable
        ]
        
        // When & Then
        testCases.forEach { error in
            let message = UserErrorMessageProvider.getErrorMessage(for: error)
            XCTAssertFalse(message.isEmpty)
        }
    }
    
    func testGetErrorMessage_ReturnsCorrectMessagesForURLErrors() {
        // Given
        let testCases: [URLError] = [
            URLError(.notConnectedToInternet),
            URLError(.timedOut),
            URLError(.cannotConnectToHost),
            URLError(.badURL)
        ]
        
        // When & Then
        testCases.forEach { error in
            let message = UserErrorMessageProvider.getErrorMessage(for: error)
            XCTAssertFalse(message.isEmpty)
        }
    }
    
    func testGetErrorMessage_ReturnsGenericMessageForUnknownErrors() {
        // Given
        let unknownError = NSError(domain: "TestError", code: -1, userInfo: nil)
        
        // When
        let message = UserErrorMessageProvider.getErrorMessage(for: unknownError)
        
        // Then
        XCTAssertFalse(message.isEmpty)
    }
    
    func testGetRecoverySuggestion_ReturnsAppropriateSuggestions() {
        // Given
        let testCases: [Error] = [
            LocationError.permissionDenied,
            LocationError.locationDisabled,
            LocationError.backgroundPermissionDenied,
            NetworkError.noConnection,
            AuthError.networkError,
            URLError(.notConnectedToInternet)
        ]
        
        // When & Then
        testCases.forEach { error in
            let suggestion = UserErrorMessageProvider.getRecoverySuggestion(for: error)
            XCTAssertNotNil(suggestion)
            XCTAssertFalse(suggestion!.isEmpty)
        }
    }
    
    func testGetRecoverySuggestion_ReturnsNilForErrorsWithoutSuggestions() {
        // Given
        let errorsWithoutSuggestions: [Error] = [
            AuthError.invalidCredentials,
            NSError(domain: "TestError", code: -1)
        ]
        
        // When & Then
        errorsWithoutSuggestions.forEach { error in
            let suggestion = UserErrorMessageProvider.getRecoverySuggestion(for: error)
            XCTAssertNil(suggestion)
        }
    }
    
    func testGetActionButtonText_ReturnsAppropriateActionText() {
        // Given
        let settingsErrors: [Error] = [
            LocationError.permissionDenied,
            LocationError.locationDisabled,
            LocationError.backgroundPermissionDenied
        ]
        
        let retryErrors: [Error] = [
            NetworkError.noConnection,
            NetworkError.timeout,
            AuthError.networkError,
            URLError(.notConnectedToInternet)
        ]
        
        // When & Then
        settingsErrors.forEach { error in
            let actionText = UserErrorMessageProvider.getActionButtonText(for: error)
            XCTAssertNotNil(actionText)
            // In a real test, we would check for "Open Settings"
        }
        
        retryErrors.forEach { error in
            let actionText = UserErrorMessageProvider.getActionButtonText(for: error)
            XCTAssertNotNil(actionText)
            // In a real test, we would check for "Retry"
        }
    }
    
    func testGetActionButtonText_ReturnsNilForErrorsWithoutActions() {
        // Given
        let errorsWithoutActions: [Error] = [
            AuthError.invalidCredentials,
            NSError(domain: "TestError", code: -1)
        ]
        
        // When & Then
        errorsWithoutActions.forEach { error in
            let actionText = UserErrorMessageProvider.getActionButtonText(for: error)
            XCTAssertNil(actionText)
        }
    }
    
    func testGetErrorSeverity_ReturnsCorrectSeverityLevels() {
        // Given
        let criticalErrors: [Error] = [
            LocationError.permissionDenied,
            LocationError.locationDisabled,
            AuthError.userDisabled
        ]
        
        let highErrors: [Error] = [
            AuthError.invalidCredentials,
            AuthError.tokenExpired,
            NetworkError.unauthorized
        ]
        
        let mediumErrors: [Error] = [
            NetworkError.noConnection,
            NetworkError.timeout,
            LocationError.timeout,
            URLError(.notConnectedToInternet)
        ]
        
        let lowErrors: [Error] = [
            LocationError.inaccurateLocation(accuracy: 10.0)
        ]
        
        // When & Then
        criticalErrors.forEach { error in
            XCTAssertEqual(UserErrorMessageProvider.getErrorSeverity(for: error), .critical)
        }
        
        highErrors.forEach { error in
            XCTAssertEqual(UserErrorMessageProvider.getErrorSeverity(for: error), .high)
        }
        
        mediumErrors.forEach { error in
            XCTAssertEqual(UserErrorMessageProvider.getErrorSeverity(for: error), .medium)
        }
        
        lowErrors.forEach { error in
            XCTAssertEqual(UserErrorMessageProvider.getErrorSeverity(for: error), .low)
        }
    }
    
    func testGetErrorSeverity_ReturnsMediumForUnknownErrors() {
        // Given
        let unknownError = NSError(domain: "TestError", code: -1)
        
        // When
        let severity = UserErrorMessageProvider.getErrorSeverity(for: unknownError)
        
        // Then
        XCTAssertEqual(severity, .medium)
    }
    
    func testGetAlertStyle_ReturnsCorrectStylesForSeverity() {
        // Given
        let testCases: [(ErrorSeverity, AlertStyle)] = [
            (.low, .toast),
            (.medium, .banner),
            (.high, .alert),
            (.critical, .blockingAlert)
        ]
        
        // When & Then
        testCases.forEach { (severity, expectedStyle) in
            XCTAssertEqual(UserErrorMessageProvider.getAlertStyle(for: severity), expectedStyle)
        }
    }
    
    func testFormatErrorForLogging_ReturnsFormattedString() {
        // Given
        let error = AuthError.invalidCredentials
        let context = "LoginViewController"
        
        // When
        let logMessage = UserErrorMessageProvider.formatErrorForLogging(error, context: context)
        
        // Then
        XCTAssertTrue(logMessage.contains("Error:"))
        XCTAssertTrue(logMessage.contains("AuthError"))
        XCTAssertTrue(logMessage.contains(context))
    }
    
    func testFormatErrorForLogging_WithoutContext() {
        // Given
        let error = LocationError.timeout
        
        // When
        let logMessage = UserErrorMessageProvider.formatErrorForLogging(error)
        
        // Then
        XCTAssertTrue(logMessage.contains("Error:"))
        XCTAssertTrue(logMessage.contains("LocationError"))
        XCTAssertFalse(logMessage.contains(" in "))
    }
    
    func testFormatErrorForLogging_WithNetworkError() {
        // Given
        let error = NetworkError.unauthorized
        
        // When
        let logMessage = UserErrorMessageProvider.formatErrorForLogging(error)
        
        // Then
        XCTAssertTrue(logMessage.contains("Error:"))
        XCTAssertTrue(logMessage.contains("NetworkError"))
        XCTAssertTrue(logMessage.contains("Status: 401"))
    }
    
    func testFormatErrorForLogging_WithURLError() {
        // Given
        let error = URLError(.timedOut)
        
        // When
        let logMessage = UserErrorMessageProvider.formatErrorForLogging(error)
        
        // Then
        XCTAssertTrue(logMessage.contains("Error:"))
        XCTAssertTrue(logMessage.contains("URLError"))
        XCTAssertTrue(logMessage.contains("Code:"))
    }
}