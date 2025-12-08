plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
}


android {
    namespace = "org.wahid.attendancehub"
    compileSdk {
        version = release(36)
    }


    compileSdk = 36

    defaultConfig {
        applicationId = "org.wahid.attendancehub"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
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

        getByName("debug") { isMinifyEnabled = false }
        getByName("release") { isMinifyEnabled = true }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
//        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }


    flavorDimensions("user_type")
    productFlavors {
        create("teacher") {
            isDefault = true
            dimension = "user_type"
            applicationId = "com.attendancehub.teacher"
            buildConfigField("boolean", "IS_TEACHER", "true")
        }

        create("student") {
            dimension = "user_type"
            applicationId = "com.attendancehub.student"
            buildConfigField("boolean", "IS_TEACHER", "false")
        }
    }
}

dependencies {
    // Core module provides all shared dependencies via 'api'
    implementation(project(":core"))
    implementation(libs.zxing.android.embedded)
    implementation(libs.kotlinx.serialization.json)

    // Testing dependencies (each module needs its own)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug dependencies (each module needs its own)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.material)
}
