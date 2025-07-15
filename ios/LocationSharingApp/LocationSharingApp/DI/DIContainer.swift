import Foundation
import FirebaseAuth
import FirebaseFirestore
import FirebaseDatabase

class DIContainer {
    static let shared = DIContainer()
    
    private init() {}
    
    // MARK: - Firebase Services
    lazy var firebaseAuth: Auth = {
        return Auth.auth()
    }()
    
    lazy var firebaseFirestore: Firestore = {
        return Firestore.firestore()
    }()
    
    lazy var firebaseDatabase: Database = {
        return Database.database()
    }()
    
    // MARK: - Repository Dependencies
    // Repository implementations will be added in later tasks
    
    // MARK: - Service Dependencies  
    // Service implementations will be added in later tasks
}