import Foundation

/**
 * Provides data validation and sanitization functionality for security purposes.
 */
class DataValidator {
    
    // MARK: - Constants
    
    private struct Constants {
        static let maxEmailLength = 254
        static let maxPasswordLength = 128
        static let maxNameLength = 100
        static let maxMessageLength = 1000
        static let minLatitude = -90.0
        static let maxLatitude = 90.0
        static let minLongitude = -180.0
        static let maxLongitude = 180.0
    }
    
    // MARK: - Singleton
    
    static let shared = DataValidator()
    
    // MARK: - Private Properties
    
    private let emailRegex = try! NSRegularExpression(
        pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )
    
    private let passwordRegex = try! NSRegularExpression(
        pattern: "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$"
    )
    
    private let userIdRegex = try! NSRegularExpression(
        pattern: "^[a-zA-Z0-9_.-]{3,50}$"
    )
    
    private let base64UrlRegex = try! NSRegularExpression(
        pattern: "^[A-Za-z0-9_-]+$"
    )
    
    // MARK: - Initialization
    
    private init() {}
    
    // MARK: - Validation Methods
    
    /**
     * Validates and sanitizes email address.
     * 
     * - Parameter email: The email address to validate
     * - Returns: ValidationResult containing the sanitized email or error
     */
    func validateEmail(_ email: String?) -> ValidationResult<String> {
        guard let email = email?.trimmingCharacters(in: .whitespacesAndNewlines),
              !email.isEmpty else {
            return .error("Email cannot be empty")
        }
        
        let sanitizedEmail = sanitizeInput(email.lowercased())
        
        guard sanitizedEmail.count <= Constants.maxEmailLength else {
            return .error("Email too long")
        }
        
        let range = NSRange(location: 0, length: sanitizedEmail.count)
        guard emailRegex.firstMatch(in: sanitizedEmail, range: range) != nil else {
            return .error("Invalid email format")
        }
        
        return .success(sanitizedEmail)
    }
    
    /**
     * Validates password strength.
     * 
     * - Parameter password: The password to validate
     * - Returns: ValidationResult containing validation status
     */
    func validatePassword(_ password: String?) -> ValidationResult<String> {
        guard let password = password, !password.isEmpty else {
            return .error("Password cannot be empty")
        }
        
        guard password.count <= Constants.maxPasswordLength else {
            return .error("Password too long")
        }
        
        guard password.count >= 8 else {
            return .error("Password must be at least 8 characters")
        }
        
        let range = NSRange(location: 0, length: password.count)
        guard passwordRegex.firstMatch(in: password, range: range) != nil else {
            return .error("Password must contain at least one uppercase letter, one lowercase letter, and one digit")
        }
        
        // Check for common weak passwords
        if isCommonPassword(password) {
            return .error("Password is too common, please choose a stronger password")
        }
        
        return .success(password)
    }
    
    /**
     * Validates and sanitizes user ID.
     * 
     * - Parameter userId: The user ID to validate
     * - Returns: ValidationResult containing the sanitized user ID or error
     */
    func validateUserId(_ userId: String?) -> ValidationResult<String> {
        guard let userId = userId?.trimmingCharacters(in: .whitespacesAndNewlines),
              !userId.isEmpty else {
            return .error("User ID cannot be empty")
        }
        
        let sanitizedUserId = sanitizeInput(userId)
        
        let range = NSRange(location: 0, length: sanitizedUserId.count)
        guard userIdRegex.firstMatch(in: sanitizedUserId, range: range) != nil else {
            return .error("User ID must be 3-50 characters and contain only letters, numbers, underscore, hyphen, or dot")
        }
        
        return .success(sanitizedUserId)
    }
    
    /**
     * Validates and sanitizes display name.
     * 
     * - Parameter name: The display name to validate
     * - Returns: ValidationResult containing the sanitized name or error
     */
    func validateDisplayName(_ name: String?) -> ValidationResult<String> {
        guard let name = name?.trimmingCharacters(in: .whitespacesAndNewlines),
              !name.isEmpty else {
            return .error("Display name cannot be empty")
        }
        
        let sanitizedName = sanitizeInput(name)
        
        guard sanitizedName.count <= Constants.maxNameLength else {
            return .error("Display name too long")
        }
        
        guard sanitizedName.count >= 2 else {
            return .error("Display name must be at least 2 characters")
        }
        
        // Check for potentially harmful content
        if containsSuspiciousContent(sanitizedName) {
            return .error("Display name contains invalid characters")
        }
        
        return .success(sanitizedName)
    }
    
    /**
     * Validates location coordinates.
     * 
     * - Parameters:
     *   - latitude: The latitude coordinate
     *   - longitude: The longitude coordinate
     * - Returns: ValidationResult containing validation status
     */
    func validateLocation(latitude: Double, longitude: Double) -> ValidationResult<(Double, Double)> {
        guard latitude >= Constants.minLatitude && latitude <= Constants.maxLatitude else {
            return .error("Invalid latitude: must be between \(Constants.minLatitude) and \(Constants.maxLatitude)")
        }
        
        guard longitude >= Constants.minLongitude && longitude <= Constants.maxLongitude else {
            return .error("Invalid longitude: must be between \(Constants.minLongitude) and \(Constants.maxLongitude)")
        }
        
        // Check for obviously fake coordinates (0,0 or other suspicious values)
        if latitude == 0.0 && longitude == 0.0 {
            return .error("Invalid location coordinates")
        }
        
        return .success((latitude, longitude))
    }
    
