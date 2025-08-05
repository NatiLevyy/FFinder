import Foundation

/**
 * Manages user data export functionality for privacy compliance.
 */
class DataExportManager {
    
    // MARK: - Constants
    
    private struct Constants {
        static let exportDirectory = "DataExports"
        static let exportFilePrefix = "user_data_export"
    }
    
    // MARK: - Singleton
    
    static let shared = DataExportManager()
    
    // MARK: - Private Properties
    
    private let sessionManager = SessionManager.shared
    private let secureStorage = SecureStorage.shared
    private let fileManager = FileManager.default
    
    // MARK: - Initialization
    
    private init() {}
    
    // MARK: - Public Methods
    
    /**
     * Exports all user data to a JSON file.
     * 
     * - Returns: Result containing the export file path or error
     */
    func exportUserData() async -> Result<String, Error> {
        do {
            guard let currentUser = sessionManager.getCurrentUserSync() else {
                return .failure(DataExportError.userNotAuthenticated)
            }
            
            // Collect all user data
            let exportData = UserDataExport(
                user: UserExportData(
                    id: currentUser.id,
                    email: currentUser.email,
                    displayName: currentUser.displayName,
                    profileImageUrl: currentUser.profileImageUrl,
                    createdAt: currentUser.createdAt,
                    lastActiveAt: currentUser.lastActiveAt
                ),
                friends: await collectFriendsData(),
                locations: await collectLocationData(),
                privacySettings: await collectPrivacySettings(),
                exportMetadata: ExportMetadata(
                    exportDate: Int64(Date().timeIntervalSince1970 * 1000),
                    exportVersion: "1.0",
                    dataTypes: ["user", "friends", "locations", "privacy_settings"]
                )
            )
            
            // Create export file
            let exportFile = try createExportFile()
            let jsonData = try JSONEncoder().encode(exportData)
            try jsonData.write(to: exportFile)
            
            return .success(exportFile.path)
        } catch {
            return .failure(error)
        }
    }
    
    /**
     * Exports specific data types only.
     * 
     * - Parameter dataTypes: Array of data types to export
     * - Returns: Result containing the export file path or error
     */
    func exportSpecificData(dataTypes: [DataType]) async -> Result<String, Error> {
        do {
            guard let currentUser = sessionManager.getCurrentUserSync() else {
                return .failure(DataExportError.userNotAuthenticated)
            }
            
            let exportData = UserDataExport(
                user: dataTypes.contains(.user) ? UserExportData(
                    id: currentUser.id,
                    email: currentUser.email,
                    displayName: currentUser.displayName,
                    profileImageUrl: currentUser.profileImageUrl,
                    createdAt: currentUser.createdAt,
                    lastActiveAt: currentUser.lastActiveAt
                ) : nil,
                friends: dataTypes.contains(.friends) ? await collectFriendsData() : [],
                locations: dataTypes.contains(.locations) ? await collectLocationData() : [],
                privacySettings: dataTypes.contains(.privacySettings) ? await collectPrivacySettings() : nil,
                exportMetadata: ExportMetadata(
                    exportDate: Int64(Date().timeIntervalSince1970 * 1000),
                    exportVersion: "1.0",
                    dataTypes: dataTypes.map { $0.rawValue.lowercased() }
                )
            )
            
            let exportFile = try createExportFile()
            let jsonData = try JSONEncoder().encode(exportData)
            try jsonData.write(to: exportFile)
            
            return .success(exportFile.path)
        } catch {
            return .failure(error)
        }
    }
    
    /**
     * Gets the list of available export files.
     * 
     * - Returns: Array of export file information
     */
    func getExportHistory() async -> [ExportFileInfo] {
        do {
            let exportDir = try getExportDirectory()
            let files = try fileManager.contentsOfDirectory(at: exportDir, includingPropertiesForKeys: [.fileSizeKey, .creationDateKey])
            
            return files.compactMap { fileURL in
                guard fileURL.lastPathComponent.hasPrefix(Constants.exportFilePrefix),
                      fileURL.pathExtension == "json" else {
                    return nil
                }
                
                do {
                    let attributes = try fileManager.attributesOfItem(atPath: fileURL.path)
                    let fileSize = attributes[.size] as? Int64 ?? 0
                    let createdAt = (attributes[.creationDate] as? Date)?.timeIntervalSince1970 ?? 0
                    
                    return ExportFileInfo(
                        fileName: fileURL.lastPathComponent,
                        filePath: fileURL.path,
                        fileSize: fileSize,
                        createdAt: Int64(createdAt * 1000)
                    )
                } catch {
                    return nil
                }
            }.sorted { $0.createdAt > $1.createdAt }
        } catch {
            return []
        }
    }
    
