import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val mapsApiKey: String = localProperties.getProperty("MAPS_ANDROID_API_KEY")
    ?: System.getenv("MAPS_ANDROID_API_KEY")
    ?: ""

val placesApiKey: String = localProperties.getProperty("PLACES_API_KEY")
    ?: System.getenv("PLACES_API_KEY")
    ?: mapsApiKey

val placesProxyBaseUrl: String = localProperties.getProperty("PLACES_PROXY_BASE_URL")
    ?: System.getenv("PLACES_PROXY_BASE_URL")
    ?: ""

android {
    namespace = "com.zahid.aroundme"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.zahid.aroundme"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["MAPS_ANDROID_API_KEY"] = mapsApiKey
        manifestPlaceholders["PLACES_API_KEY"] = placesApiKey
        manifestPlaceholders["PLACES_PROXY_BASE_URL"] = placesProxyBaseUrl
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.maps.compose)
}
