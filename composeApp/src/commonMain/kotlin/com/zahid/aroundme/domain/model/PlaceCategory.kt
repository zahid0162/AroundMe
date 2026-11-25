package com.zahid.aroundme.domain.model

enum class CategoryGroup(val titleEn: String, val titleAr: String) {
    FOOD_AND_DRINK("Food & Drink", "طعام ومشروبات"),
    SHOPPING("Shopping", "تسوق"),
    HEALTH("Health", "صحة"),
    TRAVEL("Travel", "سفر وسياحة"),
    SERVICES("Services", "خدمات"),
    LIFESTYLE("Lifestyle", "أسلوب حياة")
}

enum class PlaceCategory(
    val group: CategoryGroup,
    val labelEn: String,
    val labelAr: String
) {
    RESTAURANTS(CategoryGroup.FOOD_AND_DRINK, "Restaurants", "مطاعم"),
    CAFES(CategoryGroup.FOOD_AND_DRINK, "Cafes", "مقاهي"),
    BAKERIES(CategoryGroup.FOOD_AND_DRINK, "Bakeries", "مخابز"),
    FAST_FOOD(CategoryGroup.FOOD_AND_DRINK, "Fast Food", "وجبات سريعة"),
    ICE_CREAM(CategoryGroup.FOOD_AND_DRINK, "Ice Cream", "آيس كريم"),

    SHOPPING_MALLS(CategoryGroup.SHOPPING, "Malls", "مراكز تسوق"),
    SUPERMARKETS(CategoryGroup.SHOPPING, "Supermarkets", "سوبرماركت"),
    GROCERY_STORES(CategoryGroup.SHOPPING, "Groceries", "بقالات"),
    CLOTHING_STORES(CategoryGroup.SHOPPING, "Clothing", "ملابس"),

    HOSPITALS(CategoryGroup.HEALTH, "Hospitals", "مستشفيات"),
    PHARMACIES(CategoryGroup.HEALTH, "Pharmacies", "صيدليات"),
    CLINICS(CategoryGroup.HEALTH, "Clinics", "عيادات"),
    DENTISTS(CategoryGroup.HEALTH, "Dentists", "أطباء أسنان"),

    HOTELS(CategoryGroup.TRAVEL, "Hotels", "فنادق"),
    TOURIST_ATTRACTIONS(CategoryGroup.TRAVEL, "Attractions", "معالم سياحية"),
    MUSEUMS(CategoryGroup.TRAVEL, "Museums", "متاحف"),
    PARKS(CategoryGroup.TRAVEL, "Parks", "حدائق"),

    ATMS(CategoryGroup.SERVICES, "ATMs", "صراف آلي"),
    BANKS(CategoryGroup.SERVICES, "Banks", "بنوك"),
    PETROL_STATIONS(CategoryGroup.SERVICES, "Fuel", "محطات وقود"),
    CAR_WASHES(CategoryGroup.SERVICES, "Car Wash", "غسيل سيارات"),
    CAR_REPAIR(CategoryGroup.SERVICES, "Car Repair", "صيانة سيارات"),
    LAUNDRY(CategoryGroup.SERVICES, "Laundry", "مغاسل"),
    BEAUTY_SALONS(CategoryGroup.SERVICES, "Salons", "صالونات"),

    GYMS(CategoryGroup.LIFESTYLE, "Gyms", "نوادٍ رياضية"),
    SPORTS_FACILITIES(CategoryGroup.LIFESTYLE, "Sports", "مرافق رياضية"),
    MOSQUES(CategoryGroup.LIFESTYLE, "Mosques", "مساجد"),
    ENTERTAINMENT(CategoryGroup.LIFESTYLE, "Entertainment", "ترفيه");

    fun label(isArabic: Boolean): String = if (isArabic) labelAr else labelEn

    companion object {
        val quickExploreDefaults: List<PlaceCategory> = listOf(
            RESTAURANTS,
            CAFES,
            PHARMACIES,
            PARKS,
            TOURIST_ATTRACTIONS,
            SUPERMARKETS,
            PETROL_STATIONS,
            MOSQUES
        )
    }
}
