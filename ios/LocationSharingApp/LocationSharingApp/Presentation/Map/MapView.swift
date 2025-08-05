import SwiftUI
import MapKit

struct MapView: View {
    @StateObject private var mapViewModel = MapViewModel()
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 37.7749, longitude: -122.4194),
        span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
    )
    
    var body: some View {
        NavigationView {
            ZStack {
                Map(coordinateRegion: $region, 
                    showsUserLocation: true,
                    userTrackingMode: .constant(.none),
                    annotationItems: mapViewModel.friendLocations) { friend in
                    MapAnnotation(coordinate: CLLocationCoordinate2D(
                        latitude: friend.latitude,
                        longitude: friend.longitude
                    )) {
                        FriendMarkerView(friend: friend)
                    }
                }
                .ignoresSafeArea()
                
                VStack {
                    Spacer()
                    
                    HStack {
                        Spacer()
                        
                        Button(action: {
                            Task {
                                await mapViewModel.centerOnUserLocation()
                            }
                        }) {
                            Image(systemName: "location.fill")
                                .font(.title2)
                                .foregroundColor(.white)
                                .frame(width: 50, height: 50)
                                .background(Color.blue)
                                .clipShape(Circle())
                                .shadow(radius: 4)
                        }
                        .padding(.trailing, 20)
                        .padding(.bottom, 100)
                    }
                }
            }
            .navigationTitle("Map")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Friends") {
                        // Navigate to friends view
                    }
                }
            }
            .onAppear {
                Task {
                    await mapViewModel.initializeMap()
                }
            }
            .onReceive(mapViewModel.$currentLocation) { location in
                if let location = location {
                    region.center = CLLocationCoordinate2D(
                        latitude: location.latitude,
                        longitude: location.longitude
                    )
                }
            }
            .alert("Error", isPresented: .constant(mapViewModel.errorMessage != nil)) {
                Button("OK") {
                    mapViewModel.clearError()
                }
            } message: {
                if let errorMessage = mapViewModel.errorMessage {
                    Text(errorMessage)
                }
            }
        }
    }
}

struct FriendMarkerView: View {
    let friend: MapViewModel.FriendLocation
    
    var body: some View {
        VStack {
            Image(systemName: "person.circle.fill")
                .font(.title)
                .foregroundColor(.blue)
                .background(Color.white)
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(Color.white, lineWidth: 2)
                )
                .shadow(radius: 4)
            
            Text(friend.name)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.primary)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.white.opacity(0.9))
                .cornerRadius(8)
                .shadow(radius: 2)
        }
    }
}

#Preview {
    MapView()
}