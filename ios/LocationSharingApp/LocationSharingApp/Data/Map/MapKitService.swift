import Foundation
import MapKit
import os.log

/**
 * MapKit implementation of MapService for iOS platform.
 * 
 * This service manages MapKit integration including map initialization,
 * user location display, and friend location markers with real-time updates.
 */
class MapKitService: NSObject, MapService {
    
    private weak var mapView: MKMapView?
    private var userLocationAnnotation: MKPointAnnotation?
    private var friendAnnotations: [String: MKPointAnnotation] = [:]
    
    private static let logger = Logger(subsystem: "com.locationsharing.app", category: "MapKitService")
    private static let defaultSpan = MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
    
    /**
     * Initializes the MapKit service.
     * This method serves as a placeholder for any additional setup.
     */
    func initializeMap() {
        Self.logger.debug("Initializing MapKit service")
        // Map initialization will be handled by the view controller
        // This method serves as a placeholder for any additional setup
    }
    
    /**
     * Sets the MKMapView instance for this service.
     * 
     * - Parameter mapView: The MKMapView instance from the view controller
     */
    func setMapView(_ mapView: MKMapView) {
        self.mapView = mapView
        configureMap()
        Self.logger.debug("MapView instance set and configured")
    }
    
    /**
     * Updates the user's current location on the map.
     * 
     * - Parameter location: The user's current location
     */
    func updateUserLocation(_ location: Location) {
        guard let mapView = mapView else {
            Self.logger.warning("Cannot update user location: MapView not initialized")
            return
        }
        
        do {
            let coordinate = CLLocationCoordinate2D(latitude: location.latitude, longitude: location.longitude)
            
            // Remove existing user location annotation
            if let existingAnnotation = userLocationAnnotation {
                mapView.removeAnnotation(existingAnnotation)
            }
            
            // Create new user location annotation
            let annotation = MKPointAnnotation()
            annotation.coordinate = coordinate
            annotation.title = "Your Location"
            annotation.subtitle = "Current position"
            
            mapView.addAnnotation(annotation)
            userLocationAnnotation = annotation
            
            // Center map on user location
            let region = MKCoordinateRegion(center: coordinate, span: Self.defaultSpan)
            mapView.setRegion(region, animated: true)
            
            Self.logger.debug("Updated user location: \(location.latitude), \(location.longitude)")
        } catch {
            Self.logger.error("Error updating user location: \(error.localizedDescription)")
        }
    }
    
    /**
     * Updates multiple friend locations on the map.
     * 
     * - Parameter friendLocations: Dictionary of friend IDs to their locations
     */
    func updateFriendLocations(_ friendLocations: [String: Location]) {
        for (friendId, location) in friendLocations {
            addLocationMarker(friendId: friendId, location: location, friendName: "Friend")
        }
        Self.logger.debug("Updated \(friendLocations.count) friend locations")
    }
    
    /**
     * Adds or updates a location marker for a specific friend.
     * 
     * - Parameters:
     *   - friendId: Unique identifier for the friend
     *   - location: The friend's location
     *   - friendName: Display name for the friend
     */
    func addLocationMarker(friendId: String, location: Location, friendName: String) {
        guard let mapView = mapView else {
            Self.logger.warning("Cannot add location marker: MapView not initialized")
            return
        }
        
        do {
            let coordinate = CLLocationCoordinate2D(latitude: location.latitude, longitude: location.longitude)
            
            // Remove existing annotation for this friend
            if let existingAnnotation = friendAnnotations[friendId] {
                mapView.removeAnnotation(existingAnnotation)
            }
            
            // Create new annotation
            let annotation = MKPointAnnotation()
            annotation.coordinate = coordinate
            annotation.title = friendName
            annotation.subtitle = "Last updated: \(formatTimestamp(location.timestamp))"
            
            mapView.addAnnotation(annotation)
            friendAnnotations[friendId] = annotation
            
            Self.logger.debug("Added/updated marker for friend: \(friendId) at \(location.latitude), \(location.longitude)")
        } catch {
            Self.logger.error("Error adding location marker for friend \(friendId): \(error.localizedDescription)")
        }
    }
    
    /**
     * Removes a location marker for a specific friend.
     * 
     * - Parameter friendId: Unique identifier for the friend
     */
    func removeLocationMarker(friendId: String) {
        guard let mapView = mapView else {
            Self.logger.warning("Cannot remove location marker: MapView not initialized")
            return
        }
        
        if let annotation = friendAnnotations[friendId] {
            mapView.removeAnnotation(annotation)
            friendAnnotations.removeValue(forKey: friendId)
            Self.logger.debug("Removed marker for friend: \(friendId)")
        }
    }
    
