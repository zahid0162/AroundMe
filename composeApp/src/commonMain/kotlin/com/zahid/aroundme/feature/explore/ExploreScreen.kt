package com.zahid.aroundme.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Tour
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.zahid.aroundme.domain.model.PlaceCategory
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExploreScreen(
    onOpenSearch: () -> Unit,
    onOpenPlaceDetails: (String) -> Unit,
    viewModel: ExploreViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var locationMenuExpanded by remember { mutableStateOf(false) }

    val sheetFraction by animateFloatAsState(
        targetValue = when (state.sheetDetent) {
            SheetDetent.COLLAPSED -> 0.16f
            SheetDetent.HALF_EXPANDED -> 0.44f
            SheetDetent.EXPANDED -> 0.80f
        }
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 1. Background interactive map or full List mode
        if (state.viewMode == ExploreViewMode.MAP) {
            AroundMeMapSurface(
                center = state.activeLocation.coordinates,
                places = state.places,
                selectedPlaceId = state.selectedPlaceId,
                onSelectPlace = viewModel::selectPlace,
                onMapCameraMoved = { viewModel.onMapMoved() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Top Search, Brand, Location Selector & Category Rail
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

        // 3. Content Area: Full List View OR Map + 3-State Draggable Sheet
        if (state.viewMode == ExploreViewMode.LIST) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 196.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                PlacesListContent(
                    state = state,
                    onToggleOpenNow = viewModel::toggleOpenNow,
                    onOpenPlaceDetails = onOpenPlaceDetails,
                    onToggleSave = viewModel::toggleSavePlace
                )
            }
        } else {
            // My Location FAB positioned cleanly above the sheet
            SmallFloatingActionButton(
                onClick = { viewModel.selectLocation(state.availableLocations.first()) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AroundMeSpacing.md, bottom = (maxHeight * sheetFraction) + 12.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = "Center on current location")
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
                                modifier = Modifier.padding(horizontal = AroundMeSpacing.md)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacesListContent(
    state: ExploreUiState,
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
