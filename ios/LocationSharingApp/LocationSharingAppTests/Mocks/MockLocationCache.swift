import Foundation
@testable import LocationSharingApp

class MockLocationCache: LocationCache {
    
    var cacheUserLocationCalled = false
    var getCachedUserLocationCalled = false
    var cacheFriendLocationCalled = false
    var getCachedFriendLocationCalled = false
    var clearAllCacheCalled = false
    
    var cachedUserLocation: Location?
    var cachedFriendLocations: [String: Location] = [:]
    
    override func cacheUserLocation(userId: String, location: Location) async {
        cacheUserLocationCalled = true
        cachedUserLocation = location
    }
    
    override func getCachedUserLocation(userId: String) async -> Location? {
        getCachedUserLocationCalled = true
        return cachedUserLocation
    }
    
    override func cacheFriendLocation(friendId: String, location: Location) async {
        cacheFriendLocationCalled = true
        cachedFriendLocations[friendId] = location
    }
    
    override func getCachedFriendLocation(friendId: String) async -> Location? {
        getCachedFriendLocationCalled = true
        return cachedFriendLocations[friendId]
    }
    
    override func clearAllCache() async {
        clearAllCacheCalled = true
        cachedUserLocation = nil
        cachedFriendLocations.removeAll()
    }
}