    /**
     * Clears all markers from the map.
     */
    func clearAllMarkers() {
        guard let mapView = mapView else {
            Self.logger.warning("Cannot clear markers: MapView not initialized")
            return
        }
        
        do {
            // Remove user location annotation
            if let userAnnotation = userLocationAnnotation {
                mapView.removeAnnotation(userAnnotation)
                userLocationAnnotation = nil
            }
            
            // Remove all friend annotations
            let annotations = Array(friendAnnotations.values)
            mapView.removeAnnotations(annotations)
            friendAnnotations.removeAll()
            
            Self.logger.debug("Cleared all markers from map")
        } catch {
            Self.logger.error("Error clearing markers: \(error.localizedDescription)")
        }
    }
    
    /**
     * Configures the MapKit view with default settings.
     */
    private func configureMap() {
        guard let mapView = mapView else { return }
        
        do {
            // Configure map settings
            mapView.mapType = .standard
            mapView.isZoomEnabled = true
            mapView.isScrollEnabled = true
            mapView.isRotateEnabled = true
            mapView.isPitchEnabled = true
            
            // Show user location (iOS will handle permission requests)
            mapView.showsUserLocation = false // We'll handle location display manually
            mapView.userTrackingMode = .none
            
            // Configure appearance
            if #available(iOS 13.0, *) {
                mapView.overrideUserInterfaceStyle = .unspecified
            }
            
            Self.logger.debug("MapKit configured successfully")
        } catch {
            Self.logger.error("Error configuring MapKit: \(error.localizedDescription)")
        }
    }
    
    /**
     * Formats timestamp for display in annotation subtitle.
     * 
     * - Parameter timestamp: Unix timestamp in milliseconds
     * - Returns: Formatted time string
     */
    private func formatTimestamp(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.timeStyle = .medium
        formatter.dateStyle = .none
        return formatter.string(from: date)
    }
    
    /**
     * Sets up annotation selection handlers for interactive functionality.
     * 
     * - Parameter onAnnotationSelect: Callback function for annotation selection events
     */
    func setOnAnnotationSelectHandler(_ onAnnotationSelect: @escaping (String, String, Location) -> Void) {
        self.onAnnotationSelect = onAnnotationSelect
    }
    
    /**
     * Sets up map error handlers for error recovery.
     * 
     * - Parameter onMapError: Callback function for map errors
     */
    func setOnMapErrorHandler(_ onMapError: @escaping (MapError) -> Void) {
        self.onMapError = onMapError
    }
    
    /**
     * Gets the current MKMapView instance.
     * 
     * - Returns: The MKMapView instance or nil if not initialized
     */
    func getMapView() -> MKMapView? {
        return mapView
    }
    
    // MARK: - Private Properties
    private var onAnnotationSelect: ((String, String, Location) -> Void)?
    private var onMapError: ((MapError) -> Void)?
}

// MARK: - MKMapViewDelegate
extension MapKitService: MKMapViewDelegate {
    
    /**
     * Customizes the appearance of map annotations.
     */
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        // Don't customize user location annotation
        if annotation is MKUserLocation {
            return nil
        }
        
        let identifier = "FriendLocationPin"
        var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier)
        
        if annotationView == nil {
            annotationView = MKPinAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            annotationView?.canShowCallout = true
            
            // Customize pin color based on annotation type
            if let pinView = annotationView as? MKPinAnnotationView {
                if annotation.title == "Your Location" {
                    pinView.pinTintColor = .blue
                } else {
                    pinView.pinTintColor = .green
                }
            }
        } else {
            annotationView?.annotation = annotation
        }
        
        return annotationView
    }
    
    /**
     * Handles annotation selection events.
     */
    func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
        guard let annotation = view.annotation,
              let title = annotation.title,
              let friendId = friendAnnotations.first(where: { $0.value === annotation })?.key else {
            Self.logger.debug("Selected annotation: \(view.annotation?.title ?? "Unknown")")
            return
        }
        
        // Create location from annotation
        let location = Location(
            latitude: annotation.coordinate.latitude,
            longitude: annotation.coordinate.longitude,
            accuracy: 0.0, // Accuracy not available from annotation
            timestamp: Int64(Date().timeIntervalSince1970 * 1000)
        )
        
        // Call the selection handler
        onAnnotationSelect?(friendId, title ?? "Unknown Friend", location)
        
        Self.logger.debug("Selected friend annotation: \(title ?? "Unknown")")
    }
    
    /**
     * Handles map loading errors.
     */
    func mapView(_ mapView: MKMapView, didFailToLocateUserWithError error: Error) {
        Self.logger.error("Map failed to locate user: \(error.localizedDescription)")
        onMapError?(.mapInitializationFailed)
    }
    
    /**
     * Handles region change errors.
     */
    func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
        // This can be used to detect if the map region change was successful
        // For now, we'll just log it
        Self.logger.debug("Map region changed")
    }
}