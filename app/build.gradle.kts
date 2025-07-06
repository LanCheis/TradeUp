plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)

    id("kotlin-kapt") // ✅ Glide cần kapt để xử lý annotation
}

apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.example.tradeup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tradeup"
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
        viewBinding = true
    }
}

dependencies {
    // AndroidX & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2") // ✅ RecyclerView

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.3.0")                // ✅ Firebase Auth
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")      // ✅ Firestore
    implementation("com.google.android.gms:play-services-auth:21.0.0")       // ✅ Google Sign-In

    // Glide (load ảnh từ URL)
    implementation("com.github.bumptech.glide:glide:4.16.0")     // ✅ Glide
    kapt("com.github.bumptech.glide:compiler:4.16.0")            // ✅ Required for annotation

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