    /**
     * Validates and sanitizes message content.
     * 
     * - Parameter message: The message to validate
     * - Returns: ValidationResult containing the sanitized message or error
     */
    func validateMessage(_ message: String?) -> ValidationResult<String> {
        guard let message = message?.trimmingCharacters(in: .whitespacesAndNewlines),
              !message.isEmpty else {
            return .error("Message cannot be empty")
        }
        
        let sanitizedMessage = sanitizeInput(message)
        
        guard sanitizedMessage.count <= Constants.maxMessageLength else {
            return .error("Message too long")
        }
        
        // Check for potentially harmful content
        if containsSuspiciousContent(sanitizedMessage) {
            return .error("Message contains invalid content")
        }
        
        return .success(sanitizedMessage)
    }
    
    /**
     * Validates JWT token format.
     * 
     * - Parameter token: The JWT token to validate
     * - Returns: ValidationResult containing validation status
     */
    func validateJwtToken(_ token: String?) -> ValidationResult<String> {
        guard let token = token, !token.isEmpty else {
            return .error("Token cannot be empty")
        }
        
        let parts = token.components(separatedBy: ".")
        guard parts.count == 3 else {
            return .error("Invalid JWT token format")
        }
        
        // Validate each part is base64url encoded
        for part in parts {
            guard !part.isEmpty else {
                return .error("Invalid JWT token encoding")
            }
            
            let range = NSRange(location: 0, length: part.count)
            guard base64UrlRegex.firstMatch(in: part, range: range) != nil else {
                return .error("Invalid JWT token encoding")
            }
        }
        
        return .success(token)
    }
    
    /**
     * Sanitizes input by removing potentially harmful characters.
     * 
     * - Parameter input: The input string to sanitize
     * - Returns: The sanitized string
     */
    func sanitizeInput(_ input: String) -> String {
        var sanitized = input
        
        // Remove control characters
        sanitized = sanitized.replacingOccurrences(
            of: "[\u{00}-\u{1F}\u{7F}]",
            with: "",
            options: .regularExpression
        )
        
        // Remove potential script injection characters
        sanitized = sanitized.replacingOccurrences(
            of: "[<>\"'&]",
            with: "",
            options: .regularExpression
        )
        
        // Normalize whitespace
        sanitized = sanitized.replacingOccurrences(
            of: "\\s+",
            with: " ",
            options: .regularExpression
        )
        
        return sanitized.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    /**
     * Validates file upload data.
     * 
     * - Parameters:
     *   - fileName: The file name
     *   - fileSize: The file size in bytes
     *   - allowedExtensions: Array of allowed file extensions
     *   - maxSize: Maximum allowed file size
     * - Returns: ValidationResult containing validation status
     */
    func validateFileUpload(
        fileName: String?,
        fileSize: Int64,
        allowedExtensions: [String],
        maxSize: Int64
    ) -> ValidationResult<String> {
        guard let fileName = fileName, !fileName.isEmpty else {
            return .error("File name cannot be empty")
        }
        
        let sanitizedFileName = sanitizeInput(fileName)
        
        guard fileSize > 0 else {
            return .error("Invalid file size")
        }
        
        guard fileSize <= maxSize else {
            return .error("File too large")
        }
        
        let components = sanitizedFileName.components(separatedBy: ".")
        guard components.count > 1 else {
            return .error("File must have an extension")
        }
        
        let extension = components.last!.lowercased()
        guard allowedExtensions.contains(extension) else {
            return .error("File type not allowed")
        }
        
        // Check for potentially dangerous file names
        if containsSuspiciousContent(sanitizedFileName) {
            return .error("Invalid file name")
        }
        
        return .success(sanitizedFileName)
    }
    
    // MARK: - Private Methods
    
    private func isCommonPassword(_ password: String) -> Bool {
        let commonPasswords: Set<String> = [
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "1234567890", "password1"
        ]
        return commonPasswords.contains(password.lowercased())
    }
    
    private func containsSuspiciousContent(_ input: String) -> Bool {
        let suspiciousPatterns = [
            // Script injection patterns
            "(?i)<script",
            "(?i)javascript:",
            "(?i)vbscript:",
            "(?i)onload=",
            "(?i)onerror=",
            // SQL injection patterns
            "(?i)(union|select|insert|update|delete|drop|create|alter)\\s",
            // Path traversal patterns
            "\\.\\./",
            "\\\\\\.\\.\\\\",
            // Null bytes
            "\\x00"
        ]
        
        for pattern in suspiciousPatterns {
            do {
                let regex = try NSRegularExpression(pattern: pattern, options: .caseInsensitive)
                let range = NSRange(location: 0, length: input.count)
                if regex.firstMatch(in: input, range: range) != nil {
                    return true
                }
            } catch {
                continue
            }
        }
        
        return false
    }
}

// MARK: - ValidationResult

/**
 * Represents the result of a validation operation.
 */
enum ValidationResult<T> {
    case success(T)
    case error(String)
    
    var isSuccess: Bool {
        switch self {
        case .success:
            return true
        case .error:
            return false
        }
    }
    
    var isError: Bool {
        return !isSuccess
    }
    
    func getOrNull() -> T? {
        switch self {
        case .success(let data):
            return data
        case .error:
            return nil
        }
    }
    
    func getErrorMessage() -> String? {
        switch self {
        case .success:
            return nil
        case .error(let message):
            return message
        }
    }
}