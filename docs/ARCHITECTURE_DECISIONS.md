# AroundMe — Architecture Decisions & Unresolved Issues

## Accepted Architecture Decision Records (ADRs)

### ADR-001: Package-Modularized `composeApp` Over Premature Gradle Micro-Modules
- **Decision:** Keep `domain`, `data`, `core`, and `feature/*` as clean package boundaries inside a single `:composeApp` KMP module, paired with thin `:androidApp` and `iosApp` shells.
- **Rationale:** Minimizes Gradle configuration and iOS framework linking overhead during MVP while preserving strict dependency direction (`feature` -> `domain` <- `data`). Can be split into feature Gradle modules later if team scale warrants it.

### ADR-002: `navigation-compose` 2.9.1 with Type-Safe Serializable Routes
- **Decision:** Use `org.jetbrains.androidx.navigation:navigation-compose:2.9.1` with `@Serializable` route objects (`AroundMeDestination.PlaceDetails(placeId, sessionToken)`).
- **Rationale:** Mature bottom-navigation backstack preservation (`saveState = true`, `restoreState = true`) across Android and iOS, cleanly forwarding Places Autocomplete `sessionToken` into Place Details selection for accurate session billing.

### ADR-003: Room 3 (`androidx.room3` 3.0.3) with `place_id`-Only Persistence
- **Decision:** Persist only user-owned collections, notes, preferences, recent queries, and Google `place_id` references (`SavedPlaceRefEntity`) in Room 3 via `BundledSQLiteDriver`.
- **Rationale:** Strictly complies with Google Maps Platform Section 3.2.3 ToS prohibiting permanent storage of Google Places content (names, addresses, ratings, reviews, hours, or photo resource names).

### ADR-004: Strict Sample Data Isolation
- **Decision:** Isolate offline fixture records inside `data/sample/SamplePlacesDataSource.kt` behind `PlacesRepository`, used only when neither a Places API key nor `PLACES_PROXY_BASE_URL` is configured (or inside `commonTest` doubles).
- **Rationale:** Ensures zero hardcoded sample fixtures leak into `feature/*` or `domain/*`, and live builds strictly display verified Google Places API (New) responses without fabricating unavailable fields.

### ADR-005: Map Padding Synced Only to Settled Bottom Sheet Detents
- **Decision:** Update map `contentPadding` only when the 3-state Explore sheet settles into `Collapsed` or `HalfExpanded`, never on every drag frame.
- **Rationale:** Prevents `GMSMapView` (`UIKitView`) layout thrashing during touch gestures on iOS.

### ADR-006: Swift `IosMapViewProvider` Bridge Over Gradle CocoaPods Cinterop
- **Decision:** Bridge `GMSMapView` on iOS via a Kotlin-defined `IosMapViewProvider` / `IosMapHandle` interface registered at startup by `AroundMeGoogleMapProvider.swift` in `iosApp`.
- **Rationale:** Allows `iosApp` to consume Google Maps SDK for iOS cleanly via Swift Package Manager (`https://github.com/googlemaps/ios-maps-sdk`) without fragile CocoaPods Gradle plugin cinterop build failures, while keeping `CoreLocation` (`CLLocationManager`) directly in `iosMain` Kotlin.

### ADR-007: Session-Only Photo URI Resolution & 30-Day Coordinate Expiry
- **Decision:** Resolve Google Place Photos on demand via `?skipHttpRedirect=true` into a bounded in-memory session cache (loaded with Coil 3 from `lh3.googleusercontent.com`, always displaying `authorAttributions`), and enforce a strict 30-day `geoExpiresAtEpochMs` TTL on optional `cachedLat`/`cachedLng` in `SavedPlaceRefEntity`.
- **Rationale:** Prevents exposing API keys in image URLs, avoids stale or prohibited persistent caching of Google Places photos/attributes, and enforces ToS coordinate retention limits at both query and repository read time.

## Unresolved Issues & Pre-Launch Gates
1. **Local JVM / Android SDK / Xcode Toolchain Required for Native Compilation:** The current AI Studio web container runs Node/Vite without JDK, Gradle, Android SDK, or macOS/Xcode. Native Gradle/Xcode builds and `commonTest` runs (`./gradlew :composeApp:testDebugUnitTest :androidApp:assembleDebug`) must be executed in a local Android Studio / Xcode environment.
2. **Pre-Launch Places Proxy Gate:** `PlatformPlacesConfig` already supports `PLACES_PROXY_BASE_URL` / `PlacesProxyBaseUrl` so a production BFF proxy (Cloud Run / Django DRF) can inject the Places API key server-side and enforce rate limits before public store release without modifying presentation or domain code.
3. **Google Maps Platform API Key Configuration (Local):** Developers must copy `local.properties.example` to `local.properties` (`MAPS_ANDROID_API_KEY`, `PLACES_API_KEY`) and `iosApp/Config/Secrets.xcconfig.example` to `Secrets.xcconfig` (`MAPS_IOS_API_KEY`, `PLACES_API_KEY`) with restricted Android/iOS keys, and add `https://github.com/googlemaps/ios-maps-sdk` via Xcode Swift Package Manager.
