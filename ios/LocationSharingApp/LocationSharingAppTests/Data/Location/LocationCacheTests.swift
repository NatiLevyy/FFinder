import XCTest
@testable import LocationSharingApp

class LocationCacheTests: XCTestCase {
    
    var locationCache: LocationCache!
    var mockUserDefaults: MockUserDefaults!
    
    let testUserId = "test-user-id"
    let testFriendId = "test-friend-id"
    let testLocation = Location(
        latitude: 37.7749,
        longitude: -122.4194,
        accuracy: 5.0,
        timestamp: 1640995200000,
        altitude: 100.0
    )
    
    override func setUp() {
        super.setUp()
        mockUserDefaults = MockUserDefaults()
        locationCache = LocationCache(userDefaults: mockUserDefaults)
    }
    
    override func tearDown() {
        locationCache = nil
        mockUserDefaults = nil
        super.tearDown()
    }
    
    func testCacheUserLocation_StoresLocationData() async {
        // When
        await locationCache.cacheUserLocation(userId: testUserId, location: testLocation)
        
        // Then
        XCTAssertTrue(mockUserDefaults.setDataCalled)
        XCTAssertTrue(mockUserDefaults.setDoubleCalled)
        XCTAssertNotNil(mockUserDefaults.storedData["user_location_\(testUserId)"])
        XCTAssertNotNil(mockUserDefaults.storedDoubles["user_location_\(testUserId)_timestamp"])
    }
    
