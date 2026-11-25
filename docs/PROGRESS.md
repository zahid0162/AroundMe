# AroundMe — Project Progress Tracker

## Current Phase: Phase 2 — Google Maps & Location (Completed)

### Phase 0: Product & Architecture Planning — COMPLETED
- Validated KMP/CMP feasibility, dependencies, Clean Architecture, ToS-compliant storage, and billing controls.

### Phase 1: Project Foundation — COMPLETED
- Configured KMP/CMP (`com.zahid.aroundme`), Koin DI, Design System, type-safe navigation, EN/AR RTL resources, and Phase 1 screens.

### Phase 2: Google Maps & Location — COMPLETED
- Integrated native **Google Maps SDK for Android** (`maps-compose:9.0.1`, `ComposeMapColorScheme`, custom `MarkerComposable` pins, `MapsInitializer.Renderer.LATEST`, usage attribution ID).
- Integrated native **Google Maps SDK for iOS** (`GMSMapView`, `GMSMapViewDelegate`, `GMSMarker`, dark/light `overrideUserInterfaceStyle`, `UIEdgeInsets` bottom padding sync, usage attribution ID) via `IosMapBridge` + `AroundMeGoogleMapProvider.swift`.
- Implemented cross-platform foreground location management (`AndroidLocationRepository` via `FusedLocationProviderClient` + `IosLocationRepository` via `CLLocationManager`) supporting precise, approximate, denied, permanently denied, disabled GPS/services, and manual location fallback.
- Wired gesture-only camera idle detection with 20% / 350m movement threshold for `"Search this area"`, map-to-list scroll synchronization, and settled-detent bottom padding.
- Added `commonTest` tests covering camera pan thresholds, Search This Area, and marker-to-sheet synchronization.

### Next Phase: Phase 3 — Google Places API (New)
- Configure Ktor HTTP client (`OkHttp` on Android, `Darwin` on iOS) with mandatory `X-Goog-FieldMask` headers and platform restriction headers.
- Implement `CategoryRegistry` mapping all AroundMe UI categories to Google Places API (New) `includedTypes` and text queries.
- Replace `SamplePlacesRepository` with live `searchNearby` / `searchText` discovery, request deduplication, in-memory TTL cache, and filters/sorting.
