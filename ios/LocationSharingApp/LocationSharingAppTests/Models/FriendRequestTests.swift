import XCTest
@testable import LocationSharingApp

class FriendRequestTests: XCTestCase {
    
    func testFriendRequestCreationWithAllParameters() {
        // Given
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let respondedTime = currentTime + 3600000 // 1 hour later
        
        // When
        let friendRequest = FriendRequest(
            id: "request123",
            fromUserId: "user1",
            toUserId: "user2",
            status: .accepted,
            createdAt: currentTime,
            respondedAt: respondedTime
        )
        
        // Then
        XCTAssertEqual(friendRequest.id, "request123")
        XCTAssertEqual(friendRequest.fromUserId, "user1")
        XCTAssertEqual(friendRequest.toUserId, "user2")
        XCTAssertEqual(friendRequest.status, .accepted)
        XCTAssertEqual(friendRequest.createdAt, currentTime)
        XCTAssertEqual(friendRequest.respondedAt, respondedTime)
    }
    
    func testFriendRequestCreationWithMinimalParameters() {
        // Given
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        
        // When
        let friendRequest = FriendRequest(
            id: "request123",
            fromUserId: "user1",
            toUserId: "user2",
            status: .pending,
            createdAt: currentTime
        )
        
        // Then
        XCTAssertEqual(friendRequest.id, "request123")
        XCTAssertEqual(friendRequest.fromUserId, "user1")
        XCTAssertEqual(friendRequest.toUserId, "user2")
        XCTAssertEqual(friendRequest.status, .pending)
        XCTAssertEqual(friendRequest.createdAt, currentTime)
        XCTAssertNil(friendRequest.respondedAt)
    }
    
    func testRequestStatusEnumValues() {
        XCTAssertEqual(RequestStatus.pending.rawValue, "PENDING")
        XCTAssertEqual(RequestStatus.accepted.rawValue, "ACCEPTED")
        XCTAssertEqual(RequestStatus.rejected.rawValue, "REJECTED")
        XCTAssertEqual(RequestStatus.expired.rawValue, "EXPIRED")
    }
    
    func testRequestStatusCaseIterable() {
        let allCases = RequestStatus.allCases
        XCTAssertEqual(allCases.count, 4)
        XCTAssertTrue(allCases.contains(.pending))
        XCTAssertTrue(allCases.contains(.accepted))
        XCTAssertTrue(allCases.contains(.rejected))
        XCTAssertTrue(allCases.contains(.expired))
    }
    
    func testFriendRequestWithNullRespondedAt() {
        // Given
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        
        // When
        let friendRequest = FriendRequest(
            id: "request123",
            fromUserId: "user1",
            toUserId: "user2",
            status: .pending,
            createdAt: currentTime,
            respondedAt: nil
        )
        
        // Then
        XCTAssertNil(friendRequest.respondedAt)
        XCTAssertEqual(friendRequest.status, .pending)
    }
    
    func testFriendRequestCodable() throws {
        // Given
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let originalRequest = FriendRequest(
            id: "request123",
            fromUserId: "user1",
            toUserId: "user2",
            status: .pending,
            createdAt: currentTime,
            respondedAt: nil
        )
        
        // When
        let encodedData = try JSONEncoder().encode(originalRequest)
        let decodedRequest = try JSONDecoder().decode(FriendRequest.self, from: encodedData)
        
        // Then
        XCTAssertEqual(decodedRequest.id, originalRequest.id)
        XCTAssertEqual(decodedRequest.fromUserId, originalRequest.fromUserId)
        XCTAssertEqual(decodedRequest.toUserId, originalRequest.toUserId)
        XCTAssertEqual(decodedRequest.status, originalRequest.status)
        XCTAssertEqual(decodedRequest.createdAt, originalRequest.createdAt)
        XCTAssertEqual(decodedRequest.respondedAt, originalRequest.respondedAt)
    }
    
    func testFriendRequestCodableWithRespondedAt() throws {
        // Given
        let currentTime = Int64(Date().timeIntervalSince1970 * 1000)
        let respondedTime = currentTime + 3600000
        let originalRequest = FriendRequest(
            id: "request123",
            fromUserId: "user1",
            toUserId: "user2",
            status: .accepted,
            createdAt: currentTime,
            respondedAt: respondedTime
        )
        
        // When
        let encodedData = try JSONEncoder().encode(originalRequest)
        let decodedRequest = try JSONDecoder().decode(FriendRequest.self, from: encodedData)
        
        // Then
        XCTAssertEqual(decodedRequest.id, originalRequest.id)
        XCTAssertEqual(decodedRequest.fromUserId, originalRequest.fromUserId)
        XCTAssertEqual(decodedRequest.toUserId, originalRequest.toUserId)
        XCTAssertEqual(decodedRequest.status, originalRequest.status)
        XCTAssertEqual(decodedRequest.createdAt, originalRequest.createdAt)
        XCTAssertEqual(decodedRequest.respondedAt, originalRequest.respondedAt)
    }
}