    /**
     * Deletes an export file.
     * 
     * - Parameter filePath: Path to the export file
     * - Returns: True if deleted successfully, false otherwise
     */
    func deleteExportFile(filePath: String) async -> Bool {
        do {
            let fileURL = URL(fileURLWithPath: filePath)
            let exportDir = try getExportDirectory()
            
            // Ensure file is in export directory for security
            guard fileURL.path.hasPrefix(exportDir.path) else {
                return false
            }
            
            try fileManager.removeItem(at: fileURL)
            return true
        } catch {
            return false
        }
    }
    
    /**
     * Cleans up old export files (older than 30 days).
     */
    func cleanupOldExports() async {
        do {
            let exportDir = try getExportDirectory()
            let files = try fileManager.contentsOfDirectory(at: exportDir, includingPropertiesForKeys: [.creationDateKey])
            
            let thirtyDaysAgo = Date().timeIntervalSince1970 - (30 * 24 * 60 * 60)
            
            for fileURL in files {
                do {
                    let attributes = try fileManager.attributesOfItem(atPath: fileURL.path)
                    if let creationDate = attributes[.creationDate] as? Date,
                       creationDate.timeIntervalSince1970 < thirtyDaysAgo {
                        try fileManager.removeItem(at: fileURL)
                    }
                } catch {
                    // Continue with other files
                }
            }
        } catch {
            // Log error but don't throw
        }
    }
    
    // MARK: - Private Methods
    
    private func collectFriendsData() async -> [FriendExportData] {
        // In a real implementation, this would fetch from FriendsRepository
        // For now, return empty array
        return []
    }
    
    private func collectLocationData() async -> [LocationExportData] {
        // In a real implementation, this would fetch from LocationRepository
        // For now, return empty array
        return []
    }
    
    private func collectPrivacySettings() async -> PrivacySettingsExportData? {
        do {
            let privacyManager = PrivacySettingsManager.shared
            let settings = await privacyManager.getPrivacySettings()
            
            return PrivacySettingsExportData(
                locationDataRetentionDays: settings.locationDataRetentionDays,
                shareLocationWithFriends: settings.shareLocationWithFriends,
                allowFriendRequests: settings.allowFriendRequests,
                showOnlineStatus: settings.showOnlineStatus,
                dataProcessingConsent: settings.dataProcessingConsent,
                marketingConsent: settings.marketingConsent,
                lastUpdated: settings.lastUpdated
            )
        } catch {
            return nil
        }
    }
    
    private func createExportFile() throws -> URL {
        let exportDir = try getExportDirectory()
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = formatter.string(from: Date())
        let fileName = "\(Constants.exportFilePrefix)_\(timestamp).json"
        
        return exportDir.appendingPathComponent(fileName)
    }
    
    private func getExportDirectory() throws -> URL {
        let documentsDir = try fileManager.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
        let exportDir = documentsDir.appendingPathComponent(Constants.exportDirectory)
        
        if !fileManager.fileExists(atPath: exportDir.path) {
            try fileManager.createDirectory(at: exportDir, withIntermediateDirectories: true)
        }
        
        return exportDir
    }
}

// MARK: - Data Structures

struct UserDataExport: Codable {
    let user: UserExportData?
    let friends: [FriendExportData]
    let locations: [LocationExportData]
    let privacySettings: PrivacySettingsExportData?
    let exportMetadata: ExportMetadata
}

struct UserExportData: Codable {
    let id: String
    let email: String
    let displayName: String
    let profileImageUrl: String?
    let createdAt: Int64
    let lastActiveAt: Int64
}

struct FriendExportData: Codable {
    let id: String
    let userId: String
    let userEmail: String
    let userDisplayName: String
    let friendshipStatus: String
    let locationSharingEnabled: Bool
    let locationSharingPermission: String
    let lastKnownLocation: LocationExportData?
}

struct LocationExportData: Codable {
    let latitude: Double
    let longitude: Double
    let accuracy: Float
    let timestamp: Int64
    let altitude: Double?
}

struct PrivacySettingsExportData: Codable {
    let locationDataRetentionDays: Int
    let shareLocationWithFriends: Bool
    let allowFriendRequests: Bool
    let showOnlineStatus: Bool
    let dataProcessingConsent: Bool
    let marketingConsent: Bool
    let lastUpdated: Int64
}

struct ExportMetadata: Codable {
    let exportDate: Int64
    let exportVersion: String
    let dataTypes: [String]
}

struct ExportFileInfo {
    let fileName: String
    let filePath: String
    let fileSize: Int64
    let createdAt: Int64
}

enum DataType: String, CaseIterable {
    case user = "USER"
    case friends = "FRIENDS"
    case locations = "LOCATIONS"
    case privacySettings = "PRIVACY_SETTINGS"
}

// MARK: - Error Types

enum DataExportError: Error, LocalizedError {
    case userNotAuthenticated
    case exportFailed(String)
    case fileCreationFailed
    
    var errorDescription: String? {
        switch self {
        case .userNotAuthenticated:
            return "User not authenticated"
        case .exportFailed(let message):
            return "Export failed: \(message)"
        case .fileCreationFailed:
            return "Failed to create export file"
        }
    }
}