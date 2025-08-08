plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // <-- MAKE SURE THIS LINE EXISTS
}

android {
    namespace = "com.kx.snapspend"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kx.snapspend"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.play.services.location)
    implementation(libs.androidx.appcompat)
    // Room for local database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version") // <-- MUST BE 'kapt', NOT 'implementation'
    implementation("androidx.room:room-ktx:$room_version")

    implementation(libs.androidx.runtime.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Retrofit for networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutines support for Google Play Services Tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Adds basic coroutine functions
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Adds .asFlow() for LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Adds .asLiveData() for Flow
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Navigation for Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Extended Material Icons
    implementation("androidx.compose.material:material-icons-extended-android:1.6.7")
}