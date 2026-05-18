import java.util.Properties
import java.io.FileReader

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ═══════════════════════════════════════════════════════════════════
// 📄 Đọc file local.properties de lay bien moi truong
// ═══════════════════════════════════════════════════════════════════
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    FileReader(localPropertiesFile).use { reader ->
        localProperties.load(reader)
    }
}

// Lấy giá trị từ local.properties, fallback về giá trị mặc định (cho lần build đầu)
val stylemateBaseUrl: String = localProperties.getProperty("STYLEMATE_BASE_URL", "http://10.0.2.2:3000/")

android {
    namespace = "com.example.stylemate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.stylemate"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ═══════════════════════════════════════════════════════════════
        // 🏗️ BuildConfig fields — giá trị lấy từ local.properties
        // ═══════════════════════════════════════════════════════════════
        //
        // STYLEMATE_BASE_URL: URL của Backend Node.js
        //   - Emulator:        http://10.0.2.2:3000/
        //   - Thiết bị thật:   http://<IP_WiFi_máy_tính>:3000/
        //
        // ⚠️ WEATHER_API_KEY không cần ở Android nữa!
        //    Android gọi Backend proxy /api/weather/forecast
        //    Backend mới giữ key (trong .env)
        // ─────────────────────────────────────────────────────────────
        buildConfigField("String", "STYLEMATE_BASE_URL", "\"${stylemateBaseUrl}\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    implementation(libs.kotlinx.serialization.json)

    // Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
