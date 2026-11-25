package com.zahid.aroundme.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zahid.aroundme.core.designsystem.AroundMeCategoryChip
import com.zahid.aroundme.core.designsystem.AroundMeSpacing
import com.zahid.aroundme.core.designsystem.PlacePreviewCard
import com.zahid.aroundme.core.designsystem.SheetGrabHandle
import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.core.platform.AroundMeMapSurface
import com.zahid.aroundme.core.platform.rememberLocationPermissionRequester
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.model.PlaceCategory
import com.zahid.aroundme.domain.model.ThemePreference
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExploreScreen(
    onOpenSearch: () -> Unit,
    onOpenPlaceDetails: (String) -> Unit,
    viewModel: ExploreViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var locationMenuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val requestLocationPermission = rememberLocationPermissionRequester { status ->
        viewModel.onPermissionResult(status)
    }

    val isDarkTheme = when (state.themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val sheetFraction by animateFloatAsState(
        targetValue = when (state.sheetDetent) {
            SheetDetent.COLLAPSED -> 0.16f
            SheetDetent.HALF_EXPANDED -> 0.44f
            SheetDetent.EXPANDED -> 0.80f
        }
    )

    // Scroll list smoothly to match marker selection on map (sync map -> list)
    LaunchedEffect(state.selectedPlaceId, state.places) {
        val selectedId = state.selectedPlaceId ?: return@LaunchedEffect
        val index = state.places.indexOfFirst { it.id == selectedId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Settled-detent map bottom padding avoids GMSMapView UIKitView drag thrashing (ADR-005)
        val settledBottomPaddingDp = when (state.sheetDetent) {
            SheetDetent.COLLAPSED -> (maxHeight.value * 0.16f).toInt()
            SheetDetent.HALF_EXPANDED -> (maxHeight.value * 0.44f).toInt()
            SheetDetent.EXPANDED -> 0
        }

        // 1. Background native Google Map
        if (state.viewMode == ExploreViewMode.MAP) {
            AroundMeMapSurface(
                cameraCommand = state.cameraCommand,
                places = state.places,
                selectedPlaceId = state.selectedPlaceId,
                isDarkTheme = isDarkTheme,
                isMyLocationEnabled = state.permissionStatus.isGranted,
                bottomContentPaddingDp = settledBottomPaddingDp,
                onSelectPlace = viewModel::onMarkerSelected,
                onCameraIdle = viewModel::onCameraIdle,
                onMapTap = viewModel::onMapTap,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Top Search, Brand, Location Selector, Category Rail & Permission Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = AroundMeSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AroundMeSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        onClick = { locationMenuExpanded = true },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AroundMe",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = state.activeLocation.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Change location",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = locationMenuExpanded,
                        onDismissRequest = { locationMenuExpanded = false }
                    ) {
                        if (state.permissionStatus.isGranted) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (state.isArabic) "استخدام موقعي الحالي" else "Use Current Location")
                                },
                                onClick = {
                                    locationMenuExpanded = false
                                    viewModel.centerOnDeviceLocation()
                                }
                            )
                        }
                        state.availableLocations.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc.label) },
                                onClick = {
                                    viewModel.selectLocation(loc)
                                    locationMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Map / List Mode Toggle
                Surface(
                    onClick = {
                        viewModel.setViewMode(
                            if (state.viewMode == ExploreViewMode.MAP) ExploreViewMode.LIST else ExploreViewMode.MAP
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (state.viewMode == ExploreViewMode.MAP) {
                                Icons.Rounded.FormatListBulleted
                            } else {
                                Icons.Rounded.Map
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (state.viewMode == ExploreViewMode.MAP) {
                                if (state.isArabic) "قائمة" else "List"
                            } else {
                                if (state.isArabic) "خريطة" else "Map"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Search bar trigger
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AroundMeSpacing.md)
                    .clickable(onClick = onOpenSearch),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (state.isArabic) "ابحث عن مطاعم، مقاهي، صيدليات…" else "Search places, cafes, pharmacies…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Horizontally scrollable category chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = AroundMeSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
            ) {
                item {
                    AroundMeCategoryChip(
                        label = if (state.isArabic) "الكل" else "All",
                        icon = Icons.Rounded.Apps,
                        selected = state.filter.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) }
                    )
                }
                items(PlaceCategory.quickExploreDefaults) { category ->
                    AroundMeCategoryChip(
                        label = category.label(state.isArabic),
                        icon = categoryIcon(category),
                        selected = state.filter.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }

            // Non-blocking foreground location rationale / recovery card
            AnimatedVisibility(visible = state.showLocationBanner) {
                LocationPermissionBanner(
                    status = state.permissionStatus,
                    isArabic = state.isArabic,
                    onRequestPermission = requestLocationPermission,
                    onOpenAppSettings = viewModel::openAppSettings,
                    onOpenLocationServices = viewModel::openLocationServicesSettings,
                    onDismiss = viewModel::dismissLocationBanner,
                    modifier = Modifier.padding(horizontal = AroundMeSpacing.md)
                )
            }

            AnimatedVisibility(visible = state.showSearchThisArea && state.viewMode == ExploreViewMode.MAP) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = viewModel::searchThisArea,
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = if (state.isArabic) "ابحث في هذه المنطقة" else "Search this area",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. Content Area: Full List View OR Map + 3-State Bottom Sheet
        if (state.viewMode == ExploreViewMode.LIST) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 196.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                PlacesListContent(
                    state = state,
                    listState = listState,
                    onToggleOpenNow = viewModel::toggleOpenNow,
                    onOpenPlaceDetails = onOpenPlaceDetails,
                    onToggleSave = viewModel::toggleSavePlace
                )
            }
        } else {
            // My Location FAB positioned cleanly above the sheet
            SmallFloatingActionButton(
                onClick = {
                    if (state.permissionStatus.isGranted) {
                        viewModel.centerOnDeviceLocation()
                    } else if (state.permissionStatus == LocationPermissionStatus.PERMANENTLY_DENIED) {
                        viewModel.openAppSettings()
                    } else if (state.permissionStatus == LocationPermissionStatus.SERVICES_DISABLED) {
                        viewModel.openLocationServicesSettings()
                    } else {
                        requestLocationPermission()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AroundMeSpacing.md, bottom = (maxHeight * sheetFraction) + 12.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                if (state.isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Rounded.MyLocation, contentDescription = "Center on current location")
                }
            }

            // 3-State Bottom Sheet (Collapsed, Half Expanded, Expanded)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(sheetFraction),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = viewModel::cycleSheetDetent),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SheetGrabHandle()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AroundMeSpacing.md, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.isArabic) {
                                    "${state.places.size} مكان قريب"
                                } else {
                                    "${state.places.size} places nearby"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                imageVector = if (state.sheetDetent == SheetDetent.EXPANDED) {
                                    Icons.Rounded.ExpandMore
                                } else {
                                    Icons.Rounded.ExpandLess
                                },
                                contentDescription = "Toggle sheet height",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (state.sheetDetent != SheetDetent.COLLAPSED) {
                        PlacesListContent(
                            state = state,
                            listState = listState,
                            onToggleOpenNow = viewModel::toggleOpenNow,
                            onOpenPlaceDetails = onOpenPlaceDetails,
                            onToggleSave = viewModel::toggleSavePlace
                        )
                    } else {
                        val previewPlace = state.places.find { it.id == state.selectedPlaceId } ?: state.places.firstOrNull()
                        if (previewPlace != null) {
                            Text(
                                text = "${previewPlace.name} · ${previewPlace.primaryCategoryLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = AroundMeSpacing.md)
                                    .clickable { onOpenPlaceDetails(previewPlace.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionBanner(
    status: LocationPermissionStatus,
    isArabic: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationServices: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (message, actionLabel, onAction) = when (status) {
        LocationPermissionStatus.PERMANENTLY_DENIED -> Triple(
            if (isArabic) "تم إيقاف إذن الموقع. فعّله من الإعدادات لاكتشاف الأماكن حولك."
            else "Location access is turned off. Enable it in Settings or choose a city above.",
            if (isArabic) "الإعدادات" else "Settings",
            onOpenAppSettings
        )
        LocationPermissionStatus.SERVICES_DISABLED -> Triple(
            if (isArabic) "خدمات الموقع معطلة على الجهاز."
            else "Device location services are off. Enable GPS or browse manually.",
            if (isArabic) "تفعيل" else "Enable",
            onOpenLocationServices
        )
        else -> Triple(
            if (isArabic) "اسمح بالوصول للموقع لحساب المسافات واكتشاف الأماكن القريبة."
            else "Allow foreground location to center the map and measure nearby distances.",
            if (isArabic) "سماح" else "Allow",
            onRequestPermission
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PlacesListContent(
    state: ExploreUiState,
    listState: LazyListState,
    onToggleOpenNow: () -> Unit,
    onOpenPlaceDetails: (String) -> Unit,
    onToggleSave: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AroundMeSpacing.md, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
        ) {
            FilterChip(
                selected = state.filter.openNowOnly,
                onClick = onToggleOpenNow,
                label = { Text(if (state.isArabic) "مفتوح الآن" else "Open now") }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = AroundMeSpacing.md,
                vertical = AroundMeSpacing.xs
            ),
            verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
        ) {
            items(state.places, key = { it.id }) { place ->
                val distance = GeoUtils.haversineMeters(state.activeLocation.coordinates, place.location)
                PlacePreviewCard(
                    place = place,
                    distanceMeters = distance,
                    distanceUnit = state.distanceUnit,
                    isSaved = state.savedPlaceIds.contains(place.id),
                    isSelected = place.id == state.selectedPlaceId,
                    isArabic = state.isArabic,
                    onClick = { onOpenPlaceDetails(place.id) },
                    onToggleSave = { onToggleSave(place.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun categoryIcon(category: PlaceCategory): ImageVector = when (category) {
    PlaceCategory.RESTAURANTS -> Icons.Rounded.Restaurant
    PlaceCategory.CAFES -> Icons.Rounded.Coffee
    PlaceCategory.PHARMACIES -> Icons.Rounded.LocalPharmacy
    PlaceCategory.PARKS -> Icons.Rounded.Park
    PlaceCategory.TOURIST_ATTRACTIONS -> Icons.Rounded.Tour
    PlaceCategory.SUPERMARKETS -> Icons.Rounded.ShoppingCart
    PlaceCategory.PETROL_STATIONS -> Icons.Rounded.LocalGasStation
    PlaceCategory.MOSQUES -> Icons.Rounded.Mosque
    else -> Icons.Rounded.Apps
}
