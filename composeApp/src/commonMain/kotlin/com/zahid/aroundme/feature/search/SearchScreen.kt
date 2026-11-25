package com.zahid.aroundme.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zahid.aroundme.core.designsystem.AroundMeSpacing
import com.zahid.aroundme.core.designsystem.PlacePreviewCard
import com.zahid.aroundme.core.designsystem.SectionHeader
import com.zahid.aroundme.core.geo.GeoUtils
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onOpenPlaceDetails: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = AroundMeSpacing.md)
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AroundMeSpacing.md),
            placeholder = {
                Text(if (state.isArabic) "ابحث عن مكان أو فئة أو حي…" else "Search businesses, categories, areas…")
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AroundMeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
        ) {
            if (state.query.isBlank()) {
                item {
                    SectionHeader(
                        title = if (state.isArabic) "عمليات البحث الأخيرة" else "Recent Searches"
                    )
                }
                items(state.recentSearches) { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.submitQuery(recent) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = recent,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else if (state.results.isEmpty()) {
                item {
                    Text(
                        text = if (state.isArabic) "لم يتم العثور على نتائج مطابقة." else "No matching places found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.results, key = { it.id }) { place ->
                    PlacePreviewCard(
                        place = place,
                        distanceMeters = GeoUtils.haversineMeters(state.referenceCenter, place.location),
                        distanceUnit = state.distanceUnit,
                        isSaved = state.savedPlaceIds.contains(place.id),
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
