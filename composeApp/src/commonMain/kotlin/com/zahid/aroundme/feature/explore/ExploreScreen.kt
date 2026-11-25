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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.zahid.aroundme.core.designsystem.SectionHeader
import com.zahid.aroundme.core.designsystem.SheetGrabHandle
import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.core.platform.AroundMeMapSurface
import com.zahid.aroundme.core.platform.rememberLocationPermissionRequester
import com.zahid.aroundme.domain.model.CategoryGroup
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.model.PlaceCategory
import com.zahid.aroundme.domain.model.SortOption
import com.zahid.aroundme.domain.model.ThemePreference
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    onOpenSearch: () -> Unit,
    onOpenPlaceDetails: (String) -> Unit,
    viewModel: ExploreViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var locationMenuExpanded by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
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

    LaunchedEffect(state.selectedPlaceId, state.places) {
        val selectedId = state.selectedPlaceId ?: return@LaunchedEffect
        val index = state.places.indexOfFirst { it.id == selectedId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    val chipCategories = remember(state.filter.selectedCategory) {
        val defaults = PlaceCategory.quickExploreDefaults
        val selected = state.filter.selectedCategory
        if (selected != null && !defaults.contains(selected)) {
            listOf(selected) + defaults
        } else {
            defaults
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val settledBottomPaddingDp = when (state.sheetDetent) {
            SheetDetent.COLLAPSED -> (maxHeight.value * 0.16f).toInt()
            SheetDetent.HALF_EXPANDED -> (maxHeight.value * 0.44f).toInt()
            SheetDetent.EXPANDED -> 0
        }

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
                                contentDescription = if (state.isArabic) "تغيير الموقع" else "Change location",
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
                items(chipCategories, key = { it.name }) { category ->
                    AroundMeCategoryChip(
                        label = category.label(state.isArabic),
                        icon = categoryIcon(category),
                        selected = state.filter.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
                item {
                    AroundMeCategoryChip(
                        label = if (state.isArabic) "المزيد" else "More",
                        icon = Icons.Rounded.MoreHoriz,
                        selected = false,
                        onClick = { showCategorySheet = true }
                    )
                }
            }

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
                    onToggleMinRating = viewModel::setMinRating,
                    onSelectRadius = viewModel::setMaxDistance,
                    onSelectSort = viewModel::selectSortOption,
                    onResetFilters = viewModel::resetFilters,
                    onRetry = viewModel::retryNearby,
                    onOpenPlaceDetails = onOpenPlaceDetails,
                    onToggleSave = viewModel::toggleSavePlace,
                    onResolvePhotoUri = viewModel::resolvePhotoUri
                )
            }
        } else {
            SmallFloatingActionButton(
                onClick = {
                    when {
                        state.permissionStatus.isGranted -> viewModel.centerOnDeviceLocation()
                        state.permissionStatus == LocationPermissionStatus.PERMANENTLY_DENIED -> viewModel.openAppSettings()
                        state.permissionStatus == LocationPermissionStatus.SERVICES_DISABLED -> viewModel.openLocationServicesSettings()
                        else -> requestLocationPermission()
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!state.isLiveApi) {
                                    Text(
                                        text = if (state.isArabic) "بيانات تجريبية" else "Sample places",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                    }

                    if (state.sheetDetent != SheetDetent.COLLAPSED) {
                        PlacesListContent(
                            state = state,
                            listState = listState,
                            onToggleOpenNow = viewModel::toggleOpenNow,
                            onToggleMinRating = viewModel::setMinRating,
                            onSelectRadius = viewModel::setMaxDistance,
                            onSelectSort = viewModel::selectSortOption,
                            onResetFilters = viewModel::resetFilters,
                            onRetry = viewModel::retryNearby,
                            onOpenPlaceDetails = onOpenPlaceDetails,
                            onToggleSave = viewModel::toggleSavePlace,
                            onResolvePhotoUri = viewModel::resolvePhotoUri
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

    if (showCategorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AroundMeSpacing.md, vertical = AroundMeSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.md)
            ) {
                SectionHeader(
                    title = if (state.isArabic) "جميع الفئات" else "All Categories",
                    trailing = {
                        if (state.filter.selectedCategory != null) {
                            TextButton(
                                onClick = {
                                    viewModel.selectCategory(null)
                                    showCategorySheet = false
                                }
                            ) {
                                Text(if (state.isArabic) "عرض الكل" else "Show All")
                            }
                        }
                    }
                )

                CategoryGroup.entries.forEach { group ->
                    val groupCategories = PlaceCategory.entries.filter { it.group == group }
                    Column(verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)) {
                        Text(
                            text = if (state.isArabic) group.titleAr else group.titleEn,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
                        ) {
                            groupCategories.forEach { category ->
                                AroundMeCategoryChip(
                                    label = category.label(state.isArabic),
                                    icon = categoryIcon(category),
                                    selected = state.filter.selectedCategory == category,
                                    onClick = {
                                        viewModel.selectCategory(category)
                                        showCategorySheet = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
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
    onToggleMinRating: (Double) -> Unit,
    onSelectRadius: (Double) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onResetFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpenPlaceDetails: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onResolvePhotoUri: suspend (String) -> String?
) {
    var radiusMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val hasActiveFilter = state.filter.selectedCategory != null ||
        state.filter.openNowOnly ||
        state.filter.minRating != null

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = AroundMeSpacing.md, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
        ) {
            item {
                FilterChip(
                    selected = state.filter.openNowOnly,
                    onClick = onToggleOpenNow,
                    label = { Text(if (state.isArabic) "مفتوح الآن" else "Open now") }
                )
            }
            item {
                FilterChip(
                    selected = state.filter.minRating == 4.0,
                    onClick = { onToggleMinRating(4.0) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("4.0+") }
                )
            }
            item {
                FilterChip(
                    selected = state.filter.minRating == 4.5,
                    onClick = { onToggleMinRating(4.5) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("4.5+") }
                )
            }
            item {
                Box {
                    FilterChip(
                        selected = false,
                        onClick = { radiusMenuExpanded = true },
                        leadingIcon = {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(
                                GeoUtils.formatDistance(
                                    state.filter.maxDistanceMeters,
                                    state.distanceUnit,
                                    state.isArabic
                                )
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = radiusMenuExpanded,
                        onDismissRequest = { radiusMenuExpanded = false }
                    ) {
                        listOf(1000.0, 3000.0, 5000.0, 10000.0).forEach { meters ->
                            DropdownMenuItem(
                                text = {
                                    Text(GeoUtils.formatDistance(meters, state.distanceUnit, state.isArabic))
                                },
                                onClick = {
                                    onSelectRadius(meters)
                                    radiusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                Box {
                    FilterChip(
                        selected = state.filter.sortOption != SortOption.RECOMMENDED,
                        onClick = { sortMenuExpanded = true },
                        leadingIcon = {
                            Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = {
                            Text(
                                if (state.isArabic) state.filter.sortOption.labelAr
                                else state.filter.sortOption.labelEn
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(if (state.isArabic) option.labelAr else option.labelEn) },
                                onClick = {
                                    onSelectSort(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (hasActiveFilter) {
                item {
                    TextButton(onClick = onResetFilters) {
                        Text(if (state.isArabic) "إعادة ضبط" else "Reset")
                    }
                }
            }
        }

        if (state.isLoadingPlaces) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.placesErrorMessage?.let { errorMsg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AroundMeSpacing.md, vertical = AroundMeSpacing.xs),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" ${if (state.isArabic) "إعادة" else "Retry"}")
                    }
                }
            }
        }

        if (!state.isLoadingPlaces && state.places.isEmpty() && state.placesErrorMessage == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AroundMeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.isArabic) "لا توجد أماكن مطابقة في هذه المنطقة." else "No matching places found in this area.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasActiveFilter) {
                    OutlinedButton(onClick = onResetFilters) {
                        Text(if (state.isArabic) "إعادة ضبط الفلاتر" else "Clear filters")
                    }
                }
            }
        } else {
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
                        onToggleSave = { onToggleSave(place.id) },
                        onResolvePhotoUri = onResolvePhotoUri
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
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
