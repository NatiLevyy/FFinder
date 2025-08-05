import XCTest
@testable import LocationSharingApp

class FriendTests: XCTestCase {
    
    func testFriendCreationWithAllParameters() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            profileImageUrl: "https://example.com/image.jpg",
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        
        let location = Location(
            latitude: 37.7749,
            longitude: -122.4194,
            accuracy: 5.0,
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            altitude: 100.0
        )
        
        // When
        let friend = Friend(
            id: "friend123",
            user: user,
            friendshipStatus: .accepted,
            locationSharingEnabled: true,
            lastKnownLocation: location,
            locationSharingPermission: .granted
        )
        
        // Then
        XCTAssertEqual(friend.id, "friend123")
        XCTAssertEqual(friend.user.id, user.id)
        XCTAssertEqual(friend.user.email, user.email)
        XCTAssertEqual(friend.friendshipStatus, .accepted)
        XCTAssertTrue(friend.locationSharingEnabled)
        XCTAssertNotNil(friend.lastKnownLocation)
        XCTAssertEqual(friend.locationSharingPermission, .granted)
    }
    
    func testFriendCreationWithMinimalParameters() {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        
        // When
        let friend = Friend(
            id: "friend123",
            user: user,
            friendshipStatus: .pending,
            locationSharingEnabled: false,
            locationSharingPermission: .none
        )
        
        // Then
        XCTAssertEqual(friend.id, "friend123")
        XCTAssertEqual(friend.user.id, user.id)
        XCTAssertEqual(friend.friendshipStatus, .pending)
        XCTAssertFalse(friend.locationSharingEnabled)
        XCTAssertNil(friend.lastKnownLocation)
        XCTAssertEqual(friend.locationSharingPermission, .none)
    }
    
    func testFriendshipStatusEnumValues() {
        XCTAssertEqual(FriendshipStatus.pending.rawValue, "PENDING")
        XCTAssertEqual(FriendshipStatus.accepted.rawValue, "ACCEPTED")
        XCTAssertEqual(FriendshipStatus.blocked.rawValue, "BLOCKED")
    }
    
    func testLocationSharingPermissionEnumValues() {
        XCTAssertEqual(LocationSharingPermission.none.rawValue, "NONE")
        XCTAssertEqual(LocationSharingPermission.requested.rawValue, "REQUESTED")
        XCTAssertEqual(LocationSharingPermission.granted.rawValue, "GRANTED")
        XCTAssertEqual(LocationSharingPermission.denied.rawValue, "DENIED")
    }
    
    func testFriendshipStatusCaseIterable() {
        let allCases = FriendshipStatus.allCases
        XCTAssertEqual(allCases.count, 3)
        XCTAssertTrue(allCases.contains(.pending))
        XCTAssertTrue(allCases.contains(.accepted))
        XCTAssertTrue(allCases.contains(.blocked))
    }
    
    func testLocationSharingPermissionCaseIterable() {
        let allCases = LocationSharingPermission.allCases
        XCTAssertEqual(allCases.count, 4)
        XCTAssertTrue(allCases.contains(.none))
        XCTAssertTrue(allCases.contains(.requested))
        XCTAssertTrue(allCases.contains(.granted))
        XCTAssertTrue(allCases.contains(.denied))
    }
    
    func testFriendCodable() throws {
        // Given
        let user = User(
            id: "user123",
            email: "test@example.com",
            displayName: "Test User",
            createdAt: 1640995200000,
            lastActiveAt: 1640995200000
        )
        
        let originalFriend = Friend(
            id: "friend123",
            user: user,
            friendshipStatus: .accepted,
            locationSharingEnabled: true,
            locationSharingPermission: .granted
        )
        
        // When
        let encodedData = try JSONEncoder().encode(originalFriend)
        let decodedFriend = try JSONDecoder().decode(Friend.self, from: encodedData)
        
        // Then
        XCTAssertEqual(decodedFriend.id, originalFriend.id)
        XCTAssertEqual(decodedFriend.user.id, originalFriend.user.id)
        XCTAssertEqual(decodedFriend.friendshipStatus, originalFriend.friendshipStatus)
        XCTAssertEqual(decodedFriend.locationSharingEnabled, originalFriend.locationSharingEnabled)
        XCTAssertEqual(decodedFriend.locationSharingPermission, originalFriend.locationSharingPermission)
    }
}