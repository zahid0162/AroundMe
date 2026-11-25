package com.zahid.aroundme.feature.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.zahid.aroundme.core.designsystem.AroundMeSpacing
import com.zahid.aroundme.core.designsystem.LocalAroundMeColors
import com.zahid.aroundme.core.designsystem.SectionHeader
import com.zahid.aroundme.core.geo.GeoUtils
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaceDetailsScreen(
    placeId: String,
    sessionToken: String? = null,
    onBack: () -> Unit,
    viewModel: PlaceDetailsViewModel = koinViewModel(key = placeId) {
        parametersOf(placeId, sessionToken)
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extended = LocalAroundMeColors.current
    val place = state.place
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(place?.primaryCategoryLabel ?: if (state.isArabic) "تفاصيل المكان" else "Place Details")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = if (state.isArabic) "رجوع" else "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCollectionSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = if (state.isArabic) "إضافة إلى قائمة" else "Save to collection"
                        )
                    }
                    IconButton(onClick = viewModel::toggleSave) {
                        Icon(
                            imageVector = if (state.isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = if (state.isArabic) "حفظ المكان" else "Save place"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (place == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(AroundMeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.errorMessage ?: if (state.isArabic) "تعذر تحميل تفاصيل المكان" else "Place unavailable",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedButton(onClick = viewModel::retry) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" ${if (state.isArabic) "إعادة المحاولة" else "Retry"}")
                }
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
            // Hero photo gallery (Places Photos New + author attribution strip)
            if (state.resolvedPhotos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AroundMeSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
                    ) {
                        items(state.resolvedPhotos, key = { it.photo.resourceName }) { item ->
                            Column(
                                modifier = Modifier.width(300.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AsyncImage(
                                    model = item.resolvedUrl,
                                    contentDescription = place.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                )
                                item.photo.attributionName?.let { author ->
                                    Text(
                                        text = if (state.isArabic) {
                                            "تصوير: $author"
                                        } else {
                                            "Photo: $author"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
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
                    place.isOpenNow?.let { isOpen ->
                        Text(
                            text = "· ${
                                if (isOpen) {
                                    if (state.isArabic) "مفتوح الآن" else "Open now"
                                } else {
                                    if (state.isArabic) "مغلق" else "Closed"
                                }
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOpen) extended.openStatus else extended.closedStatus
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
                ) {
                    Button(onClick = viewModel::openDirections, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" ${if (state.isArabic) "الاتجاهات" else "Directions"}")
                    }
                    if (place.phoneNumber != null) {
                        OutlinedButton(onClick = viewModel::dialPhone) {
                            Icon(Icons.Rounded.Call, contentDescription = if (state.isArabic) "اتصال" else "Call", modifier = Modifier.size(18.dp))
                        }
                    }
                    if (place.websiteUrl != null) {
                        OutlinedButton(onClick = viewModel::openWebsite) {
                            Icon(Icons.Rounded.Language, contentDescription = if (state.isArabic) "الموقع" else "Website", modifier = Modifier.size(18.dp))
                        }
                    }
                    OutlinedButton(onClick = viewModel::sharePlace) {
                        Icon(Icons.Rounded.Share, contentDescription = if (state.isArabic) "مشاركة" else "Share", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = extended.subtleDivider)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(place.address, style = MaterialTheme.typography.bodyLarge)
                }

                if (place.weekdayHours.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = place.openingHoursSummary ?: if (state.isArabic) "ساعات العمل" else "Opening Hours",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        place.weekdayHours.forEach { dayLine ->
                            Text(
                                text = dayLine,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                        }
                    }
                } else {
                    place.openingHoursSummary?.let { hours ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(hours, style = MaterialTheme.typography.bodyLarge)
                        }
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

                // Personal local note (persisted in Room 3)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNoteDialog = true },
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
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isArabic) "ملاحظة شخصية" else "Personal Note",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = state.userNote.ifBlank {
                                    if (state.isArabic) "أضف ملاحظة خاصة بك حول هذا المكان…" else "Tap to add a private note about this place…"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.userNote.isBlank()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }

                if (place.reviews.isNotEmpty()) {
                    HorizontalDivider(color = extended.subtleDivider)
                    SectionHeader(
                        title = if (state.isArabic) "أحدث التقييمات" else "Recent Reviews"
                    )
                    place.reviews.forEach { review ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(AroundMeSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = review.authorName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    review.rating?.let { r ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = extended.ratingStar,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(" ${GeoUtils.formatRating(r)}", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                                review.relativePublishTime?.let { time ->
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                review.text?.takeIf { it.isNotBlank() }?.let { reviewText ->
                                    Text(
                                        text = reviewText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCollectionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var newCollectionName by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showCollectionSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AroundMeSpacing.md, vertical = AroundMeSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.sm)
            ) {
                SectionHeader(
                    title = if (state.isArabic) "حفظ في القوائم" else "Save to Collections"
                )
                state.collections.forEach { collection ->
                    val checked = state.assignedCollectionIds.contains(collection.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleCollectionAssignment(collection.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { viewModel.toggleCollectionAssignment(collection.id) }
                        )
                        Text(
                            text = collection.localizedName(state.isArabic),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                HorizontalDivider(color = extended.subtleDivider)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(if (state.isArabic) "قائمة جديدة…" else "New list name…")
                        },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newCollectionName.isNotBlank()) {
                                viewModel.createCollectionAndAssign(newCollectionName)
                                newCollectionName = ""
                            }
                        }
                    ) {
                        Text(if (state.isArabic) "إضافة" else "Create")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showNoteDialog) {
        var draftNote by remember(state.userNote) { mutableStateOf(state.userNote) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(if (state.isArabic) "ملاحظة شخصية" else "Personal Note") },
            text = {
                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = {
                        Text(if (state.isArabic) "اكتب ملاحظتك…" else "Write what to remember…")
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateNote(draftNote)
                        showNoteDialog = false
                    }
                ) {
                    Text(if (state.isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text(if (state.isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
