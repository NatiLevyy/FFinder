import Foundation
import Network
import Combine

/**
 * Handles network-specific error scenarios and connectivity monitoring.
 */
class NetworkErrorHandler: ObservableObject {
    
    @Published private(set) var networkState: NetworkState = .unknown
    
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")
    
    init() {
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    /**
     * Starts monitoring network connectivity.
     */
    private func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.networkState = path.status == .satisfied ? .connected : .disconnected
            }
        }
        monitor.start(queue: queue)
    }
    
    /**
     * Stops monitoring network connectivity.
     */
    private func stopMonitoring() {
        monitor.cancel()
    }
    
    /**
     * Checks if the device has an active network connection.
     */
    var isNetworkAvailable: Bool {
        return networkState == .connected
    }
    
    /**
     * Handles HTTP error responses and converts them to appropriate error types.
     */
    func handleHttpError(statusCode: Int, data: Data? = nil) -> NetworkError {
        switch statusCode {
        case 401:
            return .unauthorized
        case 403:
            return .forbidden
        case 404:
            return .notFound
        case 409:
            return .conflict
        case 413:
            return .payloadTooLarge
        case 503:
            return .serviceUnavailable
        case 504:
            return .timeout
        case 400..<500:
            let message = extractErrorMessage(from: data) ?? "Client error"
            return .clientError(statusCode, message)
        case 500..<600:
            let message = extractErrorMessage(from: data) ?? "Server error"
            return .serverError(statusCode, message)
        default:
            let message = extractErrorMessage(from: data) ?? "Unknown error"
            return .unknown(statusCode, message)
        }
    }
    
    /**
     * Handles URLError and converts to NetworkError.
     */
    func handleURLError(_ urlError: URLError) -> NetworkError {
        switch urlError.code {
        case .notConnectedToInternet, .networkConnectionLost:
            return .noConnection
        case .timedOut:
            return .timeout
        case .cannotConnectToHost:
            return .serverError(0, "Cannot connect to server")
        case .badURL:
            return .clientError(400, "Invalid URL")
        default:
            return .unknown(urlError.errorCode, urlError.localizedDescription)
        }
    }
    
    /**
     * Determines the appropriate retry strategy based on the network error.
     */
    func getRetryStrategy(for error: NetworkError) -> RetryStrategy {
        switch error {
        case .timeout, .serviceUnavailable:
            return .exponentialBackoff
            
        case .serverError:
            return .linearBackoff
            
        case .unauthorized, .forbidden, .notFound, .conflict:
            return .noRetry
            
        default:
            return .immediateRetry
        }
    }
    
    /**
     * Gets user-friendly message for network errors.
     */
    func getNetworkErrorMessage(for error: NetworkError) -> String {
        switch error {
        case .noConnection:
            return "No internet connection. Please check your network settings."
        case .timeout:
            return "Request timed out. Please try again."
        case .unauthorized:
            return "Authentication failed. Please sign in again."
        case .forbidden:
            return "Access denied. You don't have permission for this action."
        case .notFound:
            return "The requested resource was not found."
        case .conflict:
            return "There was a conflict with your request. Please try again."
        case .payloadTooLarge:
            return "The data you're trying to send is too large."
        case .serviceUnavailable:
            return "Service is temporarily unavailable. Please try again later."
        case .clientError(_, let message):
            return "Request error: \(message)"
        case .serverError(_, let message):
            return "Server error: \(message)"
        case .unknown(_, let message):
            return "Network error: \(message)"
        }
    }
    
    /**
     * Extracts error message from response data if available.
     */
    private func extractErrorMessage(from data: Data?) -> String? {
        guard let data = data else { return nil }
        
        do {
            if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
               let message = json["message"] as? String {
                return message
            } else if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                      let error = json["error"] as? String {
                return error
            }
        } catch {
            // If JSON parsing fails, try to extract as plain text
            return String(data: data, encoding: .utf8)
        }
        
        return nil
    }
}

/**
 * Represents different network states.
 */
enum NetworkState {
    case connected
    case disconnected
    case unknown
}

/**
 * Represents different types of network errors.
 */
enum NetworkError: Error, LocalizedError {
    case noConnection
    case timeout
    case unauthorized
    case forbidden
    case notFound
    case conflict
    case payloadTooLarge
    case serviceUnavailable
    case clientError(Int, String)
    case serverError(Int, String)
    case unknown(Int, String)
    
    var errorDescription: String? {
        switch self {
        case .noConnection:
            return "No network connection"
        case .timeout:
            return "Network request timed out"
        case .unauthorized:
            return "Unauthorized access"
        case .forbidden:
            return "Access forbidden"
        case .notFound:
            return "Resource not found"
        case .conflict:
            return "Request conflict"
        case .payloadTooLarge:
            return "Payload too large"
        case .serviceUnavailable:
            return "Service unavailable"
        case .clientError(let code, let message):
            return "Client error (\(code)): \(message)"
        case .serverError(let code, let message):
            return "Server error (\(code)): \(message)"
        case .unknown(let code, let message):
            return "Unknown error (\(code)): \(message)"
        }
    }
    
    var statusCode: Int {
        switch self {
        case .noConnection:
            return -1
        case .timeout:
            return 408
        case .unauthorized:
            return 401
        case .forbidden:
            return 403
        case .notFound:
            return 404
        case .conflict:
            return 409
        case .payloadTooLarge:
            return 413
        case .serviceUnavailable:
            return 503
        case .clientError(let code, _), .serverError(let code, _), .unknown(let code, _):
            return code
        }
    }
}

/**
 * Defines retry strategies for different error types.
 */
enum RetryStrategy {
    case noRetry
    case immediateRetry
    case linearBackoff
    case exponentialBackoff
}