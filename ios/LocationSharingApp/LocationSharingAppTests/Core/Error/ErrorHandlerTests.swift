import XCTest
@testable import LocationSharingApp

class ErrorHandlerTests: XCTestCase {
    
    func testExecuteWithRetry_SucceedsOnFirstAttempt() async {
        // Given
        var attemptCount = 0
        let operation = {
            attemptCount += 1
            return "success"
        }
        
        // When
        let result = await ErrorHandler.executeWithRetry(operation: operation)
        
        // Then
        switch result {
        case .success(let value):
            XCTAssertEqual(value, "success")
            XCTAssertEqual(attemptCount, 1)
        case .failure:
            XCTFail("Expected success")
        }
    }
    
    func testExecuteWithRetry_RetriesOnRetryableErrors() async {
        // Given
        var attemptCount = 0
        let operation = {
            attemptCount += 1
            if attemptCount < 3 {
                throw URLError(.timedOut)
            }
            return "success"
        }
        
        // When
        let result = await ErrorHandler.executeWithRetry(
            maxRetries: 3,
            initialDelay: 0.01, // Short delay for testing
            operation: operation
        )
        
        // Then
        switch result {
        case .success(let value):
            XCTAssertEqual(value, "success")
            XCTAssertEqual(attemptCount, 3)
        case .failure:
            XCTFail("Expected success after retries")
        }
    }
    
