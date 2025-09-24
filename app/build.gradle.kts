plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.bitterguardmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bitterguardmobile"
        minSdk = 21
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
        viewBinding =  true
    }
    
    // Ensure compatibility with API 21
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
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
    
    // AppCompat and Layout dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    
    // Material Design Components (single implementation)
    implementation("com.google.android.material:material:1.11.0")
    
    // CardView for modern card layouts
    implementation("androidx.cardview:cardview:1.0.0")
    
    // RecyclerView for lists
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // ViewPager2 for swipeable content
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // SwipeRefreshLayout for pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // HTTP client for Supabase API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // PyTorch for ML model inference
    implementation("org.pytorch:pytorch_android:1.13.1")
    implementation("org.pytorch:pytorch_android_torchvision:1.13.1")
    
    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}