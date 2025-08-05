import XCTest
@testable import LocationSharingApp

class DataValidatorTests: XCTestCase {
    
    var dataValidator: DataValidator!
    
    override func setUp() {
        super.setUp()
        dataValidator = DataValidator.shared
    }
    
    func testValidateEmailReturnsSuccessForValidEmail() {
        // Given
        let validEmails = [
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "123@example.com"
        ]
        
        // When & Then
        for email in validEmails {
            let result = dataValidator.validateEmail(email)
            XCTAssertTrue(result.isSuccess, "Email \(email) should be valid")
            XCTAssertEqual(email.lowercased(), result.getOrNull())
        }
    }
    
    func testValidateEmailReturnsErrorForInvalidEmail() {
        // Given
        let invalidEmails: [String?] = [
            nil,
            "",
            "   ",
            "invalid-email",
            "@example.com",
            "user@",
            "user@.com",
            "user..name@example.com",
            "user@example",
            String(repeating: "a", count: 255) + "@example.com" // Too long
        ]
        
        // When & Then
        for email in invalidEmails {
            let result = dataValidator.validateEmail(email)
            XCTAssertTrue(result.isError, "Email \(String(describing: email)) should be invalid")
        }
    }
    
    func testValidatePasswordReturnsSuccessForStrongPassword() {
        // Given
        let strongPasswords = [
            "StrongPass123",
            "MySecure@Pass1",
            "Complex$Password9",
            "Aa1bcdefgh"
        ]
        
        // When & Then
        for password in strongPasswords {
            let result = dataValidator.validatePassword(password)
            XCTAssertTrue(result.isSuccess, "Password \(password) should be valid")
            XCTAssertEqual(password, result.getOrNull())
        }
    }
    
    func testValidatePasswordReturnsErrorForWeakPassword() {
        // Given
        let weakPasswords: [String?] = [
            nil,
            "",
            "short",
            "alllowercase",
            "ALLUPPERCASE",
            "NoNumbers!",
            "nonumbers123",
            "NONUMBERS123",
            "password", // Common password
            "123456", // Common password
            String(repeating: "a", count: 129) // Too long
        ]
        
        // When & Then
        for password in weakPasswords {
            let result = dataValidator.validatePassword(password)
            XCTAssertTrue(result.isError, "Password \(String(describing: password)) should be invalid")
        }
    }
    
    func testValidateUserIdReturnsSuccessForValidUserId() {
        // Given
        let validUserIds = [
            "user123",
            "test_user",
            "user.name",
            "user-id",
            String(repeating: "a", count: 50) // Max length
        ]
        
        // When & Then
        for userId in validUserIds {
            let result = dataValidator.validateUserId(userId)
            XCTAssertTrue(result.isSuccess, "User ID \(userId) should be valid")
            XCTAssertEqual(userId, result.getOrNull())
        }
    }
    
    func testValidateUserIdReturnsErrorForInvalidUserId() {
        // Given
        let invalidUserIds: [String?] = [
            nil,
            "",
            "ab", // Too short
            "user@id", // Invalid character
            "user id", // Space not allowed
            "user#id", // Invalid character
            String(repeating: "a", count: 51) // Too long
        ]
        
        // When & Then
        for userId in invalidUserIds {
            let result = dataValidator.validateUserId(userId)
            XCTAssertTrue(result.isError, "User ID \(String(describing: userId)) should be invalid")
        }
    }
    
    func testValidateDisplayNameReturnsSuccessForValidName() {
        // Given
        let validNames = [
            "John Doe",
            "Alice",
            "Bob Smith Jr",
            "María García",
            "李小明"
        ]
        
        // When & Then
        for name in validNames {
            let result = dataValidator.validateDisplayName(name)
            XCTAssertTrue(result.isSuccess, "Display name \(name) should be valid")
        }
    }
    
