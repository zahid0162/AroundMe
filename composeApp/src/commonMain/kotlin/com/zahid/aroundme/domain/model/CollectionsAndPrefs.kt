package com.zahid.aroundme.domain.model

data class SavedCollection(
    val id: String,
    val name: String,
    val nameAr: String,
    val placeIds: List<String>,
    val isDefault: Boolean = false
) {
    fun localizedName(isArabic: Boolean): String = if (isArabic) nameAr else name
}

enum class SortOption(val labelEn: String, val labelAr: String) {
    RECOMMENDED("Recommended", "الموصى به"),
    NEAREST("Nearest", "الأقرب"),
    HIGHEST_RATED("Highest Rated", "الأعلى تقييماً"),
    MOST_REVIEWED("Most Reviewed", "الأكثر مراجعة")
}

data class FilterState(
    val selectedCategory: PlaceCategory? = null,
    val openNowOnly: Boolean = false,
    val minRating: Double? = null,
    val maxDistanceMeters: Double = 5000.0,
    val sortOption: SortOption = SortOption.RECOMMENDED
)

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية")
}

enum class DistanceUnit {
    METRIC,
    IMPERIAL
}

data class UserPreferences(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val defaultRadiusMeters: Double = 5000.0
)
