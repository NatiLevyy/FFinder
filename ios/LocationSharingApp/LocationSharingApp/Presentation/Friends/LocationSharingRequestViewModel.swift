import Foundation
import Combine

/**
 * ViewModel for managing location sharing request UI state and operations.
 * 
 * This ViewModel handles accepting and denying location sharing requests
 * from friends with appropriate error handling and user feedback.
 */
@MainActor
class LocationSharingRequestViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var requestProcessed = false
    @Published var requestAccepted = false
    
    private let locationSharingManager: LocationSharingManager
    
    init(locationSharingManager: LocationSharingManager = LocationSharingManager(friendsRepository: FriendsRepositoryImpl())) {
        self.locationSharingManager = locationSharingManager
    }
    
    func acceptLocationSharingRequest(friendId: String) {
        isLoading = true
        
        Task {
            let result = await locationSharingManager.grantLocationSharingPermission(friendId: friendId)
            
            switch result {
            case .success:
                self.requestProcessed = true
                self.requestAccepted = true
                self.isLoading = false
            case .failure(let error):
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    func denyLocationSharingRequest(friendId: String) {
        isLoading = true
        
        Task {
            let result = await locationSharingManager.denyLocationSharingPermission(friendId: friendId)
            
            switch result {
            case .success:
                self.requestProcessed = true
                self.requestAccepted = false
                self.isLoading = false
            case .failure(let error):
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
    
    func clearError() {
        errorMessage = nil
    }
}