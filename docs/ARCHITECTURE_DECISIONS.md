# AroundMe — Architecture Decisions & Unresolved Issues

## Accepted Architecture Decision Records (ADRs)

### ADR-001: Package-Modularized `composeApp` Over Premature Gradle Micro-Modules
- **Decision:** Keep `domain`, `data`, `core`, and `feature/*` as clean package boundaries inside a single `:composeApp` KMP module, paired with thin `:androidApp` and `iosApp` shells.
- **Rationale:** Minimizes Gradle configuration and iOS framework linking overhead during MVP while preserving strict dependency direction (`feature` -> `domain` <- `data`). Can be split into feature Gradle modules later if team scale warrants it.

### ADR-002: `navigation-compose` 2.9.1 with Type-Safe Serializable Routes
- **Decision:** Use `org.jetbrains.androidx.navigation:navigation-compose:2.9.1` with `@Serializable` route objects.
- **Rationale:** Mature bottom-navigation backstack preservation (`saveState = true`, `restoreState = true`) across Android and iOS. Screen composables take navigation lambdas rather than `NavController` directly, keeping future migration to Navigation 3 low-cost.

### ADR-003: Room 3 (`androidx.room3` 3.0.3) with `place_id`-Only Persistence
- **Decision:** Persist only user-owned collections, notes, preferences, recent queries, and Google `place_id` references (+ max 30-day coordinates).
- **Rationale:** Strictly complies with Google Maps Platform Section 3.2.3 ToS prohibiting permanent storage of Google Places content (names, addresses, ratings, reviews, hours, photos).

### ADR-004: Strict Sample Data Isolation in Phase 1
- **Decision:** Isolate Phase 1 prototype fixtures inside `data/sample/SamplePlacesDataSource.kt` behind `PlacesRepository` and `SavedPlacesRepository` interfaces.
- **Rationale:** Ensures zero hardcoded sample fixtures leak into `feature/*` or `domain/*`, enabling clean drop-in replacement by Ktor Places API (New) in Phase 3 and Room 3 in Phase 5.

### ADR-005: Map Padding Synced Only to Settled Bottom Sheet Detents
- **Decision:** Update map `contentPadding` only when the 3-state Explore sheet settles into `Collapsed` or `HalfExpanded`, never on every drag frame.
- **Rationale:** Prevents `GMSMapView` (`UIKitView`) layout thrashing during touch gestures on iOS.

## Unresolved Issues & Pre-Launch Gates
1. **Local JVM / Android SDK / Xcode Toolchain Required for Native Compilation:** The current AI Studio web workspace runs Node/Vite without JDK, Gradle, Android SDK, or macOS/Xcode. Native Gradle/Xcode builds must be executed in a local Android Studio / Xcode environment (or CI macOS runner).
2. **Pre-Launch Places Proxy Gate (Section 15 vs. Section 4):** Phase 3–4 development will use platform-restricted Places API keys (`X-Android-Package`/`X-Android-Cert`, `X-Ios-Bundle-Identifier`) behind `PlacesRemoteDataSource`. Before public store release, introduce a thin BFF proxy (Cloud Run / Django DRF) to protect API keys and enforce server-side rate limiting.
3. **Google Maps Platform API Key Provisioning:** Needs separate restricted Android Maps SDK key, iOS Maps SDK key, and Places API (New) key prior to Phase 2/3 testing.
