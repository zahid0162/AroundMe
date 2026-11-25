package com.zahid.aroundme.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zahid.aroundme.core.designsystem.AroundMeSpacing
import com.zahid.aroundme.core.designsystem.LocalAroundMeColors
import com.zahid.aroundme.core.designsystem.PlacePreviewCard
import com.zahid.aroundme.core.designsystem.SectionHeader
import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.domain.model.AutocompleteSuggestion
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onOpenPlaceDetails: (placeId: String, sessionToken: String?) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = LocalAroundMeColors.current

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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submitQuery() }),
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        if (state.isLoadingSuggestions || state.isSearching) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AroundMeSpacing.xs)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AroundMeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
        ) {
            state.errorMessage?.let { errorMsg ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                            OutlinedButton(onClick = { viewModel.submitQuery() }) {
                                Text(if (state.isArabic) "إعادة" else "Retry")
                            }
                        }
                    }
                }
            }

            if (state.query.isBlank()) {
                if (state.recentSearches.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = if (state.isArabic) "عمليات البحث الأخيرة" else "Recent Searches",
                            trailing = {
                                TextButton(onClick = viewModel::clearRecentSearches) {
                                    Text(if (state.isArabic) "مسح" else "Clear")
                                }
                            }
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
                } else {
                    item {
                        Text(
                            text = if (state.isArabic) {
                                "ابحث عن مقهى، صيدلية، مطعم أو معلم قريب."
                            } else {
                                "Search for cafes, pharmacies, restaurants, or landmarks."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (!state.hasSubmittedSearch) {
                items(state.suggestions) { suggestion ->
                    when (suggestion) {
                        is AutocompleteSuggestion.PlacePrediction -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val token = viewModel.consumeSessionTokenForPlaceSelection()
                                        onOpenPlaceDetails(suggestion.placeId, token)
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.primaryText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (suggestion.secondaryText.isNotBlank()) {
                                        Text(
                                            text = suggestion.secondaryText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                suggestion.distanceMeters?.takeIf { it > 0 }?.let { distMeters ->
                                    Text(
                                        text = GeoUtils.formatDistance(
                                            distMeters.toDouble(),
                                            state.distanceUnit,
                                            state.isArabic
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = extended.subtleDivider)
                        }
                        is AutocompleteSuggestion.QueryPrediction -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.submitQuery(suggestion.queryText) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = suggestion.queryText,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            HorizontalDivider(color = extended.subtleDivider)
                        }
                    }
                }
                if (state.suggestions.isEmpty() && !state.isLoadingSuggestions) {
                    item {
                        TextButton(onClick = { viewModel.submitQuery() }) {
                            Text(
                                if (state.isArabic) "البحث عن \"${state.query}\""
                                else "Search for \"${state.query}\""
                            )
                        }
                    }
                }
            } else if (!state.isSearching && state.results.isEmpty() && state.errorMessage == null) {
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
                        onClick = { onOpenPlaceDetails(place.id, null) },
                        onToggleSave = { viewModel.toggleSavePlace(place.id) },
                        onResolvePhotoUri = viewModel::resolvePhotoUri
                    )
                }
            }
        }
    }
}
