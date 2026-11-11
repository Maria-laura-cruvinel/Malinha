import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val mapsProperties = Properties().apply {
    val file = rootProject.file("maps.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val flightsProperties = Properties().apply {
    val file = rootProject.file("flights.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val placesProperties = Properties().apply {
    val file = rootProject.file("places.properties")
    if (file.exists()) file.inputStream().use { load(it) }
    // Local demonstration key supplied by the project owner; never copied into source control.
    if (getProperty("PLACES_API_KEY", "").isBlank()) {
        val demoFile = File(System.getProperty("user.home"), "Desktop/key.txt")
        if (demoFile.exists()) {
            val key = Regex("AIza[0-9A-Za-z_-]{35}").find(demoFile.readText())?.value
            if (key != null) setProperty("PLACES_API_KEY", key)
        }
    }
}

android {
    namespace = "com.example.malinha"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.malinha"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["PLACES_API_KEY"] = placesProperties.getProperty("PLACES_API_KEY", "")
        manifestPlaceholders["FLIGHTS_PROXY_URL"] = flightsProperties.getProperty("FLIGHTS_PROXY_URL", "")
        manifestPlaceholders["MAPS_API_KEY"] = mapsProperties.getProperty("MAPS_API_KEY", "")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-ai")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.android.libraries.places:places:5.2.0")
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
