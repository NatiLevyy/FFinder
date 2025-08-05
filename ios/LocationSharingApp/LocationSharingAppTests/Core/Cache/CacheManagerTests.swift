import XCTest
import CoreData
@testable import LocationSharingApp

class CacheManagerTests: XCTestCase {
    
    var cacheManager: CacheManager!
    var testContext: NSManagedObjectContext!
    
    override func setUp() {
        super.setUp()
        cacheManager = CacheManager.shared
        
        // Set up in-memory Core Data stack for testing
        let container = NSPersistentContainer(name: "LocationSharingCache")
        let description = NSPersistentStoreDescription()
        description.type = NSInMemoryStoreType
        container.persistentStoreDescriptions = [description]
        
        container.loadPersistentStores { _, error in
            XCTAssertNil(error)
        }
        
        testContext = container.viewContext
    }
    
    override func tearDown() {
        cacheManager = nil
        testContext = nil
        super.tearDown()
    }
    
    func testCacheLocation_ShouldStoreLocationData() async throws {
        // Given
        let userId = "user123"
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        
        // When
        try await cacheManager.cacheLocation(userId: userId, location: location)
        
        // Then
        let cachedLocation = try await cacheManager.getLastKnownLocation(userId: userId)
        XCTAssertNotNil(cachedLocation)
        XCTAssertEqual(location.latitude, cachedLocation?.latitude)
        XCTAssertEqual(location.longitude, cachedLocation?.longitude)
        XCTAssertEqual(location.accuracy, cachedLocation?.accuracy)
        XCTAssertEqual(location.timestamp, cachedLocation?.timestamp)
        XCTAssertEqual(location.altitude, cachedLocation?.altitude)
    }
    
    func testGetLastKnownLocation_ShouldReturnNilForUnknownUser() async throws {
        // Given
        let unknownUserId = "unknown"
        
        // When
        let result = try await cacheManager.getLastKnownLocation(userId: unknownUserId)
        
        // Then
        XCTAssertNil(result)
    }
    
    func testGetCachedLocations_ShouldReturnLocationsWithinTimeRange() async throws {
        // Given
        let userId = "user123"
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let oneHourAgo = now - (60 * 60 * 1000)
        let twoHoursAgo = now - (2 * 60 * 60 * 1000)
        
        let recentLocation = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: now, altitude: nil)
        let oldLocation = Location(latitude: 37.7750, longitude: -122.4195, accuracy: 5.0, timestamp: twoHoursAgo, altitude: nil)
        
        // When
        try await cacheManager.cacheLocation(userId: userId, location: recentLocation)
        try await cacheManager.cacheLocation(userId: userId, location: oldLocation)
        
        let recentLocations = try await cacheManager.getCachedLocations(userId: userId, since: oneHourAgo)
        
