plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") // 8/15
}


android {
    namespace = "com.example.a0725"
    //compileSdk = 35
    compileSdk = 36


    defaultConfig {
        applicationId = "com.example.a0725"
        //minSdk = 26
        minSdk = 29
        //targetSdk = 36
        targetSdk = 33 //33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "CRON_BASE_URL", "\"https://aqi-weather-service-134800518696.asia-east1.run.app/\"")
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
        buildConfig = true   // ← 一定要加這行
    }
    // 8/17
    composeOptions {
        // 若你 Kotlin = 1.9.24 → 用 1.5.14；若 Kotlin = 2.0.x → 用 1.6.11+
        kotlinCompilerExtensionVersion = "1.5.14"
    }


    // ← 放這裡 10/29
    androidResources {
        // 這些副檔名不壓縮，避免 .task / .tflite 被壓縮後 Mediapipe 讀取失敗
        noCompress += listOf("tflite", "lite", "task", "bin")
    }
}


dependencies {
    implementation(libs.androidx.animation)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.litert.api)


    testImplementation(libs.junit)


    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))


    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // firebase
    implementation ("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.firebase:firebase-storage-ktx")
    implementation ("com.google.firebase:firebase-firestore-ktx")
    //implementation ("com.google.firebase:firebase-firestore-ktx:24.10.3") // 7/21
    implementation (platform("com.google.firebase:firebase-bom:33.5.1")) // 請用最新


    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.google.code.gson:gson:2.11.0")


    implementation ("com.squareup.retrofit2:retrofit:2.11.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")


    // MediaPipe Tasks (Vision)
    implementation ("com.google.mediapipe:tasks-vision:latest.release")


    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")


    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")


    implementation ("androidx.activity:activity-ktx:1.9.2")
    implementation ("androidx.activity:activity-compose:1.9.0")


    implementation ("io.coil-kt:coil-compose:2.6.0")


    implementation ("androidx.compose.material:material")
    implementation ("androidx.compose.foundation:foundation")
    implementation ("androidx.compose.material:material-icons-extended")
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    implementation ("androidx.compose.material3:material3")
    implementation ("androidx.compose.material:material-icons-extended")
    debugImplementation ("androidx.compose.ui:ui-tooling")
    implementation (platform("androidx.compose:compose-bom:2024.09.01"))




    // media3（全部版本號要一致）
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")
    implementation("androidx.media3:media3-transformer:1.5.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")   // 需要播放器就留著
    implementation("androidx.media3:media3-ui:1.5.1")          // 有用到 PlayerView 再留




    // TextureOverlay/OverlayEffect 會用到 ImmutableList
    implementation("com.google.guava:guava:33.3.1-android")


    implementation ("androidx.navigation:navigation-compose:2.7.7")


    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
    //implementation("androidx.health.connect:connect-client:1.1.0")




}

