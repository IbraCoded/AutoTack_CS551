//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.compose)
//}
//
//android {
//    namespace = "com.autotrack"
//    compileSdk {
//        version = release(36) {
//            minorApiLevel = 1
//        }
//    }
//
//    defaultConfig {
//        applicationId = "com.autotrack"
//        minSdk = 24
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    buildFeatures {
//        compose = true
//    }
//
//    ksp {
//        arg("room.schemaLocation", "$projectDir/schemas")
//    }
//}
//
//
//dependencies {
//    // Compose BOM
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    implementation(libs.androidx.material.icons.extended)
//
//    // Core
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.lifecycle.viewmodel.compose)
//    implementation(libs.androidx.lifecycle.runtime.compose)
//
//    // Navigation
//    implementation(libs.androidx.navigation.compose)
//
//    // Hilt
//    implementation(libs.hilt.android)
//    implementation(libs.hilt.navigation.compose)
//    implementation(libs.hilt.work)
//    ksp(libs.hilt.compiler)
//    ksp(libs.hilt.androidx.compiler)
//
//    // Room
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.ktx)
//    ksp(libs.androidx.room.compiler)
//
//    // WorkManager
//    implementation(libs.androidx.work.runtime.ktx)
//
//    // DataStore
//    implementation(libs.androidx.datastore.preferences)
//
//    // Retrofit + OkHttp
//    implementation(libs.retrofit)
//    implementation(libs.retrofit.gson)
//    implementation(libs.okhttp.logging)
//
//    // Coil
//    implementation(libs.coil.compose)
//
//    // Adaptive layout + permissions
//    implementation(libs.androidx.material3.adaptive)
//    implementation(libs.accompanist.permissions)
//
//    // Testing
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//}
//
////dependencies {
////    implementation(libs.androidx.core.ktx)
////    implementation(libs.androidx.lifecycle.runtime.ktx)
////    implementation(libs.androidx.activity.compose)
////    implementation(platform(libs.androidx.compose.bom))
////    implementation(libs.androidx.compose.ui)
////    implementation(libs.androidx.compose.ui.graphics)
////    implementation(libs.androidx.compose.ui.tooling.preview)
////    implementation(libs.androidx.compose.material3)
////
////    // Navigation
////    val nav_version = "2.9.7"
////    implementation(libs.androidx.navigation.compose)
////    // Views/Fragments integration
////    implementation("androidx.navigation:navigation-fragment:$nav_version")
////    implementation("androidx.navigation:navigation-ui:$nav_version")
////
////    // Feature module support for Fragments
////    implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")
////
////    // Testing Navigation
////    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")
////
////    // JSON serialization library, works with the Kotlin serialization plugin
////    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
////
//////  workmanager dependencies
////    implementation(libs.androidx.work.runtime.ktx)
////
//////    retrofit dependencies
////    implementation("com.squareup.retrofit2:retrofit:2.9.0")
////    implementation("com.squareup.retrofit2:converter-scalars:3.0.0")
////    testImplementation(libs.junit)
////    androidTestImplementation(libs.androidx.junit)
////    androidTestImplementation(libs.androidx.espresso.core)
////    androidTestImplementation(platform(libs.androidx.compose.bom))
////    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
////    debugImplementation(libs.androidx.compose.ui.tooling)
////    debugImplementation(libs.androidx.compose.ui.test.manifest)
////}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.autotrack"
    compileSdk = 35

    defaultConfig {
        applicationId             = "com.autotrack"
        minSdk                    = 26
        targetSdk                 = 35
        versionCode               = 1
        versionName               = "1.0"
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

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    implementation(libs.play.services.location)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.androidx.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    implementation(libs.coil.compose)

    implementation(libs.androidx.material3.adaptive)
    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

