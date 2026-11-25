package com.zahid.aroundme.feature.saved

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SavedScreen(
    onOpenPlaceDetails: (String) -> Unit,
    viewModel: SavedViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = AroundMeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
    ) {
        SectionHeader(
            title = if (state.isArabic) "الأماكن المحفوظة" else "Saved Places",
            trailing = {
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" ${if (state.isArabic) "قائمة جديدة" else "New List"}")
                }
            },
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
                    },
                    trailingIcon = if (!collection.isDefault) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = if (state.isArabic) "حذف القائمة" else "Delete collection",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.deleteCollection(collection.id) }
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (!state.isLoading && state.items.isEmpty()) {
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
                items(state.items, key = { it.placeId }) { item ->
                    val place = item.place
                    if (place != null) {
                        PlacePreviewCard(
                            place = place,
                            distanceMeters = GeoUtils.haversineMeters(state.referenceCenter, place.location),
                            distanceUnit = state.distanceUnit,
                            isSaved = true,
                            isSelected = false,
                            isArabic = state.isArabic,
                            userNote = item.userNote,
                            onClick = { onOpenPlaceDetails(place.id) },
                            onToggleSave = { viewModel.toggleSavePlace(place.id) },
                            onResolvePhotoUri = viewModel::resolvePhotoUri
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPlaceDetails(item.placeId) },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(AroundMeSpacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (state.isArabic) "مكان محفوظ" else "Saved Place",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val fallbackDist = item.fallbackCoordinates?.let { coords ->
                                        GeoUtils.formatDistance(
                                            GeoUtils.haversineMeters(state.referenceCenter, coords),
                                            state.distanceUnit,
                                            state.isArabic
                                        )
                                    }
                                    Text(
                                        text = fallbackDist ?: if (state.isArabic) {
                                            "تفاصيل المكان متوفرة عند الاتصال بالإنترنت"
                                        } else {
                                            "Details refresh when online"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.userNote.isNotBlank()) {
                                        Text(
                                            text = item.userNote,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.toggleSavePlace(item.placeId) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = if (state.isArabic) "إزالة الحفظ" else "Remove bookmark"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var listName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (state.isArabic) "إنشاء قائمة جديدة" else "Create Collection") },
            text = {
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    singleLine = true,
                    placeholder = {
                        Text(if (state.isArabic) "مثال: مقاهي هادئة" else "e.g., Quiet Work Cafes")
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (listName.isNotBlank()) {
                            viewModel.createCollection(listName)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text(if (state.isArabic) "إنشاء" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(if (state.isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
