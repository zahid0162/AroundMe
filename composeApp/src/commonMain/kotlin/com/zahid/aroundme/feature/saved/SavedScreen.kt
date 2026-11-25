package com.zahid.aroundme.feature.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zahid.aroundme.core.designsystem.AroundMeSpacing
import com.zahid.aroundme.core.designsystem.PlacePreviewCard
import com.zahid.aroundme.core.designsystem.SectionHeader
import com.zahid.aroundme.core.geo.GeoUtils
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SavedScreen(
    onOpenPlaceDetails: (String) -> Unit,
    viewModel: SavedViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = AroundMeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
    ) {
        SectionHeader(
            title = if (state.isArabic) "الأماكن المحفوظة" else "Saved Places",
            modifier = Modifier.padding(horizontal = AroundMeSpacing.md)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = AroundMeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
        ) {
            item {
                FilterChip(
                    selected = state.selectedCollectionId == null,
                    onClick = { viewModel.selectCollection(null) },
                    label = { Text(if (state.isArabic) "الكل" else "All Saved") }
                )
            }
            items(state.collections, key = { it.id }) { collection ->
                FilterChip(
                    selected = state.selectedCollectionId == collection.id,
                    onClick = { viewModel.selectCollection(collection.id) },
                    label = {
                        Text("${collection.localizedName(state.isArabic)} (${collection.placeIds.size})")
                    }
                )
            }
        }

        if (state.hydratedPlaces.isEmpty()) {
            Column(modifier = Modifier.padding(AroundMeSpacing.md)) {
                Text(
                    text = if (state.isArabic) "لا توجد أماكن محفوظة في هذه المجموعة" else "No saved places in this collection yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AroundMeSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
            ) {
                items(state.hydratedPlaces, key = { it.id }) { place ->
                    PlacePreviewCard(
                        place = place,
                        distanceMeters = GeoUtils.haversineMeters(state.referenceCenter, place.location),
                        distanceUnit = state.distanceUnit,
                        isSaved = true,
                        isSelected = false,
                        isArabic = state.isArabic,
                        onClick = { onOpenPlaceDetails(place.id) },
                        onToggleSave = { viewModel.toggleSavePlace(place.id) }
                    )
                }
            }
        }
    }
}
