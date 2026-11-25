package com.zahid.aroundme.core.platform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zahid.aroundme.core.designsystem.LocalAroundMeColors
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.Place

@Composable
actual fun AroundMeMapSurface(
    center: GeoPoint,
    places: List<Place>,
    selectedPlaceId: String?,
    onSelectPlace: (String) -> Unit,
    onMapCameraMoved: (GeoPoint) -> Unit,
    modifier: Modifier
) {
    val extended = LocalAroundMeColors.current
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val roadColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(extended.mapBackdrop)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 64.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
            drawLine(roadColor, Offset(0f, size.height * 0.36f), Offset(size.width, size.height * 0.28f), strokeWidth = 14f)
            drawLine(roadColor, Offset(size.width * 0.28f, 0f), Offset(size.width * 0.46f, size.height), strokeWidth = 10f)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .padding(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        places.take(8).forEachIndexed { index, place ->
            val dx = ((place.location.longitude - center.longitude) * 2800.0).coerceIn(-140.0, 140.0)
            val dy = ((center.latitude - place.location.latitude) * 2800.0).coerceIn(-140.0, 140.0)
            val isSelected = place.id == selectedPlaceId

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = dx.dp, y = dy.dp)
                    .clickable { onSelectPlace(place.id) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    shadowElevation = if (isSelected) 6.dp else 2.dp
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = place.name,
                        modifier = Modifier.padding(6.dp).size(if (isSelected) 22.dp else 18.dp)
                    )
                }
                if (isSelected || index < 2) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
