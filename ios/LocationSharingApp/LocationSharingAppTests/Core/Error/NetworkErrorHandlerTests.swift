import XCTest
import Network
@testable import LocationSharingApp

class NetworkErrorHandlerTests: XCTestCase {
    
    var networkErrorHandler: NetworkErrorHandler!
    
    override func setUp() {
        super.setUp()
        networkErrorHandler = NetworkErrorHandler()
    }
    
    override func tearDown() {
        networkErrorHandler = nil
        super.tearDown()
    }
    
    func testHandleHttpError_ReturnsCorrectErrorTypesForDifferentStatusCodes() {
        // Given
        let testCases: [(Int, NetworkError)] = [
            (401, .unauthorized),
            (403, .forbidden),
            (404, .notFound),
            (409, .conflict),
            (413, .payloadTooLarge),
            (503, .serviceUnavailable),
            (504, .timeout)
        ]
        
        // When & Then
        testCases.forEach { (statusCode, expectedError) in
            let error = networkErrorHandler.handleHttpError(statusCode: statusCode)
            
            switch (error, expectedError) {
            case (.unauthorized, .unauthorized),
                 (.forbidden, .forbidden),
                 (.notFound, .notFound),
                 (.conflict, .conflict),
                 (.payloadTooLarge, .payloadTooLarge),
                 (.serviceUnavailable, .serviceUnavailable),
                 (.timeout, .timeout):
                XCTAssertTrue(true) // Expected match
            default:
                XCTFail("Expected \(expectedError) for status code \(statusCode), got \(error)")
            }
        }
    }
    
    func testHandleHttpError_ReturnsClientErrorForClientStatusCodes() {
        // Given
        let clientStatusCodes = [400, 422, 429, 499]
        
        // When & Then
        clientStatusCodes.forEach { statusCode in
            let error = networkErrorHandler.handleHttpError(statusCode: statusCode)
            
            switch error {
            case .clientError(let code, _):
                XCTAssertEqual(code, statusCode)
            default:
                XCTFail("Expected clientError for status code \(statusCode), got \(error)")
            }
        }
    }
    
    func testHandleHttpError_ReturnsServerErrorForServerStatusCodes() {
        // Given
        let serverStatusCodes = [500, 502, 505, 599]
        
        // When & Then
        serverStatusCodes.forEach { statusCode in
            let error = networkErrorHandler.handleHttpError(statusCode: statusCode)
            
            switch error {
            case .serverError(let code, _):
                XCTAssertEqual(code, statusCode)
            default:
                XCTFail("Expected serverError for status code \(statusCode), got \(error)")
            }
        }
    }
    
    func testHandleHttpError_ReturnsUnknownErrorForOtherStatusCodes() {
        // Given
        let unknownStatusCodes = [200, 300, 600, 700]
        
        // When & Then
        unknownStatusCodes.forEach { statusCode in
            let error = networkErrorHandler.handleHttpError(statusCode: statusCode)
            
            switch error {
            case .unknown(let code, _):
                XCTAssertEqual(code, statusCode)
            default:
                XCTFail("Expected unknown error for status code \(statusCode), got \(error)")
            }
        }
    }
    
    func testHandleURLError_ReturnsCorrectNetworkErrors() {
        // Given
        let testCases: [(URLError.Code, NetworkError)] = [
            (.notConnectedToInternet, .noConnection),
            (.networkConnectionLost, .noConnection),
            (.timedOut, .timeout),
            (.cannotConnectToHost, .serverError(0, "Cannot connect to server")),
            (.badURL, .clientError(400, "Invalid URL"))
        ]
        
        // When & Then
        testCases.forEach { (urlErrorCode, expectedError) in
            let urlError = URLError(urlErrorCode)
            let networkError = networkErrorHandler.handleURLError(urlError)
            
            switch (networkError, expectedError) {
            case (.noConnection, .noConnection),
                 (.timeout, .timeout):
                XCTAssertTrue(true) // Expected match
            case (.serverError(let code1, let message1), .serverError(let code2, let message2)):
                XCTAssertEqual(code1, code2)
                XCTAssertEqual(message1, message2)
            case (.clientError(let code1, let message1), .clientError(let code2, let message2)):
                XCTAssertEqual(code1, code2)
                XCTAssertEqual(message1, message2)
            default:
                XCTFail("Expected \(expectedError) for URLError code \(urlErrorCode), got \(networkError)")
            }
        }
    }
    
    func testGetRetryStrategy_ReturnsAppropriateStrategies() {
        // Given
        let testCases: [(NetworkError, RetryStrategy)] = [
            (.timeout, .exponentialBackoff),
            (.serviceUnavailable, .exponentialBackoff),
            (.serverError(500, "Server error"), .linearBackoff),
            (.unauthorized, .noRetry),
            (.forbidden, .noRetry),
            (.notFound, .noRetry),
            (.conflict, .noRetry),
            (.noConnection, .immediateRetry)
        ]
        
        // When & Then
        testCases.forEach { (error, expectedStrategy) in
            XCTAssertEqual(networkErrorHandler.getRetryStrategy(for: error), expectedStrategy)
        }
    }
    
