import Foundation
@testable import LocationSharingApp

class MockJwtTokenManager: JwtTokenManager {
    
    // MARK: - Method Call Tracking
    
    var storeTokensCalled = false
    var getAccessTokenCalled = false
    var getRefreshTokenCalled = false
    var getTokenExpiresAtCalled = false
    var isTokenValidCalled = false
    var shouldRefreshTokenCalled = false
    var clearTokensCalled = false
    var hasStoredTokensCalled = false
    
    // MARK: - Method Results
    
    var storeTokensResult: Result<Void, Error> = .success(())
    var getAccessTokenResult: String?
    var getRefreshTokenResult: String?
    var getTokenExpiresAtResult: Int64 = 0
    var isTokenValidResult: Bool = false
    var shouldRefreshTokenResult: Bool = false
    var clearTokensResult: Result<Void, Error> = .success(())
    var hasStoredTokensResult: Bool = false
    
    // MARK: - Mock Methods
    
    override func storeTokens(accessToken: String, refreshToken: String, expiresAt: Int64) async throws {
        storeTokensCalled = true
        
        switch storeTokensResult {
        case .success:
            break
        case .failure(let error):
            throw error
        }
    }
    
    override func getAccessToken() async -> String? {
        getAccessTokenCalled = true
        return getAccessTokenResult
    }
    
    override func getRefreshToken() async -> String? {
        getRefreshTokenCalled = true
        return getRefreshTokenResult
    }
    
    override func getTokenExpiresAt() async -> Int64 {
        getTokenExpiresAtCalled = true
        return getTokenExpiresAtResult
    }
    
    override func isTokenValid() async -> Bool {
        isTokenValidCalled = true
        return isTokenValidResult
    }
    
    override func shouldRefreshToken() async -> Bool {
        shouldRefreshTokenCalled = true
        return shouldRefreshTokenResult
    }
    
    override func clearTokens() async throws {
        clearTokensCalled = true
        
        switch clearTokensResult {
        case .success:
            break
        case .failure(let error):
            throw error
        }
    }
    
    override func hasStoredTokens() async -> Bool {
        hasStoredTokensCalled = true
        return hasStoredTokensResult
    }
    
    // MARK: - Reset Methods
    
    func reset() {
        storeTokensCalled = false
        getAccessTokenCalled = false
        getRefreshTokenCalled = false
        getTokenExpiresAtCalled = false
        isTokenValidCalled = false
        shouldRefreshTokenCalled = false
        clearTokensCalled = false
        hasStoredTokensCalled = false
        
        storeTokensResult = .success(())
        getAccessTokenResult = nil
        getRefreshTokenResult = nil
        getTokenExpiresAtResult = 0
        isTokenValidResult = false
        shouldRefreshTokenResult = false
        clearTokensResult = .success(())
        hasStoredTokensResult = false
    }
}