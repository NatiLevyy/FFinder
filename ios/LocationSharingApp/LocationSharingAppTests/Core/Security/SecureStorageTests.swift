import XCTest
@testable import LocationSharingApp

class SecureStorageTests: XCTestCase {
    
    var secureStorage: SecureStorage!
    
    override func setUp() {
        super.setUp()
        secureStorage = SecureStorage.shared
    }
    
    override func tearDown() {
        Task {
            try await secureStorage.clear()
        }
        super.tearDown()
    }
    
    func testStoreAndRetrieveStringValue() async throws {
        // Given
        let key = "test_key"
        let value = "test_value"
        
        // When
        try await secureStorage.store(key: key, value: value)
        let retrievedValue = try await secureStorage.retrieve(key: key)
        
        // Then
        XCTAssertEqual(value, retrievedValue)
    }
    
    func testStoreAndRetrieveWithValidation() async throws {
        // Given
        let key = "validated_key"
        let value = "validated_value"
        
        // When
        try await secureStorage.store(key: key, value: value, requireValidation: true)
        let retrievedValue = try await secureStorage.retrieve(key: key, requireValidation: true)
        
        // Then
        XCTAssertEqual(value, retrievedValue)
    }
    
    func testRetrieveNonExistentKeyReturnsNil() async throws {
        // Given
        let key = "non_existent_key"
        
        // When
        let retrievedValue = try await secureStorage.retrieve(key: key)
        
        // Then
        XCTAssertNil(retrievedValue)
    }
    
    func testStoreAndRetrieveBinaryData() async throws {
        // Given
        let key = "binary_key"
        let data = "test binary data".data(using: .utf8)!
        
        // When
        try await secureStorage.storeData(key: key, data: data)
        let retrievedData = try await secureStorage.retrieveData(key: key)
        
        // Then
        XCTAssertEqual(data, retrievedData)
    }
    
    func testContainsReturnsTrueForExistingKey() async throws {
        // Given
        let key = "existing_key"
        let value = "existing_value"
        
        // When
        try await secureStorage.store(key: key, value: value)
        let exists = try await secureStorage.contains(key: key)
        
        // Then
        XCTAssertTrue(exists)
    }
    
    func testContainsReturnsFalseForNonExistentKey() async throws {
        // Given
        let key = "non_existent_key"
        
        // When
        let exists = try await secureStorage.contains(key: key)
        
        // Then
        XCTAssertFalse(exists)
    }
    
    func testRemoveKeySuccessfully() async throws {
        // Given
        let key = "removable_key"
        let value = "removable_value"
        
        // When
        try await secureStorage.store(key: key, value: value)
        XCTAssertTrue(try await secureStorage.contains(key: key))
        
        try await secureStorage.remove(key: key)
        
        // Then
        XCTAssertFalse(try await secureStorage.contains(key: key))
        XCTAssertNil(try await secureStorage.retrieve(key: key))
    }
    
    func testClearRemovesAllData() async throws {
        // Given
        try await secureStorage.store(key: "key1", value: "value1")
        try await secureStorage.store(key: "key2", value: "value2")
        try await secureStorage.store(key: "key3", value: "value3")
        
        // When
        try await secureStorage.clear()
        
        // Then
        XCTAssertFalse(try await secureStorage.contains(key: "key1"))
        XCTAssertFalse(try await secureStorage.contains(key: "key2"))
        XCTAssertFalse(try await secureStorage.contains(key: "key3"))
    }
    
    func testGetAllKeysReturnsAllStoredKeys() async throws {
        // Given
        let keys = ["key1", "key2", "key3"]
        for key in keys {
            try await secureStorage.store(key: key, value: "value_\(key)")
        }
        
        // When
        let retrievedKeys = await secureStorage.getAllKeys()
        
        // Then
        for key in keys {
            XCTAssertTrue(retrievedKeys.contains(key))
        }
    }
    
    func testInvalidKeyThrowsException() async {
        // Given
        let invalidKeys = ["", " ", "key with spaces", "key@with#special$chars"]
        
        // When & Then
        for invalidKey in invalidKeys {
            do {
                try await secureStorage.store(key: invalidKey, value: "value")
                XCTFail("Should have thrown an exception for invalid key: \(invalidKey)")
            } catch {
                // Expected to throw
            }
        }
    }
    
    func testTooLongKeyThrowsException() async {
        // Given
        let tooLongKey = String(repeating: "a", count: 101)
        
        // When & Then
        do {
            try await secureStorage.store(key: tooLongKey, value: "value")
            XCTFail("Should have thrown an exception for too long key")
        } catch {
            // Expected to throw
        }
    }
    
