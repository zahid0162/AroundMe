package com.zahid.aroundme.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zahid.aroundme.core.designsystem.AroundMeSpacing
import com.zahid.aroundme.core.designsystem.LocalAroundMeColors
import com.zahid.aroundme.core.geo.GeoUtils
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaceDetailsScreen(
    placeId: String,
    onBack: () -> Unit,
    viewModel: PlaceDetailsViewModel = koinViewModel(key = placeId) { parametersOf(placeId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = LocalAroundMeColors.current
    val place = state.place

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(place?.primaryCategoryLabel ?: "Place Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleSave) {
                        Icon(
                            imageVector = if (state.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Save place"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (place == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Place unavailable", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.md)
        ) {
            // Hero photo surface (Coil Place Photo in Phase 4)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = place.primaryCategoryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(AroundMeSpacing.md)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = AroundMeSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    place.rating?.let { rating ->
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = extended.ratingStar,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${GeoUtils.formatRating(rating)}${place.reviewCount?.let { " ($it)" } ?: ""}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    state.distanceLabel?.let { dist ->
                        Text("· $dist", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    place.priceLevel?.let { price ->
                        Text("· ${price.symbol}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                place.editorialSummary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
                ) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" ${if (state.isArabic) "الاتجاهات" else "Directions"}")
                    }
                    if (place.phoneNumber != null) {
                        OutlinedButton(onClick = {}) {
                            Icon(Icons.Rounded.Call, contentDescription = "Call", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (place.websiteUrl != null) {
                        OutlinedButton(onClick = {}) {
                            Icon(Icons.Rounded.Language, contentDescription = "Website", modifier = Modifier.size(18.dp))
                        }
                    }
                    OutlinedButton(onClick = {}) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = extended.subtleDivider)

                // Unavailable fields are hidden rather than fabricated
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(place.address, style = MaterialTheme.typography.bodyLarge)
                }

                place.openingHoursSummary?.let { hours ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(hours, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (place.attributes.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xxs)
                    ) {
                        place.attributes.forEach { attr ->
                            SuggestionChip(onClick = {}, label = { Text(attr) })
                        }
                    }
                }
            }
        }
    }
}