    func testExecuteWithRetry_DoesNotRetryOnNonRetryableErrors() async {
        // Given
        var attemptCount = 0
        let operation = {
            attemptCount += 1
            throw AuthError.invalidCredentials
        }
        
        // When
        let result = await ErrorHandler.executeWithRetry(
            maxRetries: 3,
            initialDelay: 0.01,
            operation: operation
        )
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure(let error):
            XCTAssertEqual(attemptCount, 1)
            XCTAssertTrue(error is AuthError)
        }
    }
    
    func testExecuteWithRetry_FailsAfterMaxRetries() async {
        // Given
        var attemptCount = 0
        let operation = {
            attemptCount += 1
            throw URLError(.timedOut)
        }
        
        // When
        let result = await ErrorHandler.executeWithRetry(
            maxRetries: 2,
            initialDelay: 0.01,
            operation: operation
        )
        
        // Then
        switch result {
        case .success:
            XCTFail("Expected failure")
        case .failure(let error):
            XCTAssertEqual(attemptCount, 3) // Initial attempt + 2 retries
            XCTAssertTrue(error is URLError)
        }
    }
    
    func testCategorizeError_CorrectlyCategorizeAuthenticationErrors() {
        // Given
        let authErrors: [Error] = [
            AuthError.invalidCredentials,
            AuthError.userNotFound,
            AuthError.weakPassword
        ]
        
        // When & Then
        authErrors.forEach { error in
            XCTAssertEqual(ErrorHandler.categorizeError(error), .authentication)
        }
    }
    
    func testCategorizeError_CorrectlyCategorizePermissionErrors() {
        // Given
        let permissionErrors: [Error] = [
            LocationError.permissionDenied,
            LocationError.backgroundPermissionDenied
        ]
        
        // When & Then
        permissionErrors.forEach { error in
            XCTAssertEqual(ErrorHandler.categorizeError(error), .permission)
        }
    }
    
    func testCategorizeError_CorrectlyCategorizeNetworkErrors() {
        // Given
        let networkErrors: [Error] = [
            AuthError.networkError,
            LocationError.networkNotAvailable,
            URLError(.timedOut),
            URLError(.notConnectedToInternet)
        ]
        
        // When & Then
        networkErrors.forEach { error in
            XCTAssertEqual(ErrorHandler.categorizeError(error), .network)
        }
    }
    
    func testCategorizeError_CorrectlyCategorizeConfigurationErrors() {
        // Given
        let configErrors: [Error] = [
            LocationError.locationDisabled,
            LocationError.settingsResolutionRequired
        ]
        
        // When & Then
        configErrors.forEach { error in
            XCTAssertEqual(ErrorHandler.categorizeError(error), .configuration)
        }
    }
    
    func testGetUserFriendlyMessage_ReturnsAppropriateMessagesForAuthErrors() {
        // Given
        let testCases: [(Error, String)] = [
            (AuthError.invalidCredentials, "Please check your email and password and try again."),
            (AuthError.userNotFound, "No account found with this email address."),
            (AuthError.emailAlreadyInUse, "An account with this email already exists."),
            (AuthError.weakPassword, "Please choose a stronger password with at least 8 characters."),
            (AuthError.tooManyRequests, "Too many attempts. Please wait a moment before trying again.")
        ]
        
        // When & Then
        testCases.forEach { (error, expectedMessage) in
            XCTAssertEqual(ErrorHandler.getUserFriendlyMessage(for: error), expectedMessage)
        }
    }
    
    func testGetUserFriendlyMessage_ReturnsAppropriateMessagesForLocationErrors() {
        // Given
        let testCases: [(Error, String)] = [
            (LocationError.permissionDenied, "Location permission is required. Please enable it in Settings."),
            (LocationError.locationDisabled, "Location services are disabled. Please enable them in Settings."),
            (LocationError.backgroundPermissionDenied, "Background location permission is needed for continuous sharing."),
            (LocationError.timeout, "Unable to get your location. Please try again.")
        ]
        
        // When & Then
        testCases.forEach { (error, expectedMessage) in
            XCTAssertEqual(ErrorHandler.getUserFriendlyMessage(for: error), expectedMessage)
        }
    }
    
    func testGetUserFriendlyMessage_ReturnsAppropriateMessagesForNetworkErrors() {
        // Given
        let testCases: [(Error, String)] = [
            (URLError(.notConnectedToInternet), "Please check your internet connection and try again."),
            (URLError(.timedOut), "Connection timed out. Please try again."),
            (AuthError.networkError, "Network error. Please check your connection.")
        ]
        
        // When & Then
        testCases.forEach { (error, expectedMessage) in
            XCTAssertEqual(ErrorHandler.getUserFriendlyMessage(for: error), expectedMessage)
        }
    }
    
    func testGetRecoverySuggestion_ReturnsAppropriateSuggestions() {
        // Given
        let testCases: [(Error, String)] = [
            (LocationError.permissionDenied, "Go to Settings > Privacy & Security > Location Services and enable location for this app"),
            (LocationError.locationDisabled, "Go to Settings > Privacy & Security > Location Services and turn it on"),
            (LocationError.backgroundPermissionDenied, "Go to Settings > Privacy & Security > Location Services > Location Sharing and select 'Always'"),
            (AuthError.networkError, "Check your WiFi or cellular data connection")
        ]
        
        // When & Then
        testCases.forEach { (error, expectedSuggestion) in
            XCTAssertEqual(ErrorHandler.getRecoverySuggestion(for: error), expectedSuggestion)
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
            XCTAssertNil(ErrorHandler.getRecoverySuggestion(for: error))
        }
    }
    
    func testShouldLogError_ReturnsFalseForUserInputErrors() {
        // Given
        let userInputErrors: [Error] = [
            AuthError.invalidCredentials,
            AuthError.weakPassword
        ]
        
        // When & Then
        userInputErrors.forEach { error in
            XCTAssertFalse(ErrorHandler.shouldLogError(error))
        }
    }
    
    func testShouldLogError_ReturnsTrueForSystemErrors() {
        // Given
        let systemErrors: [Error] = [
            LocationError.networkNotAvailable,
            URLError(.timedOut),
            NSError(domain: "SystemError", code: -1)
        ]
        
        // When & Then
        systemErrors.forEach { error in
            XCTAssertTrue(ErrorHandler.shouldLogError(error))
        }
    }
    
    func testErrorHandlingStrategies_HaveAppropriateConfigurations() {
        // Authentication strategy
        let authStrategy = ErrorHandlingStrategy.authentication
        XCTAssertEqual(authStrategy.maxRetries, 2)
        XCTAssertEqual(authStrategy.initialDelay, 2.0)
        XCTAssertTrue(authStrategy.showUserMessage)
        XCTAssertFalse(authStrategy.logError)
        
        // Location tracking strategy
        let locationStrategy = ErrorHandlingStrategy.locationTracking
        XCTAssertEqual(locationStrategy.maxRetries, 5)
        XCTAssertEqual(locationStrategy.initialDelay, 1.0)
        XCTAssertEqual(locationStrategy.maxDelay, 10.0)
        XCTAssertFalse(locationStrategy.showUserMessage)
        XCTAssertTrue(locationStrategy.logError)
        
        // Network requests strategy
        let networkStrategy = ErrorHandlingStrategy.networkRequests
        XCTAssertEqual(networkStrategy.maxRetries, 3)
        XCTAssertEqual(networkStrategy.initialDelay, 1.0)
        XCTAssertEqual(networkStrategy.maxDelay, 30.0)
        XCTAssertTrue(networkStrategy.showUserMessage)
        XCTAssertTrue(networkStrategy.logError)
        
        // Friend operations strategy
        let friendStrategy = ErrorHandlingStrategy.friendOperations
        XCTAssertEqual(friendStrategy.maxRetries, 2)
        XCTAssertEqual(friendStrategy.initialDelay, 1.5)
        XCTAssertTrue(friendStrategy.showUserMessage)
        XCTAssertTrue(friendStrategy.logError)
        
        // Map operations strategy
        let mapStrategy = ErrorHandlingStrategy.mapOperations
        XCTAssertEqual(mapStrategy.maxRetries, 3)
        XCTAssertEqual(mapStrategy.initialDelay, 0.5)
        XCTAssertEqual(mapStrategy.maxDelay, 5.0)
        XCTAssertFalse(mapStrategy.showUserMessage)
        XCTAssertTrue(mapStrategy.logError)
    }
}