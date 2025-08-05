import Foundation

/**
 * Cache implementation for storing location data locally for offline scenarios.
 * Uses UserDefaults for simple key-value storage with JSON serialization.
 */
actor LocationCache {
    
    private static let userLocationPrefix = "user_location_"
    private static let friendLocationPrefix = "friend_location_"
    private static let cacheExpiryMs: Int64 = 30 * 60 * 1000 // 30 minutes
    
    private let userDefaults: UserDefaults
    
    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
    }
    
    /**
     * Cache user's own location
     */
    func cacheUserLocation(userId: String, location: Location) {
        do {
            let key = "\(Self.userLocationPrefix)\(userId)"
            let locationData = try JSONEncoder().encode(location)
            
            userDefaults.set(locationData, forKey: key)
            userDefaults.set(Date().timeIntervalSince1970 * 1000, forKey: "\(key)_timestamp")
            
            print("Cached user location for: \(userId)")
        } catch {
            print("Failed to cache user location: \(error)")
        }
    }
    
    /**
     * Get cached user location
     */
    func getCachedUserLocation(userId: String) -> Location? {
        do {
            let key = "\(Self.userLocationPrefix)\(userId)"
            guard let locationData = userDefaults.data(forKey: key) else {
                return nil
            }
            
            let cacheTimestamp = userDefaults.double(forKey: "\(key)_timestamp")
            
            if !isCacheExpired(cacheTimestamp: Int64(cacheTimestamp)) {
                let location = try JSONDecoder().decode(Location.self, from: locationData)
                return location
            } else {
                print("Cached user location expired for: \(userId)")
                clearUserLocationCache(userId: userId)
                return nil
            }
        } catch {
            print("Failed to get cached user location: \(error)")
            return nil
        }
    }
    
    /**
     * Cache friend's location
     */
    func cacheFriendLocation(friendId: String, location: Location) {
        do {
            let key = "\(Self.friendLocationPrefix)\(friendId)"
            let locationData = try JSONEncoder().encode(location)
            
            userDefaults.set(locationData, forKey: key)
            userDefaults.set(Date().timeIntervalSince1970 * 1000, forKey: "\(key)_timestamp")
            
            print("Cached friend location for: \(friendId)")
        } catch {
            print("Failed to cache friend location: \(error)")
        }
    }
    
    /**
     * Get cached friend location
     */
    func getCachedFriendLocation(friendId: String) -> Location? {
        do {
            let key = "\(Self.friendLocationPrefix)\(friendId)"
            guard let locationData = userDefaults.data(forKey: key) else {
                return nil
            }
            
            let cacheTimestamp = userDefaults.double(forKey: "\(key)_timestamp")
            
            if !isCacheExpired(cacheTimestamp: Int64(cacheTimestamp)) {
                let location = try JSONDecoder().decode(Location.self, from: locationData)
                return location
            } else {
                print("Cached friend location expired for: \(friendId)")
                clearFriendLocationCache(friendId: friendId)
                return nil
            }
        } catch {
            print("Failed to get cached friend location: \(error)")
            return nil
        }
    }
    
    /**
     * Clear all cached locations
     */
    func clearAllCache() {
        let keys = userDefaults.dictionaryRepresentation().keys
        let locationKeys = keys.filter { 
            $0.hasPrefix(Self.userLocationPrefix) || $0.hasPrefix(Self.friendLocationPrefix) 
        }
        
        for key in locationKeys {
            userDefaults.removeObject(forKey: key)
        }
        
        print("Cleared all location cache")
    }
    
    /**
     * Clear cached location for specific user
     */
    private func clearUserLocationCache(userId: String) {
        let key = "\(Self.userLocationPrefix)\(userId)"
        userDefaults.removeObject(forKey: key)
        userDefaults.removeObject(forKey: "\(key)_timestamp")
    }
    
    /**
     * Clear cached location for specific friend
     */
    private func clearFriendLocationCache(friendId: String) {
        let key = "\(Self.friendLocationPrefix)\(friendId)"
        userDefaults.removeObject(forKey: key)
        userDefaults.removeObject(forKey: "\(key)_timestamp")
    }
    
    /**
     * Check if cache entry is expired
     */
    private func isCacheExpired(cacheTimestamp: Int64) -> Bool {
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        return currentTime - cacheTimestamp > Self.cacheExpiryMs
    }
}