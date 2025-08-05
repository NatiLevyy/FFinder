import XCTest
import Foundation
@testable import LocationSharingApp

/**
 * Base class for all iOS integration tests providing common setup and utilities.
 * This class handles dependency injection setup and provides access to
 * application context and testing utilities.
 */
class BaseIntegrationTest: XCTestCase {
    
    // Test utilities
    var app: XCUIApplication!
    
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        
        // Initialize test app
        app = XCUIApplication()
        app.launchEnvironment["TESTING"] = "1"
        app.launchEnvironment["USE_MOCK_SERVICES"] = "1"
    }
    
    override func tearDown() {
        app = nil
        super.tearDown()
    }
    
    /**
     * Waits for the specified condition to be true within the timeout period.
     * Useful for waiting for asynchronous operations to complete.
     */
    func waitForCondition(
        timeout: TimeInterval = 5.0,
        condition: () -> Bool
    ) -> Bool {
        let startTime = Date()
        while Date().timeIntervalSince(startTime) < timeout {
            if condition() {
                return true
            }
            RunLoop.current.run(until: Date(timeIntervalSinceNow: 0.1))
        }
        return false
    }
    
    /**
     * Waits for an expectation to be fulfilled within the timeout period.
     */
    func waitForExpectation(
        description: String,
        timeout: TimeInterval = 5.0,
        handler: @escaping (XCTestExpectation) -> Void
    ) {
        let expectation = XCTestExpectation(description: description)
        handler(expectation)
        wait(for: [expectation], timeout: timeout)
    }
    
    /**
     * Executes an async block and waits for completion.
     */
    func runAsync<T>(
        timeout: TimeInterval = 5.0,
        operation: @escaping () async throws -> T
    ) throws -> T {
        var result: Result<T, Error>?
        let expectation = XCTestExpectation(description: "Async operation")
        
        Task {
            do {
                let value = try await operation()
                result = .success(value)
            } catch {
                result = .failure(error)
            }
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: timeout)
        
        switch result {
        case .success(let value):
            return value
        case .failure(let error):
            throw error
        case .none:
            throw NSError(domain: "TestError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Operation timed out"])
        }
    }
    
    /**
     * Creates a test location with the specified coordinates.
     */
    func createTestLocation(
        latitude: Double = 37.7749,
        longitude: Double = -122.4194,
        accuracy: Double = 10.0
    ) -> Location {
        return Location(
            latitude: latitude,
            longitude: longitude,
            accuracy: Float(accuracy),
            timestamp: Date().timeIntervalSince1970 * 1000
        )
    }
    
    /**
     * Creates a test user with the specified properties.
     */
    func createTestUser(
        id: String = "test-user-id",
        email: String = "test@example.com",
        displayName: String = "Test User"
    ) -> User {
        return User(
            id: id,
            email: email,
            displayName: displayName,
            profileImageUrl: nil,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000),
            lastActiveAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
    }
}