    func testTooLongValueThrowsException() async {
        // Given
        let key = "valid_key"
        let tooLongValue = String(repeating: "a", count: 10001)
        
        // When & Then
        do {
            try await secureStorage.store(key: key, value: tooLongValue)
            XCTFail("Should have thrown an exception for too long value")
        } catch {
            // Expected to throw
        }
    }
    
    func testSanitizationRemovesControlCharacters() async throws {
        // Given
        let key = "sanitization_key"
        let valueWithControlChars = "test\u{0000}\u{0001}\u{001F}value"
        let expectedValue = "testvalue"
        
        // When
        try await secureStorage.store(key: key, value: valueWithControlChars)
        let retrievedValue = try await secureStorage.retrieve(key: key)
        
        // Then
        XCTAssertEqual(expectedValue, retrievedValue)
    }
    
    func testConcurrentAccessIsThreadSafe() async throws {
        // Given
        let key = "concurrent_key"
        let numTasks = 10
        let numOperationsPerTask = 100
        
        // When
        await withTaskGroup(of: Void.self) { group in
            for taskId in 1...numTasks {
                group.addTask {
                    for operationId in 1...numOperationsPerTask {
                        let value = "task_\(taskId)_operation_\(operationId)"
                        let taskKey = "\(key)_\(taskId)_\(operationId)"
                        
                        do {
                            try await self.secureStorage.store(key: taskKey, value: value)
                            let retrieved = try await self.secureStorage.retrieve(key: taskKey)
                            XCTAssertEqual(value, retrieved)
                        } catch {
                            XCTFail("Concurrent operation failed: \(error)")
                        }
                    }
                }
            }
        }
        
        // Then
        let allKeys = await secureStorage.getAllKeys()
        let testKeys = allKeys.filter { $0.hasPrefix(key) }
        XCTAssertEqual(numTasks * numOperationsPerTask, testKeys.count)
    }
    
    func testEncryptionAndDecryptionOfBinaryData() async throws {
        // Given
        let key = "encryption_test_key"
        let originalData = Data((0..<1000).map { UInt8($0 % 256) })
        
        // When
        try await secureStorage.storeData(key: key, data: originalData)
        let decryptedData = try await secureStorage.retrieveData(key: key)
        
        // Then
        XCTAssertNotNil(decryptedData)
        XCTAssertEqual(originalData, decryptedData)
    }
    
    func testCorruptedBinaryDataReturnsNil() async throws {
        // Given
        let key = "corruption_test_key"
        let originalData = "test data".data(using: .utf8)!
        
        // When
        try await secureStorage.storeData(key: key, data: originalData)
        
        // Simulate corruption by storing invalid base64 data directly in keychain
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.locationsharing.app.secure",
            kSecAttrAccount as String: key,
            kSecValueData as String: "invalid_base64_data!@#".data(using: .utf8)!,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
        
        // Then
        let retrievedData = try await secureStorage.retrieveData(key: key)
        XCTAssertNil(retrievedData)
    }
    
    func testValidationHashPreventsDataTampering() async throws {
        // Given
        let key = "tamper_test_key"
        let value = "original_value"
        
        // When
        try await secureStorage.store(key: key, value: value, requireValidation: true)
        
        // Simulate tampering by directly modifying stored value in keychain
        let tamperedValue = "tampered:modified_value"
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.locationsharing.app.secure",
            kSecAttrAccount as String: key,
            kSecValueData as String: tamperedValue.data(using: .utf8)!,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
        
        // Then
        let retrievedValue = try await secureStorage.retrieve(key: key, requireValidation: true)
        XCTAssertNil(retrievedValue) // Should return nil due to validation failure
    }
    
    func testLargeDataHandling() async throws {
        // Given
        let key = "large_data_key"
        let largeData = Data(repeating: 0xFF, count: 1024 * 1024) // 1MB of data
        
        // When
        try await secureStorage.storeData(key: key, data: largeData)
        let retrievedData = try await secureStorage.retrieveData(key: key)
        
        // Then
        XCTAssertNotNil(retrievedData)
        XCTAssertEqual(largeData, retrievedData)
    }
    
    func testSpecialCharactersInValues() async throws {
        // Given
        let key = "special_chars_key"
        let specialValue = "Value with émojis 🔐, unicode ñ, and symbols @#$%^&*()"
        
        // When
        try await secureStorage.store(key: key, value: specialValue)
        let retrievedValue = try await secureStorage.retrieve(key: key)
        
        // Then
        XCTAssertEqual(specialValue, retrievedValue)
    }
}