    func testValidateDisplayNameReturnsErrorForInvalidName() {
        // Given
        let invalidNames: [String?] = [
            nil,
            "",
            "A", // Too short
            String(repeating: "a", count: 101), // Too long
            "<script>alert('xss')</script>", // Suspicious content
            "SELECT * FROM users" // Suspicious content
        ]
        
        // When & Then
        for name in invalidNames {
            let result = dataValidator.validateDisplayName(name)
            XCTAssertTrue(result.isError, "Display name \(String(describing: name)) should be invalid")
        }
    }
    
    func testValidateLocationReturnsSuccessForValidCoordinates() {
        // Given
        let validCoordinates = [
            (37.7749, -122.4194), // San Francisco
            (40.7128, -74.0060), // New York
            (-33.8688, 151.2093), // Sydney
            (90.0, 180.0), // Max values
            (-90.0, -180.0) // Min values
        ]
        
        // When & Then
        for (lat, lng) in validCoordinates {
            let result = dataValidator.validateLocation(latitude: lat, longitude: lng)
            XCTAssertTrue(result.isSuccess, "Coordinates (\(lat), \(lng)) should be valid")
            XCTAssertEqual(lat, result.getOrNull()?.0)
            XCTAssertEqual(lng, result.getOrNull()?.1)
        }
    }
    
    func testValidateLocationReturnsErrorForInvalidCoordinates() {
        // Given
        let invalidCoordinates = [
            (91.0, 0.0), // Latitude too high
            (-91.0, 0.0), // Latitude too low
            (0.0, 181.0), // Longitude too high
            (0.0, -181.0), // Longitude too low
            (0.0, 0.0) // Suspicious coordinates
        ]
        
        // When & Then
        for (lat, lng) in invalidCoordinates {
            let result = dataValidator.validateLocation(latitude: lat, longitude: lng)
            XCTAssertTrue(result.isError, "Coordinates (\(lat), \(lng)) should be invalid")
        }
    }
    
    func testValidateMessageReturnsSuccessForValidMessage() {
        // Given
        let validMessages = [
            "Hello, world!",
            "This is a test message.",
            "Message with numbers 123 and symbols !@#",
            String(repeating: "a", count: 1000) // Max length
        ]
        
        // When & Then
        for message in validMessages {
            let result = dataValidator.validateMessage(message)
            XCTAssertTrue(result.isSuccess, "Message should be valid")
        }
    }
    
    func testValidateMessageReturnsErrorForInvalidMessage() {
        // Given
        let invalidMessages: [String?] = [
            nil,
            "",
            "   ",
            String(repeating: "a", count: 1001), // Too long
            "<script>alert('xss')</script>", // Suspicious content
            "DROP TABLE users;" // Suspicious content
        ]
        
        // When & Then
        for message in invalidMessages {
            let result = dataValidator.validateMessage(message)
            XCTAssertTrue(result.isError, "Message should be invalid")
        }
    }
    
    func testValidateJwtTokenReturnsSuccessForValidJWT() {
        // Given
        let validTokens = [
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            "header.payload.signature",
            "abc123.def456.ghi789"
        ]
        
        // When & Then
        for token in validTokens {
            let result = dataValidator.validateJwtToken(token)
            XCTAssertTrue(result.isSuccess, "JWT token should be valid")
            XCTAssertEqual(token, result.getOrNull())
        }
    }
    
    func testValidateJwtTokenReturnsErrorForInvalidJWT() {
        // Given
        let invalidTokens: [String?] = [
            nil,
            "",
            "invalid.token", // Only 2 parts
            "too.many.parts.here", // Too many parts
            "invalid@chars.payload.signature", // Invalid characters
            ".payload.signature", // Empty header
            "header..signature" // Empty payload
        ]
        
        // When & Then
        for token in invalidTokens {
            let result = dataValidator.validateJwtToken(token)
            XCTAssertTrue(result.isError, "JWT token \(String(describing: token)) should be invalid")
        }
    }
    
