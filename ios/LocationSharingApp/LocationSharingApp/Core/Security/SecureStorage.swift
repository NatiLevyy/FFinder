import Foundation
import Security
import CryptoKit

/**
 * Provides secure storage functionality with encryption and data validation.
 * Uses iOS Keychain with additional security layers.
 */
class SecureStorage {
    
    // MARK: - Constants
    
    private struct Constants {
        static let keychainService = "com.locationsharing.app.secure"
        static let validationSaltKey = "validation_salt"
        static let maxKeyLength = 100
        static let maxValueLength = 10000
    }
    
    // MARK: - Singleton
    
    static let shared = SecureStorage()
    
    // MARK: - Private Properties
    
    private let queue = DispatchQueue(label: "secure.storage.queue", qos: .utility)
    
    // MARK: - Initialization
    
    private init() {}
    
    // MARK: - Public Methods
    
    /**
     * Stores a value securely with additional encryption and validation.
     * 
     * - Parameters:
     *   - key: The storage key
     *   - value: The value to store
     *   - requireValidation: Whether to add validation hash
     */
    func store(key: String, value: String, requireValidation: Bool = true) async throws {
        try validateKey(key)
        try validateValue(value)
        
        let sanitizedValue = sanitizeValue(value)
        let finalValue = requireValidation ? try await addValidationHash(sanitizedValue) : sanitizedValue
        
        try await withCheckedThrowingContinuation { continuation in
            queue.async {
                do {
                    try self.storeInKeychain(key: key, value: finalValue)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Retrieves a value from secure storage with validation.
     * 
     * - Parameters:
     *   - key: The storage key
     *   - requireValidation: Whether to validate the retrieved value
     * - Returns: The stored value or nil if not found or validation fails
     */
    func retrieve(key: String, requireValidation: Bool = true) async throws -> String? {
        try validateKey(key)
        
        return try await withCheckedThrowingContinuation { continuation in
            queue.async {
                do {
                    guard let storedValue = self.retrieveFromKeychain(key: key) else {
                        continuation.resume(returning: nil)
                        return
                    }
                    
                    if requireValidation {
                        let validatedValue = try await self.validateAndExtractValue(storedValue)
                        continuation.resume(returning: validatedValue)
                    } else {
                        continuation.resume(returning: storedValue)
                    }
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Stores binary data securely.
     * 
     * - Parameters:
     *   - key: The storage key
     *   - data: The binary data to store
     */
    func storeData(key: String, data: Data) async throws {
        try validateKey(key)
        
        let encryptedData = try encryptData(data)
        let encodedData = encryptedData.base64EncodedString()
        
        try await withCheckedThrowingContinuation { continuation in
            queue.async {
                do {
                    try self.storeInKeychain(key: key, value: encodedData)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Retrieves binary data from secure storage.
     * 
     * - Parameter key: The storage key
     * - Returns: The stored binary data or nil if not found
     */
    func retrieveData(key: String) async throws -> Data? {
        try validateKey(key)
        
        return try await withCheckedThrowingContinuation { continuation in
            queue.async {
                do {
                    guard let encodedData = self.retrieveFromKeychain(key: key),
                          let encryptedData = Data(base64Encoded: encodedData) else {
                        continuation.resume(returning: nil)
                        return
                    }
                    
                    let decryptedData = try self.decryptData(encryptedData)
                    continuation.resume(returning: decryptedData)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Removes a value from secure storage.
     * 
     * - Parameter key: The storage key
     */
    func remove(key: String) async throws {
        try validateKey(key)
        
        try await withCheckedThrowingContinuation { continuation in
            queue.async {
                do {
                    try self.deleteFromKeychain(key: key)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Checks if a key exists in secure storage.
     * 
     * - Parameter key: The storage key
     * - Returns: True if the key exists, false otherwise
     */
    func contains(key: String) async throws -> Bool {
        try validateKey(key)
        
        return await withCheckedContinuation { continuation in
            queue.async {
                let exists = self.keyExistsInKeychain(key: key)
                continuation.resume(returning: exists)
            }
        }
    }
    
    /**
     * Clears all data from secure storage.
     */
    func clear() async throws {
        try await withCheckedThrowingContinuation { continuation in
            queue.async {
                do {
                    try self.clearAllFromKeychain()
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
    
    /**
     * Gets all keys from secure storage.
     * 
     * - Returns: Array of all keys
     */
    func getAllKeys() async -> [String] {
        return await withCheckedContinuation { continuation in
            queue.async {
                let keys = self.getAllKeysFromKeychain()
                continuation.resume(returning: keys)
            }
        }
    }
    
    // MARK: - Private Methods
    
    private func validateKey(_ key: String) throws {
        guard !key.isEmpty else {
            throw SecureStorageError.invalidKey("Storage key cannot be empty")
        }
        
        guard key.count <= Constants.maxKeyLength else {
            throw SecureStorageError.invalidKey("Storage key too long")
        }
        
        let validKeyRegex = "^[a-zA-Z0-9_.-]+$"
        let predicate = NSPredicate(format: "SELF MATCHES %@", validKeyRegex)
        guard predicate.evaluate(with: key) else {
            throw SecureStorageError.invalidKey("Storage key contains invalid characters")
        }
    }
    
    private func validateValue(_ value: String) throws {
        guard value.count <= Constants.maxValueLength else {
            throw SecureStorageError.invalidValue("Storage value too long")
        }
    }
    
    private func sanitizeValue(_ value: String) -> String {
        // Remove any potential control characters
        return value.replacingOccurrences(of: "[\u{00}-\u{1F}\u{7F}]", with: "", options: .regularExpression)
    }
    
    private func addValidationHash(_ value: String) async throws -> String {
        let salt = try await getOrCreateValidationSalt()
        let hash = generateHash(value + salt)
        return "\(hash):\(value)"
    }
    
    private func validateAndExtractValue(_ storedValue: String) async throws -> String? {
        let parts = storedValue.components(separatedBy: ":")
        guard parts.count == 2 else { return nil }
        
        let hash = parts[0]
        let value = parts[1]
        let salt = try await getOrCreateValidationSalt()
        let expectedHash = generateHash(value + salt)
        
        return hash == expectedHash ? value : nil
    }
    
    private func getOrCreateValidationSalt() async throws -> String {
        if let existingSalt = retrieveFromKeychain(key: Constants.validationSaltKey) {
            return existingSalt
        }
        
        let newSalt = generateRandomString(length: 32)
        try storeInKeychain(key: Constants.validationSaltKey, value: newSalt)
        return newSalt
    }
    
    private func generateHash(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashed = SHA256.hash(data: inputData)
        return Data(hashed).base64EncodedString()
    }
    
    private func generateRandomString(length: Int) -> String {
        let chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return String((0..<length).map { _ in chars.randomElement()! })
    }
    
    private func encryptData(_ data: Data) throws -> Data {
        let key = SymmetricKey(size: .bits256)
        let sealedBox = try AES.GCM.seal(data, using: key)
        
        // Store the key securely
        let keyData = key.withUnsafeBytes { Data($0) }
        let keyAlias = "data_encryption_key_\(Date().timeIntervalSince1970)"
        try storeInKeychain(key: "key_\(keyAlias)", value: keyData.base64EncodedString())
        
        // Combine key alias and encrypted data
        let keyAliasData = keyAlias.data(using: .utf8)!
        let keyAliasLength = UInt32(keyAliasData.count).bigEndian
        
        var result = Data()
        result.append(Data(bytes: &keyAliasLength, count: MemoryLayout<UInt32>.size))
        result.append(keyAliasData)
        result.append(sealedBox.combined!)
        
        return result
    }
    
    private func decryptData(_ encryptedData: Data) throws -> Data {
        var offset = 0
        
        // Extract key alias length
        let keyAliasLength = encryptedData.subdata(in: offset..<offset + MemoryLayout<UInt32>.size)
            .withUnsafeBytes { $0.load(as: UInt32.self).bigEndian }
        offset += MemoryLayout<UInt32>.size
        
        // Extract key alias
        let keyAliasData = encryptedData.subdata(in: offset..<offset + Int(keyAliasLength))
        let keyAlias = String(data: keyAliasData, encoding: .utf8)!
        offset += Int(keyAliasLength)
        
        // Extract encrypted data
        let sealedData = encryptedData.subdata(in: offset..<encryptedData.count)
        
        // Retrieve key
        guard let keyString = retrieveFromKeychain(key: "key_\(keyAlias)"),
              let keyData = Data(base64Encoded: keyString) else {
            throw SecureStorageError.decryptionFailed("Encryption key not found")
        }
        
        let key = SymmetricKey(data: keyData)
        let sealedBox = try AES.GCM.SealedBox(combined: sealedData)
        
        return try AES.GCM.open(sealedBox, using: key)
    }
    
    // MARK: - Keychain Operations
    
    private func storeInKeychain(key: String, value: String) throws {
        let data = value.data(using: .utf8)!
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        
        // Delete existing item first
        SecItemDelete(query as CFDictionary)
        
        // Add new item
        let status = SecItemAdd(query as CFDictionary, nil)
        
        guard status == errSecSuccess else {
            throw SecureStorageError.keychainError("Failed to store in keychain: \(status)")
        }
    }
    
    private func retrieveFromKeychain(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let data = result as? Data,
              let string = String(data: data, encoding: .utf8) else {
            return nil
        }
        
        return string
    }
    
    private func deleteFromKeychain(key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecAttrAccount as String: key
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        // It's okay if the item doesn't exist
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw SecureStorageError.keychainError("Failed to delete from keychain: \(status)")
        }
    }
    
    private func keyExistsInKeychain(key: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecAttrAccount as String: key,
            kSecReturnData as String: false
        ]
        
        let status = SecItemCopyMatching(query as CFDictionary, nil)
        return status == errSecSuccess
    }
    
    private func clearAllFromKeychain() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService
        ]
        
        let status = SecItemDelete(query as CFDictionary)
        
        // It's okay if no items exist
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw SecureStorageError.keychainError("Failed to clear keychain: \(status)")
        }
    }
    
    private func getAllKeysFromKeychain() -> [String] {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Constants.keychainService,
            kSecReturnAttributes as String: true,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let items = result as? [[String: Any]] else {
            return []
        }
        
        return items.compactMap { item in
            item[kSecAttrAccount as String] as? String
        }
    }
}

// MARK: - Error Types

enum SecureStorageError: Error, LocalizedError {
    case invalidKey(String)
    case invalidValue(String)
    case keychainError(String)
    case encryptionFailed(String)
    case decryptionFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .invalidKey(let message):
            return "Invalid key: \(message)"
        case .invalidValue(let message):
            return "Invalid value: \(message)"
        case .keychainError(let message):
            return "Keychain error: \(message)"
        case .encryptionFailed(let message):
            return "Encryption failed: \(message)"
        case .decryptionFailed(let message):
            return "Decryption failed: \(message)"
        }
    }
}