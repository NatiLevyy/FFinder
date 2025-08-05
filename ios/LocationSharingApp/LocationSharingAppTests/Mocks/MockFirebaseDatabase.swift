import Foundation
import FirebaseDatabase
import Combine

class MockFirebaseDatabase: Database {
    
    var setValueCalled = false
    var getDataCalled = false
    var shouldSucceed = true
    var errorToThrow: Error?
    var mockLocationData: [String: Any]?
    
    private var mockReference = MockDatabaseReference()
    
    override func reference() -> DatabaseReference {
        return mockReference
    }
    
    override func reference(withPath path: String) -> DatabaseReference {
        return mockReference
    }
    
    func simulateDataChange() {
        mockReference.simulateDataChange(with: mockLocationData)
    }
}

class MockDatabaseReference: DatabaseReference {
    
    var setValueCalled = false
    var getDataCalled = false
    var observeCalled = false
    var shouldSucceed = true
    var errorToThrow: Error?
    var mockData: [String: Any]?
    
    private var observers: [(DataSnapshot) -> Void] = []
    private var childReferences: [String: MockDatabaseReference] = [:]
    
    override func setValue(_ value: Any?) async throws {
        setValueCalled = true
        if !shouldSucceed, let error = errorToThrow {
            throw error
        }
    }
    
    override func getData() async throws -> DataSnapshot {
        getDataCalled = true
        if !shouldSucceed, let error = errorToThrow {
            throw error
        }
        return MockDataSnapshot(data: mockData)
    }
    
    override func child(_ pathString: String) -> DatabaseReference {
        if let existingRef = childReferences[pathString] {
            return existingRef
        }
        
        let childRef = MockDatabaseReference()
        childRef.shouldSucceed = shouldSucceed
        childRef.errorToThrow = errorToThrow
        childRef.mockData = mockData
        childReferences[pathString] = childRef
        return childRef
    }
    
    override func observe(_ eventType: DataEventType, with block: @escaping (DataSnapshot) -> Void) -> DatabaseHandle {
        observeCalled = true
        observers.append(block)
        return DatabaseHandle(1)
    }
    
    override func observe(_ eventType: DataEventType, with block: @escaping (DataSnapshot) -> Void, withCancel cancelBlock: ((Error) -> Void)?) -> DatabaseHandle {
        observeCalled = true
        observers.append(block)
        return DatabaseHandle(1)
    }
    
    override func removeObserver(withHandle handle: DatabaseHandle) {
        // Mock implementation
    }
    
    func simulateDataChange(with data: [String: Any]?) {
        mockData = data
        let snapshot = MockDataSnapshot(data: data)
        observers.forEach { observer in
            observer(snapshot)
        }
    }
}

class MockDataSnapshot: DataSnapshot {
    
    private let mockData: [String: Any]?
    
    init(data: [String: Any]?) {
        self.mockData = data
        super.init()
    }
    
    override func exists() -> Bool {
        return mockData != nil
    }
    
    override var value: Any? {
        return mockData
    }
    
    override func child(_ path: String) -> DataSnapshot {
        let childData = mockData?[path]
        return MockDataSnapshot(data: childData as? [String: Any])
    }
    
    override func hasChild(_ path: String) -> Bool {
        return mockData?[path] != nil
    }
    
    override func value(for key: String) -> Any? {
        return mockData?[key]
    }
}

// Extension to help with value extraction
extension MockDataSnapshot {
    func getValue<T>(_ type: T.Type) -> T? {
        return value as? T
    }
}