    func testSanitizeInputRemovesHarmfulCharacters() {
        // Given
        let inputsAndExpected = [
            ("normal text", "normal text"),
            ("text\u{0000}with\u{0001}control\u{001F}chars", "textwithcontrolchars"),
            ("text<script>alert('xss')</script>", "textscriptalert('xss')/script"),
            ("text   with   multiple   spaces", "text with multiple spaces"),
            ("  leading and trailing spaces  ", "leading and trailing spaces"),
            ("text\"with'quotes&ampersand<>", "textwithquotesampersand")
        ]
        
        // When & Then
        for (input, expected) in inputsAndExpected {
            let result = dataValidator.sanitizeInput(input)
            XCTAssertEqual(expected, result, "Input '\(input)' should be sanitized to '\(expected)'")
        }
    }
    
    func testValidateFileUploadReturnsSuccessForValidFile() {
        // Given
        let fileName = "document.pdf"
        let fileSize: Int64 = 1024 * 1024 // 1MB
        let allowedExtensions = ["pdf", "doc", "docx", "txt"]
        let maxSize: Int64 = 5 * 1024 * 1024 // 5MB
        
        // When
        let result = dataValidator.validateFileUpload(
            fileName: fileName,
            fileSize: fileSize,
            allowedExtensions: allowedExtensions,
            maxSize: maxSize
        )
        
        // Then
        XCTAssertTrue(result.isSuccess)
        XCTAssertEqual(fileName, result.getOrNull())
    }
    
    func testValidateFileUploadReturnsErrorForInvalidFile() {
        // Given
        let allowedExtensions = ["pdf", "doc", "docx", "txt"]
        let maxSize: Int64 = 5 * 1024 * 1024 // 5MB
        
        let invalidFiles: [(String?, Int64)] = [
            (nil, 1024),
            ("", 1024),
            ("document.pdf", 0),
            ("document.pdf", maxSize + 1),
            ("document.exe", 1024),
            ("document", 1024),
            ("../../../etc/passwd", 1024)
        ]
        
        // When & Then
        for (fileName, fileSize) in invalidFiles {
            let result = dataValidator.validateFileUpload(
                fileName: fileName,
                fileSize: fileSize,
                allowedExtensions: allowedExtensions,
                maxSize: maxSize
            )
            XCTAssertTrue(result.isError, "File validation should fail for \(String(describing: fileName))")
        }
    }
    
    func testValidationResultHelperMethods() {
        // Given
        let successResult: ValidationResult<String> = .success("test")
        let errorResult: ValidationResult<String> = .error("error message")
        
        // When & Then
        XCTAssertTrue(successResult.isSuccess)
        XCTAssertFalse(successResult.isError)
        XCTAssertEqual("test", successResult.getOrNull())
        XCTAssertNil(successResult.getErrorMessage())
        
        XCTAssertFalse(errorResult.isSuccess)
        XCTAssertTrue(errorResult.isError)
        XCTAssertNil(errorResult.getOrNull())
        XCTAssertEqual("error message", errorResult.getErrorMessage())
    }
    
    func testEmailCaseNormalization() {
        // Given
        let mixedCaseEmail = "Test.User@EXAMPLE.COM"
        let expectedEmail = "test.user@example.com"
        
        // When
        let result = dataValidator.validateEmail(mixedCaseEmail)
        
        // Then
        XCTAssertTrue(result.isSuccess)
        XCTAssertEqual(expectedEmail, result.getOrNull())
    }
    
    func testSpecialCharacterHandling() {
        // Given
        let specialChars = "Text with émojis 🔐, unicode ñ, and symbols"
        
        // When
        let result = dataValidator.sanitizeInput(specialChars)
        
        // Then
        XCTAssertEqual(specialChars, result) // Should preserve valid special characters
    }
    
    func testLongInputHandling() {
        // Given
        let longEmail = String(repeating: "a", count: 250) + "@example.com"
        let veryLongEmail = String(repeating: "a", count: 300) + "@example.com"
        
        // When
        let validResult = dataValidator.validateEmail(longEmail)
        let invalidResult = dataValidator.validateEmail(veryLongEmail)
        
        // Then
        XCTAssertTrue(validResult.isSuccess)
        XCTAssertTrue(invalidResult.isError)
    }
}