    func testGetNetworkErrorMessage_ReturnsUserFriendlyMessages() {
        // Given
        let testCases: [(NetworkError, String)] = [
            (.noConnection, "No internet connection. Please check your network settings."),
            (.timeout, "Request timed out. Please try again."),
            (.unauthorized, "Authentication failed. Please sign in again."),
            (.forbidden, "Access denied. You don't have permission for this action."),
            (.notFound, "The requested resource was not found."),
            (.conflict, "There was a conflict with your request. Please try again."),
            (.payloadTooLarge, "The data you're trying to send is too large."),
            (.serviceUnavailable, "Service is temporarily unavailable. Please try again later."),
            (.clientError(400, "Bad request"), "Request error: Bad request"),
            (.serverError(500, "Internal error"), "Server error: Internal error"),
            (.unknown(600, "Unknown error"), "Network error: Unknown error")
        ]
        
        // When & Then
        testCases.forEach { (error, expectedMessage) in
            XCTAssertEqual(networkErrorHandler.getNetworkErrorMessage(for: error), expectedMessage)
        }
    }
    
    func testNetworkError_ErrorDescription() {
        // Given
        let errors: [NetworkError] = [
            .noConnection,
            .timeout,
            .unauthorized,
            .forbidden,
            .notFound,
            .conflict,
            .payloadTooLarge,
            .serviceUnavailable,
            .clientError(400, "Bad request"),
            .serverError(500, "Internal error"),
            .unknown(600, "Unknown error")
        ]
        
        // When & Then
        errors.forEach { error in
            XCTAssertNotNil(error.errorDescription)
            XCTAssertFalse(error.errorDescription!.isEmpty)
        }
    }
    
    func testNetworkError_StatusCode() {
        // Given
        let testCases: [(NetworkError, Int)] = [
            (.noConnection, -1),
            (.timeout, 408),
            (.unauthorized, 401),
            (.forbidden, 403),
            (.notFound, 404),
            (.conflict, 409),
            (.payloadTooLarge, 413),
            (.serviceUnavailable, 503),
            (.clientError(400, "Bad request"), 400),
            (.serverError(500, "Internal error"), 500),
            (.unknown(600, "Unknown error"), 600)
        ]
        
        // When & Then
        testCases.forEach { (error, expectedStatusCode) in
            XCTAssertEqual(error.statusCode, expectedStatusCode)
        }
    }
    
    func testNetworkState_InitialState() {
        // Given
        let newHandler = NetworkErrorHandler()
        
        // When
        let initialState = newHandler.networkState
        
        // Then
        XCTAssertEqual(initialState, .unknown)
    }
    
    func testIsNetworkAvailable_ReflectsNetworkState() {
        // Given
        let handler = NetworkErrorHandler()
        
        // When - Initial state should be unknown, so isNetworkAvailable should be false
        XCTAssertFalse(handler.isNetworkAvailable)
        
        // Note: Testing actual network state changes would require more complex setup
        // with network path monitoring, which is difficult to mock in unit tests
    }
    
    func testExtractErrorMessage_WithValidJSON() {
        // Given
        let jsonData = """
        {"message": "Test error message"}
        """.data(using: .utf8)!
        
        // When
        let error = networkErrorHandler.handleHttpError(statusCode: 400, data: jsonData)
        
        // Then
        switch error {
        case .clientError(_, let message):
            XCTAssertEqual(message, "Test error message")
        default:
            XCTFail("Expected clientError with extracted message")
        }
    }
    
    func testExtractErrorMessage_WithErrorField() {
        // Given
        let jsonData = """
        {"error": "Test error field"}
        """.data(using: .utf8)!
        
        // When
        let error = networkErrorHandler.handleHttpError(statusCode: 400, data: jsonData)
        
        // Then
        switch error {
        case .clientError(_, let message):
            XCTAssertEqual(message, "Test error field")
        default:
            XCTFail("Expected clientError with extracted error field")
        }
    }
    
    func testExtractErrorMessage_WithPlainText() {
        // Given
        let textData = "Plain text error".data(using: .utf8)!
        
        // When
        let error = networkErrorHandler.handleHttpError(statusCode: 400, data: textData)
        
        // Then
        switch error {
        case .clientError(_, let message):
            XCTAssertEqual(message, "Plain text error")
        default:
            XCTFail("Expected clientError with plain text message")
        }
    }
    
    func testExtractErrorMessage_WithInvalidJSON() {
        // Given
        let invalidJsonData = "invalid json".data(using: .utf8)!
        
        // When
        let error = networkErrorHandler.handleHttpError(statusCode: 400, data: invalidJsonData)
        
        // Then
        switch error {
        case .clientError(_, let message):
            XCTAssertEqual(message, "invalid json")
        default:
            XCTFail("Expected clientError with fallback message")
        }
    }
}