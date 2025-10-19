plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.lulu"
    compileSdk = 36 // use 34 for now — 36 isn’t released yet

    defaultConfig {
        applicationId = "com.example.lulu"
        minSdk = 24
        //noinspection ExpiredTargetSdkVersion,OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}
dependencies {
    // Core
    implementation("androidx.core:core:1.9.0")
    implementation("androidx.core:core-ktx:1.9.0")

    // Lifecycle & Activity
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.activity:activity-compose:1.7.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.5.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.6.0")

    // Room - ALL SAME VERSION
    implementation("androidx.room:room-runtime:2.8.2")     // ← Updated to 2.8.2
    implementation("androidx.room:room-ktx:2.8.2")         // ← Updated to 2.8.2
    ksp("androidx.room:room-compiler:2.8.2")
}