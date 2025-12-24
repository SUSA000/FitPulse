plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.wellnesstrack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wellnesstrack"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = false
    }
}

dependencies {
    val navVersion = "2.7.7"
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // WorkManager for reminders
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // MPAndroidChart for Advanced feature
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
