import Foundation
import Network
import Combine

/**
 * Monitors network connectivity changes.
 */
class ConnectivityMonitor: ObservableObject {
    
    @Published private(set) var networkStatus: NetworkStatus = .unavailable
    @Published private(set) var connectionType: ConnectionType = .none
    @Published private(set) var connectionQuality: ConnectionQuality = .none
    
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")
    
    init() {
        startMonitoring()
    }
    
    deinit {
        stopMonitoring()
    }
    
    // MARK: - Monitoring
    
    /**
     * Starts monitoring network connectivity.
     */
    private func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.updateNetworkStatus(path: path)
            }
        }
        monitor.start(queue: queue)
    }
    
    /**
     * Stops monitoring network connectivity.
     */
    private func stopMonitoring() {
        monitor.cancel()
    }
    
    /**
     * Updates network status based on path information.
     */
    private func updateNetworkStatus(path: NWPath) {
        // Update network status
        networkStatus = path.status == .satisfied ? .available : .unavailable
        
        // Update connection type
        connectionType = getConnectionType(from: path)
        
        // Update connection quality
        connectionQuality = getConnectionQuality(from: path)
    }
    
    /**
     * Determines connection type from network path.
     */
    private func getConnectionType(from path: NWPath) -> ConnectionType {
        if path.usesInterfaceType(.wifi) {
            return .wifi
        } else if path.usesInterfaceType(.cellular) {
            return .cellular
        } else if path.usesInterfaceType(.wiredEthernet) {
            return .ethernet
        } else if path.usesInterfaceType(.other) {
            return .other
        } else {
            return .none
        }
    }
    
    /**
     * Determines connection quality from network path.
     */
    private func getConnectionQuality(from path: NWPath) -> ConnectionQuality {
        guard path.status == .satisfied else {
            return .none
        }
        
        if path.usesInterfaceType(.wiredEthernet) {
            return .excellent
        } else if path.usesInterfaceType(.wifi) {
            return .good
        } else if path.usesInterfaceType(.cellular) {
            // Check if it's expensive (cellular data)
            return path.isExpensive ? .fair : .good
        } else {
            return .poor
        }
    }
    
    // MARK: - Public Interface
    
    /**
     * Checks if network is currently available.
     */
    var isNetworkAvailable: Bool {
        return networkStatus == .available
    }
    
    /**
     * Checks if network is metered (cellular data with potential data charges).
     */
    var isNetworkMetered: Bool {
        return monitor.currentPath.isExpensive
    }
    
    /**
     * Checks if network is constrained (low data mode).
     */
    var isNetworkConstrained: Bool {
        return monitor.currentPath.isConstrained
    }
    
    /**
     * Gets current network path information.
     */
    var currentPath: NWPath {
        return monitor.currentPath
    }
    
    /**
     * Observes network connectivity changes.
     */
    func observeConnectivity() -> AnyPublisher<NetworkStatus, Never> {
        return $networkStatus.eraseToAnyPublisher()
    }
    
    /**
     * Observes connection type changes.
     */
    func observeConnectionType() -> AnyPublisher<ConnectionType, Never> {
        return $connectionType.eraseToAnyPublisher()
    }
    
    /**
     * Observes connection quality changes.
     */
    func observeConnectionQuality() -> AnyPublisher<ConnectionQuality, Never> {
        return $connectionQuality.eraseToAnyPublisher()
    }
    
    /**
     * Gets detailed network information.
     */
    func getNetworkInfo() -> NetworkInfo {
        let path = monitor.currentPath
        
        return NetworkInfo(
            isAvailable: path.status == .satisfied,
            connectionType: getConnectionType(from: path),
            connectionQuality: getConnectionQuality(from: path),
            isExpensive: path.isExpensive,
            isConstrained: path.isConstrained,
            supportsIPv4: path.supportsIPv4,
            supportsIPv6: path.supportsIPv6,
            supportsDNS: path.supportsDNS,
            availableInterfaces: path.availableInterfaces.map { interface in
                InterfaceInfo(
                    name: interface.name,
                    type: interface.type.description,
                    index: interface.index
                )
            }
        )
    }
}

// MARK: - Supporting Types

/**
 * Represents network connectivity status.
 */
enum NetworkStatus {
    case available
    case unavailable
}

/**
 * Types of network connections.
 */
enum ConnectionType {
    case none
    case wifi
    case cellular
    case ethernet
    case other
}

/**
 * Network connection quality levels.
 */
enum ConnectionQuality {
    case none
    case poor
    case fair
    case good
    case excellent
}

/**
 * Detailed network information.
 */
struct NetworkInfo {
    let isAvailable: Bool
    let connectionType: ConnectionType
    let connectionQuality: ConnectionQuality
    let isExpensive: Bool
    let isConstrained: Bool
    let supportsIPv4: Bool
    let supportsIPv6: Bool
    let supportsDNS: Bool
    let availableInterfaces: [InterfaceInfo]
}

/**
 * Network interface information.
 */
struct InterfaceInfo {
    let name: String
    let type: String
    let index: Int32
}

// MARK: - Extensions

extension NWInterface.InterfaceType {
    var description: String {
        switch self {
        case .wifi:
            return "WiFi"
        case .cellular:
            return "Cellular"
        case .wiredEthernet:
            return "Ethernet"
        case .loopback:
            return "Loopback"
        case .other:
            return "Other"
        @unknown default:
            return "Unknown"
        }
    }
}