        // Then
        XCTAssertEqual(1, recentLocations.count)
        XCTAssertEqual(recentLocation.timestamp, recentLocations[0].timestamp)
    }
    
    func testCacheFriends_ShouldStoreFriendsData() async throws {
        // Given
        let user = User(
            id: "user1",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: nil,
            createdAt: 0,
            lastActiveAt: 0
        )
        let friend = Friend(
            id: "friend1",
            user: user,
            friendshipStatus: .accepted,
            locationSharingEnabled: true,
            lastKnownLocation: nil,
            locationSharingPermission: .granted
        )
        let friends = [friend]
        
        // When
        try await cacheManager.cacheFriends(friends)
        
        // Then
        let cachedFriends = try await cacheManager.getCachedFriends()
        XCTAssertEqual(1, cachedFriends.count)
        XCTAssertEqual(friend.id, cachedFriends[0].id)
        XCTAssertEqual(friend.user.email, cachedFriends[0].user.email)
        XCTAssertEqual(friend.friendshipStatus, cachedFriends[0].friendshipStatus)
    }
    
    func testQueuePendingOperation_ShouldAddOperationToQueue() async throws {
        // Given
        let operationType = OperationType.locationUpdate
        let data = """{"userId":"user123","latitude":37.7749,"longitude":-122.4194}"""
        let priority = 10
        
        // When
        try await cacheManager.queuePendingOperation(
            operationType: operationType,
            data: data,
            priority: priority
        )
        
        // Then
        let operations = try await cacheManager.getPendingOperations()
        XCTAssertEqual(1, operations.count)
        XCTAssertEqual(operationType.rawValue, operations[0].operationType)
        XCTAssertEqual(data, operations[0].data)
        XCTAssertEqual(Int32(priority), operations[0].priority)
    }
    
    func testGetPendingOperations_ShouldReturnOperationsOrderedByPriority() async throws {
        // Given
        try await cacheManager.queuePendingOperation(operationType: .friendRequest, data: "data1", priority: 5)
        try await cacheManager.queuePendingOperation(operationType: .locationUpdate, data: "data2", priority: 10)
        try await cacheManager.queuePendingOperation(operationType: .friendRemoval, data: "data3", priority: 3)
        
        // When
        let operations = try await cacheManager.getPendingOperations()
        
        // Then
        XCTAssertEqual(3, operations.count)
        XCTAssertEqual(10, operations[0].priority) // Highest priority first
        XCTAssertEqual(5, operations[1].priority)
        XCTAssertEqual(3, operations[2].priority)
    }
    
    func testCompletePendingOperation_ShouldRemoveOperationFromQueue() async throws {
        // Given
        try await cacheManager.queuePendingOperation(operationType: .locationUpdate, data: "data", priority: 10)
        let operations = try await cacheManager.getPendingOperations()
        let operation = operations[0]
        
        // When
        try await cacheManager.completePendingOperation(operation)
        
        // Then
        let remainingOperations = try await cacheManager.getPendingOperations()
        XCTAssertEqual(0, remainingOperations.count)
    }
    
    func testIncrementRetryCount_ShouldIncreaseRetryCount() async throws {
        // Given
        try await cacheManager.queuePendingOperation(operationType: .locationUpdate, data: "data", priority: 10)
        let operations = try await cacheManager.getPendingOperations()
        let operation = operations[0]
        let initialRetryCount = operation.retryCount
        
        // When
        try await cacheManager.incrementRetryCount(operation)
        
        // Then
        let updatedOperations = try await cacheManager.getPendingOperations()
        XCTAssertEqual(initialRetryCount + 1, updatedOperations[0].retryCount)
    }
    
    func testCleanupFailedOperations_ShouldRemoveOperationsWithMaxRetries() async throws {
        // Given
        let maxRetries = 3
        try await cacheManager.queuePendingOperation(operationType: .locationUpdate, data: "data1", priority: 10)
        try await cacheManager.queuePendingOperation(operationType: .friendRequest, data: "data2", priority: 5)
        
        let operations = try await cacheManager.getPendingOperations()
        let failedOperation = operations[0]
        
        // Simulate failed operations
        for _ in 0...maxRetries {
            try await cacheManager.incrementRetryCount(failedOperation)
        }
        
        // When
        try await cacheManager.cleanupFailedOperations(maxRetries: maxRetries)
        
        // Then
        let remainingOperations = try await cacheManager.getPendingOperations()
        XCTAssertEqual(1, remainingOperations.count) // Only one operation should remain
        XCTAssertEqual("data2", remainingOperations[0].data)
    }
    
    func testGetCacheStats_ShouldReturnCorrectStatistics() async throws {
        // Given
        let userId = "user123"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000), altitude: nil)
        let user = User(id: "user1", email: "test@example.com", displayName: "Test User", profileImageUrl: nil, createdAt: 0, lastActiveAt: 0)
        let friend = Friend(id: "friend1", user: user, friendshipStatus: .accepted, locationSharingEnabled: true, lastKnownLocation: nil, locationSharingPermission: .granted)
        
        // When
        try await cacheManager.cacheLocation(userId: userId, location: location)
        try await cacheManager.cacheFriends([friend])
        try await cacheManager.queuePendingOperation(operationType: .locationUpdate, data: "data", priority: 10)
        
        // Then
        let stats = try await cacheManager.getCacheStats()
        XCTAssertGreaterThanOrEqual(stats.locationCount, 1)
        XCTAssertGreaterThanOrEqual(stats.friendCount, 1)
        XCTAssertGreaterThanOrEqual(stats.pendingOperationCount, 1)
        XCTAssertGreaterThanOrEqual(stats.totalCacheSize, 3)
    }
    
    func testClearAllCache_ShouldRemoveAllCachedData() async throws {
        // Given
        let userId = "user123"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000), altitude: nil)
        let user = User(id: "user1", email: "test@example.com", displayName: "Test User", profileImageUrl: nil, createdAt: 0, lastActiveAt: 0)
        let friend = Friend(id: "friend1", user: user, friendshipStatus: .accepted, locationSharingEnabled: true, lastKnownLocation: nil, locationSharingPermission: .granted)
        
        try await cacheManager.cacheLocation(userId: userId, location: location)
        try await cacheManager.cacheFriends([friend])
        try await cacheManager.queuePendingOperation(operationType: .locationUpdate, data: "data", priority: 10)
        
        // When
        try await cacheManager.clearAllCache()
        
        // Then
        let stats = try await cacheManager.getCacheStats()
        XCTAssertEqual(0, stats.locationCount)
        XCTAssertEqual(0, stats.friendCount)
        XCTAssertEqual(0, stats.pendingOperationCount)
        XCTAssertEqual(0, stats.totalCacheSize)
    }
    
    func testObserveUserLocation_ShouldEmitLocationUpdates() async throws {
        // Given
        let userId = "user123"
        let location = Location(latitude: 37.7749, longitude: -122.4194, accuracy: 5.0, timestamp: Int64(Date().timeIntervalSince1970 * 1000), altitude: nil)
        
        let expectation = XCTestExpectation(description: "Location update observed")
        
        // When
        let cancellable = cacheManager.observeUserLocation(userId: userId)
            .sink { observedLocation in
                if observedLocation != nil {
                    expectation.fulfill()
                }
            }
        
        try await cacheManager.cacheLocation(userId: userId, location: location)
        
        // Then
        await fulfillment(of: [expectation], timeout: 2.0)
        cancellable.cancel()
    }
    
    func testObserveFriends_ShouldEmitFriendsUpdates() async throws {
        // Given
        let user = User(id: "user1", email: "test@example.com", displayName: "Test User", profileImageUrl: nil, createdAt: 0, lastActiveAt: 0)
        let friend = Friend(id: "friend1", user: user, friendshipStatus: .accepted, locationSharingEnabled: true, lastKnownLocation: nil, locationSharingPermission: .granted)
        
        let expectation = XCTestExpectation(description: "Friends update observed")
        
        // When
        let cancellable = cacheManager.observeFriends()
            .sink { friends in
                if !friends.isEmpty {
                    expectation.fulfill()
                }
            }
        
        try await cacheManager.cacheFriends([friend])
        
        // Then
        await fulfillment(of: [expectation], timeout: 2.0)
        cancellable.cancel()
    }
}