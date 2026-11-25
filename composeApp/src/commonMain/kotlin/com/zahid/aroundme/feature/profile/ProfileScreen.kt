package com.zahid.aroundme.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zahid.aroundme.core.designsystem.AroundMeSpacing
import com.zahid.aroundme.core.designsystem.LocalAroundMeColors
import com.zahid.aroundme.core.designsystem.SectionHeader
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.ThemePreference
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isArabic = prefs.language == AppLanguage.ARABIC
    val extended = LocalAroundMeColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(AroundMeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xxs)) {
            Text(
                text = "AroundMe",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isArabic) "اكتشف ما حولك" else "Discover What's Around You",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = extended.subtleDivider)

        Column(verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)) {
            SectionHeader(title = if (isArabic) "المظهر" else "Appearance")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
            ) {
                ThemePreference.entries.forEach { theme ->
                    FilterChip(
                        selected = prefs.themePreference == theme,
                        onClick = { viewModel.setTheme(theme) },
                        label = {
                            Text(
                                when (theme) {
                                    ThemePreference.SYSTEM -> if (isArabic) "النظام" else "System"
                                    ThemePreference.LIGHT -> if (isArabic) "فاتح" else "Light"
                                    ThemePreference.DARK -> if (isArabic) "داكن" else "Dark"
                                }
                            )
                        }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)) {
            SectionHeader(title = if (isArabic) "اللغة" else "Language")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
            ) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = prefs.language == lang,
                        onClick = { viewModel.setLanguage(lang) },
                        label = { Text(lang.displayName) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)) {
            SectionHeader(title = if (isArabic) "وحدة المسافة" else "Distance Units")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AroundMeSpacing.xs)
            ) {
                FilterChip(
                    selected = prefs.distanceUnit == DistanceUnit.METRIC,
                    onClick = { viewModel.setDistanceUnit(DistanceUnit.METRIC) },
                    label = { Text(if (isArabic) "متري (م / كم)" else "Metric (m / km)") }
                )
                FilterChip(
                    selected = prefs.distanceUnit == DistanceUnit.IMPERIAL,
                    onClick = { viewModel.setDistanceUnit(DistanceUnit.IMPERIAL) },
                    label = { Text(if (isArabic) "إمبراطوري (ميل)" else "Imperial (ft / mi)") }
                )
            }
        }
    }
}