    func testGetCachedUserLocation_WithValidCache_ReturnsLocation() async {
        // Given
        let locationData = try! JSONEncoder().encode(testLocation)
        let currentTime = Date().timeIntervalSince1970 * 1000
        
        mockUserDefaults.storedData["user_location_\(testUserId)"] = locationData
        mockUserDefaults.storedDoubles["user_location_\(testUserId)_timestamp"] = currentTime
        
        // When
        let result = await locationCache.getCachedUserLocation(userId: testUserId)
        
        // Then
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.latitude, testLocation.latitude)
        XCTAssertEqual(result?.longitude, testLocation.longitude)
        XCTAssertEqual(result?.accuracy, testLocation.accuracy)
        XCTAssertEqual(result?.timestamp, testLocation.timestamp)
        XCTAssertEqual(result?.altitude, testLocation.altitude)
    }
    
    func testGetCachedUserLocation_WithExpiredCache_ReturnsNil() async {
        // Given
        let locationData = try! JSONEncoder().encode(testLocation)
        let expiredTime = Date().timeIntervalSince1970 * 1000 - (31 * 60 * 1000) // 31 minutes ago
        
        mockUserDefaults.storedData["user_location_\(testUserId)"] = locationData
        mockUserDefaults.storedDoubles["user_location_\(testUserId)_timestamp"] = expiredTime
        
        // When
        let result = await locationCache.getCachedUserLocation(userId: testUserId)
        
        // Then
        XCTAssertNil(result)
        XCTAssertTrue(mockUserDefaults.removeObjectCalled)
    }
    
    func testGetCachedUserLocation_WithNoCache_ReturnsNil() async {
        // Given
        // No cached data
        
        // When
        let result = await locationCache.getCachedUserLocation(userId: testUserId)
        
        // Then
        XCTAssertNil(result)
    }
    
    func testCacheFriendLocation_StoresLocationData() async {
        // When
        await locationCache.cacheFriendLocation(friendId: testFriendId, location: testLocation)
        
        // Then
        XCTAssertTrue(mockUserDefaults.setDataCalled)
        XCTAssertTrue(mockUserDefaults.setDoubleCalled)
        XCTAssertNotNil(mockUserDefaults.storedData["friend_location_\(testFriendId)"])
        XCTAssertNotNil(mockUserDefaults.storedDoubles["friend_location_\(testFriendId)_timestamp"])
    }
    
    func testGetCachedFriendLocation_WithValidCache_ReturnsLocation() async {
        // Given
        let locationData = try! JSONEncoder().encode(testLocation)
        let currentTime = Date().timeIntervalSince1970 * 1000
        
        mockUserDefaults.storedData["friend_location_\(testFriendId)"] = locationData
        mockUserDefaults.storedDoubles["friend_location_\(testFriendId)_timestamp"] = currentTime
        
        // When
        let result = await locationCache.getCachedFriendLocation(friendId: testFriendId)
        
        // Then
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.latitude, testLocation.latitude)
        XCTAssertEqual(result?.longitude, testLocation.longitude)
    }
    
    func testGetCachedFriendLocation_WithExpiredCache_ReturnsNil() async {
        // Given
        let locationData = try! JSONEncoder().encode(testLocation)
        let expiredTime = Date().timeIntervalSince1970 * 1000 - (31 * 60 * 1000) // 31 minutes ago
        
        mockUserDefaults.storedData["friend_location_\(testFriendId)"] = locationData
        mockUserDefaults.storedDoubles["friend_location_\(testFriendId)_timestamp"] = expiredTime
        
        // When
        let result = await locationCache.getCachedFriendLocation(friendId: testFriendId)
        
        // Then
        XCTAssertNil(result)
        XCTAssertTrue(mockUserDefaults.removeObjectCalled)
    }
    
    func testClearAllCache_RemovesAllLocationData() async {
        // Given
        mockUserDefaults.storedData["user_location_test"] = Data()
        mockUserDefaults.storedData["friend_location_test"] = Data()
        mockUserDefaults.storedData["other_data"] = Data()
        
        // When
        await locationCache.clearAllCache()
        
        // Then
        XCTAssertTrue(mockUserDefaults.removeObjectCalled)
        // Verify that location-related keys are removed but other data remains
        XCTAssertNil(mockUserDefaults.storedData["user_location_test"])
        XCTAssertNil(mockUserDefaults.storedData["friend_location_test"])
        XCTAssertNotNil(mockUserDefaults.storedData["other_data"]) // Should remain
    }
    
    func testGetCachedUserLocation_WithCorruptedData_ReturnsNil() async {
        // Given
        let corruptedData = "invalid json".data(using: .utf8)!
        let currentTime = Date().timeIntervalSince1970 * 1000
        
        mockUserDefaults.storedData["user_location_\(testUserId)"] = corruptedData
        mockUserDefaults.storedDoubles["user_location_\(testUserId)_timestamp"] = currentTime
        
        // When
        let result = await locationCache.getCachedUserLocation(userId: testUserId)
        
        // Then
        XCTAssertNil(result)
    }
}

// MARK: - Mock UserDefaults

class MockUserDefaults: UserDefaults {
    
    var setDataCalled = false
    var setDoubleCalled = false
    var removeObjectCalled = false
    
    var storedData: [String: Data] = [:]
    var storedDoubles: [String: Double] = [:]
    var storedObjects: [String: Any] = [:]
    
    override func set(_ value: Any?, forKey defaultName: String) {
        storedObjects[defaultName] = value
        
        if value is Data {
            setDataCalled = true
            storedData[defaultName] = value as? Data
        } else if value is Double {
            setDoubleCalled = true
            storedDoubles[defaultName] = value as? Double
        }
    }
    
    override func data(forKey defaultName: String) -> Data? {
        return storedData[defaultName]
    }
    
    override func double(forKey defaultName: String) -> Double {
        return storedDoubles[defaultName] ?? 0.0
    }
    
    override func removeObject(forKey defaultName: String) {
        removeObjectCalled = true
        storedData.removeValue(forKey: defaultName)
        storedDoubles.removeValue(forKey: defaultName)
        storedObjects.removeValue(forKey: defaultName)
    }
    
    override func dictionaryRepresentation() -> [String : Any] {
        var result: [String: Any] = [:]
        result.merge(storedData) { (_, new) in new }
        result.merge(storedDoubles) { (_, new) in new }
        result.merge(storedObjects) { (_, new) in new }
        return result
    }
}