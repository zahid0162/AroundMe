import UIKit
import CoreLocation
import GoogleMaps
import ComposeApp

final class AroundMeGoogleMapProvider: NSObject, IosMapViewProvider {
    func createMap(
        initialLatitude: Double,
        initialLongitude: Double,
        initialZoom: Float,
        callbacks: any IosMapCallbacks
    ) -> any IosMapHandle {
        return AroundMeGoogleMapHandle(
            initialLatitude: initialLatitude,
            initialLongitude: initialLongitude,
            initialZoom: initialZoom,
            callbacks: callbacks
        )
    }
}

private final class AroundMeGoogleMapHandle: NSObject, IosMapHandle, GMSMapViewDelegate {
    let view: UIView
    private let mapView: GMSMapView
    private var markersById: [String: GMSMarker] = [:]
    private var lastMoveWasGesture = false
    private let callbacks: any IosMapCallbacks

    init(
        initialLatitude: Double,
        initialLongitude: Double,
        initialZoom: Float,
        callbacks: any IosMapCallbacks
    ) {
        let camera = GMSCameraPosition.camera(
            withLatitude: initialLatitude,
            longitude: initialLongitude,
            zoom: initialZoom
        )
        let options = GMSMapViewOptions()
        options.camera = camera
        let gmsMap = GMSMapView(options: options)
        gmsMap.settings.compassButton = true
        gmsMap.settings.myLocationButton = false
        gmsMap.isBuildingsEnabled = true

        self.mapView = gmsMap
        self.view = gmsMap
        self.callbacks = callbacks
        super.init()

        gmsMap.delegate = self
    }

    func updateCamera(latitude: Double, longitude: Double, zoom: Float, animate: Bool) {
        lastMoveWasGesture = false
        let update = GMSCameraUpdate.setTarget(
            CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            zoom: zoom
        )
        if animate {
            mapView.animate(with: update)
        } else {
            mapView.moveCamera(update)
        }
    }

    func updateMarkers(markers: [IosMapMarkerPayload]) {
        let incomingIds = Set(markers.map { $0.id })
        for (existingId, existingMarker) in markersById where !incomingIds.contains(existingId) {
            existingMarker.map = nil
            markersById.removeValue(forKey: existingId)
        }

        for payload in markers {
            let marker = markersById[payload.id] ?? GMSMarker()
            marker.position = CLLocationCoordinate2D(
                latitude: payload.latitude,
                longitude: payload.longitude
            )
            marker.title = payload.title
            marker.userData = payload.id
            marker.zIndex = payload.isSelected ? 10 : 1
            marker.icon = GMSMarker.markerImage(
                with: payload.isSelected
                    ? UIColor(red: 0.85, green: 0.42, blue: 0.15, alpha: 1.0)
                    : UIColor(red: 0.05, green: 0.43, blue: 0.43, alpha: 1.0)
            )
            if marker.map == nil {
                marker.map = mapView
            }
            markersById[payload.id] = marker
        }
    }

    func updateAppearance(isDarkTheme: Bool, isMyLocationEnabled: Bool, bottomPaddingPoints: Int32) {
        mapView.overrideUserInterfaceStyle = isDarkTheme ? .dark : .light
        mapView.isMyLocationEnabled = isMyLocationEnabled
        let targetBottom = CGFloat(bottomPaddingPoints)
        if mapView.padding.bottom != targetBottom {
            mapView.padding = UIEdgeInsets(top: 0, left: 0, bottom: targetBottom, right: 0)
        }
    }

    // MARK: - GMSMapViewDelegate

    func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
        if let id = marker.userData as? String {
            callbacks.onSelectPlace(placeId: id)
            return true
        }
        return false
    }

    func mapView(_ mapView: GMSMapView, didTapAt coordinate: CLLocationCoordinate2D) {
        callbacks.onMapTap()
    }

    func mapView(_ mapView: GMSMapView, willMove gesture: Bool) {
        if gesture {
            lastMoveWasGesture = true
        }
    }

    func mapView(_ mapView: GMSMapView, idleAt position: GMSCameraPosition) {
        guard lastMoveWasGesture else { return }
        lastMoveWasGesture = false

        let visibleRegion = mapView.projection.visibleRegion()
        let bounds = GMSCoordinateBounds(region: visibleRegion)
        let centerLoc = CLLocation(
            latitude: position.target.latitude,
            longitude: position.target.longitude
        )
        let cornerLoc = CLLocation(
            latitude: bounds.northEast.latitude,
            longitude: bounds.northEast.longitude
        )
        let radiusMeters = min(max(centerLoc.distance(from: cornerLoc), 300.0), 50000.0)

        callbacks.onCameraIdle(
            centerLat: position.target.latitude,
            centerLng: position.target.longitude,
            zoom: position.zoom,
            visibleRadiusMeters: radiusMeters
        )
    }
}
